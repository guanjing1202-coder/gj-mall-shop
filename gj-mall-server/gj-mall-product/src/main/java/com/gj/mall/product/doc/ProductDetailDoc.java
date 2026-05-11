package com.gj.mall.product.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情文档（MongoDB）
 * 商品 SPU 的富文本/图文详情，承载 HTML 与图片地址数组
 */
@Data
@Document(collection = "product_detail")
public class ProductDetailDoc {

    @Id
    private String id;

    /** 关联的 SPU ID */
    private Long spuId;

    /** 富文本 HTML / Markdown */
    private String html;

    /** 详情图片数组 */
    private List<String> images;

    /** 包装清单 */
    private String packingList;

    /** 售后说明 */
    private String afterSale;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
