package com.gj.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.vo.DashboardBusinessVO;
import com.gj.mall.marketing.mapper.SmsCouponMapper;
import com.gj.mall.marketing.mapper.SmsSeckillMapper;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.vo.AdminInventorySummaryVO;
import com.gj.mall.user.mapper.UmsUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceImplTest {

    private PmsSpuMapper spuMapper;
    private PmsBrandMapper brandMapper;
    private PmsCategoryMapper categoryMapper;
    private PmsSkuMapper skuMapper;
    private PmsProductCommentMapper commentMapper;
    private UmsUserMapper userMapper;
    private OmsOrderMapper orderMapper;
    private OmsAfterSaleMapper afterSaleMapper;
    private PayPaymentRecordMapper paymentRecordMapper;
    private SmsCouponMapper couponMapper;
    private SmsSeckillMapper seckillMapper;
    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        spuMapper = mock(PmsSpuMapper.class);
        brandMapper = mock(PmsBrandMapper.class);
        categoryMapper = mock(PmsCategoryMapper.class);
        skuMapper = mock(PmsSkuMapper.class);
        commentMapper = mock(PmsProductCommentMapper.class);
        userMapper = mock(UmsUserMapper.class);
        orderMapper = mock(OmsOrderMapper.class);
        afterSaleMapper = mock(OmsAfterSaleMapper.class);
        paymentRecordMapper = mock(PayPaymentRecordMapper.class);
        couponMapper = mock(SmsCouponMapper.class);
        seckillMapper = mock(SmsSeckillMapper.class);
        service = new AdminDashboardServiceImpl(
                spuMapper,
                brandMapper,
                categoryMapper,
                skuMapper,
                commentMapper,
                userMapper,
                orderMapper,
                afterSaleMapper,
                paymentRecordMapper,
                couponMapper,
                seckillMapper);

        when(spuMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(spuMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());
        when(brandMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(skuMapper.selectInventorySummary(any())).thenReturn(new AdminInventorySummaryVO());
        when(skuMapper.selectInventoryPage(any(Page.class), any())).thenReturn(new Page<>());
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(orderMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(orderMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());
        when(afterSaleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(paymentRecordMapper.selectObjs(any(Wrapper.class))).thenReturn(Collections.singletonList(BigDecimal.ZERO));
        when(couponMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(seckillMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
    }

    @Test
    void businessCountsRefundingAndRefundedOrdersAsPaidAmount() {
        AtomicInteger orderCountCall = new AtomicInteger();
        when(orderMapper.selectCount(any(Wrapper.class))).thenAnswer(invocation -> {
            int call = orderCountCall.incrementAndGet();
            if (call == 1 || call == 10) {
                return 3L;
            }
            if (call == 4 || call == 8 || call == 9) {
                return 1L;
            }
            return 0L;
        });
        when(orderMapper.selectObjs(any(Wrapper.class))).thenAnswer(invocation -> {
            Wrapper<?> wrapper = invocation.getArgument(0);
            String segment = wrapper.getSqlSegment();
            if (segment.contains("status IN")) {
                if (params(wrapper).containsValue(OrderStatus.REFUNDING.getCode())
                        && params(wrapper).containsValue(OrderStatus.REFUNDED.getCode())) {
                    return Collections.singletonList(new BigDecimal("200.00"));
                }
                return Collections.singletonList(new BigDecimal("120.00"));
            }
            return Collections.singletonList(BigDecimal.ZERO);
        });

        DashboardBusinessVO dashboard = service.business();

        assertEquals(new BigDecimal("200.00"), dashboard.getPaidAmountTotal());
        assertEquals(new BigDecimal("66.67"), dashboard.getAverageOrderAmount());
        assertEquals(new BigDecimal("100.00"), dashboard.getPaidConversionRate());
        assertEquals(new BigDecimal("33.33"), dashboard.getRefundRate());
    }

    @Test
    void businessHandlesEmptyPagesAndNullAggregateRows() {
        when(orderMapper.selectObjs(any(Wrapper.class))).thenReturn(Collections.singletonList(null));
        when(paymentRecordMapper.selectObjs(any(Wrapper.class))).thenReturn(Collections.singletonList(null));

        DashboardBusinessVO dashboard = service.business();

        assertEquals(BigDecimal.ZERO, dashboard.getPaidAmountTotal());
        assertEquals(BigDecimal.ZERO, dashboard.getTodayPaidAmount());
        assertEquals(BigDecimal.ZERO, dashboard.getAverageOrderAmount());
        assertEquals(BigDecimal.ZERO, dashboard.getRefundAmountTotal());
        assertEquals(BigDecimal.ZERO, dashboard.getTodayRefundAmount());
        assertEquals(7, dashboard.getOrderTrend().size());
        assertTrue(dashboard.getHotProducts().isEmpty());
        assertTrue(dashboard.getLowStockSkus().isEmpty());
        assertTrue(dashboard.getLatestOrders().isEmpty());
        assertEquals(LocalDate.now().minusDays(6), dashboard.getOrderTrend().get(0).getDate());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> params(Wrapper<?> wrapper) {
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
    }
}
