package com.gj.mall.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponCenterVO {
    private Long id;
    private String name;
    private Integer type;
    private String typeDesc;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal minAmount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer remainCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
