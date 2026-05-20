package com.gj.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.admin.dto.AdminAfterSaleActionDTO;
import com.gj.mall.admin.vo.AdminAfterSaleVO;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.mapper.PayRefundRecordMapper;
import com.gj.mall.order.service.AfterSaleRuleService;
import com.gj.mall.user.mapper.UmsUserMapper;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAfterSaleServiceImplTest {

    @Test
    void refundCreatesRefundRecordAndLinksItToAfterSale() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        PayPaymentRecordMapper paymentRecordMapper = mock(PayPaymentRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        AfterSaleRuleService ruleService = mock(AfterSaleRuleService.class);
        AdminAfterSaleServiceImpl service = new AdminAfterSaleServiceImpl(
                afterSaleMapper,
                orderMapper,
                orderItemMapper,
                paymentRecordMapper,
                refundRecordMapper,
                userMapper,
                messageService,
                ruleService);

        OmsAfterSale afterSale = waitRefundAfterSale();
        OmsOrder order = paidOrder();
        PayPaymentRecord payment = paidPayment();
        AtomicReference<PayRefundRecord> insertedRefund = new AtomicReference<>();

        when(afterSaleMapper.selectById(100L)).thenReturn(afterSale);
        when(orderMapper.selectById(afterSale.getOrderId())).thenReturn(order);
        when(paymentRecordMapper.selectOne(any(Wrapper.class))).thenReturn(payment);
        when(refundRecordMapper.insert(any(PayRefundRecord.class))).thenAnswer(invocation -> {
            PayRefundRecord record = invocation.getArgument(0);
            record.setId(900L);
            insertedRefund.set(record);
            return 1;
        });

        AdminAfterSaleActionDTO dto = new AdminAfterSaleActionDTO();
        dto.setAuditRemark("验收通过，全额退款");

        service.refund(100L, dto);

        PayRefundRecord refund = insertedRefund.get();
        assertNotNull(refund);
        assertEquals(afterSale.getId(), refund.getAfterSaleId());
        assertEquals(afterSale.getAfterSaleNo(), refund.getAfterSaleNo());
        assertEquals(payment.getId(), refund.getPaymentId());
        assertEquals(payment.getPayNo(), refund.getPayNo());
        assertEquals(payment.getChannel(), refund.getChannel());
        assertEquals(afterSale.getAmount(), refund.getAmount());
        assertEquals(Integer.valueOf(1), refund.getStatus());
        assertEquals("验收通过，全额退款", refund.getReason());

        verify(paymentRecordMapper).updateById(argThat(update ->
                payment.getId().equals(update.getId()) && Integer.valueOf(3).equals(update.getStatus())));
        verify(afterSaleMapper).updateById(argThat(update ->
                afterSale.getId().equals(update.getId())
                        && Integer.valueOf(4).equals(update.getStatus())
                        && Long.valueOf(900L).equals(update.getRefundPaymentId())
                        && update.getRefundTime() != null));
        verify(orderMapper).updateById(argThat(update ->
                order.getId().equals(update.getId()) && Integer.valueOf(6).equals(update.getStatus())));
    }

    @Test
    void refundRejectsWhenRefundRecordAlreadyExists() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        PayPaymentRecordMapper paymentRecordMapper = mock(PayPaymentRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        AfterSaleRuleService ruleService = mock(AfterSaleRuleService.class);
        AdminAfterSaleServiceImpl service = new AdminAfterSaleServiceImpl(
                afterSaleMapper,
                orderMapper,
                orderItemMapper,
                paymentRecordMapper,
                refundRecordMapper,
                userMapper,
                messageService,
                ruleService);

        OmsAfterSale afterSale = waitRefundAfterSale();
        PayRefundRecord existing = new PayRefundRecord();
        existing.setId(901L);
        when(afterSaleMapper.selectById(100L)).thenReturn(afterSale);
        when(refundRecordMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        BizException error = assertThrows(BizException.class, () -> service.refund(100L, new AdminAfterSaleActionDTO()));

        assertTrue(error.getMessage().contains("退款记录已存在"));
        verify(paymentRecordMapper, never()).updateById(any(PayPaymentRecord.class));
        verify(afterSaleMapper, never()).updateById(any(OmsAfterSale.class));
    }

    @Test
    void detailIncludesRefundRecord() {
        OmsAfterSaleMapper afterSaleMapper = mock(OmsAfterSaleMapper.class);
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        PayPaymentRecordMapper paymentRecordMapper = mock(PayPaymentRecordMapper.class);
        PayRefundRecordMapper refundRecordMapper = mock(PayRefundRecordMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        UserMessageService messageService = mock(UserMessageService.class);
        AfterSaleRuleService ruleService = mock(AfterSaleRuleService.class);
        AdminAfterSaleServiceImpl service = new AdminAfterSaleServiceImpl(
                afterSaleMapper,
                orderMapper,
                orderItemMapper,
                paymentRecordMapper,
                refundRecordMapper,
                userMapper,
                messageService,
                ruleService);

        OmsAfterSale afterSale = waitRefundAfterSale();
        afterSale.setStatus(4);
        afterSale.setRefundPaymentId(900L);
        PayRefundRecord refund = new PayRefundRecord();
        refund.setId(900L);
        refund.setRefundNo("RF202605200001");
        refund.setAfterSaleId(afterSale.getId());
        refund.setAfterSaleNo(afterSale.getAfterSaleNo());
        refund.setStatus(1);
        refund.setAmount(afterSale.getAmount());
        refund.setReason("售后退款");
        refund.setSuccessTime(LocalDateTime.now());

        when(afterSaleMapper.selectById(100L)).thenReturn(afterSale);
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(paidOrder()));
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(refundRecordMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(refund));

        AdminAfterSaleVO detail = service.detail(100L);

        assertNotNull(detail.getRefundRecord());
        assertEquals("RF202605200001", detail.getRefundRecord().getRefundNo());
        assertEquals("退款成功", detail.getRefundRecord().getStatusDesc());
        assertEquals(afterSale.getAmount(), detail.getRefundRecord().getAmount());
    }

    private OmsAfterSale waitRefundAfterSale() {
        OmsAfterSale afterSale = new OmsAfterSale();
        afterSale.setId(100L);
        afterSale.setAfterSaleNo("AS202605200001");
        afterSale.setOrderId(200L);
        afterSale.setOrderNo("202605200001");
        afterSale.setUserId(1L);
        afterSale.setType(1);
        afterSale.setAmount(new BigDecimal("99.00"));
        afterSale.setReason("商品问题");
        afterSale.setOrderStatusSnapshot(3);
        afterSale.setStatus(2);
        afterSale.setCreateTime(LocalDateTime.now().minusHours(1));
        return afterSale;
    }

    private OmsOrder paidOrder() {
        OmsOrder order = new OmsOrder();
        order.setId(200L);
        order.setOrderNo("202605200001");
        order.setUserId(1L);
        order.setPayAmount(new BigDecimal("99.00"));
        order.setStatus(5);
        return order;
    }

    private PayPaymentRecord paidPayment() {
        PayPaymentRecord payment = new PayPaymentRecord();
        payment.setId(300L);
        payment.setOrderId(200L);
        payment.setOrderNo("202605200001");
        payment.setUserId(1L);
        payment.setPayNo("PAY202605200001");
        payment.setThirdPayNo("THIRD202605200001");
        payment.setChannel(9);
        payment.setAmount(new BigDecimal("99.00"));
        payment.setStatus(1);
        payment.setPayTime(LocalDateTime.now().minusHours(2));
        return payment;
    }
}
