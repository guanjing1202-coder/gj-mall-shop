package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class SkuDTO {

    private Long id;
    private String skuCode;
    private String name;
    private String image;

    @NotNull(message = "售价不能为空")
    private BigDecimal price;

    private BigDecimal costPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    /** 规格 Map，例如 {"颜色":"黑","尺寸":"L"} */
    private Map<String, String> specData;
}
