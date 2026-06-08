package com.gj.mall.pay.gateway;

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
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
public class WechatPayAdapter implements ChannelPayAdapter {

    private static final String CURRENCY = "CNY";

    private final PayRuntimeConfigService runtimeConfigService;
    private final WechatNativePayClient nativePayClient;

    @Override
    public PayResultVO createPayment(OmsOrder order, String payNo) {
        assertOrder(order);
        assertText(payNo, "payNo");

        String appId = runtimeConfigService.wechatAppId();
        String mchId = runtimeConfigService.wechatMchId();
        String apiV3Key = runtimeConfigService.wechatApiV3Key();
        String merchantSerialNo = runtimeConfigService.wechatMerchantSerialNo();
        String privateKeyPath = runtimeConfigService.wechatPrivateKeyPath();
        String notifyUrl = runtimeConfigService.wechatNotifyUrl();
        assertRequiredConfig(appId, mchId, apiV3Key, merchantSerialNo, privateKeyPath, notifyUrl);

        WechatNativePayResponse response = nativePayClient.prepay(new WechatNativePayRequest(
                appId.trim(),
                mchId.trim(),
                apiV3Key.trim(),
                merchantSerialNo.trim(),
                privateKeyPath.trim(),
                notifyUrl.trim(),
                "GJ Mall 订单 " + safeText(order.getOrderNo(), payNo),
                payNo.trim(),
                totalFee(order.getPayAmount()),
                CURRENCY));
        if (response == null || isBlank(response.getCodeUrl())) {
            throw new BizException(ResultCode.PAY_FAIL, "微信 Native Pay 返回 code_url 为空");
        }

        PayResultVO result = new PayResultVO();
        result.setChannel(PayChannel.WECHAT.getName());
        result.setPayNo(payNo);
        result.setPaid(false);
        result.setAmount(order.getPayAmount());
        result.setPayInfo(response.getCodeUrl());
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
        if (isBlank(value)) {
            throw new BizException(ResultCode.PARAM_MISSING, name + " 不能为空");
        }
    }

    private void assertRequiredConfig(String appId,
                                      String mchId,
                                      String apiV3Key,
                                      String merchantSerialNo,
                                      String privateKeyPath,
                                      String notifyUrl) {
        StringJoiner missing = new StringJoiner("、");
        addMissing(missing, "mall.pay.wechat.app-id", appId);
        addMissing(missing, "mall.pay.wechat.mch-id", mchId);
        addMissing(missing, "mall.pay.wechat.api-v3-key", apiV3Key);
        addMissing(missing, "mall.pay.wechat.merchant-serial-no", merchantSerialNo);
        addMissing(missing, "mall.pay.wechat.private-key-path", privateKeyPath);
        addMissing(missing, "mall.pay.wechat.notify-url", notifyUrl);
        String missingText = missing.toString();
        if (!missingText.isEmpty()) {
            throw new BizException(ResultCode.PAY_FAIL, "微信支付配置不完整：" + missingText);
        }
    }

    private void addMissing(StringJoiner missing, String key, String value) {
        if (isBlank(value)) {
            missing.add(key);
        }
    }

    private Integer totalFee(BigDecimal amount) {
        try {
            BigDecimal cents = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2);
            if (cents.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new ArithmeticException("amount too large");
            }
            return cents.intValueExact();
        } catch (ArithmeticException ex) {
            throw new BizException(ResultCode.ORDER_AMOUNT_ERROR, "订单支付金额异常");
        }
    }

    private String safeText(String value, String fallback) {
        return isBlank(value) ? fallback.trim() : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
