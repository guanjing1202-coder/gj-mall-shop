package com.gj.mall.admin.aspect;

import com.gj.mall.admin.controller.AdminConfigController;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.entity.SysOperationLog;
import com.gj.mall.admin.entity.SysUser;
import com.gj.mall.admin.mapper.SysUserMapper;
import com.gj.mall.admin.service.AdminConfigService;
import com.gj.mall.admin.service.AdminOperationLogService;
import com.gj.mall.common.result.Result;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOperationLogAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        UserContext.clear();
    }

    @Test
    void recordOperationMasksSensitiveConfigValue() throws Throwable {
        AdminOperationLogService operationLogService = mock(AdminOperationLogService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AdminOperationLogAspect aspect = new AdminOperationLogAspect(operationLogService, userMapper);
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        when(userMapper.selectById(eq(1L))).thenReturn(admin);
        UserContext.set(UserContext.CurrentUser.builder()
                .userId(1L)
                .userType("admin")
                .username("admin")
                .build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/sys/config");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AdminConfigSaveDTO dto = saveDto("mall.pay.callback.secret", "super-callback-secret");
        ProceedingJoinPoint joinPoint = configCreateJoinPoint(dto);
        when(joinPoint.proceed()).thenReturn(Result.success(100L));

        aspect.recordOperation(joinPoint);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogService).record(captor.capture());
        String params = captor.getValue().getRequestParams();
        assertTrue(params.contains("\"configKey\":\"mall.pay.callback.secret\""));
        assertFalse(params.contains("super-callback-secret"));
        assertTrue(params.contains("\"configValue\":\"******\""));
    }

    private ProceedingJoinPoint configCreateJoinPoint(AdminConfigSaveDTO dto) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = AdminConfigController.class.getMethod("create", AdminConfigSaveDTO.class);
        when(joinPoint.getTarget()).thenReturn(new AdminConfigController(mock(AdminConfigService.class)));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        return joinPoint;
    }

    private AdminConfigSaveDTO saveDto(String key, String value) {
        AdminConfigSaveDTO dto = new AdminConfigSaveDTO();
        dto.setConfigKey(key);
        dto.setConfigName(key);
        dto.setConfigValue(value);
        dto.setValueType("text");
        dto.setGroupCode("payment");
        dto.setDescription("payment config");
        dto.setEditable(1);
        dto.setStatus(1);
        return dto;
    }
}
