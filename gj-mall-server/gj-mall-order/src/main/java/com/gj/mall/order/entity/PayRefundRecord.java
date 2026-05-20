package com.gj.mall.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水。
 */
@Data
@TableName("pay_refund_record")
public class PayRefundRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String refundNo;
    private Long paymentId;
    private String payNo;
    private String thirdPayNo;
    private Long afterSaleId;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Integer channel;
    private BigDecimal amount;
    private Integer status;
    private String reason;
    private String operatorType;
    private String callbackData;
    private LocalDateTime successTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
