package com.gj.mall.pay.service;

import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.vo.PayCallbackResultVO;
import com.gj.mall.pay.vo.PayChannelVO;
import com.gj.mall.pay.vo.PayResultVO;
import com.gj.mall.pay.vo.PayStatusVO;

import java.util.List;
import java.util.Map;

public interface PayService {

    /** 发起支付 */
    PayResultVO pay(Long userId, PayDTO dto);

    /** 用户侧可选支付渠道。 */
    List<PayChannelVO> listChannels();

    /** 第三方回调（mock 时也走此入口完成订单状态推进） */
    void notifyPaid(String payNo, String thirdPayNo, String rawCallback);

    /** 查询当前用户支付流水状态，用于第三方支付回跳后的状态确认。 */
    PayStatusVO getStatus(Long userId, String payNo);

    /** 第三方支付回调统一入口：先记录，再验签、幂等并推进流水。 */
    PayCallbackResultVO handleCallback(String channel, Map<String, Object> payload, Map<String, String> headers);

    /** 后台重放处理失败的支付回调。 */
    PayCallbackResultVO replayCallback(Long callbackId);
}
