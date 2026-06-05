package com.gj.mall.pay.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayStatusVO {

    /** 内部支付流水号 */
    private String payNo;
    /** 第三方流水号 */
    private String thirdPayNo;
    /** 支付渠道编码 */
    private Integer channel;
    /** 支付渠道名称 */
    private String channelName;
    /** 支付渠道说明 */
    private String channelDesc;
    /** 支付流水是否成功 */
    private Boolean paid;
    /** 支付流水状态：0待支付 1已支付 2失败 3已退款 */
    private Integer status;
    /** 支付流水状态说明 */
    private String statusDesc;
    /** 支付金额 */
    private BigDecimal amount;
    /** 支付完成时间 */
    private LocalDateTime payTime;
    /** 关联订单 ID */
    private Long orderId;
    /** 关联订单号 */
    private String orderNo;
    /** 订单状态 */
    private Integer orderStatus;
    /** 订单状态说明 */
    private String orderStatusDesc;
}
