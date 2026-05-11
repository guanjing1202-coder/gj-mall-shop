package com.gj.mall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartVO {

    private List<CartItemVO> items;

    /** 总件数（quantity 之和，含未选中） */
    private Integer totalCount;

    /** 已选件数 */
    private Integer selectedCount;

    /** 已选总金额 */
    private BigDecimal selectedAmount;
}
