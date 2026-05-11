package com.gj.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户领取的优惠券
 *  status: 0未使用  1已使用  2已过期
 */
@Data
@TableName("sms_coupon_user")
public class SmsCouponUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long couponId;
    private Long userId;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
    private LocalDateTime usedAt;
    private Long orderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
