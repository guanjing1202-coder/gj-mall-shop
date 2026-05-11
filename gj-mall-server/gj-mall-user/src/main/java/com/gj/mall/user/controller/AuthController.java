package com.gj.mall.user.controller;

import cn.hutool.extra.servlet.ServletUtil;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.user.dto.LoginDTO;
import com.gj.mall.user.dto.RegisterDTO;
import com.gj.mall.user.service.UserAuthService;
import com.gj.mall.user.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Tag(name = "1-用户鉴权", description = "注册 / 登录 / 刷新 / 登出")
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAuthService authService;

    @Operation(summary = "注册")
    @AuthExclude
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest req) {
        return Result.success(authService.register(dto, ServletUtil.getClientIP(req)));
    }

    @Operation(summary = "登录（用户名/手机号 + 密码）")
    @AuthExclude
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest req) {
        return Result.success(authService.login(dto, ServletUtil.getClientIP(req)));
    }

    @Operation(summary = "刷新 access token")
    @AuthExclude
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Parameter(description = "refresh token") @RequestParam String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(UserContext.getUserId());
        return Result.success();
    }
}
