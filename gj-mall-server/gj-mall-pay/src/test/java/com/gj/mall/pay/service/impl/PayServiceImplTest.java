package com.gj.mall.pay.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.config.PayCallbackProperties;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.support.PayCallbackEventSupport;
import com.gj.mall.pay.support.PayCallbackSignatureSupport;
import com.gj.mall.pay.strategy.PayStrategy;
import com.gj.mall.pay.vo.PayChannelVO;
import com.gj.mall.pay.vo.PayStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayServiceImplTest {

    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }

    @Test
    void listChannelsDisablesRealChannelsInMockMode() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("mock");
        PayServiceImpl service = newPayService(mock(OrderService.class),
                mock(PayPaymentRecordMapper.class),
                mock(PayCallbackRecordMapper.class),
                runtimeConfigService);

        List<PayChannelVO> channels = service.listChannels();

        PayChannelVO mockChannel = channel(channels, "mock");
        PayChannelVO wechat = channel(channels, "wechat");
        PayChannelVO alipay = channel(channels, "alipay");
        assertTrue(mockChannel.getEnabled());
        assertEquals("开发环境即时成功", mockChannel.getStatus());
        assertEquals(Boolean.FALSE, wechat.getEnabled());
        assertTrue(wechat.getStatus().contains("mock 支付模式"));
        assertEquals(Boolean.FALSE, alipay.getEnabled());
        assertTrue(alipay.getStatus().contains("mock 支付模式"));
    }

    @Test
    void listChannelsEnablesAlipayWhenRealConfigReady() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("real");
        when(runtimeConfigService.alipayAppId()).thenReturn("2021000123456789");
        when(runtimeConfigService.alipayPrivateKey()).thenReturn("private-key");
        when(runtimeConfigService.alipayPublicKey()).thenReturn("public-key");
        when(runtimeConfigService.alipayNotifyUrl()).thenReturn("https://shop.example.com/api/pay/callback/alipay");
        PayServiceImpl service = newPayService(mock(OrderService.class),
                mock(PayPaymentRecordMapper.class),
                mock(PayCallbackRecordMapper.class),
                runtimeConfigService);

        PayChannelVO alipay = channel(service.listChannels(), "alipay");

        assertEquals(Boolean.TRUE, alipay.getEnabled());
        assertEquals("可用", alipay.getStatus());
    }

    @Test
    void payRejectsDisabledMockChannelInRealMode() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("real");
        OmsOrder order = pendingOrder();
        when(orderService.getByIdOrThrow(order.getId())).thenReturn(order);
        PayDTO dto = new PayDTO();
        dto.setOrderId(order.getId());
        dto.setChannel("mock");
        PayServiceImpl service = newPayService(orderService,
                recordMapper,
                mock(PayCallbackRecordMapper.class),
                runtimeConfigService);

        BizException error = assertThrows(BizException.class, () -> service.pay(order.getUserId(), dto));

        assertTrue(error.getMessage().contains("真实支付模式"));
        verify(recordMapper, never()).insert(any(PayPaymentRecord.class));
    }

    @Test
    void getStatusReturnsOwnedPaymentWithOrderSnapshot() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayServiceImpl service = newPayService(orderService, recordMapper, callbackRecordMapper, null);

        PayPaymentRecord payment = pendingPayment();
        payment.setStatus(1);
        when(recordMapper.selectOne(any())).thenReturn(payment);
        when(orderService.getByIdOrThrow(payment.getOrderId())).thenReturn(paidOrder());

        PayStatusVO status = service.getStatus(1L, "P202605210001");

        assertEquals("P202605210001", status.getPayNo());
        assertEquals("MOCK-P202605210001", status.getThirdPayNo());
        assertEquals(9, status.getChannel());
        assertEquals("MOCK 模拟支付", status.getChannelDesc());
        assertTrue(status.getPaid());
        assertEquals(1, status.getStatus());
        assertEquals("已支付", status.getStatusDesc());
        assertEquals(new BigDecimal("99.00"), status.getAmount());
        assertEquals(30L, status.getOrderId());
        assertEquals("202605210001", status.getOrderNo());
        assertEquals(1, status.getOrderStatus());
        assertEquals("待发货", status.getOrderStatusDesc());
    }

    @Test
    void getStatusRejectsOtherUsersPayment() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayServiceImpl service = newPayService(orderService, recordMapper, callbackRecordMapper, null);

        PayPaymentRecord payment = pendingPayment();
        payment.setUserId(2L);
        when(recordMapper.selectOne(any())).thenReturn(payment);

        assertThrows(BizException.class, () -> service.getStatus(1L, "P202605210001"));

        verify(orderService, never()).getByIdOrThrow(any());
    }

    @Test
    void replayCallbackProcessesFailedCallbackRecord() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                null,
                new PayCallbackSignatureSupport(),
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        PayCallbackRecord failedCallback = failedCallback();
        PayPaymentRecord payment = pendingPayment();
        when(callbackRecordMapper.selectById(10L)).thenReturn(failedCallback);
        when(recordMapper.selectOne(any())).thenReturn(payment);

        service.replayCallback(10L);

        verify(recordMapper).updateById(argThat(update ->
                payment.getId().equals(update.getId())
                        && Integer.valueOf(1).equals(update.getStatus())
                        && update.getPayTime() != null
                        && update.getCallbackData().contains("channel-callback")));
        verify(orderService).markPaid(payment.getOrderId(), payment.getChannel());
        verify(callbackRecordMapper).insert(argThat(replay ->
                !failedCallback.getCallbackNo().equals(replay.getCallbackNo())
                        && Integer.valueOf(1).equals(replay.getProcessStatus())
                        && failedCallback.getPayNo().equals(replay.getPayNo())));
        verify(callbackRecordMapper).updateById(argThat(update ->
                failedCallback.getId().equals(update.getId())
                        && Integer.valueOf(1).equals(update.getProcessStatus())
                        && Integer.valueOf(2).equals(update.getRetryCount())
                        && update.getErrorMessage().contains("重放成功")));
    }

    @Test
    void replayCallbackRejectsHandledCallbackRecord() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                null,
                new PayCallbackSignatureSupport(),
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        PayCallbackRecord handled = failedCallback();
        handled.setProcessStatus(1);
        when(callbackRecordMapper.selectById(10L)).thenReturn(handled);

        BizException error = assertThrows(BizException.class, () -> service.replayCallback(10L));

        assertTrue(error.getMessage().contains("仅处理失败的回调可重放"));
        verify(callbackRecordMapper, never()).insert(any(PayCallbackRecord.class));
        verify(callbackRecordMapper, never()).updateById(any(PayCallbackRecord.class));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
    }

    @Test
    void handleCallbackIgnoresNonPaidEventWithoutMarkingOrderPaid() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                null,
                new PayCallbackSignatureSupport(),
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        PayPaymentRecord payment = pendingPayment();
        payment.setChannel(1);
        when(recordMapper.selectOne(any())).thenReturn(payment);

        service.handleCallback("wechat", callbackPayload("NOTPAY"), Collections.emptyMap());

        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderService, never()).markPaid(any(), any());
        verify(callbackRecordMapper).insert(argThat(callback ->
                Integer.valueOf(2).equals(callback.getProcessStatus())
                        && "NOTPAY".equals(callback.getEventType())));
    }

    @Test
    void handleCallbackUsesRuntimeCallbackSecretForSignatureVerification() {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        PayCallbackSignatureSupport signatureSupport = new PayCallbackSignatureSupport();
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                runtimeConfigService,
                signatureSupport,
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        Map<String, Object> payload = callbackPayload("SUCCESS");
        payload.put("signature", signatureSupport.sign(payload, "runtime-secret"));
        PayPaymentRecord payment = pendingPayment();
        when(runtimeConfigService.callbackRequireSignature()).thenReturn(true);
        when(runtimeConfigService.callbackSecret()).thenReturn("runtime-secret");
        when(recordMapper.selectOne(any())).thenReturn(payment);

        service.handleCallback("mock", payload, Collections.emptyMap());

        verify(callbackRecordMapper).insert(argThat(callback ->
                Integer.valueOf(1).equals(callback.getSignatureStatus())
                        && Integer.valueOf(1).equals(callback.getProcessStatus())));
        verify(orderService).markPaid(payment.getOrderId(), payment.getChannel());
    }

    @Test
    void handleAlipayCallbackVerifiesRsa2SignatureWithRuntimePublicKey() throws Exception {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        PayCallbackSignatureSupport signatureSupport = new PayCallbackSignatureSupport();
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                runtimeConfigService,
                signatureSupport,
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        KeyPair keyPair = rsaKeyPair();
        Map<String, Object> payload = alipayCallbackPayload();
        payload.put("sign", rsaSign(signatureSupport.alipayCanonicalPayload(payload), keyPair));
        PayPaymentRecord payment = pendingPayment();
        payment.setChannel(2);
        payment.setThirdPayNo(null);
        when(runtimeConfigService.callbackRequireSignature()).thenReturn(true);
        when(runtimeConfigService.alipayPublicKey()).thenReturn(publicKeyPem(keyPair));
        when(recordMapper.selectOne(any())).thenReturn(payment);

        service.handleCallback("alipay", payload, Collections.emptyMap());

        verify(recordMapper).updateById(argThat(update ->
                payment.getId().equals(update.getId())
                        && Integer.valueOf(1).equals(update.getStatus())
                        && "2026052222001400000001".equals(update.getThirdPayNo())));
        verify(orderService).markPaid(payment.getOrderId(), payment.getChannel());
        verify(callbackRecordMapper).insert(argThat(callback ->
                Integer.valueOf(1).equals(callback.getSignatureStatus())
                        && Integer.valueOf(1).equals(callback.getProcessStatus())
                        && "TRADE_SUCCESS".equals(callback.getEventType())));
    }

    @Test
    void handleAlipayCallbackRejectsMismatchedRuntimeAppId() throws Exception {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        PayCallbackSignatureSupport signatureSupport = new PayCallbackSignatureSupport();
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                runtimeConfigService,
                signatureSupport,
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        KeyPair keyPair = rsaKeyPair();
        Map<String, Object> payload = alipayCallbackPayload();
        payload.put("sign", rsaSign(signatureSupport.alipayCanonicalPayload(payload), keyPair));
        PayPaymentRecord payment = pendingPayment();
        payment.setChannel(2);
        when(runtimeConfigService.callbackRequireSignature()).thenReturn(true);
        when(runtimeConfigService.alipayPublicKey()).thenReturn(publicKeyPem(keyPair));
        when(runtimeConfigService.alipayAppId()).thenReturn("2021000999999999");
        when(recordMapper.selectOne(any())).thenReturn(payment);

        BizException error = assertThrows(BizException.class,
                () -> service.handleCallback("alipay", payload, Collections.emptyMap()));

        assertTrue(error.getMessage().contains("支付宝回调 AppID"));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderService, never()).markPaid(any(), any());
        verify(callbackRecordMapper).insert(argThat(callback ->
                Integer.valueOf(3).equals(callback.getProcessStatus())
                        && callback.getErrorMessage().contains("支付宝回调 AppID")));
    }

    @Test
    void handleAlipayCallbackRejectsMismatchedAmountAfterSignaturePasses() throws Exception {
        OrderService orderService = mock(OrderService.class);
        PayPaymentRecordMapper recordMapper = mock(PayPaymentRecordMapper.class);
        PayCallbackRecordMapper callbackRecordMapper = mock(PayCallbackRecordMapper.class);
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        PayCallbackSignatureSupport signatureSupport = new PayCallbackSignatureSupport();
        PayServiceImpl service = new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                runtimeConfigService,
                signatureSupport,
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);

        KeyPair keyPair = rsaKeyPair();
        Map<String, Object> payload = alipayCallbackPayload();
        payload.put("total_amount", "98.99");
        payload.put("sign", rsaSign(signatureSupport.alipayCanonicalPayload(payload), keyPair));
        PayPaymentRecord payment = pendingPayment();
        payment.setChannel(2);
        when(runtimeConfigService.callbackRequireSignature()).thenReturn(true);
        when(runtimeConfigService.alipayPublicKey()).thenReturn(publicKeyPem(keyPair));
        when(runtimeConfigService.alipayAppId()).thenReturn("2021000123456789");
        when(recordMapper.selectOne(any())).thenReturn(payment);

        BizException error = assertThrows(BizException.class,
                () -> service.handleCallback("alipay", payload, Collections.emptyMap()));

        assertTrue(error.getMessage().contains("回调金额 98.99 与支付流水金额 99.00 不一致"));
        verify(recordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(orderService, never()).markPaid(any(), any());
        verify(callbackRecordMapper).insert(argThat(callback ->
                Integer.valueOf(3).equals(callback.getProcessStatus())
                        && Integer.valueOf(1).equals(callback.getSignatureStatus())));
    }

    private PayCallbackRecord failedCallback() {
        PayCallbackRecord record = new PayCallbackRecord();
        record.setId(10L);
        record.setCallbackNo("CBMOCK202605210001");
        record.setChannel(9);
        record.setChannelName("mock");
        record.setPayNo("P202605210001");
        record.setThirdPayNo("MOCK-P202605210001");
        record.setNotifyId("N202605210001");
        record.setEventType("SUCCESS");
        record.setAmount(new BigDecimal("99.00"));
        record.setSignatureStatus(0);
        record.setProcessStatus(3);
        record.setRetryCount(1);
        record.setErrorMessage("订单服务短暂不可用");
        record.setRawData("{\"payNo\":\"P202605210001\",\"thirdPayNo\":\"MOCK-P202605210001\",\"notifyId\":\"N202605210001\",\"eventType\":\"SUCCESS\",\"amount\":\"99.00\"}");
        record.setRequestHeaders("{}");
        return record;
    }

    private PayPaymentRecord pendingPayment() {
        PayPaymentRecord payment = new PayPaymentRecord();
        payment.setId(20L);
        payment.setOrderId(30L);
        payment.setOrderNo("202605210001");
        payment.setUserId(1L);
        payment.setPayNo("P202605210001");
        payment.setThirdPayNo("MOCK-P202605210001");
        payment.setChannel(9);
        payment.setAmount(new BigDecimal("99.00"));
        payment.setStatus(0);
        return payment;
    }

    private OmsOrder paidOrder() {
        OmsOrder order = new OmsOrder();
        order.setId(30L);
        order.setOrderNo("202605210001");
        order.setUserId(1L);
        order.setPayAmount(new BigDecimal("99.00"));
        order.setStatus(1);
        return order;
    }

    private OmsOrder pendingOrder() {
        OmsOrder order = new OmsOrder();
        order.setId(30L);
        order.setOrderNo("202605210001");
        order.setUserId(1L);
        order.setPayAmount(new BigDecimal("99.00"));
        order.setStatus(0);
        return order;
    }

    private Map<String, Object> callbackPayload(String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("payNo", "P202605210001");
        payload.put("thirdPayNo", "MOCK-P202605210001");
        payload.put("notifyId", "N202605210001");
        payload.put("eventType", eventType);
        payload.put("amount", "99.00");
        return payload;
    }

    private Map<String, Object> alipayCallbackPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app_id", "2021000123456789");
        payload.put("out_trade_no", "P202605210001");
        payload.put("trade_no", "2026052222001400000001");
        payload.put("notify_id", "2026052200222000000001");
        payload.put("trade_status", "TRADE_SUCCESS");
        payload.put("total_amount", "99.00");
        payload.put("sign_type", "RSA2");
        return payload;
    }

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String publicKeyPem(KeyPair keyPair) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private String rsaSign(String content, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private PayServiceImpl newPayService(OrderService orderService,
                                         PayPaymentRecordMapper recordMapper,
                                         PayCallbackRecordMapper callbackRecordMapper,
                                         PayRuntimeConfigService runtimeConfigService) {
        return new PayServiceImpl(
                Collections.<PayStrategy>emptyList(),
                orderService,
                recordMapper,
                callbackRecordMapper,
                new PayCallbackProperties(),
                runtimeConfigService,
                new PayCallbackSignatureSupport(),
                new PayCallbackEventSupport(),
                new ObjectMapper(),
                transactionManager);
    }

    private PayChannelVO channel(List<PayChannelVO> channels, String name) {
        return channels.stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing channel " + name));
    }
}
