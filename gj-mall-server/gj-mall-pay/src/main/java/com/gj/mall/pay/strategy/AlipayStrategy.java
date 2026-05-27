package com.gj.mall.pay.strategy;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.gateway.AlipayAdapter;
import com.gj.mall.pay.vo.PayResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 支付宝占位：等用户提供 AppID/私钥/支付宝公钥后接 alipay-sdk-java
 */
@Component
@RequiredArgsConstructor
public class AlipayStrategy implements PayStrategy {

    private final PayRuntimeConfigService runtimeConfigService;
    private final AlipayAdapter adapter;

    @Override
    public String channelName() {
        return PayChannel.ALIPAY.getName();
    }

    @Override
    public Integer channelCode() {
        return PayChannel.ALIPAY.getCode();
    }

    @Override
    public PayResultVO pay(OmsOrder order, String payNo) {
        String mode = runtimeConfigService.mode();
        if (!"real".equalsIgnoreCase(mode)) {
            throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "支付宝未启用，当前 mall.pay.mode=" + mode);
        }
        return adapter.createPayment(order, payNo);
    }
}
