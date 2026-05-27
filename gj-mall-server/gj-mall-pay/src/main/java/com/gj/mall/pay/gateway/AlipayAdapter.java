package com.gj.mall.pay.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.vo.PayResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class AlipayAdapter implements ChannelPayAdapter {

    private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String METHOD = "alipay.trade.page.pay";
    private static final String VERSION = "1.0";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PayRuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public PayResultVO createPayment(OmsOrder order, String payNo) {
        assertOrder(order);
        assertText(payNo, "payNo");

        String appId = runtimeConfigService.alipayAppId();
        String privateKey = runtimeConfigService.alipayPrivateKey();
        String notifyUrl = runtimeConfigService.alipayNotifyUrl();
        assertRequiredConfig(appId, privateKey, notifyUrl);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId.trim());
        params.put("method", METHOD);
        params.put("charset", CHARSET);
        params.put("sign_type", SIGN_TYPE);
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        params.put("version", VERSION);
        params.put("notify_url", notifyUrl.trim());
        params.put("biz_content", bizContent(order, payNo));
        params.put("sign", sign(params, privateKey));

        PayResultVO result = new PayResultVO();
        result.setChannel(PayChannel.ALIPAY.getName());
        result.setPayNo(payNo);
        result.setPaid(false);
        result.setAmount(order.getPayAmount());
        result.setPayInfo(buildAutoSubmitForm(params));
        return result;
    }

    private void assertOrder(OmsOrder order) {
        if (order == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "订单不能为空");
        }
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.ORDER_AMOUNT_ERROR, "订单支付金额异常");
        }
    }

    private void assertText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, name + " 不能为空");
        }
    }

    private void assertRequiredConfig(String appId, String privateKey, String notifyUrl) {
        StringJoiner missing = new StringJoiner("、");
        if (isBlank(appId)) {
            missing.add("mall.pay.alipay.app-id");
        }
        if (isBlank(privateKey)) {
            missing.add("mall.pay.alipay.private-key");
        }
        if (isBlank(notifyUrl)) {
            missing.add("mall.pay.alipay.notify-url");
        }
        String missingText = missing.toString();
        if (!missingText.isEmpty()) {
            throw new BizException(ResultCode.PAY_FAIL, "支付宝支付配置不完整：" + missingText);
        }
    }

    private String bizContent(OmsOrder order, String payNo) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", payNo);
        biz.put("total_amount", amountText(order.getPayAmount()));
        biz.put("subject", "GJ Mall 订单 " + safeText(order.getOrderNo(), payNo));
        biz.put("product_code", PRODUCT_CODE);
        try {
            return objectMapper.writeValueAsString(biz);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.PAY_FAIL, "支付宝支付参数生成失败");
        }
    }

    private String amountText(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String sign(Map<String, String> params, String privateKeyText) {
        try {
            PrivateKey privateKey = parsePrivateKey(privateKeyText);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signContent(params).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BizException(ResultCode.PAY_FAIL, "支付宝应用私钥无效，无法生成 RSA2 签名");
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyText) throws Exception {
        String normalized = privateKeyText
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private String signContent(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("sign".equals(entry.getKey()) || isBlank(entry.getValue())) {
                continue;
            }
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    private String buildAutoSubmitForm(Map<String, String> params) {
        StringBuilder form = new StringBuilder();
        form.append("<form id=\"alipaySubmit\" name=\"alipaySubmit\" action=\"")
                .append(htmlEscape(GATEWAY_URL))
                .append("\" method=\"post\">");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            form.append("<input type=\"hidden\" name=\"")
                    .append(htmlEscape(entry.getKey()))
                    .append("\" value=\"")
                    .append(htmlEscape(entry.getValue()))
                    .append("\" />");
        }
        form.append("</form>");
        form.append("<script>document.forms['alipaySubmit'].submit();</script>");
        return form.toString();
    }

    private String htmlEscape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String safeText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
