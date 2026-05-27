package com.gj.mall.pay.gateway;

import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.pay.vo.PayResultVO;

public interface ChannelPayAdapter {

    PayResultVO createPayment(OmsOrder order, String payNo);
}
