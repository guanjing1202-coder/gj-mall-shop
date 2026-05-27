package com.gj.mall.admin.aspect;

import com.gj.mall.admin.controller.AdminConfigController;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.entity.SysOperationLog;
import com.gj.mall.admin.entity.SysUser;
import com.gj.mall.admin.mapper.SysUserMapper;
import com.gj.mall.admin.service.AdminConfigService;
import com.gj.mall.admin.service.AdminOperationLogService;
import com.gj.mall.framework.context.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOperationLogSummaryTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        UserContext.clear();
    }

    @Test
    void logSummaryDescribesPaymentConfigInitialization() throws Throwable {
        AdminOperationLogService operationLogService = mock(AdminOperationLogService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AdminOperationLogAspect aspect = new AdminOperationLogAspect(operationLogService, userMapper);
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        when(userMapper.selectById(eq(1L))).thenReturn(admin);
        UserContext.set(UserContext.CurrentUser.builder().userId(1L).userType("admin").username("admin").build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/admin/sys/config/payment/initialize")));

        ProceedingJoinPoint joinPoint = joinPointWithoutArgs("initializePaymentConfigs");
        when(joinPoint.proceed()).thenReturn(null);

        aspect.recordOperation(joinPoint);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogService).record(captor.capture());
        assertEquals("初始化支付配置缺失项", captor.getValue().getRequestSummary());
    }

    @Test
    void logSummaryDoesNotLeakSensitiveConfigValue() throws Throwable {
        AdminOperationLogService operationLogService = mock(AdminOperationLogService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AdminOperationLogAspect aspect = new AdminOperationLogAspect(operationLogService, userMapper);
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        when(userMapper.selectById(eq(1L))).thenReturn(admin);
        UserContext.set(UserContext.CurrentUser.builder().userId(1L).userType("admin").username("admin").build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("PUT", "/api/admin/sys/config")));

        AdminConfigSaveDTO dto = new AdminConfigSaveDTO();
        dto.setId(9L);
        dto.setConfigKey("mall.pay.callback.secret");
        dto.setConfigName("支付回调密钥");
        dto.setConfigValue("super-secret-value");
        dto.setGroupCode("payment");
        dto.setValueType("text");
        dto.setStatus(1);
        dto.setEditable(1);
        ProceedingJoinPoint joinPoint = joinPoint(dto, "update");
        when(joinPoint.proceed()).thenReturn(null);

        aspect.recordOperation(joinPoint);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogService).record(captor.capture());
        SysOperationLog log = captor.getValue();
        assertTrue(log.getRequestParams().contains("\"configValue\":\"******\""));
        assertFalse(log.getRequestParams().contains("super-secret-value"));
        assertTrue(log.getRequestSummary().contains("支付回调密钥"));
        assertTrue(log.getRequestSummary().contains("敏感值已更新"));
        assertFalse(log.getRequestSummary().contains("super-secret-value"));
    }

    private ProceedingJoinPoint joinPoint(AdminConfigSaveDTO dto, String methodName) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = AdminConfigController.class.getMethod(methodName, AdminConfigSaveDTO.class);
        when(joinPoint.getTarget()).thenReturn(new AdminConfigController(mock(AdminConfigService.class)));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        return joinPoint;
    }

    private ProceedingJoinPoint joinPointWithoutArgs(String methodName) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = AdminConfigController.class.getMethod(methodName);
        when(joinPoint.getTarget()).thenReturn(new AdminConfigController(mock(AdminConfigService.class)));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        return joinPoint;
    }
}
