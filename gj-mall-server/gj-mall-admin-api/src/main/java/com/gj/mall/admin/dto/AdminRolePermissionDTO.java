package com.gj.mall.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminRolePermissionDTO {

    private List<Long> permissionIds;
}
