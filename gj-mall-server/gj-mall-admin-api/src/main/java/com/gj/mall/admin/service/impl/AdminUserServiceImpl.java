package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminRolePermissionDTO;
import com.gj.mall.admin.dto.AdminRoleQueryDTO;
import com.gj.mall.admin.dto.AdminRoleSaveDTO;
import com.gj.mall.admin.dto.AdminUserQueryDTO;
import com.gj.mall.admin.dto.AdminUserSaveDTO;
import com.gj.mall.admin.entity.SysPermission;
import com.gj.mall.admin.entity.SysRole;
import com.gj.mall.admin.entity.SysRolePermission;
import com.gj.mall.admin.entity.SysUser;
import com.gj.mall.admin.entity.SysUserRole;
import com.gj.mall.admin.mapper.SysPermissionMapper;
import com.gj.mall.admin.mapper.SysRoleMapper;
import com.gj.mall.admin.mapper.SysRolePermissionMapper;
import com.gj.mall.admin.mapper.SysUserMapper;
import com.gj.mall.admin.mapper.SysUserRoleMapper;
import com.gj.mall.admin.service.AdminUserService;
import com.gj.mall.admin.vo.AdminCurrentPermissionVO;
import com.gj.mall.admin.vo.AdminPermissionVO;
import com.gj.mall.admin.vo.AdminRoleVO;
import com.gj.mall.admin.vo.AdminUserDetailVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<AdminUserDetailVO> page(AdminUserQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : query.getPageSize();
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        IPage<SysUser> result = userMapper.selectPage(page,
                Wrappers.<SysUser>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(SysUser::getUsername, query.getKeyword())
                                .or()
                                .like(SysUser::getNickname, query.getKeyword())
                                .or()
                                .like(SysUser::getPhone, query.getKeyword()))
                        .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                        .orderByDesc(SysUser::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        List<AdminUserDetailVO> list = enrichUsers(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    public AdminUserDetailVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "后台用户不存在");
        }
        return enrichUsers(Collections.singletonList(user)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminUserSaveDTO dto) {
        ensureUsernameUnique(dto.getUsername(), null);
        SysUser user = new SysUser();
        fillUser(user, dto);
        user.setId(null);
        user.setPassword(BCrypt.hashpw(
                StrUtil.blankToDefault(dto.getPassword(), "123456"),
                BCrypt.gensalt()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        saveUserRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AdminUserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少后台用户ID");
        }
        SysUser exists = userMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "后台用户不存在");
        }
        ensureUsernameUnique(dto.getUsername(), dto.getId());
        SysUser user = new SysUser();
        fillUser(user, dto);
        user.setId(dto.getId());
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        }
        userMapper.updateById(user);
        saveUserRoles(dto.getId(), dto.getRoleIds());
    }

    @Override
    public void updateStatus(Long id, Integer status, Long currentAdminId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        if (Objects.equals(id, currentAdminId) && status == 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "不能禁用当前登录账号");
        }
        SysUser exists = userMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "后台用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser exists = userMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "后台用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long currentAdminId) {
        if (Objects.equals(id, currentAdminId)) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "不能删除当前登录账号");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, id));
    }

    @Override
    public List<AdminRoleVO> roles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(AdminRoleVO::from)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<AdminRoleVO> rolePage(AdminRoleQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : query.getPageSize();
        IPage<SysRole> result = roleMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysRole>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(SysRole::getCode, query.getKeyword())
                                .or()
                                .like(SysRole::getName, query.getKeyword()))
                        .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                        .orderByDesc(SysRole::getCreateTime)
                        .orderByDesc(SysRole::getId));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                enrichRoles(result.getRecords()));
    }

    @Override
    public AdminRoleVO roleDetail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        return enrichRoles(Collections.singletonList(role)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(AdminRoleSaveDTO dto) {
        ensureRoleCodeUnique(dto.getCode(), null);
        SysRole role = new SysRole();
        fillRole(role, dto);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
        saveRolePermissions(role.getId(), dto.getPermissionIds());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(AdminRoleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少角色ID");
        }
        SysRole exists = roleMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        ensureRoleCodeUnique(dto.getCode(), dto.getId());
        SysRole role = new SysRole();
        fillRole(role, dto);
        role.setId(dto.getId());
        roleMapper.updateById(role);
        saveRolePermissions(dto.getId(), dto.getPermissionIds());
    }

    @Override
    public void updateRoleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        SysRole exists = roleMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setStatus(status);
        roleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Long boundCount = userRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, id));
        if (boundCount != null && boundCount > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "角色已分配给用户，不能删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long id, AdminRolePermissionDTO dto) {
        SysRole exists = roleMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        saveRolePermissions(id, dto == null ? Collections.emptyList() : dto.getPermissionIds());
    }

    @Override
    public List<AdminPermissionVO> permissionTree() {
        List<SysPermission> permissions = permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
        return buildPermissionTree(permissions);
    }

    @Override
    public AdminCurrentPermissionVO currentPermissions(Long adminId) {
        if (adminId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        AdminCurrentPermissionVO vo = new AdminCurrentPermissionVO();
        List<SysPermission> permissions = loadPermissionsByAdminId(adminId);
        vo.setPermissionCodes(permissions.stream()
                .map(SysPermission::getCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList()));
        Set<Long> grantedIds = permissions.stream()
                .map(SysPermission::getId)
                .collect(Collectors.toSet());
        vo.setMenus(buildPermissionTree(permissions.stream()
                .filter(permission -> grantedIds.contains(permission.getId())
                        && (permission.getType() == null || permission.getType() <= 2))
                .collect(Collectors.toList())));
        return vo;
    }

    private void fillUser(SysUser user, AdminUserSaveDTO dto) {
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
    }

    private void fillRole(SysRole role, AdminRoleSaveDTO dto) {
        role.setCode(StrUtil.trim(dto.getCode()));
        role.setName(StrUtil.trim(dto.getName()));
        role.setDescription(StrUtil.trim(dto.getDescription()));
        role.setStatus(dto.getStatus());
    }

    private void ensureUsernameUnique(String username, Long selfId) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(selfId != null, SysUser::getId, selfId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "后台用户名已存在");
        }
    }

    private void ensureRoleCodeUnique(String code, Long selfId) {
        Long count = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, StrUtil.trim(code))
                .ne(selfId != null, SysRole::getId, selfId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "角色编码已存在");
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        List<Long> distinctRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        for (Long roleId : distinctRoleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在：" + roleId);
            }
            SysUserRole rel = new SysUserRole();
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            userRoleMapper.insert(rel);
        }
    }

    private void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));
        if (CollUtil.isEmpty(permissionIds)) {
            return;
        }
        List<Long> distinctPermissionIds = permissionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(distinctPermissionIds)) {
            return;
        }
        Long permissionCount = permissionMapper.selectCount(Wrappers.<SysPermission>lambdaQuery()
                .in(SysPermission::getId, distinctPermissionIds));
        if (permissionCount == null || permissionCount != distinctPermissionIds.size()) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "部分权限不存在");
        }
        for (Long permissionId : distinctPermissionIds) {
            SysRolePermission rel = new SysRolePermission();
            rel.setRoleId(roleId);
            rel.setPermissionId(permissionId);
            rolePermissionMapper.insert(rel);
        }
    }

    private List<SysPermission> loadPermissionsByAdminId(Long adminId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, adminId))
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        roleIds = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1))
                .stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .in(SysRolePermission::getRoleId, roleIds))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(permissionIds)) {
            return Collections.emptyList();
        }
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .in(SysPermission::getId, permissionIds)
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
    }

    private List<AdminPermissionVO> buildPermissionTree(List<SysPermission> permissions) {
        Map<Long, AdminPermissionVO> nodeMap = permissions.stream()
                .map(AdminPermissionVO::from)
                .collect(Collectors.toMap(AdminPermissionVO::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        List<AdminPermissionVO> roots = new ArrayList<>();
        for (AdminPermissionVO node : nodeMap.values()) {
            if (node.getParentId() == null || Objects.equals(node.getParentId(), 0L) || !nodeMap.containsKey(node.getParentId())) {
                roots.add(node);
            } else {
                nodeMap.get(node.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    private List<AdminRoleVO> enrichRoles(List<SysRole> roles) {
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRolePermission> rels = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                .in(SysRolePermission::getRoleId, roleIds));
        Map<Long, List<Long>> permissionIdsByRole = rels.stream()
                .collect(Collectors.groupingBy(
                        SysRolePermission::getRoleId,
                        Collectors.mapping(SysRolePermission::getPermissionId, Collectors.toList())));
        return roles.stream().map(role -> {
            AdminRoleVO vo = AdminRoleVO.from(role);
            vo.setPermissionIds(permissionIdsByRole.getOrDefault(role.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AdminUserDetailVO> enrichUsers(List<SysUser> users) {
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> rels = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));
        Map<Long, List<Long>> roleIdsByUser = rels.stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

        List<Long> roleIds = rels.stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
        Map<Long, SysRole> roleMap = CollUtil.isEmpty(roleIds)
                ? Collections.emptyMap()
                : roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds))
                        .stream()
                        .collect(Collectors.toMap(SysRole::getId, r -> r));

        return users.stream().map(user -> {
            List<Long> ids = roleIdsByUser.getOrDefault(user.getId(), Collections.emptyList());
            List<String> names = ids.stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .map(SysRole::getName)
                    .collect(Collectors.toList());
            return AdminUserDetailVO.from(user, ids, names);
        }).collect(Collectors.toList());
    }
}
