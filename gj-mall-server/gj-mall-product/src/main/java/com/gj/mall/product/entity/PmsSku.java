package com.gj.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU
 */
@Data
@TableName(value = "pms_sku", autoResultMap = true)
public class PmsSku implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long spuId;
    private String skuCode;
    private String name;
    private String image;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer stock;
    private Integer lockedStock;
    private Integer warnStock;

    /** 规格 JSON：例如 {"颜色":"黑","尺寸":"L"} */
    private String specData;

    private Integer saleCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
