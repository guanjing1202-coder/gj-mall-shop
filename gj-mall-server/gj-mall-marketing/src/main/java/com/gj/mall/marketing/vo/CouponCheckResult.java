package com.gj.mall.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券校验 + 折扣计算结果（供订单模块调用）
 */
@Data
public class CouponCheckResult {
    /** 折扣金额（正值，用于 payAmount = total - discount） */
    private BigDecimal discountAmount;
    /** coupon_user.id，用于后续 use() */
    private Long couponUserId;
}
