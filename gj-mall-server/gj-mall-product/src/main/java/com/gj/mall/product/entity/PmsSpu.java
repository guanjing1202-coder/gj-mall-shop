package com.gj.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU
 */
@Data
@TableName(value = "pms_spu", autoResultMap = true)
public class PmsSpu implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String subTitle;
    private Long categoryId;
    private Long brandId;
    private String mainImage;

    /** 图册 JSON 数组：默认 fastjson/JSON 字符串存储 */
    private String images;

    /** MongoDB 详情文档 ID */
    private String detailId;

    private BigDecimal price;
    private Integer saleCount;
    private Integer publishStatus;
    private Integer newStatus;
    private Integer recommendStatus;
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
