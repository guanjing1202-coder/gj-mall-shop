package com.gj.mall.pay.support;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.pay.config.PayCallbackProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PayCallbackSignatureSupport {

    public Verification verify(Map<String, Object> payload, Map<String, String> headers, PayCallbackProperties properties) {
        Map<String, Object> safePayload = payload == null ? new LinkedHashMap<>() : payload;
        Map<String, String> safeHeaders = headers == null ? new LinkedHashMap<>() : headers;
        PayCallbackProperties safeProperties = properties == null ? new PayCallbackProperties() : properties;
        String signature = firstSignature(safePayload, safeHeaders);
        if ((signature == null || signature.trim().isEmpty()) && !safeProperties.isRequireSignature()) {
            return new Verification(0, "未提供签名，开发环境已跳过验签");
        }
        if (signature == null || signature.trim().isEmpty()) {
            return new Verification(2, "缺少支付回调签名");
        }
        String expected = sign(safePayload, safeProperties.getSecret());
        boolean matched = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8));
        return matched ? new Verification(1, "签名校验通过") : new Verification(2, "支付回调签名不匹配");
    }

    public String sign(Map<String, Object> payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(firstNonBlank(secret, "").getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(canonicalPayload(payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new BizException(ResultCode.PAY_FAIL, "支付回调签名计算失败");
        }
    }

    public String canonicalPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        return payload.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !"signature".equalsIgnoreCase(entry.getKey()) && !"sign".equalsIgnoreCase(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String firstSignature(Map<String, Object> payload, Map<String, String> headers) {
        String signature = firstText(payload, "signature", "sign");
        if (signature != null && !signature.trim().isEmpty()) {
            return signature;
        }
        return firstHeader(headers, "x-gj-pay-signature", "x-pay-signature", "signature");
    }

    private String firstText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private String firstHeader(Map<String, String> headers, String... keys) {
        for (String key : keys) {
            String expected = key.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(expected)) {
                    String value = entry.getValue();
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public static class Verification {
        private final int status;
        private final String message;

        public Verification(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int status() {
            return status;
        }

        public String message() {
            return message;
        }
    }
}
