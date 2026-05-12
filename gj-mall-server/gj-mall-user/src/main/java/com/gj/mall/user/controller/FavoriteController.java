package com.gj.mall.user.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.user.service.UserFavoriteService;
import com.gj.mall.user.vo.UserFavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4-商品收藏", description = "用户商品收藏")
@RestController
@RequestMapping("/api/user/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final UserFavoriteService favoriteService;

    @Operation(summary = "我的收藏分页")
    @GetMapping("/page")
    public Result<PageResult<UserFavoriteVO>> page(
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size) {
        return Result.success(favoriteService.page(UserContext.getUserId(), current, size));
    }

    @Operation(summary = "商品收藏状态")
    @GetMapping("/{spuId}/status")
    public Result<Boolean> status(@PathVariable Long spuId) {
        return Result.success(favoriteService.exists(UserContext.getUserId(), spuId));
    }

    @Operation(summary = "收藏商品")
    @PostMapping("/{spuId}")
    public Result<Long> add(@PathVariable Long spuId) {
        return Result.success(favoriteService.add(UserContext.getUserId(), spuId));
    }

    @Operation(summary = "取消收藏商品")
    @DeleteMapping("/{spuId}")
    public Result<Void> remove(@PathVariable Long spuId) {
        favoriteService.remove(UserContext.getUserId(), spuId);
        return Result.success();
    }
}
