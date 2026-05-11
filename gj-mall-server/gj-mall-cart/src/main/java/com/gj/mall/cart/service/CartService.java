package com.gj.mall.cart.service;

import com.gj.mall.cart.dto.AddCartDTO;
import com.gj.mall.cart.dto.MergeCartDTO;
import com.gj.mall.cart.dto.UpdateCartDTO;
import com.gj.mall.cart.vo.CartVO;

public interface CartService {

    CartVO get(Long userId);

    void add(Long userId, AddCartDTO dto);

    void update(Long userId, UpdateCartDTO dto);

    void remove(Long userId, Long skuId);

    void clear(Long userId);

    /** 全选 / 全不选 */
    void selectAll(Long userId, boolean selected);

    void merge(Long userId, MergeCartDTO dto);
}
