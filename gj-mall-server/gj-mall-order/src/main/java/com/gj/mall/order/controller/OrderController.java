package com.gj.mall.order.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.order.dto.CreateOrderDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.order.vo.OrderLogisticsVO;
import com.gj.mall.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "14-订单", description = "订单（C 端）")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单（购物车选中项或直购商品）")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的订单分页")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(OrderQueryDTO q) {
        return Result.success(orderService.page(UserContext.getUserId(), q));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(UserContext.getUserId(), id));
    }

    @Operation(summary = "订单物流轨迹")
    @GetMapping("/{id}/logistics")
    public Result<OrderLogisticsVO> logistics(@PathVariable Long id) {
        return Result.success(orderService.logistics(UserContext.getUserId(), id));
    }

    @Operation(summary = "按订单号查询订单详情")
    @GetMapping("/no/{orderNo}")
    public Result<OrderVO> detailByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.detailByOrderNo(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        orderService.confirmReceive(UserContext.getUserId(), id);
        return Result.success();
    }
}
