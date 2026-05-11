package com.gj.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class SkuVO {

    private Long id;
    private Long spuId;
    private String skuCode;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer stock;
    private Integer lockedStock;
    private Map<String, String> specData;
    private Integer saleCount;
}
