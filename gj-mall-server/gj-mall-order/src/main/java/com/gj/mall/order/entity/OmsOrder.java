package com.gj.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 */
@Data
@TableName(value = "oms_order", autoResultMap = true)
public class OmsOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long userId;

    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal freightAmount;
    private BigDecimal couponAmount;
    /** 关联 sms_coupon_user.id，取消时释放 */
    private Long couponUserId;

    /** 0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款 */
    private Integer status;

    /** 1微信 2支付宝 3余额 9MOCK */
    private Integer payType;

    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private String deliveryCompany;
    private String deliveryNo;
    private String deliveryRemark;

    /** 收货地址快照 JSON */
    private String receiverInfo;

    /** 发票信息快照 JSON */
    private String invoiceInfo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
