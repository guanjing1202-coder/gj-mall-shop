package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdminInventoryAdjustDTO {

    @NotNull(message = "调整数量不能为空")
    private Integer stockDelta;

    private String remark;
}
