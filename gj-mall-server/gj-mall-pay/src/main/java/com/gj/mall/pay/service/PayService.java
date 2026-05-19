package com.gj.mall.pay.service;

import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.vo.PayCallbackResultVO;
import com.gj.mall.pay.vo.PayResultVO;

import java.util.Map;

public interface PayService {

    /** 发起支付 */
    PayResultVO pay(Long userId, PayDTO dto);

    /** 第三方回调（mock 时也走此入口完成订单状态推进） */
    void notifyPaid(String payNo, String thirdPayNo, String rawCallback);

    /** 第三方支付回调统一入口：先记录，再验签、幂等并推进流水。 */
    PayCallbackResultVO handleCallback(String channel, Map<String, Object> payload, Map<String, String> headers);
}
