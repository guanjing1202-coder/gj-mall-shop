package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AdminInventoryBatchAdjustDTO {

    @NotEmpty(message = "调整明细不能为空")
    @Valid
    private List<Item> items;

    private String remark;

    @Data
    public static class Item {
        private Long skuId;
        private Integer stockDelta;
        private String remark;
    }
}
