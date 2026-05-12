package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "直购下单商品")
public class CreateOrderItemDTO {

    @Schema(description = "SKU ID")
    @NotNull
    private Long skuId;

    @Schema(description = "购买数量")
    @NotNull
    @Min(1)
    private Integer quantity;
}
