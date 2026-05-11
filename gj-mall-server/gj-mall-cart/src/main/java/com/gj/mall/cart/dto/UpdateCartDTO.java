package com.gj.mall.cart.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UpdateCartDTO {

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    /** 修改后的数量（非空时生效） */
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    /** 选中状态（非空时生效） 0/1 */
    private Integer selected;
}
