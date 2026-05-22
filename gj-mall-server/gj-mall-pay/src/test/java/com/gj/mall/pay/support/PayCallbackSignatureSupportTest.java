package com.gj.mall.pay.support;

import com.gj.mall.pay.config.PayCallbackProperties;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayCallbackSignatureSupportTest {

    @Test
    void signsSortedPayloadAndIgnoresSignatureFields() {
        PayCallbackSignatureSupport support = new PayCallbackSignatureSupport();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("thirdPayNo", "MOCK-P202605220001");
        payload.put("sign", "old-sign");
        payload.put("amount", "99.00");
        payload.put("payNo", "P202605220001");
        payload.put("notifyId", "N202605220001");
        payload.put("signature", "old-signature");
        payload.put("eventType", "SUCCESS");
        payload.put("empty", null);

        String signature = support.sign(payload, "gj-mall-dev-pay-callback-secret");

        assertEquals("99f8320173254d7fcad8a48a5f3565a26c8e31cc8976f1f9df6524fab183dd4e", signature);
        assertEquals("amount=99.00&eventType=SUCCESS&notifyId=N202605220001&payNo=P202605220001&thirdPayNo=MOCK-P202605220001",
                support.canonicalPayload(payload));
    }

    @Test
    void explainsUnsignedDevelopmentCallbackAsSkippedWhenSignatureIsOptional() {
        PayCallbackSignatureSupport support = new PayCallbackSignatureSupport();
        PayCallbackProperties properties = new PayCallbackProperties();
        properties.setRequireSignature(false);

        PayCallbackSignatureSupport.Verification verification = support.verify(payload("payNo", "P1"), Collections.emptyMap(), properties);

        assertEquals(0, verification.status());
        assertEquals("未提供签名，开发环境已跳过验签", verification.message());
    }

    @Test
    void verifiesSignatureFromHeadersCaseInsensitively() {
        PayCallbackSignatureSupport support = new PayCallbackSignatureSupport();
        PayCallbackProperties properties = new PayCallbackProperties();
        properties.setRequireSignature(true);

        Map<String, Object> payload = payload("payNo", "P1", "amount", "10.00");
        String signature = support.sign(payload, properties.getSecret());
        PayCallbackSignatureSupport.Verification verification = support.verify(
                payload,
                headers("X-GJ-Pay-Signature", signature),
                properties);

        assertEquals(1, verification.status());
        assertEquals("签名校验通过", verification.message());
    }

    private Map<String, Object> payload(String key, Object value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, value);
        return payload;
    }

    private Map<String, Object> payload(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key1, value1);
        payload.put(key2, value2);
        return payload;
    }

    private Map<String, String> headers(String key, String value) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(key, value);
        return headers;
    }
}
