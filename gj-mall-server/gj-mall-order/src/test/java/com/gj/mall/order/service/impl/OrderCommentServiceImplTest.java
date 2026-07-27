package com.gj.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.dto.OrderCommentCreateDTO;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCommentServiceImplTest {

    @Test
    void createRejectsOrderThatIsNotCompleted() {
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        OrderCommentServiceImpl service = service(orderMapper, mock(OmsOrderItemMapper.class), commentMapper);
        OmsOrder order = order(OrderStatus.PENDING_RECEIVE);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertThatThrownBy(() -> service.create(1L, 100L, dto(10L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单完成后才能评价");
        verify(commentMapper, never()).insert(any(PmsProductComment.class));
    }

    @Test
    void createRejectsOrderItemThatDoesNotBelongToOrder() {
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper itemMapper = mock(OmsOrderItemMapper.class);
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        OrderCommentServiceImpl service = service(orderMapper, itemMapper, commentMapper);
        when(orderMapper.selectById(100L)).thenReturn(order(OrderStatus.COMPLETED));
        OmsOrderItem item = orderItem(10L);
        item.setOrderId(999L);
        when(itemMapper.selectById(10L)).thenReturn(item);

        assertThatThrownBy(() -> service.create(1L, 100L, dto(10L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单商品不存在");
        verify(commentMapper, never()).insert(any(PmsProductComment.class));
    }

    @Test
    void createRejectsDuplicateCommentForSameOrderItem() {
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper itemMapper = mock(OmsOrderItemMapper.class);
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        OrderCommentServiceImpl service = service(orderMapper, itemMapper, commentMapper);
        when(orderMapper.selectById(100L)).thenReturn(order(OrderStatus.COMPLETED));
        when(itemMapper.selectById(10L)).thenReturn(orderItem(10L));
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(1L, 100L, dto(10L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("该商品已评价");
        verify(commentMapper, never()).insert(any(PmsProductComment.class));
    }

    @Test
    void createSavesPendingCommentSnapshotAndNormalizesInput() {
        OmsOrderMapper orderMapper = mock(OmsOrderMapper.class);
        OmsOrderItemMapper itemMapper = mock(OmsOrderItemMapper.class);
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        OrderCommentServiceImpl service = service(orderMapper, itemMapper, commentMapper);
        when(orderMapper.selectById(100L)).thenReturn(order(OrderStatus.COMPLETED));
        when(itemMapper.selectById(10L)).thenReturn(orderItem(10L));
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.create(1L, 100L, dto(10L));

        ArgumentCaptor<PmsProductComment> captor = ArgumentCaptor.forClass(PmsProductComment.class);
        verify(commentMapper).insert(captor.capture());
        PmsProductComment comment = captor.getValue();
        assertThat(comment.getOrderId()).isEqualTo(100L);
        assertThat(comment.getOrderNo()).isEqualTo("202606080001");
        assertThat(comment.getOrderItemId()).isEqualTo(10L);
        assertThat(comment.getSpuId()).isEqualTo(300L);
        assertThat(comment.getSkuId()).isEqualTo(200L);
        assertThat(comment.getContent()).isEqualTo("包装很好");
        assertThat(comment.getImages()).isEqualTo("[\"https://example.com/a.png\",\"https://example.com/b.png\"]");
        assertThat(comment.getStatus()).isEqualTo(0);
    }

    private OrderCommentServiceImpl service(OmsOrderMapper orderMapper, OmsOrderItemMapper itemMapper, PmsProductCommentMapper commentMapper) {
        return new OrderCommentServiceImpl(orderMapper, itemMapper, commentMapper);
    }

    private OmsOrder order(OrderStatus status) {
        OmsOrder order = new OmsOrder();
        order.setId(100L);
        order.setOrderNo("202606080001");
        order.setUserId(1L);
        order.setStatus(status.getCode());
        return order;
    }

    private OmsOrderItem orderItem(Long id) {
        OmsOrderItem item = new OmsOrderItem();
        item.setId(id);
        item.setOrderId(100L);
        item.setSpuId(300L);
        item.setSkuId(200L);
        return item;
    }

    private OrderCommentCreateDTO dto(Long orderItemId) {
        OrderCommentCreateDTO dto = new OrderCommentCreateDTO();
        dto.setOrderItemId(orderItemId);
        dto.setScore(5);
        dto.setContent("  包装很好  ");
        dto.setImages(Arrays.asList("https://example.com/a.png", "https://example.com/b.png"));
        return dto;
    }
}
