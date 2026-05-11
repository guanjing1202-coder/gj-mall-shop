package com.gj.mall.pay.service;

import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.vo.PayResultVO;

public interface PayService {

    /** 发起支付 */
    PayResultVO pay(Long userId, PayDTO dto);

    /** 第三方回调（mock 时也走此入口完成订单状态推进） */
    void notifyPaid(String payNo, String thirdPayNo, String rawCallback);
}
