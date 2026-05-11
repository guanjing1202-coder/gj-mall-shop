package com.gj.mall.product.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AdminInventoryVO {

    private Long skuId;
    private Long spuId;
    private String spuName;
    private String subTitle;
    private String skuCode;
    private String skuName;
    private String mainImage;
    private String skuImage;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private Integer lockedStock;
    private Integer warnStock;
    private Integer totalStock;
    private Integer alertGap;
    private Integer saleCount;
    private Integer publishStatus;
    private String stockStatus;
    private String stockStatusDesc;
    private Map<String, String> specData;
    private LocalDateTime updateTime;

    @JsonIgnore
    private String specDataJson;
}
