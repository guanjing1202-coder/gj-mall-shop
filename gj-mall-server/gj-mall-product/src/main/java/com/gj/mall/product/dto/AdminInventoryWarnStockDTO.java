package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdminInventoryWarnStockDTO {

    @NotNull(message = "预警库存不能为空")
    private Integer warnStock;
}
