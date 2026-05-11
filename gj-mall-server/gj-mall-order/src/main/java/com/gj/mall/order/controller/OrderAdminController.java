package com.gj.mall.order.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.order.dto.AdminOrderDeliverDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.order.vo.AdminOrderFulfillmentSummaryVO;
import com.gj.mall.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "14-订单-后台", description = "订单（后台）")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @Operation(summary = "订单分页查询")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(OrderQueryDTO q) {
        return Result.success(orderService.adminPage(q));
    }

    @Operation(summary = "订单履约汇总")
    @GetMapping("/fulfillment/summary")
    public Result<AdminOrderFulfillmentSummaryVO> fulfillmentSummary() {
        return Result.success(orderService.adminFulfillmentSummary());
    }

    @Operation(summary = "发货")
    @PostMapping("/{id}/deliver")
    public Result<OrderVO> deliver(@PathVariable Long id,
                                   @RequestBody(required = false) AdminOrderDeliverDTO dto) {
        return Result.success(orderService.deliver(id, dto));
    }

    @Operation(summary = "取消订单（强制）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        // 后台直接走 timeoutCancel：仅当还在待付款时释放库存并取消
        orderService.timeoutCancel(id);
        return Result.success();
    }
}
