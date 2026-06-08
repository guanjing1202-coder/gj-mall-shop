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
public class AlipayAdapter implements ChannelPayAdapter {

    private final PayRuntimeConfigService runtimeConfigService;
    private final AlipayPagePayClient pagePayClient;

    @Override
    public PayResultVO createPayment(OmsOrder order, String payNo) {
        assertOrder(order);
        assertText(payNo, "payNo");

        String appId = runtimeConfigService.alipayAppId();
        String privateKey = runtimeConfigService.alipayPrivateKey();
        String alipayPublicKey = runtimeConfigService.alipayPublicKey();
        String notifyUrl = runtimeConfigService.alipayNotifyUrl();
        assertRequiredConfig(appId, privateKey, alipayPublicKey, notifyUrl);
        String form = pagePayClient.pagePay(new AlipayPagePayRequest(
                appId.trim(),
                privateKey.trim(),
                alipayPublicKey.trim(),
                notifyUrl.trim(),
                payNo.trim(),
                amountText(order.getPayAmount()),
                "GJ Mall 订单 " + safeText(order.getOrderNo(), payNo)));
        if (isBlank(form)) {
            throw new BizException(ResultCode.PAY_FAIL, "支付宝 Page Pay 返回表单为空");
        }

        PayResultVO result = new PayResultVO();
        result.setChannel(PayChannel.ALIPAY.getName());
        result.setPayNo(payNo);
        result.setPaid(false);
        result.setAmount(order.getPayAmount());
        result.setPayInfo(form);
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

    private void assertRequiredConfig(String appId, String privateKey, String alipayPublicKey, String notifyUrl) {
        StringJoiner missing = new StringJoiner("、");
        if (isBlank(appId)) {
            missing.add("mall.pay.alipay.app-id");
        }
        if (isBlank(privateKey)) {
            missing.add("mall.pay.alipay.private-key");
        }
        if (isBlank(alipayPublicKey)) {
            missing.add("mall.pay.alipay.alipay-public-key");
        }
        if (isBlank(notifyUrl)) {
            missing.add("mall.pay.alipay.notify-url");
        }
        String missingText = missing.toString();
        if (!missingText.isEmpty()) {
            throw new BizException(ResultCode.PAY_FAIL, "支付宝支付配置不完整：" + missingText);
        }
    }

    private String amountText(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safeText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
