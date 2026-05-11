package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminCouponIssueDTO;
import com.gj.mall.admin.dto.AdminCouponQueryDTO;
import com.gj.mall.admin.dto.AdminCouponSaveDTO;
import com.gj.mall.admin.dto.AdminCouponUserQueryDTO;
import com.gj.mall.admin.service.AdminCouponService;
import com.gj.mall.admin.vo.AdminCouponUserVO;
import com.gj.mall.admin.vo.AdminCouponVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.marketing.entity.SmsCoupon;
import com.gj.mall.marketing.entity.SmsCouponUser;
import com.gj.mall.marketing.mapper.SmsCouponMapper;
import com.gj.mall.marketing.mapper.SmsCouponUserMapper;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl implements AdminCouponService {

    private final SmsCouponMapper couponMapper;
    private final SmsCouponUserMapper couponUserMapper;
    private final UmsUserMapper userMapper;

    @Override
    public PageResult<AdminCouponVO> page(AdminCouponQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        Page<SmsCoupon> page = new Page<>(pageNum, pageSize);
        IPage<SmsCoupon> result = couponMapper.selectPage(page,
                Wrappers.<SmsCoupon>lambdaQuery()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SmsCoupon::getName, query.getKeyword())
                        .eq(query.getType() != null, SmsCoupon::getType, query.getType())
                        .eq(query.getStatus() != null, SmsCoupon::getStatus, query.getStatus())
                        .orderByDesc(SmsCoupon::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(AdminCouponVO::from).collect(Collectors.toList()));
    }

    @Override
    public AdminCouponVO detail(Long id) {
        return AdminCouponVO.from(getByIdOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminCouponSaveDTO dto) {
        validateSaveDTO(dto);
        SmsCoupon coupon = new SmsCoupon();
        fillCoupon(coupon, dto);
        coupon.setId(null);
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AdminCouponSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少优惠券ID");
        }
        SmsCoupon exists = getByIdOrThrow(dto.getId());
        validateSaveDTO(dto);
        int receivedCount = defaultInt(exists.getReceivedCount());
        if (dto.getTotalCount() < receivedCount) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "发行总量不能小于已领取数量");
        }
        SmsCoupon coupon = new SmsCoupon();
        coupon.setId(dto.getId());
        fillCoupon(coupon, dto);
        if (dto.getStatus() != null) {
            coupon.setStatus(dto.getStatus());
        }
        couponMapper.updateById(coupon);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        getByIdOrThrow(id);
        SmsCoupon coupon = new SmsCoupon();
        coupon.setId(id);
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SmsCoupon coupon = getByIdOrThrow(id);
        Long heldCount = couponUserMapper.selectCount(Wrappers.<SmsCouponUser>lambdaQuery()
                .eq(SmsCouponUser::getCouponId, id));
        if ((heldCount != null && heldCount > 0) || defaultInt(coupon.getReceivedCount()) > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "已有领取记录的优惠券不能删除，可改为下架");
        }
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer issue(Long couponId, AdminCouponIssueDTO dto) {
        SmsCoupon coupon = getByIdOrThrow(couponId);
        if (!Integer.valueOf(1).equals(coupon.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "优惠券未开启，不能发放");
        }
        if (coupon.getEndTime() != null && LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new BizException(ResultCode.COUPON_EXPIRED);
        }
        List<Long> userIds = normalizeUserIds(dto.getUserIds());
        Map<Long, UmsUser> userMap = loadUserMap(userIds);
        List<Long> missingIds = userIds.stream()
                .filter(id -> !userMap.containsKey(id))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(missingIds)) {
            throw new BizException(ResultCode.USER_NOT_FOUND, "会员不存在：" + missingIds);
        }

        List<SmsCouponUser> existed = couponUserMapper.selectList(Wrappers.<SmsCouponUser>lambdaQuery()
                .eq(SmsCouponUser::getCouponId, couponId)
                .in(SmsCouponUser::getUserId, userIds));
        Set<Long> existedUserIds = existed.stream().map(SmsCouponUser::getUserId).collect(Collectors.toSet());
        List<Long> issueUserIds = userIds.stream()
                .filter(id -> !existedUserIds.contains(id))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(issueUserIds)) {
            return 0;
        }

        int maxReceivedBeforeIssue = defaultInt(coupon.getTotalCount()) - issueUserIds.size();
        int rows = couponMapper.update(null, Wrappers.<SmsCoupon>lambdaUpdate()
                .setSql("received_count = received_count + " + issueUserIds.size())
                .eq(SmsCoupon::getId, couponId)
                .le(SmsCoupon::getReceivedCount, maxReceivedBeforeIssue));
        if (rows == 0) {
            throw new BizException(ResultCode.COUPON_STOCK_OUT);
        }

        for (Long userId : issueUserIds) {
            SmsCouponUser couponUser = new SmsCouponUser();
            couponUser.setCouponId(couponId);
            couponUser.setUserId(userId);
            couponUser.setStatus(0);
            couponUserMapper.insert(couponUser);
        }
        return issueUserIds.size();
    }

    @Override
    public PageResult<AdminCouponUserVO> users(Long couponId, AdminCouponUserQueryDTO query) {
        getByIdOrThrow(couponId);
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        List<Long> matchedUserIds = null;
        if (StrUtil.isNotBlank(query.getUserKeyword())) {
            matchedUserIds = searchUserIds(query.getUserKeyword());
            if (CollUtil.isEmpty(matchedUserIds)) {
                return PageResult.empty(pageNum, pageSize);
            }
        }

        IPage<SmsCouponUser> result = couponUserMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<SmsCouponUser>lambdaQuery()
                        .eq(SmsCouponUser::getCouponId, couponId)
                        .eq(query.getStatus() != null, SmsCouponUser::getStatus, query.getStatus())
                        .in(CollUtil.isNotEmpty(matchedUserIds), SmsCouponUser::getUserId, matchedUserIds)
                        .orderByDesc(SmsCouponUser::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        List<Long> userIds = result.getRecords().stream()
                .map(SmsCouponUser::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UmsUser> userMap = loadUserMap(userIds);
        List<AdminCouponUserVO> list = result.getRecords().stream()
                .map(item -> AdminCouponUserVO.from(item, userMap.get(item.getUserId())))
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    private SmsCoupon getByIdOrThrow(Long id) {
        SmsCoupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BizException(ResultCode.COUPON_NOT_FOUND);
        }
        return coupon;
    }

    private void fillCoupon(SmsCoupon coupon, AdminCouponSaveDTO dto) {
        coupon.setName(dto.getName().trim());
        coupon.setType(dto.getType());
        if (Integer.valueOf(2).equals(dto.getType())) {
            coupon.setDiscountAmount(null);
            coupon.setDiscountRate(dto.getDiscountRate());
        } else {
            coupon.setDiscountAmount(dto.getDiscountAmount());
            coupon.setDiscountRate(null);
        }
        coupon.setMinAmount(dto.getMinAmount() == null ? BigDecimal.ZERO : dto.getMinAmount());
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setStartTime(dto.getStartTime());
        coupon.setEndTime(dto.getEndTime());
    }

    private void validateSaveDTO(AdminCouponSaveDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING);
        }
        validateType(dto.getType());
        validateStatus(dto.getStatus());
        if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BizException(ResultCode.PARAM_ERROR, "优惠券有效期设置不正确");
        }
        if (dto.getTotalCount() == null || dto.getTotalCount() <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "发行总量必须大于0");
        }
        if (dto.getMinAmount() != null && dto.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "使用门槛不能小于0");
        }
        if (Integer.valueOf(2).equals(dto.getType())) {
            if (dto.getDiscountRate() == null
                    || dto.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                    || dto.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "折扣率需大于0且小于1");
            }
            return;
        }
        if (dto.getDiscountAmount() == null || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "优惠金额必须大于0");
        }
    }

    private void validateType(Integer type) {
        if (!Integer.valueOf(1).equals(type)
                && !Integer.valueOf(2).equals(type)
                && !Integer.valueOf(3).equals(type)) {
            throw new BizException(ResultCode.PARAM_ERROR, "优惠券类型非法");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            throw new BizException(ResultCode.PARAM_MISSING, "请选择发券会员");
        }
        List<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            throw new BizException(ResultCode.PARAM_MISSING, "请选择发券会员");
        }
        return ids;
    }

    private List<Long> searchUserIds(String keyword) {
        return userMapper.selectList(Wrappers.<UmsUser>lambdaQuery()
                        .and(w -> w.like(UmsUser::getUsername, keyword)
                                .or()
                                .like(UmsUser::getNickname, keyword)
                                .or()
                                .like(UmsUser::getPhone, keyword))
                        .last("LIMIT 500"))
                .stream()
                .map(UmsUser::getId)
                .collect(Collectors.toList());
    }

    private Map<Long, UmsUser> loadUserMap(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(Wrappers.<UmsUser>lambdaQuery().in(UmsUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UmsUser::getId, item -> item, (a, b) -> a));
    }
}
