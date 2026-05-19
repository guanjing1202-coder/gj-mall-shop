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
    /** 结算场景：当前订单金额是否可用 */
    private Boolean usable;
    /** 结算场景：当前不可用原因 */
    private String unavailableReason;
    /** 结算场景：当前订单预计可抵扣金额 */
    private BigDecimal discountEstimate;
    /** 结算场景：距离可用门槛还差多少 */
    private BigDecimal amountGap;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;  // 领取时间
}
