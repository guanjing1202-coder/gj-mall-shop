package com.gj.mall.admin.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSON;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.entity.SysConfig;
import com.gj.mall.admin.entity.SysOperationLog;
import com.gj.mall.admin.entity.SysUser;
import com.gj.mall.admin.mapper.SysUserMapper;
import com.gj.mall.admin.service.AdminOperationLogService;
import com.gj.mall.admin.support.AdminConfigSecurity;
import com.gj.mall.admin.support.AdminConfigSummarySupport;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminOperationLogAspect {

    private static final int MAX_PARAMS_LENGTH = 4000;
    private static final int MAX_ERROR_LENGTH = 500;

    private final AdminOperationLogService operationLogService;
    private final SysUserMapper userMapper;

    @Around("within(com.gj.mall..controller..*)")
    public Object recordOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (!shouldRecord(request)) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            saveLog(joinPoint, request, result, error, System.currentTimeMillis() - start);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint,
                         HttpServletRequest request,
                         Object result,
                         Throwable error,
                         long costTime) {
        try {
            Long adminId = UserContext.getUserId();
            SysOperationLog log = new SysOperationLog();
            log.setAdminId(adminId);
            log.setUsername(resolveUsername(adminId));
            log.setModule(resolveModule(joinPoint));
            log.setOperation(resolveOperation(joinPoint));
            log.setRequestMethod(request.getMethod());
            log.setRequestUri(request.getRequestURI());
            log.setRequestParams(serializeArgs(joinPoint.getArgs()));
            log.setRequestSummary(summarizeRequest(request, joinPoint.getArgs()));
            log.setIp(ServletUtil.getClientIP(request));
            log.setCostTime(costTime);
            log.setStatus(resolveStatus(result, error));
            log.setErrorMessage(error == null ? resolveResultMessage(result) : truncate(error.getMessage(), MAX_ERROR_LENGTH));
            operationLogService.record(log);
        } catch (Exception ex) {
            log.warn("记录后台操作日志失败: {}", ex.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private boolean shouldRecord(HttpServletRequest request) {
        if (request == null || !request.getRequestURI().startsWith("/api/admin/")) {
            return false;
        }
        if (request.getRequestURI().startsWith("/api/admin/auth/login")
                || request.getRequestURI().startsWith("/api/admin/auth/refresh")
                || request.getRequestURI().startsWith("/api/admin/operation-log")) {
            return false;
        }
        return Arrays.asList("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod());
    }

    private String resolveUsername(Long adminId) {
        if (adminId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(adminId);
        return user == null ? null : user.getUsername();
    }

    private String resolveModule(ProceedingJoinPoint joinPoint) {
        Tag tag = joinPoint.getTarget().getClass().getAnnotation(Tag.class);
        if (tag != null && StrUtil.isNotBlank(tag.name())) {
            return tag.name();
        }
        return joinPoint.getTarget().getClass().getSimpleName();
    }

    private String resolveOperation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Operation operation = method.getAnnotation(Operation.class);
        if (operation != null && StrUtil.isNotBlank(operation.summary())) {
            return operation.summary();
        }
        return method.getName();
    }

    private Integer resolveStatus(Object result, Throwable error) {
        if (error != null) {
            return 0;
        }
        if (result instanceof Result) {
            Result<?> apiResult = (Result<?>) result;
            return apiResult.getCode() != null && apiResult.getCode() == 200 ? 1 : 0;
        }
        return 1;
    }

    private String resolveResultMessage(Object result) {
        if (result instanceof Result) {
            Result<?> apiResult = (Result<?>) result;
            if (apiResult.getCode() != null && apiResult.getCode() != 200) {
                return truncate(apiResult.getMessage(), MAX_ERROR_LENGTH);
            }
        }
        return null;
    }

    private String serializeArgs(Object[] args) {
        try {
            List<Object> values = new ArrayList<>();
            for (Object arg : args) {
                if (isLoggableArg(arg)) {
                    values.add(maskLogArg(arg));
                }
            }
            if (values.isEmpty()) {
                return null;
            }
            String json = JSON.toJSONString(values);
            json = json.replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"******\"");
            return truncate(json, MAX_PARAMS_LENGTH);
        } catch (Exception ex) {
            return null;
        }
    }

    private Object maskLogArg(Object arg) {
        if (arg instanceof AdminConfigSaveDTO) {
            AdminConfigSaveDTO dto = (AdminConfigSaveDTO) arg;
            if (AdminConfigSecurity.isSensitive(dto.getConfigKey())) {
                AdminConfigSaveDTO copy = new AdminConfigSaveDTO();
                copy.setId(dto.getId());
                copy.setConfigKey(dto.getConfigKey());
                copy.setConfigName(dto.getConfigName());
                copy.setConfigValue(StrUtil.isBlank(dto.getConfigValue()) ? dto.getConfigValue() : "******");
                copy.setValueType(dto.getValueType());
                copy.setGroupCode(dto.getGroupCode());
                copy.setDescription(dto.getDescription());
                copy.setEditable(dto.getEditable());
                copy.setStatus(dto.getStatus());
                return copy;
            }
        }
        return arg;
    }

    private String summarizeArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AdminConfigSaveDTO) {
                AdminConfigSaveDTO dto = (AdminConfigSaveDTO) arg;
                SysConfig config = new SysConfig();
                config.setConfigKey(StrUtil.trim(dto.getConfigKey()));
                config.setConfigName(StrUtil.trim(dto.getConfigName()));
                config.setGroupCode(StrUtil.blankToDefault(StrUtil.trim(dto.getGroupCode()), "basic"));
                config.setConfigValue(StrUtil.trim(dto.getConfigValue()));
                config.setValueType(StrUtil.blankToDefault(StrUtil.trim(dto.getValueType()), "text"));
                config.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
                return AdminConfigSummarySupport.buildChangeSummary(null, config, resolveConfigAction(dto));
            }
        }
        return null;
    }

    private String summarizeRequest(HttpServletRequest request, Object[] args) {
        if (request != null
                && "POST".equalsIgnoreCase(request.getMethod())
                && "/api/admin/sys/config/payment/initialize".equals(request.getRequestURI())) {
            return "初始化支付配置缺失项";
        }
        return summarizeArgs(args);
    }

    private String resolveConfigAction(AdminConfigSaveDTO dto) {
        return dto.getId() == null ? "create" : "update";
    }

    private boolean isLoggableArg(Object arg) {
        return arg != null
                && !(arg instanceof ServletRequest)
                && !(arg instanceof ServletResponse)
                && !(arg instanceof BindingResult)
                && !(arg instanceof MultipartFile);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
