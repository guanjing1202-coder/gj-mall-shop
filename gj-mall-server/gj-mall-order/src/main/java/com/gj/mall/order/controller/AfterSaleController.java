package com.gj.mall.order.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.order.dto.AfterSaleApplyDTO;
import com.gj.mall.order.dto.AfterSaleReturnDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.service.AfterSaleService;
import com.gj.mall.order.vo.AfterSaleEligibilityVO;
import com.gj.mall.order.vo.AfterSaleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "14-订单售后", description = "订单售后（C 端）")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @Operation(summary = "申请订单售后")
    @PostMapping("/order/{orderId}/after-sale")
    public Result<AfterSaleVO> apply(@PathVariable Long orderId, @Valid @RequestBody AfterSaleApplyDTO dto) {
        return Result.success(afterSaleService.apply(UserContext.getUserId(), orderId, dto));
    }

    @Operation(summary = "订单售后申请资格")
    @GetMapping("/order/{orderId}/after-sale/eligibility")
    public Result<AfterSaleEligibilityVO> eligibility(@PathVariable Long orderId,
                                                      @RequestParam(required = false) Integer type) {
        return Result.success(afterSaleService.eligibility(UserContext.getUserId(), orderId, type));
    }

    @Operation(summary = "订单售后列表")
    @GetMapping("/order/{orderId}/after-sale")
    public Result<List<AfterSaleVO>> listByOrder(@PathVariable Long orderId) {
        return Result.success(afterSaleService.listByOrder(UserContext.getUserId(), orderId));
    }

    @Operation(summary = "我的售后分页")
    @GetMapping("/after-sale/page")
    public Result<PageResult<AfterSaleVO>> page(OrderQueryDTO query) {
        return Result.success(afterSaleService.page(UserContext.getUserId(), query));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/after-sale/{id}")
    public Result<AfterSaleVO> detail(@PathVariable Long id) {
        return Result.success(afterSaleService.detail(UserContext.getUserId(), id));
    }

    @Operation(summary = "取消售后申请")
    @PostMapping("/after-sale/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        afterSaleService.cancel(UserContext.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "填写退货物流")
    @PostMapping("/after-sale/{id}/return")
    public Result<Void> submitReturn(@PathVariable Long id, @Valid @RequestBody AfterSaleReturnDTO dto) {
        afterSaleService.submitReturn(UserContext.getUserId(), id, dto);
        return Result.success();
    }
}
