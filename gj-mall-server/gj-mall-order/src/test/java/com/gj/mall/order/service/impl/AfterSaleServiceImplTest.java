package com.gj.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.dto.AfterSaleApplyDTO;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayRefundRecordMapper;
import com.gj.mall.order.service.AfterSaleRuleService;
import com.gj.mall.order.vo.AfterSaleVO;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, refundRecordMapper, messageService, ruleService);

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
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, refundRecordMapper, messageService, ruleService);

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

    @Test
    void listByOrderIncludesRefundRecordForCompletedAfterSale() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AfterSaleRuleService ruleService = new AfterSaleRuleServiceImpl(jdbcTemplate);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, refundRecordMapper, messageService, ruleService);

        OmsOrder order = completedOrder(LocalDateTime.now().minusDays(1));
        OmsAfterSale afterSale = completedAfterSale(order);
        PayRefundRecord refund = refundRecord(afterSale);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(afterSaleMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(afterSale));
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(refund));

        List<AfterSaleVO> result = service.listByOrder(1L, 100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRefundRecord()).isNotNull();
        assertThat(result.get(0).getRefundRecord().getRefundNo()).isEqualTo("RF202605280001");
        assertThat(result.get(0).getRefundRecord().getStatusDesc()).isEqualTo("退款成功");
        assertThat(result.get(0).getRefundRecord().getChannelDesc()).isEqualTo("MOCK 模拟支付");
        assertThat(result.get(0).getTimeline()).extracting("title")
                .containsExactly("提交申请", "商家审核", "退款处理");
    }

    @Test
    void detailIncludesRefundRecordForOwnedAfterSale() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AfterSaleRuleService ruleService = new AfterSaleRuleServiceImpl(jdbcTemplate);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, refundRecordMapper, messageService, ruleService);

        OmsOrder order = completedOrder(LocalDateTime.now().minusDays(1));
        OmsAfterSale afterSale = completedAfterSale(order);
        PayRefundRecord refund = refundRecord(afterSale);

        when(afterSaleMapper.selectById(200L)).thenReturn(afterSale);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(refund));

        AfterSaleVO result = service.detail(1L, 200L);

        assertThat(result.getAfterSaleNo()).isEqualTo("AS202605280001");
        assertThat(result.getRefundRecord()).isNotNull();
        assertThat(result.getRefundRecord().getRefundNo()).isEqualTo("RF202605280001");
        assertThat(result.getTimeline()).extracting("title")
                .containsExactly("提交申请", "商家审核", "退款处理");
    }

    @Test
    void detailTimelineShowsFailedRefundState() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AfterSaleRuleService ruleService = new AfterSaleRuleServiceImpl(jdbcTemplate);
        AfterSaleServiceImpl service = new AfterSaleServiceImpl(
                afterSaleMapper, orderMapper, orderItemMapper, refundRecordMapper, messageService, ruleService);

        OmsOrder order = completedOrder(LocalDateTime.now().minusDays(1));
        OmsAfterSale afterSale = completedAfterSale(order);
        afterSale.setStatus(2);
        PayRefundRecord refund = refundRecord(afterSale);
        refund.setStatus(2);
        refund.setSuccessTime(null);
        refund.setReason("渠道返回失败");

        when(afterSaleMapper.selectById(200L)).thenReturn(afterSale);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(refund));

        AfterSaleVO result = service.detail(1L, 200L);

        assertThat(result.getRefundRecord().getStatusDesc()).isEqualTo("退款失败");
        assertThat(result.getTimeline()).extracting("title")
                .contains("退款失败");
        assertThat(result.getTimeline()).extracting("description")
                .contains("渠道返回失败");
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

    private OmsAfterSale completedAfterSale(OmsOrder order) {
        OmsAfterSale afterSale = new OmsAfterSale();
        afterSale.setId(200L);
        afterSale.setAfterSaleNo("AS202605280001");
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setUserId(order.getUserId());
        afterSale.setType(1);
        afterSale.setAmount(order.getPayAmount());
        afterSale.setReason("商品问题");
        afterSale.setOrderStatusSnapshot(OrderStatus.COMPLETED.getCode());
        afterSale.setStatus(4);
        afterSale.setRefundPaymentId(300L);
        afterSale.setRefundTime(LocalDateTime.now().minusMinutes(10));
        afterSale.setCreateTime(LocalDateTime.now().minusHours(1));
        return afterSale;
    }

    private PayRefundRecord refundRecord(OmsAfterSale afterSale) {
        PayRefundRecord refund = new PayRefundRecord();
        refund.setId(300L);
        refund.setRefundNo("RF202605280001");
        refund.setPaymentId(400L);
        refund.setPayNo("PAY202605280001");
        refund.setThirdPayNo("THIRD202605280001");
        refund.setAfterSaleId(afterSale.getId());
        refund.setAfterSaleNo(afterSale.getAfterSaleNo());
        refund.setOrderId(afterSale.getOrderId());
        refund.setOrderNo(afterSale.getOrderNo());
        refund.setUserId(afterSale.getUserId());
        refund.setChannel(9);
        refund.setAmount(afterSale.getAmount());
        refund.setStatus(1);
        refund.setReason("售后退款");
        refund.setOperatorType("admin_after_sale");
        refund.setSuccessTime(LocalDateTime.now().minusMinutes(10));
        return refund;
    }
}
