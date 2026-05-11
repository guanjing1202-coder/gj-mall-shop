package com.gj.mall.cart.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO {

    private Long skuId;
    private Long spuId;
    private String spuName;
    private String skuName;
    private String image;
    private BigDecimal price;
    private Integer stock;
    /** 上架状态 0下架 1上架 */
    private Integer publishStatus;
    /** SKU 是否失效（下架/删除/无库存） */
    private Boolean invalid;
    private Map<String, String> specData;

    private Integer quantity;
    private Integer selected;

    /** 行小计 = price * quantity */
    private BigDecimal totalAmount;
}
