package com.gj.mall.admin.service.impl;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.mapper.PayRefundRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.service.PayService;
import com.gj.mall.pay.support.PayCallbackSignatureSupport;
import com.gj.mall.admin.vo.AdminPaymentAccessVO;
import com.gj.mall.admin.vo.AdminPaymentVO;
import com.gj.mall.user.entity.UmsUserMessage;
import com.gj.mall.user.mapper.UmsUserMessageMapper;
import com.gj.mall.user.mapper.UmsUserMapper;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private AdminPaymentServiceImpl createService(
            PayPaymentRecordMapper recordMapper,
            PayCallbackRecordMapper callbackRecordMapper,
            PayRefundRecordMapper refundRecordMapper,
            OmsOrderMapper orderMapper,
            UmsUserMapper userMapper,
            OrderService orderService,
            PayService payService,
            PayRuntimeConfigService runtimeConfigService,
            UserMessageService messageService) {
        return new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfigService,
                messageService);
    }

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
        AdminPaymentServiceImpl service = createService(
                recordMapper, callbackRecordMapper, refundRecordMapper, orderMapper, userMapper,
                orderService, payService, defaultRuntimeConfig(), messageService);

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
        UserMessageService messageService = mock(UserMessageService.class);
        AdminPaymentServiceImpl service = createService(
                recordMapper, callbackRecordMapper, refundRecordMapper, orderMapper, userMapper,
                orderService, payService, defaultRuntimeConfig(), messageService);

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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
        UserMessageService messageService = mock(UserMessageService.class);
        AdminPaymentServiceImpl service = createService(
                recordMapper, callbackRecordMapper, refundRecordMapper, orderMapper, userMapper,
                orderService, payService, defaultRuntimeConfig(), messageService);

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
        verify(messageService).create(
                refund.getUserId(),
                "after_sale",
                "退款失败",
                "订单 " + refund.getOrderNo() + " 的退款处理失败，原因：渠道返回失败，请等待商家重新处理。",
                "order",
                refund.getOrderId(),
                refund.getOrderNo());
    }

    @Test
    void markRefundFailedFallsBackWhenReasonIsBlank() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = createService(
                recordMapper, callbackRecordMapper, refundRecordMapper, orderMapper, userMapper,
                orderService, payService, defaultRuntimeConfig(), mock(UserMessageService.class));

        PayRefundRecord refund = failedRefund();
        refund.setStatus(0);
        when(refundRecordMapper.selectById(900L)).thenReturn(refund);

        service.markRefundFailed(900L, "   ");

        verify(refundRecordMapper).updateById(argThat(update ->
                refund.getId().equals(update.getId())
                        && Integer.valueOf(2).equals(update.getStatus())
                        && update.getCallbackData().contains("reason=-")));
    }

    @Test
    void markRefundFailedRejectsNonProcessingRefundRecord() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = createService(
                recordMapper, callbackRecordMapper, refundRecordMapper, orderMapper, userMapper,
                orderService, payService, defaultRuntimeConfig(), mock(UserMessageService.class));

        PayRefundRecord refund = failedRefund();
        when(refundRecordMapper.selectById(900L)).thenReturn(refund);

        BizException error = assertThrows(BizException.class, () -> service.markRefundFailed(900L, "重复失败"));

        assertTrue(error.getMessage().contains("仅退款中记录可标记失败"));
        verify(refundRecordMapper, never()).updateById(any(PayRefundRecord.class));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderMapper, never()).updateById(any(OmsOrder.class));
    }

    @Test
    void retryRefundRejectsMissingOrder() {
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
                mock(UserMessageService.class));

        PayRefundRecord refund = failedRefund();
        PayPaymentRecord payment = paidPayment();
        when(refundRecordMapper.selectById(900L)).thenReturn(refund);
        when(recordMapper.selectById(refund.getPaymentId())).thenReturn(payment);
        when(orderMapper.selectById(refund.getOrderId())).thenReturn(null);

        BizException error = assertThrows(BizException.class, () -> service.retryRefund(900L));

        assertEquals(ResultCode.ORDER_NOT_FOUND.getCode(), error.getCode());
        verify(refundRecordMapper, never()).updateById(any(PayRefundRecord.class));
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertTrue(access.getDevSignaturePayload().contains("payNo=P202605220001"));
        assertTrue(access.getDevSignatureHeader().contains("x-gj-pay-signature"));
        assertTrue(access.getDevSignature().length() >= 32);
        assertTrue(access.getDevCallbackExample().contains("P202605220001"));
    }

    @Test
    void accessExplainsMissingProductionPaymentConfiguration() {
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                missingRealRuntimeConfig(),
                mock(UserMessageService.class));
        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertEquals("真实支付配置未就绪", access.getReadinessText());
        assertTrue(access.getReadinessTips().contains("真实支付模式建议开启 mall.pay.callback.require-signature"));
        assertTrue(access.getReadinessTips().contains("请配置 mall.pay.callback.secret"));
        assertTrue(access.getReadinessTips().contains("请配置 mall.pay.wechat.app-id"));
        assertTrue(access.getReadinessTips().contains("请配置 mall.pay.alipay.app-id"));
        assertEquals("缺少 app-id、mch-id、api-v3-key、merchant-serial-no、private-key-path、notify-url", access.getChannels().get(1).getStatus());
        assertEquals("缺少 app-id、private-key、alipay-public-key、notify-url", access.getChannels().get(2).getStatus());
    }

    @Test
    void accessWarnsWhenRealModeUsesDefaultDevelopmentCallbackSecret() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("gj-mall-dev-pay-callback-secret");
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertTrue(access.getReadinessTips().contains("请替换默认开发回调密钥 mall.pay.callback.secret"));
        assertEquals("可用", access.getChannels().get(1).getStatus());
        assertEquals("可用", access.getChannels().get(2).getStatus());
    }

    @Test
    void accessWarnsWhenWechatPrivateKeyPathIsUnreadable() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("prod-callback-secret");
        when(runtimeConfig.wechatPrivateKeyFileReadable()).thenReturn(false);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertTrue(access.getReadinessTips().contains("请确认 mall.pay.wechat.private-key-path 文件存在且服务进程可读取"));
        assertEquals("私钥文件不可读", access.getChannels().get(1).getStatus());
    }

    @Test
    void accessWarnsWhenWechatNotifyUrlIsNotHttps() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("prod-callback-secret");
        when(runtimeConfig.wechatNotifyUrl()).thenReturn("http://shop.guanjing.cloud/api/pay/callback/wechat");
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertTrue(access.getReadinessTips().contains("请将 mall.pay.wechat.notify-url 配置为 https:// 开头的有效地址"));
        assertEquals("回调地址需使用 HTTPS", access.getChannels().get(1).getStatus());
    }

    @Test
    void accessWarnsWhenWechatNotifyUrlPathDoesNotMatchCallbackEndpoint() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("prod-callback-secret");
        when(runtimeConfig.wechatNotifyUrl()).thenReturn("https://shop.guanjing.cloud/pay/wx-notify");
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertTrue(access.getReadinessTips().contains("请将 mall.pay.wechat.notify-url 指向 /api/pay/callback/wechat"));
        assertEquals("回调地址路径需为 /api/pay/callback/wechat", access.getChannels().get(1).getStatus());
    }

    @Test
    void accessWarnsWhenAlipayNotifyUrlIsNotHttps() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("prod-callback-secret");
        when(runtimeConfig.alipayNotifyUrl()).thenReturn("http://shop.guanjing.cloud/api/pay/callback/alipay");
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertTrue(access.getReadinessTips().contains("请将 mall.pay.alipay.notify-url 配置为 https:// 开头的有效地址"));
        assertEquals("回调地址需使用 HTTPS", access.getChannels().get(2).getStatus());
    }

    @Test
    void accessWarnsWhenAlipayNotifyUrlPathDoesNotMatchCallbackEndpoint() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("prod-callback-secret");
        when(runtimeConfig.alipayNotifyUrl()).thenReturn("https://shop.guanjing.cloud/pay/notify");
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertFalse(access.getReady());
        assertTrue(access.getReadinessTips().contains("请将 mall.pay.alipay.notify-url 指向 /api/pay/callback/alipay"));
        assertEquals("回调地址路径需为 /api/pay/callback/alipay", access.getChannels().get(2).getStatus());
    }

    @Test
    void accessReadyWhenRealRuntimeConfigIsCompleteAndPrivateKeyReadable() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        PayRuntimeConfigService runtimeConfig = completeRealRuntimeConfig("prod-callback-secret");
        when(runtimeConfig.wechatPrivateKeyFileReadable()).thenReturn(true);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                runtimeConfig,
                mock(UserMessageService.class));

        AdminPaymentAccessVO access = service.access();

        assertTrue(access.getReady());
        assertEquals("真实支付配置已就绪", access.getReadinessText());
        assertEquals("可用", access.getChannels().get(1).getStatus());
        assertEquals("可用", access.getChannels().get(2).getStatus());
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
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

    @Test
    void detailIncludesFailedRefundRecordReason() {
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
                mock(UmsUserMessageMapper.class),
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = paidPayment();
        OmsOrder order = paidOrder();
        PayRefundRecord refund = failedRefund();
        refund.setReason("渠道余额不足");
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.singletonList(refund));
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        AdminPaymentVO detail = service.detail(payment.getId());

        assertNotNull(detail.getRefundRecord());
        assertEquals(Integer.valueOf(2), detail.getRefundRecord().getStatus());
        assertEquals("退款失败", detail.getRefundRecord().getStatusDesc());
        assertEquals("渠道余额不足", detail.getRefundRecord().getReason());
    }

    @Test
    void detailIncludesLatestUserOrderMessage() {
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        UmsUserMessageMapper userMessageMapper = mock(UmsUserMessageMapper.class);
        OrderService orderService = mock(OrderService.class);
        PayService payService = mock(PayService.class);
        AdminPaymentServiceImpl service = new AdminPaymentServiceImpl(
                recordMapper,
                callbackRecordMapper,
                refundRecordMapper,
                orderMapper,
                userMapper,
                userMessageMapper,
                orderService,
                payService,
                new PayCallbackSignatureSupport(),
                defaultRuntimeConfig(),
                mock(UserMessageService.class));

        PayPaymentRecord payment = paidPayment();
        UmsUserMessage message = orderMessage(payment.getOrderId());
        when(recordMapper.selectById(payment.getId())).thenReturn(payment);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(callbackRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMessageMapper.selectList(any())).thenReturn(Collections.singletonList(message));

        AdminPaymentVO detail = service.detail(payment.getId());

        assertEquals("退款已完成", detail.getLatestUserMessageTitle());
        assertEquals("订单退款已经到账", detail.getLatestUserMessageContent());
        assertEquals(message.getCreateTime(), detail.getLatestUserMessageTime());
    }

    private PayRuntimeConfigService defaultRuntimeConfig() {
        return runtimeConfig("mock", false, "gj-mall-dev-pay-callback-secret");
    }

    private PayRuntimeConfigService missingRealRuntimeConfig() {
        return runtimeConfig("real", false, "");
    }

    private PayRuntimeConfigService completeRealRuntimeConfig(String callbackSecret) {
        PayRuntimeConfigService service = runtimeConfig("real", true, callbackSecret);
        when(service.wechatAppId()).thenReturn("wx-app");
        when(service.wechatMchId()).thenReturn("wx-mch");
        when(service.wechatApiV3Key()).thenReturn("wx-api-v3-key");
        when(service.wechatMerchantSerialNo()).thenReturn("wx-serial");
        when(service.wechatPrivateKeyPath()).thenReturn("/secure/wx-private.pem");
        when(service.wechatNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/wechat");
        when(service.wechatPrivateKeyFileReadable()).thenReturn(true);
        when(service.alipayAppId()).thenReturn("ali-app");
        when(service.alipayPrivateKey()).thenReturn("ali-private");
        when(service.alipayPublicKey()).thenReturn("ali-public");
        when(service.alipayNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/alipay");
        return service;
    }

    private PayRuntimeConfigService runtimeConfig(String mode, boolean requireSignature, String callbackSecret) {
        PayRuntimeConfigService service = mock(PayRuntimeConfigService.class);
        Map<String, String> values = new HashMap<>();
        values.put("mall.pay.mode", mode);
        values.put("mall.pay.callback.secret", callbackSecret);
        when(service.mode()).thenReturn(mode);
        when(service.callbackRequireSignature()).thenReturn(requireSignature);
        when(service.callbackSecret()).thenReturn(callbackSecret);
        when(service.usingDefaultCallbackSecret()).thenReturn("gj-mall-dev-pay-callback-secret".equals(callbackSecret));
        when(service.text(any(String.class), any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String defaultValue = invocation.getArgument(1);
            String value = values.get(key);
            return value == null ? defaultValue : value;
        });
        return service;
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

    private UmsUserMessage orderMessage(Long orderId) {
        UmsUserMessage message = new UmsUserMessage();
        message.setId(501L);
        message.setUserId(1L);
        message.setType("payment");
        message.setTitle("退款已完成");
        message.setContent("订单退款已经到账");
        message.setBizType("order");
        message.setBizId(orderId);
        message.setBizNo("202605210001");
        message.setCreateTime(LocalDateTime.now().minusMinutes(5));
        return message;
    }
}
