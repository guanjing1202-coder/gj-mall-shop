package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建/更新 SPU + 关联 SKU + 富文本详情
 */
@Data
public class SpuSaveDTO {

    private Long id;

    @NotBlank(message = "商品名不能为空")
    private String name;

    private String subTitle;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long brandId;

    private String mainImage;

    /** 图册 URL 列表 */
    private List<String> images;

    /** 富文本详情 HTML/Markdown */
    private String detailHtml;

    /** 详情图片数组 */
    private List<String> detailImages;

    /** 包装清单 */
    private String packingList;

    /** 售后说明 */
    private String afterSale;

    /** 0下架 1上架，默认 0 */
    private Integer publishStatus;

    @NotEmpty(message = "至少需要一个 SKU")
    @Valid
    private List<SkuDTO> skus;
}
