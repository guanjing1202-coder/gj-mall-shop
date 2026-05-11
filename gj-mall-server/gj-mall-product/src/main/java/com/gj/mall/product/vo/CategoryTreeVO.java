package com.gj.mall.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO {

    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer level;
    private Integer sort;
    private Integer showStatus;

    private List<CategoryTreeVO> children = new ArrayList<>();
}
