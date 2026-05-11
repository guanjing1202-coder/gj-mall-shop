package com.gj.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "秒杀活动添加 SKU")
public class SeckillSkuAddDTO {

    @NotNull
    @Schema(description = "活动 ID")
    private Long seckillId;

    @NotNull
    @Schema(description = "商品 SPU ID")
    private Long spuId;

    @NotNull
    @Schema(description = "商品 SKU ID")
    private Long skuId;

    @NotNull
    @Schema(description = "秒杀价")
    private BigDecimal seckillPrice;

    @NotNull
    @Min(1)
    @Schema(description = "秒杀库存")
    private Integer seckillStock;

    @NotNull
    @Min(1)
    @Schema(description = "每人限购数量")
    private Integer seckillLimit;
}
