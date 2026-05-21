package com.gj.mall.admin.service.impl;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.mapper.PayRefundRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.user.mapper.UmsUserMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPaymentServiceImplTest {

    @Test
    void retryRefundSucceedsForFailedRefundRecord() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService);

        PayRefundRecord refund = failedRefund();
        PayPaymentRecord payment = paidPayment();
        OmsOrder order = paidOrder();
        when(refundRecordMapper.selectById(900L)).thenReturn(refund);
        when(recordMapper.selectById(refund.getPaymentId())).thenReturn(payment);
        when(orderMapper.selectById(refund.getOrderId())).thenReturn(order);

        service.retryRefund(900L);

        verify(refundRecordMapper).updateById(argThat(update ->
                refund.getId().equals(update.getId())
                        && Integer.valueOf(1).equals(update.getStatus())
                        && update.getSuccessTime() != null
                        && update.getCallbackData().contains("admin-retry-refund success")));
        verify(recordMapper).updateById(argThat(update ->
                payment.getId().equals(update.getId())
                        && Integer.valueOf(3).equals(update.getStatus())
                        && update.getCallbackData().contains("admin-retry-refund")));
        verify(orderMapper).updateById(argThat(update ->
                order.getId().equals(update.getId()) && Integer.valueOf(6).equals(update.getStatus())));
    }

    @Test
    void retryRefundRejectsSucceededRefundRecord() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService);

        PayRefundRecord refund = failedRefund();
        refund.setStatus(1);
        when(refundRecordMapper.selectById(900L)).thenReturn(refund);

        BizException error = assertThrows(BizException.class, () -> service.retryRefund(900L));

        assertTrue(error.getMessage().contains("仅退款中或退款失败记录可重试"));
        verify(refundRecordMapper, never()).updateById(any(PayRefundRecord.class));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderMapper, never()).updateById(any(OmsOrder.class));
    }

    @Test
    void markRefundFailedOnlyUpdatesRefundRecord() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService);

        PayRefundRecord refund = failedRefund();
        refund.setStatus(0);
        refund.setCallbackData("created");
        when(refundRecordMapper.selectById(900L)).thenReturn(refund);

        service.markRefundFailed(900L, "渠道返回失败");

        verify(refundRecordMapper).updateById(argThat(update ->
                refund.getId().equals(update.getId())
                        && Integer.valueOf(2).equals(update.getStatus())
                        && update.getCallbackData().contains("admin-mark-refund-failed")
                        && update.getCallbackData().contains("渠道返回失败")));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderMapper, never()).updateById(any(OmsOrder.class));
    }

    private PayRefundRecord failedRefund() {
        PayRefundRecord refund = new PayRefundRecord();
        refund.setId(900L);
        refund.setRefundNo("RF202605210001");
        refund.setPaymentId(300L);
        refund.setPayNo("PAY202605210001");
        refund.setThirdPayNo("THIRD202605210001");
        refund.setOrderId(200L);
        refund.setOrderNo("202605210001");
        refund.setUserId(1L);
        refund.setChannel(9);
        refund.setAmount(new BigDecimal("99.00"));
        refund.setStatus(2);
        refund.setReason("后台退款");
        refund.setOperatorType("admin_payment");
        refund.setCallbackData("refund failed");
        return refund;
    }

    private PayPaymentRecord paidPayment() {
        PayPaymentRecord payment = new PayPaymentRecord();
        payment.setId(300L);
        payment.setOrderId(200L);
        payment.setOrderNo("202605210001");
        payment.setUserId(1L);
        payment.setPayNo("PAY202605210001");
        payment.setThirdPayNo("THIRD202605210001");
        payment.setChannel(9);
        payment.setAmount(new BigDecimal("99.00"));
        payment.setStatus(1);
        payment.setPayTime(LocalDateTime.now().minusHours(2));
        return payment;
    }

    private OmsOrder paidOrder() {
        OmsOrder order = new OmsOrder();
        order.setId(200L);
        order.setOrderNo("202605210001");
        order.setUserId(1L);
        order.setPayAmount(new BigDecimal("99.00"));
        order.setStatus(5);
        return order;
    }
}
