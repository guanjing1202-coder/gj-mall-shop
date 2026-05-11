package com.gj.mall.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "用户注册请求")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度 4-20")
    @Schema(description = "用户名", example = "alice")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "昵称", example = "Alice")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;
}
