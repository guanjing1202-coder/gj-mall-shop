package com.gj.mall.pay.strategy;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.vo.PayResultVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信支付占位：等用户提供 AppID/MchID/证书后接 wechatpay-java SDK
 */
@Component
public class WechatPayStrategy implements PayStrategy {

    @Value("${mall.pay.mode:mock}")
    private String mode;

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
        if (!"real".equalsIgnoreCase(mode)) {
            throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "微信支付未启用，当前 mall.pay.mode=" + mode);
        }
        // TODO: 接入 wechatpay-java，调用「APP / Native / JSAPI」下单
        throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "微信支付尚未对接");
    }
}
