package com.gj.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpuListVO {

    private Long id;
    private String name;
    private String subTitle;
    private Long categoryId;
    private Long brandId;
    private String mainImage;
    private BigDecimal price;
    private Integer saleCount;
    private Integer publishStatus;
    private Integer newStatus;
    private Integer recommendStatus;
    private Integer sort;
}
