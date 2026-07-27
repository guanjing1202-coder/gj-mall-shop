package com.gj.mall.pay.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.Constant;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WechatPayCallbackParser {

    private final PayRuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;

    public WechatPayCallbackPayload parse(String rawBody, Map<String, String> headers) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, "微信支付回调原始报文不能为空");
        }
        try {
            String eventId = eventId(rawBody);
            Transaction transaction = parser().parse(requestParam(rawBody, headers), Transaction.class);
            return WechatPayCallbackPayload.from(eventId, transaction);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.PAY_FAIL, "微信支付回调验签或解密失败：" + ex.getMessage());
        }
    }

    private NotificationParser parser() {
        Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(runtimeConfigService.wechatMchId())
                .privateKeyFromPath(runtimeConfigService.wechatPrivateKeyPath())
                .merchantSerialNumber(runtimeConfigService.wechatMerchantSerialNo())
                .apiV3Key(runtimeConfigService.wechatApiV3Key())
                .build();
        return new NotificationParser((com.wechat.pay.java.core.notification.NotificationConfig) config);
    }

    private RequestParam requestParam(String rawBody, Map<String, String> headers) {
        Map<String, String> safeHeaders = normalizedHeaders(headers);
        return new RequestParam.Builder()
                .serialNumber(requiredHeader(safeHeaders, "wechatpay-serial"))
                .timestamp(requiredHeader(safeHeaders, "wechatpay-timestamp"))
                .nonce(requiredHeader(safeHeaders, "wechatpay-nonce"))
                .signature(requiredHeader(safeHeaders, "wechatpay-signature"))
                .signType(firstNonBlank(safeHeaders.get("wechatpay-signature-type"), Constant.RSA_SIGN_TYPE))
                .body(rawBody)
                .build();
    }

    private String eventId(String rawBody) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode id = root.get("id");
        return id == null || id.asText().trim().isEmpty() ? null : id.asText().trim();
    }

    private Map<String, String> normalizedHeaders(Map<String, String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        if (headers == null) {
            return result;
        }
        headers.forEach((key, value) -> {
            if (key != null && value != null) {
                result.put(key.toLowerCase(Locale.ROOT), value);
            }
        });
        return result;
    }

    private String requiredHeader(Map<String, String> headers, String key) {
        String value = headers.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少微信支付回调请求头：" + key);
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
