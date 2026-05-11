package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminFavoriteBatchDeleteDTO;
import com.gj.mall.admin.dto.AdminFavoriteQueryDTO;
import com.gj.mall.admin.service.AdminFavoriteService;
import com.gj.mall.admin.vo.AdminFavoriteVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "后台会员收藏", description = "会员收藏查询与清理")
@RestController
@RequestMapping("/api/admin/member/favorite")
@RequiredArgsConstructor
public class AdminFavoriteController {

    private final AdminFavoriteService favoriteService;

    @Operation(summary = "会员收藏分页")
    @GetMapping("/page")
    public Result<PageResult<AdminFavoriteVO>> page(AdminFavoriteQueryDTO query) {
        return Result.success(favoriteService.page(query));
    }

    @Operation(summary = "删除收藏记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        favoriteService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除收藏记录")
    @DeleteMapping
    public Result<Void> batchDelete(@Valid @RequestBody AdminFavoriteBatchDeleteDTO dto) {
        favoriteService.batchDelete(dto);
        return Result.success();
    }
}
