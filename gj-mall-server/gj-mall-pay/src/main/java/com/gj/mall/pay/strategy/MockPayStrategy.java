package com.gj.mall.pay.strategy;

import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.pay.vo.PayResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockPayStrategy implements PayStrategy {

    @Override
    public String channelName() {
        return PayChannel.MOCK.getName();
    }

    @Override
    public Integer channelCode() {
        return PayChannel.MOCK.getCode();
    }

    @Override
    public PayResultVO pay(OmsOrder order, String payNo) {
        log.info("[pay-mock] paying orderNo={} payNo={} amount={}",
                order.getOrderNo(), payNo, order.getPayAmount());
        PayResultVO vo = new PayResultVO();
        vo.setChannel(channelName());
        vo.setPayNo(payNo);
        vo.setThirdPayNo("MOCK-" + payNo);
        vo.setAmount(order.getPayAmount());
        vo.setPaid(true);          // 模拟即时支付成功
        vo.setPayInfo("mock://paid?orderNo=" + order.getOrderNo());
        return vo;
    }
}
