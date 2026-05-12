package com.gj.mall.user.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.user.service.UserBrowseHistoryService;
import com.gj.mall.user.vo.UserBrowseHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4-浏览足迹", description = "用户商品浏览足迹")
@RestController
@RequestMapping("/api/user/history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final UserBrowseHistoryService historyService;

    @Operation(summary = "我的浏览足迹分页")
    @GetMapping("/page")
    public Result<PageResult<UserBrowseHistoryVO>> page(
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size) {
        return Result.success(historyService.page(UserContext.getUserId(), current, size));
    }

    @Operation(summary = "记录浏览足迹")
    @PostMapping("/{spuId}")
    public Result<Void> record(@PathVariable Long spuId) {
        historyService.record(UserContext.getUserId(), spuId);
        return Result.success();
    }

    @Operation(summary = "删除单个浏览足迹")
    @DeleteMapping("/{spuId}")
    public Result<Void> remove(@PathVariable Long spuId) {
        historyService.remove(UserContext.getUserId(), spuId);
        return Result.success();
    }

    @Operation(summary = "清空浏览足迹")
    @DeleteMapping
    public Result<Void> clear() {
        historyService.clear(UserContext.getUserId());
        return Result.success();
    }
}
