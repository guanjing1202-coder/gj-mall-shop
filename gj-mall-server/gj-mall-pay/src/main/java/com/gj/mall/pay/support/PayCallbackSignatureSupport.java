package com.gj.mall.pay.support;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.config.PayCallbackProperties;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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

    public Verification verify(PayChannel channel,
                               Map<String, Object> payload,
                               Map<String, String> headers,
                               PayCallbackProperties properties,
                               PayRuntimeConfigService runtimeConfigService) {
        if (PayChannel.ALIPAY.equals(channel)) {
            return verifyAlipay(payload, headers, properties, runtimeConfigService);
        }
        return verify(payload, headers, properties);
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

    public String alipayCanonicalPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        return payload.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private Verification verifyAlipay(Map<String, Object> payload,
                                      Map<String, String> headers,
                                      PayCallbackProperties properties,
                                      PayRuntimeConfigService runtimeConfigService) {
        Map<String, Object> safePayload = payload == null ? new LinkedHashMap<>() : payload;
        Map<String, String> safeHeaders = headers == null ? new LinkedHashMap<>() : headers;
        PayCallbackProperties safeProperties = properties == null ? new PayCallbackProperties() : properties;
        String signature = firstSignature(safePayload, safeHeaders);
        if ((signature == null || signature.trim().isEmpty()) && !safeProperties.isRequireSignature()) {
            return new Verification(0, "未提供签名，开发环境已跳过验签");
        }
        if (signature == null || signature.trim().isEmpty()) {
            return new Verification(2, "缺少支付宝回调签名");
        }
        String publicKeyText = runtimeConfigService == null ? null : runtimeConfigService.alipayPublicKey();
        if (publicKeyText == null || publicKeyText.trim().isEmpty()) {
            return new Verification(2, "缺少支付宝公钥，无法验签");
        }
        String signType = firstText(safePayload, "sign_type");
        if (signType != null && !"RSA2".equalsIgnoreCase(signType.trim())) {
            return new Verification(2, "支付宝签名类型仅支持 RSA2");
        }
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(parsePublicKey(publicKeyText));
            verifier.update(alipayCanonicalPayload(safePayload).getBytes(StandardCharsets.UTF_8));
            boolean matched = verifier.verify(Base64.getDecoder().decode(signature.trim()));
            return matched
                    ? new Verification(1, "支付宝 RSA2 签名校验通过")
                    : new Verification(2, "支付宝 RSA2 签名不匹配");
        } catch (Exception ex) {
            return new Verification(2, "支付宝 RSA2 签名校验失败");
        }
    }

    private PublicKey parsePublicKey(String publicKeyText) throws Exception {
        String normalized = publicKeyText
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
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
