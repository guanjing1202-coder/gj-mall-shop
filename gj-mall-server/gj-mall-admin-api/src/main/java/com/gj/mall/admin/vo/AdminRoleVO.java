package com.gj.mall.admin.vo;

import com.gj.mall.admin.entity.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "后台角色")
public class AdminRoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private List<Long> permissionIds = new ArrayList<>();

    public static AdminRoleVO from(SysRole role) {
        if (role == null) {
            return null;
        }
        AdminRoleVO vo = new AdminRoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
