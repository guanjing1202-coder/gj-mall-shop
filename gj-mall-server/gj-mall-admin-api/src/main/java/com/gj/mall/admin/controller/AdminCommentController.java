package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminCommentActionDTO;
import com.gj.mall.admin.dto.AdminCommentQueryDTO;
import com.gj.mall.admin.dto.AdminCommentReplyDTO;
import com.gj.mall.admin.service.AdminCommentService;
import com.gj.mall.admin.vo.AdminCommentVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "后台评价评论", description = "商品评价审核、回复与隐藏")
@RestController
@RequestMapping("/api/admin/product/comment")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService commentService;

    @Operation(summary = "评价分页")
    @GetMapping("/page")
    public Result<PageResult<AdminCommentVO>> page(AdminCommentQueryDTO query) {
        return Result.success(commentService.page(query));
    }

    @Operation(summary = "评价详情")
    @GetMapping("/{id}")
    public Result<AdminCommentVO> detail(@PathVariable Long id) {
        return Result.success(commentService.detail(id));
    }

    @Operation(summary = "审核通过")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AdminCommentActionDTO dto) {
        commentService.approve(id, dto);
        return Result.success();
    }

    @Operation(summary = "审核驳回")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) AdminCommentActionDTO dto) {
        commentService.reject(id, dto);
        return Result.success();
    }

    @Operation(summary = "隐藏评价")
    @PutMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable Long id, @RequestBody(required = false) AdminCommentActionDTO dto) {
        commentService.hide(id, dto);
        return Result.success();
    }

    @Operation(summary = "重新展示评价")
    @PutMapping("/{id}/show")
    public Result<Void> show(@PathVariable Long id, @RequestBody(required = false) AdminCommentActionDTO dto) {
        commentService.show(id, dto);
        return Result.success();
    }

    @Operation(summary = "商家回复")
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody AdminCommentReplyDTO dto) {
        commentService.reply(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.success();
    }
}
