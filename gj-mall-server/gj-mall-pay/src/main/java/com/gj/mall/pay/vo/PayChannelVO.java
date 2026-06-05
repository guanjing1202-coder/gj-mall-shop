package com.gj.mall.pay.vo;

import lombok.Data;

@Data
public class PayChannelVO {

    /** 支付渠道编码 */
    private Integer channel;
    /** 支付渠道名称 */
    private String name;
    /** 支付渠道说明 */
    private String desc;
    /** 用户侧是否允许选择 */
    private Boolean enabled;
    /** 当前可用状态说明 */
    private String status;
}
