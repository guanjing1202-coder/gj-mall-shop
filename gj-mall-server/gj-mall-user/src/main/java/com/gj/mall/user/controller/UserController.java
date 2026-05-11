package com.gj.mall.user.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.service.UmsUserService;
import com.gj.mall.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2-用户信息", description = "个人资料")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UmsUserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getProfile(UserContext.getUserId()));
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody UmsUser update) {
        userService.updateProfile(UserContext.getUserId(), update);
        return Result.success();
    }
}
