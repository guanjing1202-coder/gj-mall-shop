package com.gj.mall.product.dto;

import lombok.Data;

@Data
public class AdminInventoryQueryDTO {

    private String keyword;
    private Long spuId;
    private Long brandId;
    private Long categoryId;
    private Integer publishStatus;

    /** empty | low | normal | locked */
    private String stockStatus;

    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
