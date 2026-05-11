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

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public PageResult<AdminOperationLogVO> page(AdminOperationLogQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        IPage<SysOperationLog> result = operationLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<SysOperationLog>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(SysOperationLog::getUsername, query.getKeyword())
                                .or()
                                .like(SysOperationLog::getModule, query.getKeyword())
                                .or()
                                .like(SysOperationLog::getOperation, query.getKeyword())
                                .or()
                                .like(SysOperationLog::getRequestUri, query.getKeyword()))
                        .eq(query.getAdminId() != null, SysOperationLog::getAdminId, query.getAdminId())
                        .eq(StrUtil.isNotBlank(query.getRequestMethod()), SysOperationLog::getRequestMethod, query.getRequestMethod())
                        .eq(query.getStatus() != null, SysOperationLog::getStatus, query.getStatus())
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
        operationLogMapper.insert(log);
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }
}
