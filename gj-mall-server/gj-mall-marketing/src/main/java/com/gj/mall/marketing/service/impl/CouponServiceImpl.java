package com.gj.mall.marketing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.marketing.dto.CouponCreateDTO;
import com.gj.mall.marketing.entity.SmsCoupon;
import com.gj.mall.marketing.entity.SmsCouponUser;
import com.gj.mall.marketing.mapper.SmsCouponMapper;
import com.gj.mall.marketing.mapper.SmsCouponUserMapper;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.vo.CouponCheckResult;
import com.gj.mall.marketing.vo.CouponCenterVO;
import com.gj.mall.marketing.vo.CouponVO;
import com.gj.mall.marketing.vo.MyCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final String LOCK_PREFIX = "mall:coupon:lock:";

    private final SmsCouponMapper couponMapper;
    private final SmsCouponUserMapper couponUserMapper;
    private final RedissonClient redissonClient;

    // ============================== C 端 ==============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long couponId) {
        SmsCoupon coupon = getByIdOrThrow(couponId);
        validateCouponActive(coupon);

        // 分布式锁：单张券粒度，防超发
        RLock lock = redissonClient.getLock(LOCK_PREFIX + couponId);
        try {
            boolean got = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!got) throw new BizException(ResultCode.OPERATION_FORBIDDEN, "操作频繁，请稍后重试");

            // 判断是否重领
            long already = couponUserMapper.selectCount(
                    Wrappers.<SmsCouponUser>lambdaQuery()
                            .eq(SmsCouponUser::getCouponId, couponId)
                            .eq(SmsCouponUser::getUserId, userId));
            if (already > 0) throw new BizException(ResultCode.COUPON_ALREADY_RECEIVED);

            // 库存限量（原子 update）
            int rows = couponMapper.update(null, Wrappers.<SmsCoupon>lambdaUpdate()
                    .setSql("received_count = received_count + 1")
                    .eq(SmsCoupon::getId, couponId)
                    .lt(SmsCoupon::getReceivedCount, coupon.getTotalCount()));
            if (rows == 0) throw new BizException(ResultCode.COUPON_STOCK_OUT);

            SmsCouponUser cu = new SmsCouponUser();
            cu.setCouponId(couponId);
            cu.setUserId(userId);
            cu.setStatus(0);
            couponUserMapper.insert(cu);
            log.info("[coupon] userId={} received couponId={}", userId, couponId);
        } catch (BizException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.SYSTEM_ERROR, "领券中断");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override
    public List<MyCouponVO> myList(Long userId, Integer status) {
        List<SmsCouponUser> list = couponUserMapper.selectList(
                Wrappers.<SmsCouponUser>lambdaQuery()
                        .eq(SmsCouponUser::getUserId, userId)
                        .eq(status != null, SmsCouponUser::getStatus, status)
                        .orderByDesc(SmsCouponUser::getId));
        return list.stream().map(cu -> {
            SmsCoupon c = couponMapper.selectById(cu.getCouponId());
            return toMyCouponVO(cu, c);
        }).collect(Collectors.toList());
    }

    @Override
    public List<MyCouponVO> available(Long userId, BigDecimal orderAmount) {
        return checkoutList(userId, orderAmount).stream()
                .filter(item -> Boolean.TRUE.equals(item.getUsable()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MyCouponVO> checkoutList(Long userId, BigDecimal orderAmount) {
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount.max(BigDecimal.ZERO);
        List<SmsCouponUser> list = couponUserMapper.selectList(
                Wrappers.<SmsCouponUser>lambdaQuery()
                        .eq(SmsCouponUser::getUserId, userId)
                        .orderByDesc(SmsCouponUser::getStatus)
                        .orderByDesc(SmsCouponUser::getId));
        LocalDateTime now = LocalDateTime.now();
        return list.stream()
                .map(cu -> {
                    SmsCoupon c = couponMapper.selectById(cu.getCouponId());
                    return toCheckoutCouponVO(cu, c, amount, now);
                })
                .sorted((a, b) -> {
                    int usableCompare = Boolean.compare(Boolean.TRUE.equals(b.getUsable()), Boolean.TRUE.equals(a.getUsable()));
                    if (usableCompare != 0) return usableCompare;
                    int discountCompare = safeMoney(b.getDiscountEstimate()).compareTo(safeMoney(a.getDiscountEstimate()));
                    if (discountCompare != 0) return discountCompare;
                    return safeMoney(a.getAmountGap()).compareTo(safeMoney(b.getAmountGap()));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CouponCenterVO> centerList() {
        LocalDateTime now = LocalDateTime.now();
        List<SmsCoupon> list = couponMapper.selectList(
                Wrappers.<SmsCoupon>lambdaQuery()
                        .eq(SmsCoupon::getStatus, 1)
                        .le(SmsCoupon::getStartTime, now)
                        .ge(SmsCoupon::getEndTime, now)
                        .apply("received_count < total_count")
                        .orderByAsc(SmsCoupon::getMinAmount)
                        .orderByDesc(SmsCoupon::getDiscountAmount)
                        .orderByDesc(SmsCoupon::getId));
        return list.stream().map(this::toCenterVO).collect(Collectors.toList());
    }

    @Override
    public CouponCheckResult check(Long userId, Long couponId, BigDecimal orderAmount) {
        CouponCheckResult result = new CouponCheckResult();
        if (couponId == null) {
            result.setDiscountAmount(BigDecimal.ZERO);
            return result;
        }
        SmsCouponUser cu = couponUserMapper.selectOne(
                Wrappers.<SmsCouponUser>lambdaQuery()
                        .eq(SmsCouponUser::getCouponId, couponId)
                        .eq(SmsCouponUser::getUserId, userId)
                        .eq(SmsCouponUser::getStatus, 0));
        if (cu == null) throw new BizException(ResultCode.COUPON_NOT_FOUND, "未持有该优惠券或已使用");
        SmsCoupon c = getByIdOrThrow(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (!Integer.valueOf(1).equals(c.getStatus())) throw new BizException(ResultCode.COUPON_NOT_FOUND, "优惠券未启用");
        if (c.getStartTime() != null && now.isBefore(c.getStartTime())) throw new BizException(ResultCode.COUPON_EXPIRED, "优惠券未到可用时间");
        if (c.getEndTime() != null && now.isAfter(c.getEndTime())) throw new BizException(ResultCode.COUPON_EXPIRED);
        if (c.getMinAmount() != null && orderAmount.compareTo(c.getMinAmount()) < 0) {
            throw new BizException(ResultCode.COUPON_NOT_MATCH,
                    "需满 " + c.getMinAmount() + " 元才可使用");
        }
        BigDecimal discount = calcDiscount(c, orderAmount);
        result.setDiscountAmount(discount);
        result.setCouponUserId(cu.getId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(Long couponUserId, Long orderId) {
        if (couponUserId == null) return;
        SmsCouponUser cu = couponUserMapper.selectById(couponUserId);
        if (cu == null || !Integer.valueOf(0).equals(cu.getStatus())) return;
        SmsCouponUser upd = new SmsCouponUser();
        upd.setId(couponUserId);
        upd.setStatus(1);
        upd.setUsedAt(LocalDateTime.now());
        upd.setOrderId(orderId);
        couponUserMapper.updateById(upd);
        // 更新 used_count（乐观，不严格）
        couponMapper.update(null, Wrappers.<SmsCoupon>lambdaUpdate()
                .setSql("used_count = used_count + 1")
                .eq(SmsCoupon::getId, cu.getCouponId()));
        log.info("[coupon] used couponUserId={} orderId={}", couponUserId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long couponUserId) {
        if (couponUserId == null) return;
        SmsCouponUser cu = couponUserMapper.selectById(couponUserId);
        if (cu == null || !Integer.valueOf(1).equals(cu.getStatus())) return;
        SmsCouponUser upd = new SmsCouponUser();
        upd.setId(couponUserId);
        upd.setStatus(0);
        upd.setUsedAt(null);
        upd.setOrderId(null);
        couponUserMapper.updateById(upd);
        couponMapper.update(null, Wrappers.<SmsCoupon>lambdaUpdate()
                .setSql("used_count = GREATEST(used_count - 1, 0)")
                .eq(SmsCoupon::getId, cu.getCouponId()));
        log.info("[coupon] released couponUserId={}", couponUserId);
    }

    // ============================== Admin 端 ==============================

    @Override
    public Page<CouponVO> adminPage(long pageNum, long pageSize, Integer status) {
        Page<SmsCoupon> p = couponMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<SmsCoupon>lambdaQuery()
                        .eq(status != null, SmsCoupon::getStatus, status)
                        .orderByDesc(SmsCoupon::getId));
        Page<CouponVO> res = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        res.setRecords(p.getRecords().stream().map(this::toCouponVO).collect(Collectors.toList()));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(CouponCreateDTO dto) {
        SmsCoupon coupon = new SmsCoupon();
        BeanUtil.copyProperties(dto, coupon);
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(0);  // 草稿，需手动上架
        if (coupon.getMinAmount() == null) coupon.setMinAmount(BigDecimal.ZERO);
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    @Override
    public void adminSetStatus(Long id, Integer status) {
        SmsCoupon upd = new SmsCoupon();
        upd.setId(id);
        upd.setStatus(status);
        couponMapper.updateById(upd);
    }

    @Override
    public SmsCoupon getByIdOrThrow(Long id) {
        SmsCoupon c = couponMapper.selectById(id);
        if (c == null) throw new BizException(ResultCode.COUPON_NOT_FOUND);
        return c;
    }

    // ============================== 私有 ==============================

    private void validateCouponActive(SmsCoupon c) {
        if (!Integer.valueOf(1).equals(c.getStatus())) {
            throw new BizException(ResultCode.COUPON_NOT_FOUND, "优惠券未开放领取");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(c.getStartTime()) || now.isAfter(c.getEndTime())) {
            throw new BizException(ResultCode.COUPON_EXPIRED, "优惠券不在有效期内");
        }
    }

    private BigDecimal calcDiscount(SmsCoupon c, BigDecimal amount) {
        if (c.getType() == 1) {
            // 满减
            BigDecimal d = c.getDiscountAmount() == null ? BigDecimal.ZERO : c.getDiscountAmount();
            return d.compareTo(amount) > 0 ? amount : d;
        }
        if (c.getType() == 2) {
            // 折扣：amount * (1 - rate)
            BigDecimal rate = c.getDiscountRate() == null ? BigDecimal.ONE : c.getDiscountRate();
            return amount.subtract(amount.multiply(rate)).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        // 新人券按 discountAmount 处理
        BigDecimal d = c.getDiscountAmount() == null ? BigDecimal.ZERO : c.getDiscountAmount();
        return d.compareTo(amount) > 0 ? amount : d;
    }

    private CouponVO toCouponVO(SmsCoupon c) {
        CouponVO vo = new CouponVO();
        BeanUtil.copyProperties(c, vo);
        vo.setTypeDesc(typeDesc(c.getType()));
        vo.setRemainCount(c.getTotalCount() - c.getReceivedCount());
        return vo;
    }

    private CouponCenterVO toCenterVO(SmsCoupon c) {
        CouponCenterVO vo = new CouponCenterVO();
        BeanUtil.copyProperties(c, vo);
        vo.setTypeDesc(typeDesc(c.getType()));
        vo.setRemainCount(c.getTotalCount() - c.getReceivedCount());
        return vo;
    }

    private MyCouponVO toMyCouponVO(SmsCouponUser cu, SmsCoupon c) {
        MyCouponVO vo = new MyCouponVO();
        vo.setId(cu.getId());
        vo.setCouponId(cu.getCouponId());
        vo.setStatus(cu.getStatus());
        vo.setStatusDesc(couponUserStatusDesc(cu.getStatus()));
        vo.setCreateTime(cu.getCreateTime());
        if (c != null) {
            vo.setName(c.getName());
            vo.setType(c.getType());
            vo.setTypeDesc(typeDesc(c.getType()));
            vo.setDiscountAmount(c.getDiscountAmount());
            vo.setDiscountRate(c.getDiscountRate());
            vo.setMinAmount(c.getMinAmount());
            vo.setStartTime(c.getStartTime());
            vo.setEndTime(c.getEndTime());
        }
        return vo;
    }

    private MyCouponVO toCheckoutCouponVO(SmsCouponUser cu, SmsCoupon c, BigDecimal orderAmount, LocalDateTime now) {
        MyCouponVO vo = toMyCouponVO(cu, c);
        vo.setUsable(false);
        vo.setDiscountEstimate(BigDecimal.ZERO);
        vo.setAmountGap(BigDecimal.ZERO);
        if (c == null) {
            vo.setUnavailableReason("优惠券不存在");
            return vo;
        }
        if (!Integer.valueOf(0).equals(cu.getStatus())) {
            vo.setUnavailableReason(couponUserStatusDesc(cu.getStatus()));
            return vo;
        }
        if (!Integer.valueOf(1).equals(c.getStatus())) {
            vo.setUnavailableReason("优惠券未启用");
            return vo;
        }
        if (c.getStartTime() != null && now.isBefore(c.getStartTime())) {
            vo.setUnavailableReason("未到可用时间");
            return vo;
        }
        if (c.getEndTime() != null && now.isAfter(c.getEndTime())) {
            vo.setUnavailableReason("优惠券已过期");
            return vo;
        }
        BigDecimal minAmount = c.getMinAmount() == null ? BigDecimal.ZERO : c.getMinAmount();
        if (orderAmount.compareTo(minAmount) < 0) {
            BigDecimal gap = minAmount.subtract(orderAmount).setScale(2, RoundingMode.HALF_UP);
            vo.setAmountGap(gap);
            vo.setUnavailableReason("还差 " + gap.stripTrailingZeros().toPlainString() + " 元可用");
            return vo;
        }
        vo.setUsable(true);
        vo.setUnavailableReason(null);
        vo.setDiscountEstimate(calcDiscount(c, orderAmount));
        return vo;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String typeDesc(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "满减券";
            case 2: return "折扣券";
            case 3: return "新人券";
            default: return "未知";
        }
    }

    private String couponUserStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "未使用";
            case 1: return "已使用";
            case 2: return "已过期";
            default: return "未知";
        }
    }
}
