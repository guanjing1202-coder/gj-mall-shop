package com.gj.mall.marketing.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.vo.CouponCenterVO;
import com.gj.mall.marketing.vo.MyCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "16-优惠券", description = "优惠券（C 端）")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "领券中心列表")
    @AuthExclude
    @GetMapping("/center")
    public Result<List<CouponCenterVO>> center() {
        return Result.success(couponService.centerList());
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/{couponId}/receive")
    public Result<Void> receive(@PathVariable Long couponId) {
        couponService.receive(UserContext.getUserId(), couponId);
        return Result.success();
    }

    @Operation(summary = "我的优惠券（status: 0未用 1已用 2过期，不传=全部）")
    @GetMapping("/my")
    public Result<List<MyCouponVO>> my(@RequestParam(required = false) Integer status) {
        return Result.success(couponService.myList(UserContext.getUserId(), status));
    }

    @Operation(summary = "查询可用优惠券（下单时传订单金额筛选门槛）")
    @GetMapping("/available")
    public Result<List<MyCouponVO>> available(
            @RequestParam(required = false) BigDecimal orderAmount) {
        return Result.success(couponService.available(UserContext.getUserId(), orderAmount));
    }
}
