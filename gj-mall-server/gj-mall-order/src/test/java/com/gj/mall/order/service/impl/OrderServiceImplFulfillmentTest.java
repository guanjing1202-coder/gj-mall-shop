package com.gj.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.cart.service.CartService;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.service.impl.SeckillServiceImpl;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mq.OrderTimeoutProducer;
import com.gj.mall.order.service.FreightService;
import com.gj.mall.order.vo.OrderLogisticsTraceVO;
import com.gj.mall.order.vo.OrderLogisticsVO;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.user.service.UserAddressService;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceImplFulfillmentTest {

    @Test
    void completedOrderLogisticsMarksDeliveryStepActiveEvenWhenDeliveryTimeMissing() {
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper itemMapper = mock(OmsOrderItemMapper.class);
        OrderServiceImpl service = new OrderServiceImpl(orderMapper, itemMapper, mock(CartService.class),
                mock(SkuService.class), mock(UserAddressService.class), mock(OrderTimeoutProducer.class),
                mock(CouponService.class), mock(PmsSpuMapper.class), mock(UserMessageService.class),
                mock(FreightService.class), mock(SeckillServiceImpl.class));
        LocalDateTime receiveTime = LocalDateTime.of(2026, 6, 8, 16, 30);
        OmsOrder order = order(100L, OrderStatus.COMPLETED, receiveTime);
        order.setDeliveryCompany("顺丰速运");
        order.setDeliveryNo("SF123456789");
        order.setDeliveryTime(null);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        OrderLogisticsVO logistics = service.logistics(7L, 100L);

        OrderLogisticsTraceVO deliveryTrace = logistics.getTraces().stream()
                .filter(trace -> "商家发货".equals(trace.getTitle()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(deliveryTrace.getActive()).isTrue();
        assertThat(deliveryTrace.getTime()).isEqualTo(receiveTime);
        assertThat(deliveryTrace.getDescription()).contains("顺丰速运", "SF123456789");
    }

    private static OmsOrder order(Long id, OrderStatus status, LocalDateTime receiveTime) {
        OmsOrder order = new OmsOrder();
        order.setId(id);
        order.setOrderNo("202606080001");
        order.setUserId(7L);
        order.setStatus(status.getCode());
        order.setCreateTime(receiveTime.minusDays(3));
        order.setPayTime(receiveTime.minusDays(2));
        order.setReceiveTime(receiveTime);
        return order;
    }
}
