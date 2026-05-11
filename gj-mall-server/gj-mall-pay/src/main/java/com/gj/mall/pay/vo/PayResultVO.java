package com.gj.mall.pay.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayResultVO {

    /** 渠道 */
    private String channel;
    /** 内部支付流水号 */
    private String payNo;
    /** 第三方流水号（mock 时同 payNo） */
    private String thirdPayNo;
    /** 是否已支付（mock=true，真实渠道=false 等待回调） */
    private Boolean paid;
    /** 支付金额 */
    private BigDecimal amount;
    /** 跳转 URL / 二维码 / 调起参数（真实渠道用） */
    private String payInfo;
}
