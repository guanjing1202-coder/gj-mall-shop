package com.gj.mall.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.dto.OrderCommentCreateDTO;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.service.OrderCommentService;
import com.gj.mall.order.vo.OrderCommentVO;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCommentServiceImpl implements OrderCommentService {

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final PmsProductCommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, Long orderId, OrderCommentCreateDTO dto) {
        OmsOrder order = mustOwnCompletedOrder(userId, orderId);
        if (dto == null || dto.getOrderItemId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少订单商品");
        }
        OmsOrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null || !order.getId().equals(item.getOrderId())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "订单商品不存在");
        }
        Long exists = commentMapper.selectCount(Wrappers.<PmsProductComment>lambdaQuery()
                .eq(PmsProductComment::getOrderItemId, item.getId())
                .eq(PmsProductComment::getUserId, userId));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "该商品已评价");
        }
        PmsProductComment comment = new PmsProductComment();
        comment.setOrderId(order.getId());
        comment.setOrderNo(order.getOrderNo());
        comment.setOrderItemId(item.getId());
        comment.setUserId(userId);
        comment.setSpuId(item.getSpuId());
        comment.setSkuId(item.getSkuId());
        comment.setScore(dto.getScore());
        comment.setContent(StrUtil.sub(StrUtil.trim(dto.getContent()), 0, 1000));
        comment.setImages(JSON.toJSONString(dto.getImages() == null ? Collections.emptyList() : dto.getImages()));
        comment.setStatus(0);
        commentMapper.insert(comment);
    }

    @Override
    public List<OrderCommentVO> listByOrder(Long userId, Long orderId) {
        mustOwnOrder(userId, orderId);
        List<PmsProductComment> comments = commentMapper.selectList(Wrappers.<PmsProductComment>lambdaQuery()
                .eq(PmsProductComment::getOrderId, orderId)
                .eq(PmsProductComment::getUserId, userId)
                .orderByDesc(PmsProductComment::getCreateTime));
        return comments.stream().map(OrderCommentVO::from).collect(Collectors.toList());
    }

    private OmsOrder mustOwnCompletedOrder(Long userId, Long orderId) {
        OmsOrder order = mustOwnOrder(userId, orderId);
        if (!OrderStatus.COMPLETED.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单完成后才能评价");
        }
        return order;
    }

    private OmsOrder mustOwnOrder(Long userId, Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }
}
