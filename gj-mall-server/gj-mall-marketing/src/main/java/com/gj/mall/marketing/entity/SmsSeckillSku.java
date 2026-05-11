package com.gj.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动商品（SKU 维度）
 */
@Data
@TableName("sms_seckill_sku")
public class SmsSeckillSku implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long seckillId;
    private Long spuId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    /** 每人限购 */
    private Integer seckillLimit;
    private Integer soldCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
