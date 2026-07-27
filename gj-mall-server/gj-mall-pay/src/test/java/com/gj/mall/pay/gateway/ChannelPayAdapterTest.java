package com.gj.mall.pay.gateway;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.vo.PayResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelPayAdapterTest {

    @Test
    void wechatAdapterReturnsNativeCodeUrlWhenRuntimeConfigIsReady() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.wechatAppId()).thenReturn("wx1234567890");
        when(runtimeConfigService.wechatMchId()).thenReturn("1900000109");
        when(runtimeConfigService.wechatApiV3Key()).thenReturn("0123456789abcdef0123456789abcdef");
        when(runtimeConfigService.wechatMerchantSerialNo()).thenReturn("77777777777777777777777777777777");
        when(runtimeConfigService.wechatPrivateKeyPath()).thenReturn("D:/certs/apiclient_key.pem");
        when(runtimeConfigService.wechatNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/wechat");
        WechatNativePayClient nativePayClient = mock(WechatNativePayClient.class);
        when(nativePayClient.prepay(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WechatNativePayResponse("weixin://wxpay/bizpayurl?pr=abc123"));
        WechatPayAdapter adapter = new WechatPayAdapter(runtimeConfigService, nativePayClient);

        PayResultVO result = adapter.createPayment(order("202605270001", "199.90"), "P2026052700010001");

        assertEquals("wechat", result.getChannel());
        assertEquals("P2026052700010001", result.getPayNo());
        assertFalse(result.getPaid());
        assertEquals(new BigDecimal("199.90"), result.getAmount());
        assertEquals("weixin://wxpay/bizpayurl?pr=abc123", result.getPayInfo());
        verify(nativePayClient).prepay(org.mockito.ArgumentMatchers.argThat(request ->
                "wx1234567890".equals(request.getAppId())
                        && "1900000109".equals(request.getMchId())
                        && "P2026052700010001".equals(request.getOutTradeNo())
                        && Integer.valueOf(19990).equals(request.getTotalFee())
                        && "CNY".equals(request.getCurrency())
                        && "https://shop.guanjing.cloud/api/pay/callback/wechat".equals(request.getNotifyUrl())
                        && request.getDescription().contains("202605270001")));
    }

    @Test
    void wechatAdapterFailsClearlyWhenRequiredRuntimeConfigIsMissing() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.wechatAppId()).thenReturn("");
        when(runtimeConfigService.wechatMchId()).thenReturn("1900000109");
        when(runtimeConfigService.wechatApiV3Key()).thenReturn("0123456789abcdef0123456789abcdef");
        when(runtimeConfigService.wechatMerchantSerialNo()).thenReturn("77777777777777777777777777777777");
        when(runtimeConfigService.wechatPrivateKeyPath()).thenReturn("D:/certs/apiclient_key.pem");
        when(runtimeConfigService.wechatNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/wechat");
        WechatPayAdapter adapter = new WechatPayAdapter(runtimeConfigService, mock(WechatNativePayClient.class));

        BizException error = assertThrows(BizException.class,
                () -> adapter.createPayment(order("202605270002", "66.00"), "P2026052700020001"));

        assertTrue(error.getMessage().contains("微信支付配置不完整"));
        assertTrue(error.getMessage().contains("mall.pay.wechat.app-id"));
    }

    @Test
    void alipayAdapterReturnsSdkPagePayFormWhenRuntimeConfigIsReady() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.alipayAppId()).thenReturn("2021000123456789");
        when(runtimeConfigService.alipayPrivateKey()).thenReturn("app-private-key");
        when(runtimeConfigService.alipayPublicKey()).thenReturn("alipay-public-key");
        when(runtimeConfigService.alipayNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/alipay");
        AlipayPagePayClient pagePayClient = mock(AlipayPagePayClient.class);
        when(pagePayClient.pagePay(org.mockito.ArgumentMatchers.any()))
                .thenReturn("<form id=\"alipaySubmit\">sdk-page-pay-form</form>");
        AlipayAdapter adapter = new AlipayAdapter(runtimeConfigService, pagePayClient);

        PayResultVO result = adapter.createPayment(order("202605270001", "199.90"), "P2026052700010001");

        assertEquals("alipay", result.getChannel());
        assertEquals("P2026052700010001", result.getPayNo());
        assertFalse(result.getPaid());
        assertEquals(new BigDecimal("199.90"), result.getAmount());
        assertEquals("<form id=\"alipaySubmit\">sdk-page-pay-form</form>", result.getPayInfo());
        verify(pagePayClient).pagePay(org.mockito.ArgumentMatchers.argThat(request ->
                "2021000123456789".equals(request.getAppId())
                        && "app-private-key".equals(request.getPrivateKey())
                        && "alipay-public-key".equals(request.getAlipayPublicKey())
                        && "https://shop.guanjing.cloud/api/pay/callback/alipay".equals(request.getNotifyUrl())
                        && "P2026052700010001".equals(request.getOutTradeNo())
                        && "199.90".equals(request.getTotalAmount())
                        && request.getSubject().contains("202605270001")));
    }

    @Test
    void alipayAdapterFailsClearlyWhenRequiredRuntimeConfigIsMissing() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.alipayAppId()).thenReturn("");
        when(runtimeConfigService.alipayPrivateKey()).thenReturn("private-key");
        when(runtimeConfigService.alipayPublicKey()).thenReturn("public-key");
        when(runtimeConfigService.alipayNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/alipay");
        AlipayAdapter adapter = new AlipayAdapter(runtimeConfigService, mock(AlipayPagePayClient.class));

        BizException error = assertThrows(BizException.class,
                () -> adapter.createPayment(order("202605270002", "66.00"), "P2026052700020001"));

        assertTrue(error.getMessage().contains("支付宝支付配置不完整"));
        assertTrue(error.getMessage().contains("mall.pay.alipay.app-id"));
    }

    private OmsOrder order(String orderNo, String amount) {
        OmsOrder order = new OmsOrder();
        order.setOrderNo(orderNo);
        order.setPayAmount(new BigDecimal(amount));
        return order;
    }

}
