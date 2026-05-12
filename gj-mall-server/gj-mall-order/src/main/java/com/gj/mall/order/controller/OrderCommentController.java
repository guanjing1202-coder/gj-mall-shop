package com.gj.mall.order.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.order.dto.OrderCommentCreateDTO;
import com.gj.mall.order.service.OrderCommentService;
import com.gj.mall.order.vo.OrderCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "14-订单评价", description = "订单商品评价（C 端）")
@RestController
@RequestMapping("/api/order/{orderId}/comment")
@RequiredArgsConstructor
public class OrderCommentController {

    private final OrderCommentService commentService;

    @Operation(summary = "提交订单商品评价")
    @PostMapping
    public Result<Void> create(@PathVariable Long orderId, @Valid @RequestBody OrderCommentCreateDTO dto) {
        commentService.create(UserContext.getUserId(), orderId, dto);
        return Result.success();
    }

    @Operation(summary = "订单评价列表")
    @GetMapping
    public Result<List<OrderCommentVO>> list(@PathVariable Long orderId) {
        return Result.success(commentService.listByOrder(UserContext.getUserId(), orderId));
    }
}
