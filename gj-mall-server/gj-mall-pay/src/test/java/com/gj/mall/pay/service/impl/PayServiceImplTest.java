package com.gj.mall.pay.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.config.PayCallbackProperties;
import com.gj.mall.pay.support.PayCallbackSignatureSupport;
import com.gj.mall.pay.strategy.PayStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.Collections;

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
                new PayCallbackSignatureSupport(),
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
                new PayCallbackSignatureSupport(),
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
}
