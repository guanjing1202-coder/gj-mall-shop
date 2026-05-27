package com.gj.mall.pay.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.vo.PayResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelPayAdapterTest {

    @Test
    void wechatAdapterFailsClearlyBeforeSdkIsConnected() {
        WechatPayAdapter adapter = new WechatPayAdapter();

        BizException error = assertThrows(BizException.class, () -> adapter.createPayment(null, "P1"));

        assertTrue(error.getMessage().contains("微信支付 SDK 尚未接入"));
    }

    @Test
    void alipayAdapterBuildsSandboxPagePayFormWhenRuntimeConfigIsReady() throws Exception {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.alipayAppId()).thenReturn("2021000123456789");
        when(runtimeConfigService.alipayPrivateKey()).thenReturn(privateKeyPem(rsaKeyPair()));
        when(runtimeConfigService.alipayNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/alipay");
        AlipayAdapter adapter = new AlipayAdapter(runtimeConfigService, new ObjectMapper());

        PayResultVO result = adapter.createPayment(order("202605270001", "199.90"), "P2026052700010001");

        assertEquals("alipay", result.getChannel());
        assertEquals("P2026052700010001", result.getPayNo());
        assertFalse(result.getPaid());
        assertEquals(new BigDecimal("199.90"), result.getAmount());
        assertTrue(result.getPayInfo().contains("https://openapi-sandbox.dl.alipaydev.com/gateway.do"));

        Map<String, String> form = hiddenInputs(result.getPayInfo());
        assertEquals("2021000123456789", form.get("app_id"));
        assertEquals("alipay.trade.page.pay", form.get("method"));
        assertEquals("RSA2", form.get("sign_type"));
        assertEquals("https://shop.guanjing.cloud/api/pay/callback/alipay", form.get("notify_url"));
        assertNotNull(form.get("timestamp"));
        assertNotNull(form.get("sign"));
        assertTrue(htmlUnescape(form.get("biz_content")).contains("\"out_trade_no\":\"P2026052700010001\""));
        assertTrue(htmlUnescape(form.get("biz_content")).contains("\"total_amount\":\"199.90\""));
    }

    @Test
    void alipayAdapterFailsClearlyWhenRequiredRuntimeConfigIsMissing() {
        PayRuntimeConfigService runtimeConfigService = mock(PayRuntimeConfigService.class);
        when(runtimeConfigService.alipayAppId()).thenReturn("");
        when(runtimeConfigService.alipayPrivateKey()).thenReturn("private-key");
        when(runtimeConfigService.alipayNotifyUrl()).thenReturn("https://shop.guanjing.cloud/api/pay/callback/alipay");
        AlipayAdapter adapter = new AlipayAdapter(runtimeConfigService, new ObjectMapper());

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

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String privateKeyPem(KeyPair keyPair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private Map<String, String> hiddenInputs(String html) {
        Pattern pattern = Pattern.compile("<input type=\"hidden\" name=\"([^\"]+)\" value=\"([^\"]*)\" />");
        Matcher matcher = pattern.matcher(html);
        Map<String, String> values = new LinkedHashMap<>();
        while (matcher.find()) {
            values.put(matcher.group(1), htmlUnescape(matcher.group(2)));
        }
        return values;
    }

    private String htmlUnescape(String value) {
        return value == null ? null : value
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
