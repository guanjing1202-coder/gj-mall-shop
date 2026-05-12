package com.gj.mall.order.service;

import com.gj.mall.order.dto.OrderCommentCreateDTO;
import com.gj.mall.order.vo.OrderCommentVO;

import java.util.List;

public interface OrderCommentService {

    void create(Long userId, Long orderId, OrderCommentCreateDTO dto);

    List<OrderCommentVO> listByOrder(Long userId, Long orderId);
}
