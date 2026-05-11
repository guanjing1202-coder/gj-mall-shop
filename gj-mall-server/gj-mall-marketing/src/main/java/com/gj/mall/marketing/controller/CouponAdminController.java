package com.gj.mall.marketing.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.result.Result;
import com.gj.mall.marketing.dto.CouponCreateDTO;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "16-优惠券-后台", description = "优惠券（后台管理）")
@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponService couponService;

    @Operation(summary = "优惠券分页")
    @GetMapping("/page")
    public Result<Page<CouponVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(couponService.adminPage(pageNum, pageSize, status));
    }

    @Operation(summary = "创建优惠券")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponCreateDTO dto) {
        return Result.success(couponService.adminCreate(dto));
    }

    @Operation(summary = "上架/下架优惠券（status: 0关闭 1开启）")
    @PutMapping("/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.adminSetStatus(id, status);
        return Result.success();
    }
}
