package com.gj.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.dto.AfterSaleApplyDTO;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.service.AfterSaleRuleService;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleServiceImplTest {

    @Test
    void applyRejectsCompletedOrderOutsideConfiguredWindow() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AfterSaleRuleService ruleService = new AfterSaleRuleServiceImpl(jdbcTemplate);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, messageService, ruleService);

        OmsOrder order = completedOrder(LocalDateTime.now().minusDays(8));
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq("after.sale.window.days")))
                .thenReturn("7");

        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setType(2);
        dto.setReason("商品问题");

        assertThatThrownBy(() -> service.apply(1L, 100L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("售后申请已超过 7 天");
        verify(afterSaleMapper, never()).insert(any(OmsAfterSale.class));
    }

    @Test
    void applySavesRequestedTypeWhenInsideWindow() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AfterSaleRuleService ruleService = new AfterSaleRuleServiceImpl(jdbcTemplate);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, messageService, ruleService);

        OmsOrder order = completedOrder(LocalDateTime.now().minusDays(2));
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(afterSaleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq("after.sale.window.days")))
                .thenReturn("7");

        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setType(2);
        dto.setReason("商品问题");

        service.apply(1L, 100L, dto);

        ArgumentCaptor<OmsAfterSale> captor = ArgumentCaptor.forClass(OmsAfterSale.class);
        verify(afterSaleMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(2);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("88.00");
    }

    private OmsOrder completedOrder(LocalDateTime receiveTime) {
        OmsOrder order = new OmsOrder();
        order.setId(100L);
        order.setOrderNo("202605200001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setPayAmount(new BigDecimal("88.00"));
        order.setReceiveTime(receiveTime);
        order.setCreateTime(receiveTime.minusDays(1));
        order.setUpdateTime(receiveTime);
        return order;
    }
}
