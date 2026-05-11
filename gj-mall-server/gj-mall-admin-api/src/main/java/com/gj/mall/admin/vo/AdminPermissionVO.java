package com.gj.mall.admin.vo;

import com.gj.mall.admin.entity.SysPermission;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminPermissionVO {

    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer type;
    private String path;
    private String icon;
    private Integer sort;
    private List<AdminPermissionVO> children = new ArrayList<>();

    public static AdminPermissionVO from(SysPermission permission) {
        AdminPermissionVO vo = new AdminPermissionVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setType(permission.getType());
        vo.setPath(permission.getPath());
        vo.setIcon(permission.getIcon());
        vo.setSort(permission.getSort());
        return vo;
    }
}
