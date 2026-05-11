package com.gj.mall.cart.controller;

import com.gj.mall.cart.dto.AddCartDTO;
import com.gj.mall.cart.dto.MergeCartDTO;
import com.gj.mall.cart.dto.UpdateCartDTO;
import com.gj.mall.cart.service.CartService;
import com.gj.mall.cart.vo.CartVO;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "13-购物车", description = "登录态购物车（Redis 主存）")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "我的购物车")
    @GetMapping
    public Result<CartVO> get() {
        return Result.success(cartService.get(UserContext.getUserId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddCartDTO dto) {
        cartService.add(UserContext.getUserId(), dto);
        return Result.success();
    }

    @Operation(summary = "修改数量 / 选中状态")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UpdateCartDTO dto) {
        cartService.update(UserContext.getUserId(), dto);
        return Result.success();
    }

    @Operation(summary = "删除某个 SKU")
    @DeleteMapping("/{skuId}")
    public Result<Void> remove(@PathVariable Long skuId) {
        cartService.remove(UserContext.getUserId(), skuId);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "全选 / 全不选")
    @PutMapping("/select-all")
    public Result<Void> selectAll(@RequestParam boolean selected) {
        cartService.selectAll(UserContext.getUserId(), selected);
        return Result.success();
    }

    @Operation(summary = "登录后合并匿名端购物车")
    @PostMapping("/merge")
    public Result<Void> merge(@Valid @RequestBody MergeCartDTO dto) {
        cartService.merge(UserContext.getUserId(), dto);
        return Result.success();
    }
}
