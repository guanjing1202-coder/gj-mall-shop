package com.gj.mall.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class AdminRoleSaveDTO {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String description;
    private Integer status;
    private List<Long> permissionIds;
}
