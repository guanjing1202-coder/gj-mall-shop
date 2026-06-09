package com.gj.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminOperationLogQueryDTO;
import com.gj.mall.admin.entity.SysOperationLog;
import com.gj.mall.admin.mapper.SysOperationLogMapper;
import com.gj.mall.admin.vo.AdminOperationLogVO;
import com.gj.mall.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOperationLogServiceImplTest {

    private SysOperationLogMapper operationLogMapper;
    private AdminOperationLogServiceImpl service;

    @BeforeEach
    void setUp() {
        operationLogMapper = mock(SysOperationLogMapper.class);
        service = new AdminOperationLogServiceImpl(operationLogMapper);
    }

    @Test
    void pageUsesDefaultQueryWhenQueryIsNull() {
        Page<SysOperationLog> mapperPage = new Page<>(1, 10);
        mapperPage.setTotal(0);
        mapperPage.setRecords(Collections.emptyList());
        when(operationLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(mapperPage);

        PageResult<AdminOperationLogVO> result = service.page(null);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getPageNum());
        assertEquals(10L, result.getPageSize());
        verify(operationLogMapper).selectPage(argThat(page ->
                page.getCurrent() == 1L && page.getSize() == 10L), any(Wrapper.class));
    }

    @Test
    void pageCapsPageSizeAndMapsRecords() {
        AdminOperationLogQueryDTO query = new AdminOperationLogQueryDTO();
        query.setPageNum(-5L);
        query.setPageSize(500L);
        query.setKeyword("订单");
        query.setRequestMethod("POST");
        query.setStatus(1);

        Page<SysOperationLog> mapperPage = new Page<>(1, 100);
        mapperPage.setTotal(1);
        mapperPage.setRecords(Collections.singletonList(log()));
        when(operationLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(mapperPage);

        PageResult<AdminOperationLogVO> result = service.page(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getPageNum());
        assertEquals(100L, result.getPageSize());
        assertEquals(1, result.getList().size());
        assertEquals("订单管理", result.getList().get(0).getModule());
        assertEquals("成功", result.getList().get(0).getStatusDesc());
        verify(operationLogMapper).selectPage(argThat(page ->
                page.getCurrent() == 1L && page.getSize() == 100L), any(Wrapper.class));
    }

    @Test
    void recordIgnoresNullLog() {
        service.record(null);

        verify(operationLogMapper, never()).insert(any(SysOperationLog.class));
    }

    private SysOperationLog log() {
        SysOperationLog log = new SysOperationLog();
        log.setId(1L);
        log.setAdminId(10L);
        log.setUsername("admin");
        log.setModule("订单管理");
        log.setOperation("发货");
        log.setRequestMethod("POST");
        log.setRequestUri("/api/admin/orders/1/delivery");
        log.setStatus(1);
        log.setCostTime(25L);
        log.setCreateTime(LocalDateTime.parse("2026-06-09T10:00:00"));
        return log;
    }
}
