package com.gj.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水
 */
@Data
@TableName("pay_payment_record")
public class PayPaymentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private String payNo;
    private String thirdPayNo;

    /** 1微信 2支付宝 3余额 9MOCK */
    private Integer channel;
    private BigDecimal amount;

    /** 0待支付 1已支付 2失败 3已退款 */
    private Integer status;

    private LocalDateTime payTime;
    private String callbackData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
