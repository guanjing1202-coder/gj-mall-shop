package com.gj.mall.product.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminInventoryLogVO {

    private Long id;
    private Long spuId;
    private String spuName;
    private Long skuId;
    private String skuName;
    private String skuCode;
    private Integer changeType;
    private String changeTypeDesc;
    private Integer changeQuantity;
    private Integer stockBefore;
    private Integer stockAfter;
    private Integer lockedStockBefore;
    private Integer lockedStockAfter;
    private String remark;
    private LocalDateTime createTime;
}
