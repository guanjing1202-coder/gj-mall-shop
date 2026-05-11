package com.gj.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 *  type: 1满减  2折扣  3新人
 */
@Data
@TableName("sms_coupon")
public class SmsCoupon implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    /** 1满减 2折扣 3新人 */
    private Integer type;
    /** 满减：减免金额 */
    private BigDecimal discountAmount;
    /** 折扣：折扣率（0.85 = 85折） */
    private BigDecimal discountRate;
    /** 使用门槛（0=无门槛） */
    private BigDecimal minAmount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0关闭 1开启 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
