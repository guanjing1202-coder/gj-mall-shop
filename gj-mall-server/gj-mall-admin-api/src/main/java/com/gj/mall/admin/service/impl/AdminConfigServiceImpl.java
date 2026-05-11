package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminConfigQueryDTO;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.entity.SysConfig;
import com.gj.mall.admin.mapper.SysConfigMapper;
import com.gj.mall.admin.service.AdminConfigService;
import com.gj.mall.admin.vo.AdminConfigVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private static final List<String> VALUE_TYPES = Arrays.asList("text", "number", "boolean", "json");

    private final SysConfigMapper configMapper;

    @Override
    public PageResult<AdminConfigVO> page(AdminConfigQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        IPage<SysConfig> result = configMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<SysConfig>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(SysConfig::getConfigKey, query.getKeyword())
                                .or()
                                .like(SysConfig::getConfigName, query.getKeyword())
                                .or()
                                .like(SysConfig::getDescription, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getGroupCode()), SysConfig::getGroupCode, query.getGroupCode())
                        .eq(StrUtil.isNotBlank(query.getValueType()), SysConfig::getValueType, query.getValueType())
                        .eq(query.getStatus() != null, SysConfig::getStatus, query.getStatus())
                        .orderByAsc(SysConfig::getGroupCode)
                        .orderByAsc(SysConfig::getConfigKey)
                        .orderByDesc(SysConfig::getUpdateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(AdminConfigVO::from).collect(Collectors.toList()));
    }

    @Override
    public AdminConfigVO detail(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "系统配置不存在");
        }
        return AdminConfigVO.from(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminConfigSaveDTO dto) {
        validateConfig(dto);
        ensureKeyUnique(dto.getConfigKey(), null);
        SysConfig config = new SysConfig();
        fillConfig(config, dto);
        config.setId(null);
        configMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AdminConfigSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少配置ID");
        }
        SysConfig exists = configMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "系统配置不存在");
        }
        ensureEditable(exists);
        validateConfig(dto);
        ensureKeyUnique(dto.getConfigKey(), dto.getId());
        SysConfig update = new SysConfig();
        fillConfig(update, dto);
        update.setId(dto.getId());
        configMapper.updateById(update);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        SysConfig exists = configMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "系统配置不存在");
        }
        ensureEditable(exists);
        SysConfig update = new SysConfig();
        update.setId(id);
        update.setStatus(status);
        configMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        SysConfig exists = configMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "系统配置不存在");
        }
        ensureEditable(exists);
        configMapper.deleteById(id);
    }

    private void fillConfig(SysConfig config, AdminConfigSaveDTO dto) {
        config.setConfigKey(StrUtil.trim(dto.getConfigKey()));
        config.setConfigName(StrUtil.trim(dto.getConfigName()));
        config.setConfigValue(StrUtil.trim(dto.getConfigValue()));
        config.setValueType(normalizeValueType(dto.getValueType()));
        config.setGroupCode(StrUtil.blankToDefault(StrUtil.trim(dto.getGroupCode()), "basic"));
        config.setDescription(StrUtil.trim(dto.getDescription()));
        config.setEditable(dto.getEditable() == null ? 1 : dto.getEditable());
        config.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private void validateConfig(AdminConfigSaveDTO dto) {
        String configKey = StrUtil.trim(dto.getConfigKey());
        if (!configKey.matches("^[A-Za-z][A-Za-z0-9_.-]{1,99}$")) {
            throw new BizException(ResultCode.PARAM_ERROR, "配置键只能使用字母、数字、点、下划线和短横线，并以字母开头");
        }
        if (dto.getEditable() != null && dto.getEditable() != 0 && dto.getEditable() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "可编辑标记非法");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        String valueType = normalizeValueType(dto.getValueType());
        if (!VALUE_TYPES.contains(valueType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "配置类型非法");
        }
        validateValue(valueType, dto.getConfigValue());
    }

    private void validateValue(String valueType, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        try {
            if ("number".equals(valueType)) {
                new BigDecimal(StrUtil.trim(value));
            } else if ("boolean".equals(valueType)) {
                String normalized = StrUtil.trim(value).toLowerCase();
                if (!"true".equals(normalized) && !"false".equals(normalized)) {
                    throw new IllegalArgumentException();
                }
            } else if ("json".equals(valueType)) {
                JSON.parse(value);
            }
        } catch (Exception ex) {
            throw new BizException(ResultCode.PARAM_ERROR, "配置值与类型不匹配");
        }
    }

    private void ensureKeyUnique(String configKey, Long selfId) {
        Long count = configMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, StrUtil.trim(configKey))
                .ne(selfId != null, SysConfig::getId, selfId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "配置键已存在");
        }
    }

    private void ensureEditable(SysConfig config) {
        if (config.getEditable() != null && config.getEditable() == 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "该配置不允许修改");
        }
    }

    private String normalizeValueType(String valueType) {
        return StrUtil.blankToDefault(StrUtil.trim(valueType), "text").toLowerCase();
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }
}
