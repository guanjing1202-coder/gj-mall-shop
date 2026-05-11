package com.gj.mall.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class SeckillSkuVO {
    private Long id;          // seckill_sku.id
    private Long seckillId;
    private Long spuId;
    private Long skuId;
    private String spuName;
    private String skuName;
    private String image;
    private Map<String, String> specData;
    /** 原价 */
    private BigDecimal originalPrice;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
    /** 剩余秒杀库存（从 Redis 读） */
    private Integer remainStock;
    private Integer seckillLimit;
    private Integer soldCount;
}
