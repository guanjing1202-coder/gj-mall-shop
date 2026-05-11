package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminPasswordDTO;
import com.gj.mall.admin.dto.AdminRolePermissionDTO;
import com.gj.mall.admin.dto.AdminRoleQueryDTO;
import com.gj.mall.admin.dto.AdminRoleSaveDTO;
import com.gj.mall.admin.dto.AdminUserQueryDTO;
import com.gj.mall.admin.dto.AdminUserSaveDTO;
import com.gj.mall.admin.service.AdminUserService;
import com.gj.mall.admin.vo.AdminCurrentPermissionVO;
import com.gj.mall.admin.vo.AdminPermissionVO;
import com.gj.mall.admin.vo.AdminRoleVO;
import com.gj.mall.admin.vo.AdminUserDetailVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "后台用户", description = "后台用户 / 角色管理")
@RestController
@RequestMapping("/api/admin/sys")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "后台用户分页")
    @GetMapping("/user/page")
    public Result<PageResult<AdminUserDetailVO>> page(AdminUserQueryDTO query) {
        return Result.success(adminUserService.page(query));
    }

    @Operation(summary = "后台用户详情")
    @GetMapping("/user/{id}")
    public Result<AdminUserDetailVO> detail(@PathVariable Long id) {
        return Result.success(adminUserService.detail(id));
    }

    @Operation(summary = "新增后台用户")
    @PostMapping("/user")
    public Result<Long> create(@Valid @RequestBody AdminUserSaveDTO dto) {
        return Result.success(adminUserService.create(dto));
    }

    @Operation(summary = "修改后台用户")
    @PutMapping("/user")
    public Result<Void> update(@Valid @RequestBody AdminUserSaveDTO dto) {
        adminUserService.update(dto);
        return Result.success();
    }

    @Operation(summary = "启停后台用户")
    @PutMapping("/user/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateStatus(id, status, UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "重置后台用户密码")
    @PutMapping("/user/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordDTO dto) {
        adminUserService.resetPassword(id, dto.getPassword());
        return Result.success();
    }

    @Operation(summary = "删除后台用户")
    @DeleteMapping("/user/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "角色列表")
    @GetMapping("/role/list")
    public Result<List<AdminRoleVO>> roles() {
        return Result.success(adminUserService.roles());
    }

    @Operation(summary = "角色分页")
    @GetMapping("/role/page")
    public Result<PageResult<AdminRoleVO>> rolePage(AdminRoleQueryDTO query) {
        return Result.success(adminUserService.rolePage(query));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/role/{id}")
    public Result<AdminRoleVO> roleDetail(@PathVariable Long id) {
        return Result.success(adminUserService.roleDetail(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping("/role")
    public Result<Long> createRole(@Valid @RequestBody AdminRoleSaveDTO dto) {
        return Result.success(adminUserService.createRole(dto));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/role")
    public Result<Void> updateRole(@Valid @RequestBody AdminRoleSaveDTO dto) {
        adminUserService.updateRole(dto);
        return Result.success();
    }

    @Operation(summary = "启停角色")
    @PutMapping("/role/{id}/status")
    public Result<Void> updateRoleStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateRoleStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/role/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        adminUserService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/role/{id}/permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id,
                                              @RequestBody AdminRolePermissionDTO dto) {
        adminUserService.updateRolePermissions(id, dto);
        return Result.success();
    }

    @Operation(summary = "权限树")
    @GetMapping("/permission/tree")
    public Result<List<AdminPermissionVO>> permissionTree() {
        return Result.success(adminUserService.permissionTree());
    }

    @Operation(summary = "当前后台用户权限")
    @GetMapping("/current-permissions")
    public Result<AdminCurrentPermissionVO> currentPermissions() {
        return Result.success(adminUserService.currentPermissions(UserContext.getUserId()));
    }
}
