package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "后台保存秒杀 SKU")
public class AdminSeckillSkuSaveDTO {

    @Schema(description = "秒杀 SKU ID，新增时为空")
    private Long id;

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
