package com.gj.mall.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MyCouponVO {
    private Long id;           // coupon_user.id
    private Long couponId;
    private String name;
    private Integer type;
    private String typeDesc;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal minAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;  // 领取时间
}
