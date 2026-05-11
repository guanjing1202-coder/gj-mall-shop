package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminCouponIssueDTO;
import com.gj.mall.admin.dto.AdminCouponQueryDTO;
import com.gj.mall.admin.dto.AdminCouponSaveDTO;
import com.gj.mall.admin.dto.AdminCouponUserQueryDTO;
import com.gj.mall.admin.service.AdminCouponService;
import com.gj.mall.admin.vo.AdminCouponUserVO;
import com.gj.mall.admin.vo.AdminCouponVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "后台营销-优惠券", description = "后台优惠券管理")
@RestController
@RequestMapping("/api/admin/marketing/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService couponService;

    @Operation(summary = "优惠券分页")
    @GetMapping("/page")
    public Result<PageResult<AdminCouponVO>> page(AdminCouponQueryDTO query) {
        return Result.success(couponService.page(query));
    }

    @Operation(summary = "优惠券详情")
    @GetMapping("/{id}")
    public Result<AdminCouponVO> detail(@PathVariable Long id) {
        return Result.success(couponService.detail(id));
    }

    @Operation(summary = "新增优惠券")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminCouponSaveDTO dto) {
        return Result.success(couponService.create(dto));
    }

    @Operation(summary = "修改优惠券")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AdminCouponSaveDTO dto) {
        couponService.update(dto);
        return Result.success();
    }

    @Operation(summary = "上架/下架优惠券")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除优惠券")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success();
    }

    @Operation(summary = "给会员发放优惠券")
    @PostMapping("/{id}/issue")
    public Result<Integer> issue(@PathVariable Long id, @Valid @RequestBody AdminCouponIssueDTO dto) {
        return Result.success(couponService.issue(id, dto));
    }

    @Operation(summary = "优惠券领取记录")
    @GetMapping("/{id}/users")
    public Result<PageResult<AdminCouponUserVO>> users(
            @PathVariable Long id,
            AdminCouponUserQueryDTO query) {
        return Result.success(couponService.users(id, query));
    }
}
