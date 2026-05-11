package com.gj.mall.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminCurrentPermissionVO {
    private List<String> permissionCodes = new ArrayList<>();
    private List<AdminPermissionVO> menus = new ArrayList<>();
}
