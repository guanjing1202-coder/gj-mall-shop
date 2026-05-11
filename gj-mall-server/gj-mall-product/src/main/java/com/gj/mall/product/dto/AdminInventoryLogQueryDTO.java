package com.gj.mall.product.dto;

import lombok.Data;

@Data
public class AdminInventoryLogQueryDTO {

    private Long skuId;
    private Long spuId;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
