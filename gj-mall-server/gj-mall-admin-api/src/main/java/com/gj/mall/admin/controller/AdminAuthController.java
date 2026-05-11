package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminLoginDTO;
import com.gj.mall.admin.service.AdminAuthService;
import com.gj.mall.admin.vo.AdminLoginVO;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.framework.security.AuthExclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "后台鉴权", description = "管理后台登录 / 刷新 / 登出")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    @Operation(summary = "后台登录")
    @AuthExclude
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "刷新后台 access token")
    @AuthExclude
    @PostMapping("/refresh")
    public Result<AdminLoginVO> refresh(@Parameter(description = "refresh token") @RequestParam String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "后台登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(UserContext.getUserId());
        return Result.success();
    }
}
