package com.gj.mall.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CouponPlanVO {

    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private Integer usableCount;
    private Integer unavailableCount;
    private MyCouponVO bestCoupon;
    private MyCouponVO nextCoupon;
    private BigDecimal nextAmountGap;
    private String summary;
    private String nextHint;
    private List<MyCouponVO> candidates;
}
