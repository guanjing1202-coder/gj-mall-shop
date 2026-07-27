package com.gj.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.cart.service.CartService;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.service.impl.SeckillServiceImpl;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mq.OrderTimeoutProducer;
import com.gj.mall.order.service.FreightService;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.user.service.UserAddressService;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplMarketingIntegrationTest {

    @Test
    void markPaidConsumesSkuStockAndIncrementsSeckillSoldCountForSeckillItems() {
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper itemMapper = mock(OmsOrderItemMapper.class);
        SkuService skuService = mock(SkuService.class);
        CouponService couponService = mock(CouponService.class);
        SeckillServiceImpl seckillService = mock(SeckillServiceImpl.class);
        OrderServiceImpl service = new OrderServiceImpl(orderMapper, itemMapper, mock(CartService.class), skuService,
                mock(UserAddressService.class), mock(OrderTimeoutProducer.class), couponService, mock(PmsSpuMapper.class),
                mock(UserMessageService.class), mock(FreightService.class), seckillService);
        when(orderMapper.selectById(100L)).thenReturn(order(100L));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(seckillItem()));

        service.markPaid(100L, 9);

        verify(skuService).consumeStock(200L, 1);
        verify(seckillService).onPaid(10L, 1);
    }

    private OmsOrder order(Long id) {
        OmsOrder order = new OmsOrder();
        order.setId(id);
        order.setOrderNo("202606080001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        return order;
    }

    private OmsOrderItem seckillItem() {
        OmsOrderItem item = new OmsOrderItem();
        item.setOrderId(100L);
        item.setSkuId(200L);
        item.setQuantity(1);
        item.setSeckillSkuId(10L);
        return item;
    }
}
