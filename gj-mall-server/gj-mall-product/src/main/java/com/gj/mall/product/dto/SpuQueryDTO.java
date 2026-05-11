package com.gj.mall.product.dto;

import lombok.Data;

@Data
public class SpuQueryDTO {

    private Long categoryId;
    private Long brandId;
    private String keyword;
    private Integer publishStatus;
    private Integer newStatus;
    private Integer recommendStatus;

    /** 排序：default | operation | sales | price_asc | price_desc */
    private String sort;

    private Long current = 1L;
    private Long size = 20L;
}
