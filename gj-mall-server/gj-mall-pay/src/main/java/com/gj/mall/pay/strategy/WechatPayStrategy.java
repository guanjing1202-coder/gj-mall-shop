package com.gj.mall.pay.strategy;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.gateway.WechatPayAdapter;
import com.gj.mall.pay.vo.PayResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 微信支付占位：等用户提供 AppID/MchID/证书后接 wechatpay-java SDK
 */
@Component
@RequiredArgsConstructor
public class WechatPayStrategy implements PayStrategy {

    private final PayRuntimeConfigService runtimeConfigService;
    private final WechatPayAdapter adapter;

    @Override
    public String channelName() {
        return PayChannel.WECHAT.getName();
    }

    @Override
    public Integer channelCode() {
        return PayChannel.WECHAT.getCode();
    }

    @Override
    public PayResultVO pay(OmsOrder order, String payNo) {
        String mode = runtimeConfigService.mode();
        if (!"real".equalsIgnoreCase(mode)) {
            throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "微信支付未启用，当前 mall.pay.mode=" + mode);
        }
        return adapter.createPayment(order, payNo);
    }
}
