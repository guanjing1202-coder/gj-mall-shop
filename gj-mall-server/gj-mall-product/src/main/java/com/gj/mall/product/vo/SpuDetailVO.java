package com.gj.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SpuDetailVO {

    private Long id;
    private String name;
    private String subTitle;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String mainImage;
    private List<String> images;
    private BigDecimal price;
    private Integer saleCount;
    private Integer publishStatus;
    private Integer newStatus;
    private Integer recommendStatus;
    private Integer sort;

    /** 富文本详情 HTML */
    private String detailHtml;
    private List<String> detailImages;
    private String packingList;
    private String afterSale;

    /** SKU 列表 */
    private List<SkuVO> skus;
}
