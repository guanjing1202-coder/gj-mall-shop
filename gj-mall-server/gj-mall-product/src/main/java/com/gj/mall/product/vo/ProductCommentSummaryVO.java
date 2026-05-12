package com.gj.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCommentSummaryVO {

    private Long total;
    private BigDecimal averageScore;
    private Long goodCount;
    private BigDecimal goodRate;
    private Long imageCount;
}
