package com.gj.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderItemVO {
    private Long id;
    private Long spuId;
    private Long skuId;
    private String skuName;
    private String skuImage;
    private Map<String, String> specData;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
}
