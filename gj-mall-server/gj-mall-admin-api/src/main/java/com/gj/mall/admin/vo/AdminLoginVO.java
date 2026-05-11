package com.gj.mall.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "后台登录响应")
public class AdminLoginVO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private AdminUserVO user;
}
