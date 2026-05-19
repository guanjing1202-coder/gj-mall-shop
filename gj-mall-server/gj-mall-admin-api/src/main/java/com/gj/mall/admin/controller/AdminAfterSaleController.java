package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminAfterSaleActionDTO;
import com.gj.mall.admin.dto.AdminAfterSaleCreateDTO;
import com.gj.mall.admin.dto.AdminAfterSaleQueryDTO;
import com.gj.mall.admin.service.AdminAfterSaleService;
import com.gj.mall.admin.vo.AdminAfterSaleSummaryVO;
import com.gj.mall.admin.vo.AdminAfterSaleVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "后台售后", description = "售后 / 退货管理")
@RestController
@RequestMapping("/api/admin/after-sale")
@RequiredArgsConstructor
public class AdminAfterSaleController {

    private final AdminAfterSaleService afterSaleService;

    @Operation(summary = "售后处理汇总")
    @GetMapping("/summary")
    public Result<AdminAfterSaleSummaryVO> summary() {
        return Result.success(afterSaleService.summary());
    }

    @Operation(summary = "售后单分页")
    @GetMapping("/page")
    public Result<PageResult<AdminAfterSaleVO>> page(AdminAfterSaleQueryDTO query) {
        return Result.success(afterSaleService.page(query));
    }

    @Operation(summary = "售后单详情")
    @GetMapping("/{id}")
    public Result<AdminAfterSaleVO> detail(@PathVariable Long id) {
        return Result.success(afterSaleService.detail(id));
    }

    @Operation(summary = "后台创建售后单")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminAfterSaleCreateDTO dto) {
        return Result.success(afterSaleService.create(dto));
    }

    @Operation(summary = "审核通过")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        afterSaleService.approve(id, dto);
        return Result.success();
    }

    @Operation(summary = "审核拒绝")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        afterSaleService.reject(id, dto);
        return Result.success();
    }

    @Operation(summary = "确认退货收货")
    @PutMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id, @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        afterSaleService.receive(id, dto);
        return Result.success();
    }

    @Operation(summary = "确认退款")
    @PutMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id, @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        afterSaleService.refund(id, dto);
        return Result.success();
    }

    @Operation(summary = "取消售后单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        afterSaleService.cancel(id, dto);
        return Result.success();
    }
}
