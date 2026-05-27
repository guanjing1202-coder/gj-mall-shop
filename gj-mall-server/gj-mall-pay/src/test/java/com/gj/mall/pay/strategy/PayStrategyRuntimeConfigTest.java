package com.gj.mall.pay.strategy;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.gateway.AlipayAdapter;
import com.gj.mall.pay.gateway.WechatPayAdapter;
import com.gj.mall.pay.vo.PayResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayStrategyRuntimeConfigTest {

    @Test
    void wechatStrategyChecksRuntimePayMode() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("mock");
        WechatPayStrategy strategy = new WechatPayStrategy(runtimeConfigService, mock(WechatPayAdapter.class));

        BizException error = assertThrows(BizException.class, () -> strategy.pay(null, "P1"));

        assertTrue(error.getMessage().contains("当前 mall.pay.mode=mock"));
    }

    @Test
    void alipayStrategyChecksRuntimePayMode() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("mock");
        AlipayStrategy strategy = new AlipayStrategy(runtimeConfigService, mock(AlipayAdapter.class));

        BizException error = assertThrows(BizException.class, () -> strategy.pay(null, "P1"));

        assertTrue(error.getMessage().contains("当前 mall.pay.mode=mock"));
    }

    @Test
    void wechatStrategyDelegatesRealPayToAdapter() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("real");
        WechatPayAdapter adapter = mock(WechatPayAdapter.class);
        OmsOrder order = order("202605270001", "88.00");
        PayResultVO expected = pendingResult("wechat", "wx://pay");
        when(adapter.createPayment(order, "P1")).thenReturn(expected);
        WechatPayStrategy strategy = new WechatPayStrategy(runtimeConfigService, adapter);

        PayResultVO result = strategy.pay(order, "P1");

        assertEquals("wechat", result.getChannel());
        assertEquals("P1", result.getPayNo());
        assertFalse(result.getPaid());
        assertEquals("wx://pay", result.getPayInfo());
    }

    @Test
    void alipayStrategyDelegatesRealPayToAdapter() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.mode()).thenReturn("real");
        AlipayAdapter adapter = mock(AlipayAdapter.class);
        OmsOrder order = order("202605270002", "66.00");
        PayResultVO expected = pendingResult("alipay", "alipay://pay");
        when(adapter.createPayment(order, "P2")).thenReturn(expected);
        AlipayStrategy strategy = new AlipayStrategy(runtimeConfigService, adapter);

        PayResultVO result = strategy.pay(order, "P2");

        assertEquals("alipay", result.getChannel());
        assertEquals("P2", result.getPayNo());
        assertFalse(result.getPaid());
        assertEquals("alipay://pay", result.getPayInfo());
    }

    private OmsOrder order(String orderNo, String amount) {
        OmsOrder order = new OmsOrder();
        order.setOrderNo(orderNo);
        order.setPayAmount(new BigDecimal(amount));
        return order;
    }

    private PayResultVO pendingResult(String channel, String payInfo) {
        PayResultVO vo = new PayResultVO();
        vo.setChannel(channel);
        vo.setPayNo(channel.equals("wechat") ? "P1" : "P2");
        vo.setPaid(false);
        vo.setPayInfo(payInfo);
        return vo;
    }
}
