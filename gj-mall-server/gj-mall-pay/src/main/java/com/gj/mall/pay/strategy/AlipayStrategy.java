package com.gj.mall.pay.strategy;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.vo.PayResultVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 支付宝占位：等用户提供 AppID/私钥/支付宝公钥后接 alipay-sdk-java
 */
@Component
public class AlipayStrategy implements PayStrategy {

    @Value("${mall.pay.mode:mock}")
    private String mode;

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
        if (!"real".equalsIgnoreCase(mode)) {
            throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "支付宝未启用，当前 mall.pay.mode=" + mode);
        }
        // TODO: 接入 alipay-sdk-java，调用 alipay.trade.app.pay / page.pay
        throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "支付宝尚未对接");
    }
}
