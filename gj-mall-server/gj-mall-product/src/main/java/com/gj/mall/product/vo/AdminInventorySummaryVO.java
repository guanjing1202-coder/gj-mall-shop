package com.gj.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminInventorySummaryVO {

    private Long totalSkuCount;
    private Long emptySkuCount;
    private Long lowSkuCount;
    private Long lockedSkuCount;
    private Long totalAvailableStock;
    private Long totalLockedStock;
    private Long totalStock;
    private BigDecimal stockAmount;
}
