package com.gj.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.marketing.entity.SmsCoupon;
import com.gj.mall.marketing.entity.SmsCouponUser;
import com.gj.mall.marketing.mapper.SmsCouponMapper;
import com.gj.mall.marketing.mapper.SmsCouponUserMapper;
import com.gj.mall.marketing.vo.CouponCheckResult;
import com.gj.mall.marketing.vo.CouponPlanVO;
import com.gj.mall.marketing.vo.MyCouponVO;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponServiceImplTest {

    @Test
    void checkoutPlanPicksHighestUsableDiscountAndSuggestsNearestGap() {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        CouponServiceImpl service = service(couponMapper, couponUserMapper);
        SmsCouponUser lowDiscount = couponUser(11L, 101L, 0);
        SmsCouponUser highDiscount = couponUser(12L, 102L, 0);
        SmsCouponUser nearGap = couponUser(13L, 103L, 0);
        when(couponUserMapper.selectList(any())).thenReturn(Arrays.asList(lowDiscount, highDiscount, nearGap));
        when(couponMapper.selectById(101L)).thenReturn(coupon(101L, "满100减10", new BigDecimal("100"), new BigDecimal("10")));
        when(couponMapper.selectById(102L)).thenReturn(coupon(102L, "满120减30", new BigDecimal("120"), new BigDecimal("30")));
        when(couponMapper.selectById(103L)).thenReturn(coupon(103L, "满160减50", new BigDecimal("160"), new BigDecimal("50")));

        CouponPlanVO plan = service.checkoutPlan(1L, new BigDecimal("130"));

        assertThat(plan.getBestCoupon().getCouponId()).isEqualTo(102L);
        assertThat(plan.getDiscountAmount()).isEqualByComparingTo("30.00");
        assertThat(plan.getPayableAmount()).isEqualByComparingTo("100.00");
        assertThat(plan.getNextCoupon().getCouponId()).isEqualTo(103L);
        assertThat(plan.getNextAmountGap()).isEqualByComparingTo("30.00");
    }

    @Test
    void receiveAllowsCouponWithoutTimeLimitAndPreventsDuplicateReceive() throws Exception {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        CouponServiceImpl service = new CouponServiceImpl(couponMapper, couponUserMapper, redissonClient);
        when(redissonClient.getLock("mall:coupon:lock:100")).thenReturn(lock);
        when(lock.tryLock(5, 10, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        SmsCoupon coupon = coupon(100L, "无期限券", BigDecimal.ZERO, new BigDecimal("5"));
        coupon.setStartTime(null);
        coupon.setEndTime(null);
        when(couponMapper.selectById(100L)).thenReturn(coupon);
        when(couponUserMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.update(eq(null), any(Wrapper.class))).thenReturn(1);

        service.receive(1L, 100L);

        verify(couponUserMapper).insert(any(SmsCouponUser.class));
        verify(lock).unlock();
    }

    @Test
    void receiveRejectsDuplicateBeforeDeductingStock() throws Exception {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        CouponServiceImpl service = new CouponServiceImpl(couponMapper, couponUserMapper, redissonClient);
        when(redissonClient.getLock("mall:coupon:lock:100")).thenReturn(lock);
        when(lock.tryLock(5, 10, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(couponMapper.selectById(100L)).thenReturn(coupon(100L, "满减券", BigDecimal.ZERO, new BigDecimal("5")));
        when(couponUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.receive(1L, 100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已领取过该优惠券");

        verify(couponMapper, never()).update(eq(null), any(Wrapper.class));
        verify(couponUserMapper, never()).insert(any(SmsCouponUser.class));
    }

    @Test
    void checkRejectsNullOrderAmountBeforeComparingThreshold() {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        CouponServiceImpl service = service(couponMapper, couponUserMapper);
        SmsCouponUser couponUser = couponUser(11L, 100L, 0);
        when(couponUserMapper.selectOne(any())).thenReturn(couponUser);
        when(couponMapper.selectById(100L)).thenReturn(coupon(100L, "满100减10", new BigDecimal("100"), new BigDecimal("10")));

        assertThatThrownBy(() -> service.check(1L, 100L, null))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getCode(), Throwable::getMessage)
                .containsExactly(ResultCode.COUPON_NOT_MATCH.getCode(), "需满 100 元才可使用");
    }

    @Test
    void checkCalculatesDiscountAndReturnsCouponUserId() {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        CouponServiceImpl service = service(couponMapper, couponUserMapper);
        SmsCouponUser couponUser = couponUser(11L, 100L, 0);
        when(couponUserMapper.selectOne(any())).thenReturn(couponUser);
        when(couponMapper.selectById(100L)).thenReturn(coupon(100L, "满100减10", new BigDecimal("100"), new BigDecimal("10")));

        CouponCheckResult result = service.check(1L, 100L, new BigDecimal("120"));

        assertThat(result.getCouponUserId()).isEqualTo(11L);
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("10");
    }

    @Test
    void useOnlyMarksUnusedCouponAndIncrementsUsedCount() {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        CouponServiceImpl service = service(couponMapper, couponUserMapper);
        when(couponUserMapper.selectById(11L)).thenReturn(couponUser(11L, 100L, 0));

        service.use(11L, 900L);

        verify(couponUserMapper).updateById(any(SmsCouponUser.class));
        verify(couponMapper).update(eq(null), any(Wrapper.class));
    }

    @Test
    void releaseOnlyReopensUsedCouponAndDoesNotLowerUsedCountBelowZero() {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        CouponServiceImpl service = service(couponMapper, couponUserMapper);
        when(couponUserMapper.selectById(11L)).thenReturn(couponUser(11L, 100L, 1));

        service.release(11L);

        verify(couponUserMapper).updateById(any(SmsCouponUser.class));
        verify(couponMapper).update(eq(null), any(Wrapper.class));
    }

    @Test
    void myListKeepsDeletedCouponRecordsVisibleAsUnavailableData() {
        SmsCouponMapper couponMapper = mock(SmsCouponMapper.class);
        SmsCouponUserMapper couponUserMapper = mock(SmsCouponUserMapper.class);
        CouponServiceImpl service = service(couponMapper, couponUserMapper);
        when(couponUserMapper.selectList(any())).thenReturn(Collections.singletonList(couponUser(11L, 100L, 0)));
        when(couponMapper.selectById(100L)).thenReturn(null);

        assertThat(service.myList(1L, null))
                .hasSize(1)
                .first()
                .extracting(MyCouponVO::getCouponId, MyCouponVO::getStatusDesc)
                .containsExactly(100L, "未使用");
    }

    private CouponServiceImpl service(SmsCouponMapper couponMapper, SmsCouponUserMapper couponUserMapper) {
        return new CouponServiceImpl(couponMapper, couponUserMapper, mock(RedissonClient.class));
    }

    private SmsCoupon coupon(Long id, String name, BigDecimal minAmount, BigDecimal discountAmount) {
        SmsCoupon coupon = new SmsCoupon();
        coupon.setId(id);
        coupon.setName(name);
        coupon.setType(1);
        coupon.setDiscountAmount(discountAmount);
        coupon.setDiscountRate(BigDecimal.ONE);
        coupon.setMinAmount(minAmount);
        coupon.setTotalCount(100);
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(1);
        coupon.setStartTime(LocalDateTime.now().minusDays(1));
        coupon.setEndTime(LocalDateTime.now().plusDays(1));
        return coupon;
    }

    private SmsCouponUser couponUser(Long id, Long couponId, Integer status) {
        SmsCouponUser couponUser = new SmsCouponUser();
        couponUser.setId(id);
        couponUser.setCouponId(couponId);
        couponUser.setUserId(1L);
        couponUser.setStatus(status);
        return couponUser;
    }
}
