package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminRolePermissionDTO;
import com.gj.mall.admin.dto.AdminRoleQueryDTO;
import com.gj.mall.admin.dto.AdminRoleSaveDTO;
import com.gj.mall.admin.dto.AdminUserQueryDTO;
import com.gj.mall.admin.dto.AdminUserSaveDTO;
import com.gj.mall.admin.vo.AdminCurrentPermissionVO;
import com.gj.mall.admin.vo.AdminPermissionVO;
import com.gj.mall.admin.vo.AdminRoleVO;
import com.gj.mall.admin.vo.AdminUserDetailVO;
import com.gj.mall.common.result.PageResult;

import java.util.List;

public interface AdminUserService {

    PageResult<AdminUserDetailVO> page(AdminUserQueryDTO query);

    AdminUserDetailVO detail(Long id);

    Long create(AdminUserSaveDTO dto);

    void update(AdminUserSaveDTO dto);

    void updateStatus(Long id, Integer status, Long currentAdminId);

    void resetPassword(Long id, String password);

    void delete(Long id, Long currentAdminId);

    List<AdminRoleVO> roles();

    PageResult<AdminRoleVO> rolePage(AdminRoleQueryDTO query);

    AdminRoleVO roleDetail(Long id);

    Long createRole(AdminRoleSaveDTO dto);

    void updateRole(AdminRoleSaveDTO dto);

    void updateRoleStatus(Long id, Integer status);

    void deleteRole(Long id);

    void updateRolePermissions(Long id, AdminRolePermissionDTO dto);

    List<AdminPermissionVO> permissionTree();

    AdminCurrentPermissionVO currentPermissions(Long adminId);
}
