package com.gj.mall.admin.aspect;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.admin.entity.SysPermission;
import com.gj.mall.admin.entity.SysRole;
import com.gj.mall.admin.entity.SysRolePermission;
import com.gj.mall.admin.entity.SysUserRole;
import com.gj.mall.admin.mapper.SysPermissionMapper;
import com.gj.mall.admin.mapper.SysRoleMapper;
import com.gj.mall.admin.mapper.SysRolePermissionMapper;
import com.gj.mall.admin.mapper.SysUserRoleMapper;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.framework.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class AdminPermissionAspect {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (!shouldCheck(request)) {
            return joinPoint.proceed();
        }
        List<String> requiredCodes = resolveRequiredCodes(request.getRequestURI());
        if (CollUtil.isEmpty(requiredCodes)) {
            return joinPoint.proceed();
        }
        Long adminId = UserContext.getUserId();
        if (!"admin".equals(UserContext.getUserType()) || adminId == null) {
            throw new BizException(ResultCode.FORBIDDEN, "无后台访问权限");
        }
        if (!hasAnyPermission(adminId, requiredCodes)) {
            throw new BizException(ResultCode.FORBIDDEN, "缺少权限：" + String.join("/", requiredCodes));
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private boolean shouldCheck(HttpServletRequest request) {
        if (request == null || !request.getRequestURI().startsWith("/api/admin/")) {
            return false;
        }
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/admin/auth/")
                && !uri.startsWith("/api/admin/sys/current-permissions");
    }

    private List<String> resolveRequiredCodes(String uri) {
        List<PathPermissionRule> rules = permissionRules();
        for (PathPermissionRule rule : rules) {
            if (pathMatcher.match(rule.pattern, uri)) {
                return rule.codes;
            }
        }
        return Collections.emptyList();
    }

    private boolean hasAnyPermission(Long adminId, List<String> requiredCodes) {
        Set<String> grantedCodes = loadPermissionCodes(adminId);
        return requiredCodes.stream().anyMatch(grantedCodes::contains);
    }

    private Set<String> loadPermissionCodes(Long adminId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, adminId))
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        roleIds = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1))
                .stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .in(SysRolePermission::getRoleId, roleIds))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(permissionIds)) {
            return Collections.emptySet();
        }
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                        .in(SysPermission::getId, permissionIds))
                .stream()
                .map(SysPermission::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<PathPermissionRule> permissionRules() {
        List<PathPermissionRule> rules = new ArrayList<>();
        rules.add(rule("/api/admin/dashboard/**", "menu.dashboard"));
        rules.add(rule("/api/admin/report/**", "menu.dashboard"));
        rules.add(rule("/api/admin/product/comment/**", "product.comment.manage"));
        rules.add(rule("/api/admin/product/inventory/**", "product.inventory.manage"));
        rules.add(rule("/api/admin/product/brand/**", "product.brand.manage"));
        rules.add(rule("/api/admin/product/category/**", "product.category.manage"));
        rules.add(rule("/api/admin/product/spu/**", "product.spu.manage", "product.operation.manage"));
        rules.add(rule("/api/admin/order/**", "order.manage"));
        rules.add(rule("/api/admin/logistics/**", "logistics.manage"));
        rules.add(rule("/api/admin/payment/**", "payment.manage"));
        rules.add(rule("/api/admin/after-sale/**", "after.sale.manage"));
        rules.add(rule("/api/admin/marketing/coupon/**", "coupon.manage"));
        rules.add(rule("/api/admin/coupon/**", "coupon.manage"));
        rules.add(rule("/api/admin/marketing/seckill/**", "seckill.manage"));
        rules.add(rule("/api/admin/seckill/**", "seckill.manage"));
        rules.add(rule("/api/admin/member/favorite/**", "member.favorite.manage"));
        rules.add(rule("/api/admin/member/**", "member.manage"));
        rules.add(rule("/api/admin/sys/role/list", "sys.user.manage", "sys.role.manage"));
        rules.add(rule("/api/admin/sys/role/**", "sys.role.manage"));
        rules.add(rule("/api/admin/sys/permission/**", "sys.role.manage"));
        rules.add(rule("/api/admin/sys/config/**", "sys.config.manage"));
        rules.add(rule("/api/admin/sys/user/**", "sys.user.manage"));
        rules.add(rule("/api/admin/operation-log/**", "sys.operation.log.manage"));
        return rules;
    }

    private PathPermissionRule rule(String pattern, String... codes) {
        return new PathPermissionRule(pattern, Arrays.asList(codes));
    }

    private static class PathPermissionRule {
        private final String pattern;
        private final List<String> codes;

        private PathPermissionRule(String pattern, List<String> codes) {
            this.pattern = pattern;
            this.codes = codes;
        }
    }
}
