package com.gj.mall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestVO {

    private String keyword;
    private String type;
    private String label;
    private Long targetId;
    private String image;
}
