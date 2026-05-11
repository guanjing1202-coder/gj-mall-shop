package com.gj.mall.pay.strategy;

import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.pay.vo.PayResultVO;

/**
 * 支付策略：每个渠道一个实现。
 *  - MockPayStrategy：直接返回 paid=true
 *  - WechatPayStrategy / AlipayStrategy：占位实现，等接入
 */
public interface PayStrategy {

    /** 渠道名（小写）：wechat / alipay / mock */
    String channelName();

    /** 渠道编码：1/2/3/9 */
    Integer channelCode();

    /** 发起支付。返回 PayResultVO（含 payNo + paid 标记） */
    PayResultVO pay(OmsOrder order, String payNo);
}
