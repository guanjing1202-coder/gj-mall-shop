package com.gj.mall.user.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.user.service.UserMessageService;
import com.gj.mall.user.vo.UserMessageSummaryVO;
import com.gj.mall.user.vo.UserMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4-站内消息", description = "用户站内消息和未读提醒")
@RestController
@RequestMapping("/api/user/message")
@RequiredArgsConstructor
public class MessageController {

    private final UserMessageService messageService;

    @Operation(summary = "我的消息分页")
    @GetMapping("/page")
    public Result<PageResult<UserMessageVO>> page(
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) String type) {
        return Result.success(messageService.page(UserContext.getUserId(), current, size, readStatus, type));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.unreadCount(UserContext.getUserId()));
    }

    @Operation(summary = "消息汇总")
    @GetMapping("/summary")
    public Result<UserMessageSummaryVO> summary() {
        return Result.success(messageService.summary(UserContext.getUserId()));
    }

    @Operation(summary = "标记单条消息已读")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(UserContext.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead(UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(UserContext.getUserId(), id);
        return Result.success();
    }
}
