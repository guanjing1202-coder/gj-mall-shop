package com.gj.mall.cart.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 匿名端登录后合并购物车
 */
@Data
public class MergeCartDTO {

    @NotEmpty(message = "合并项不能为空")
    @Valid
    private List<AddCartDTO> items;
}
