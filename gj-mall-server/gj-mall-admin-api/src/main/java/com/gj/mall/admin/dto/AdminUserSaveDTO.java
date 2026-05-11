package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@Schema(description = "后台用户保存请求")
public class AdminUserSaveDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;
    private List<Long> roleIds;
}
