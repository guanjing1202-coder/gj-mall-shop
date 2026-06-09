package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminOperationLogQueryDTO;
import com.gj.mall.admin.entity.SysOperationLog;
import com.gj.mall.admin.mapper.SysOperationLogMapper;
import com.gj.mall.admin.service.AdminOperationLogService;
import com.gj.mall.admin.vo.AdminOperationLogVO;
import com.gj.mall.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public PageResult<AdminOperationLogVO> page(AdminOperationLogQueryDTO query) {
        AdminOperationLogQueryDTO actualQuery = query == null ? new AdminOperationLogQueryDTO() : query;
        long pageNum = normalizePageNum(actualQuery.getPageNum());
        long pageSize = normalizePageSize(actualQuery.getPageSize());
        IPage<SysOperationLog> result = operationLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<SysOperationLog>lambdaQuery()
                        .and(StrUtil.isNotBlank(actualQuery.getKeyword()), w -> w
                                .like(SysOperationLog::getUsername, actualQuery.getKeyword())
                                .or()
                                .like(SysOperationLog::getModule, actualQuery.getKeyword())
                                .or()
                                .like(SysOperationLog::getOperation, actualQuery.getKeyword())
                                .or()
                                .like(SysOperationLog::getRequestUri, actualQuery.getKeyword()))
                        .eq(actualQuery.getAdminId() != null, SysOperationLog::getAdminId, actualQuery.getAdminId())
                        .eq(StrUtil.isNotBlank(actualQuery.getRequestMethod()), SysOperationLog::getRequestMethod, actualQuery.getRequestMethod())
                        .eq(actualQuery.getStatus() != null, SysOperationLog::getStatus, actualQuery.getStatus())
                        .orderByDesc(SysOperationLog::getCreateTime)
                        .orderByDesc(SysOperationLog::getId));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(AdminOperationLogVO::from).collect(Collectors.toList()));
    }

    @Override
    public void record(SysOperationLog log) {
        if (log == null) {
            return;
        }
        operationLogMapper.insert(log);
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
