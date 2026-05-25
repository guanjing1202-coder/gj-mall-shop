package com.gj.mall.admin.service.impl;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.mapper.PayRefundRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.config.PayCallbackProperties;
import com.gj.mall.pay.service.PayService;
import com.gj.mall.pay.support.PayCallbackSignatureSupport;
import com.gj.mall.admin.vo.AdminPaymentAccessVO;
import com.gj.mall.admin.vo.AdminPaymentVO;
import com.gj.mall.user.mapper.UmsUserMapper;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void refundSendsPaymentMessageToUser() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        UserMessageService messageService = mock(UserMessageService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                messageService);

        PayPaymentRecord payment = paidPayment();
        OmsOrder order = paidOrder();
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectById(payment.getOrderId())).thenReturn(order);
        when(refundRecordMapper.selectOne(any())).thenReturn(null);

        service.refund(payment.getId(), null);

        verify(messageService).create(
                payment.getUserId(),
                "payment",
                "退款已完成",
                "订单 " + payment.getOrderNo() + " 已完成退款，退款金额 ¥99.00。",
                "order",
                payment.getOrderId(),
                payment.getOrderNo());
    }

    @Test
    void retryRefundSucceedsForFailedRefundRecord() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

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
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

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
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

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

    @Test
    void replayCallbackDelegatesToPayService() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        service.replayCallback(88L);

        verify(payService).replayCallback(88L);
    }

    @Test
    void accessIncludesDevelopmentSignatureSample() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertTrue(access.getDevSignaturePayload().contains("payNo=P202605220001"));
        assertTrue(access.getDevSignatureHeader().contains("x-gj-pay-signature"));
        assertTrue(access.getDevSignature().length() >= 32);
        assertTrue(access.getDevCallbackExample().contains("P202605220001"));
    }

    @Test
    void syncStatusMarksPendingPaymentPaidWhenSuccessfulCallbackExists() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = pendingWechatPayment();
        PayCallbackRecord callback = successfulCallback(payment);
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.singletonList(callback));

        service.syncStatus(payment.getId());

        verify(recordMapper).updateById(argThat(update ->
                payment.getId().equals(update.getId())
                        && Integer.valueOf(1).equals(update.getStatus())
                        && callback.getThirdPayNo().equals(update.getThirdPayNo())
                        && update.getPayTime() != null
                        && update.getCallbackData().contains("admin-sync-status")
                        && update.getCallbackData().contains(callback.getCallbackNo())));
        verify(orderService).markPaid(payment.getOrderId(), payment.getChannel());
    }

    @Test
    void syncStatusRejectsPendingPaymentWithoutSuccessfulCallback() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = pendingWechatPayment();
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        BizException error = assertThrows(BizException.class, () -> service.syncStatus(payment.getId()));

        assertTrue(error.getMessage().contains("未找到可入账的成功支付回调"));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void detailIncludesSyncStatusBasisWhenSuccessfulCallbackExists() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = pendingWechatPayment();
        PayCallbackRecord callback = successfulCallback(payment);
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.singletonList(callback));

        AdminPaymentVO detail = service.detail(payment.getId());

        assertTrue(detail.getSyncStatusAllowed());
        assertEquals("发现成功回调，可同步支付状态", detail.getSyncStatusReason());
        assertEquals(callback.getId(), detail.getSyncCallbackId());
        assertEquals(callback.getCallbackNo(), detail.getSyncCallbackNo());
        assertEquals(callback.getThirdPayNo(), detail.getSyncThirdPayNo());
        assertNotNull(detail.getSyncCallbackTime());
    }

    @Test
    void detailMarksUnsettledPaymentNotSyncableWithoutSuccessfulCallback() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = pendingWechatPayment();
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        AdminPaymentVO detail = service.detail(payment.getId());

        assertFalse(detail.getSyncStatusAllowed());
        assertEquals("未找到可入账的成功支付回调", detail.getSyncStatusReason());
    }

    @Test
    void detailMarksPaidPaymentRefundableWhenNoRefundExists() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = paidPayment();
        OmsOrder order = paidOrder();
        order.setStatus(1);
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        AdminPaymentVO detail = service.detail(payment.getId());

        assertTrue(detail.getRefundAllowed());
        assertEquals("已支付且未生成退款记录，可发起全额退款", detail.getRefundReason());
    }

    @Test
    void detailExplainsRefundBlockedByExistingRefundRecord() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                new PayCallbackProperties(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = paidPayment();
        OmsOrder order = paidOrder();
        PayRefundRecord refund = failedRefund();
        refund.setStatus(0);
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.singletonList(refund));
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        AdminPaymentVO detail = service.detail(payment.getId());

        assertFalse(detail.getRefundAllowed());
        assertEquals("退款记录已存在，请在退款记录中处理", detail.getRefundReason());
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

    private PayPaymentRecord pendingWechatPayment() {
        PayPaymentRecord payment = new PayPaymentRecord();
        payment.setId(301L);
        payment.setOrderId(201L);
        payment.setOrderNo("202605220001");
        payment.setUserId(1L);
        payment.setPayNo("PAY202605220001");
        payment.setChannel(1);
        payment.setAmount(new BigDecimal("99.00"));
        payment.setStatus(0);
        payment.setCallbackData("created");
        return payment;
    }

    private PayCallbackRecord successfulCallback(PayPaymentRecord payment) {
        PayCallbackRecord callback = new PayCallbackRecord();
        callback.setId(401L);
        callback.setCallbackNo("CBWECHAT202605220001");
        callback.setPayNo(payment.getPayNo());
        callback.setThirdPayNo("WX202605220001");
        callback.setChannel(payment.getChannel());
        callback.setAmount(payment.getAmount());
        callback.setSignatureStatus(1);
        callback.setProcessStatus(1);
        callback.setEventType("SUCCESS");
        callback.setCreateTime(LocalDateTime.now());
        return callback;
    }
}
