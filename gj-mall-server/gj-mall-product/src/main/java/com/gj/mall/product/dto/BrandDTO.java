package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BrandDTO {

    private Long id;

    @NotBlank(message = "品牌名不能为空")
    private String name;

    private String logo;
    private String description;
    private Integer sort;
    private Integer showStatus;
}
