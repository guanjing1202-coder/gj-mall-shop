package com.gj.mall.pay.gateway;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.pay.vo.PayResultVO;
import org.springframework.stereotype.Component;

@Component
public class WechatPayAdapter implements ChannelPayAdapter {

    @Override
    public PayResultVO createPayment(OmsOrder order, String payNo) {
        throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "微信支付 SDK 尚未接入");
    }
}
