package com.gj.mall.product.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CategoryDTO {

    private Long id;

    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    @NotBlank(message = "分类名不能为空")
    private String name;

    private String icon;

    private Integer level;

    private Integer sort;

    private Integer showStatus;
}
