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
import com.gj.mall.admin.support.AdminConfigSecurity;
import com.gj.mall.admin.support.AdminConfigSummarySupport;
import com.gj.mall.admin.vo.AdminConfigGroupSummaryVO;
import com.gj.mall.admin.vo.AdminConfigInitResultVO;
import com.gj.mall.admin.vo.AdminConfigVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private static final List<String> VALUE_TYPES = Arrays.asList("text", "number", "boolean", "json");
    private static final Map<String, ConfigSeed> PAYMENT_SEEDS = createPaymentSeeds();

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
    public AdminConfigGroupSummaryVO summary(String groupCode) {
        String normalizedGroupCode = StrUtil.blankToDefault(StrUtil.trim(groupCode), "basic");
        List<SysConfig> configs = configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getGroupCode, normalizedGroupCode)
                .orderByAsc(SysConfig::getConfigKey));
        return AdminConfigSummarySupport.buildGroupSummary(normalizedGroupCode, configs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminConfigInitResultVO initializePaymentConfigs() {
        Map<String, SysConfig> existsByKey = configMapper
                .selectByConfigKeysIncludingDeleted(AdminConfigSummarySupport.paymentRequiredKeys())
                .stream()
                .filter(item -> StrUtil.isNotBlank(item.getConfigKey()))
                .collect(Collectors.toMap(SysConfig::getConfigKey, Function.identity(), (left, right) -> left));
        AdminConfigInitResultVO result = new AdminConfigInitResultVO();
        result.setGroupCode("payment");

        for (String key : AdminConfigSummarySupport.paymentRequiredKeys()) {
            SysConfig existing = existsByKey.get(key);
            if (existing != null && !Integer.valueOf(1).equals(existing.getDeleted())) {
                result.getExistingKeys().add(key);
                continue;
            }
            ConfigSeed seed = PAYMENT_SEEDS.get(key);
            if (seed == null) {
                continue;
            }
            if (existing != null) {
                SysConfig restored = buildConfig(key, seed);
                restored.setId(existing.getId());
                restored.setDeleted(0);
                configMapper.restoreDeletedConfig(restored);
                result.getRestoredKeys().add(key);
                continue;
            }
            SysConfig config = buildConfig(key, seed);
            result.getCreatedKeys().add(key);
            configMapper.insert(config);
        }
        result.setCreatedCount(result.getCreatedKeys().size());
        result.setRestoredCount(result.getRestoredKeys().size());
        result.setExistingCount(result.getExistingKeys().size());
        return result;
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
        fillConfig(update, dto, exists);
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
        fillConfig(config, dto, null);
    }

    private void fillConfig(SysConfig config, AdminConfigSaveDTO dto, SysConfig exists) {
        config.setConfigKey(StrUtil.trim(dto.getConfigKey()));
        config.setConfigName(StrUtil.trim(dto.getConfigName()));
        config.setConfigValue(resolveConfigValue(dto, exists));
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
        validatePaymentConfig(configKey, dto.getConfigValue());
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

    private String resolveConfigValue(AdminConfigSaveDTO dto, SysConfig exists) {
        String submitted = StrUtil.trim(dto.getConfigValue());
        if (exists == null || !AdminConfigSecurity.isSensitive(dto.getConfigKey())) {
            return submitted;
        }
        if (AdminConfigSecurity.matchesMaskedValue(exists.getConfigValue(), submitted)) {
            return exists.getConfigValue();
        }
        if (AdminConfigSecurity.isMaskedPlaceholder(submitted)) {
            throw new BizException(ResultCode.PARAM_ERROR, "敏感配置值不能使用脱敏占位符，请填写新的完整明文值");
        }
        return submitted;
    }

    private void validatePaymentConfig(String configKey, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        if ("mall.pay.alipay.notify-url".equals(configKey)) {
            validateHttpUrl(value);
        }
        if ("mall.pay.wechat.notify-url".equals(configKey)) {
            validateHttpUrl(value);
        }
    }

    private void validateHttpUrl(String value) {
        try {
            URI uri = URI.create(StrUtil.trim(value));
            String scheme = StrUtil.trimToEmpty(uri.getScheme()).toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException();
            }
            if (StrUtil.isBlank(uri.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new BizException(ResultCode.PARAM_ERROR, "回调地址必须以 http:// 或 https:// 开头，并包含有效域名");
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

    private static Map<String, ConfigSeed> createPaymentSeeds() {
        Map<String, ConfigSeed> seeds = new HashMap<>();
        seeds.put("mall.pay.mode", seed("支付模式", "mock", "text", "mock 为开发模拟支付，real 为真实渠道支付"));
        seeds.put("mall.pay.callback.require-signature", seed("支付回调强制验签", "false", "boolean", "真实支付模式建议开启，开发联调可关闭"));
        seeds.put("mall.pay.callback.secret", seed("开发回调验签密钥", "gj-mall-dev-pay-callback-secret", "text", "Mock/开发回调用 HMAC 密钥，生产真实渠道以 SDK 验签为准"));
        seeds.put("mall.pay.wechat.app-id", seed("微信支付 AppID", "", "text", "微信支付商户应用 AppID"));
        seeds.put("mall.pay.wechat.mch-id", seed("微信支付商户号", "", "text", "微信支付商户号"));
        seeds.put("mall.pay.wechat.api-v3-key", seed("微信支付 APIv3 密钥", "", "text", "微信支付 APIv3 密钥"));
        seeds.put("mall.pay.wechat.merchant-serial-no", seed("微信支付商户证书序列号", "", "text", "微信支付商户证书序列号"));
        seeds.put("mall.pay.wechat.private-key-path", seed("微信支付私钥路径", "", "text", "服务端可读取的 apiclient_key.pem 路径"));
        seeds.put("mall.pay.wechat.notify-url", seed("微信支付回调地址", "", "text", "微信支付支付结果通知地址"));
        seeds.put("mall.pay.alipay.app-id", seed("支付宝 AppID", "", "text", "支付宝开放平台应用 AppID"));
        seeds.put("mall.pay.alipay.private-key", seed("支付宝应用私钥", "", "text", "支付宝应用私钥"));
        seeds.put("mall.pay.alipay.alipay-public-key", seed("支付宝公钥", "", "text", "支付宝平台公钥"));
        seeds.put("mall.pay.alipay.notify-url", seed("支付宝回调地址", "", "text", "支付宝异步通知 notify_url"));
        return seeds;
    }

    private static ConfigSeed seed(String name, String value, String valueType, String description) {
        return new ConfigSeed(name, value, valueType, description);
    }

    private SysConfig buildConfig(String key, ConfigSeed seed) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigName(seed.name);
        config.setConfigValue(seed.value);
        config.setValueType(seed.valueType);
        config.setGroupCode("payment");
        config.setDescription(seed.description);
        config.setEditable(1);
        config.setStatus(1);
        config.setDeleted(0);
        return config;
    }

    private static class ConfigSeed {
        private final String name;
        private final String value;
        private final String valueType;
        private final String description;

        private ConfigSeed(String name, String value, String valueType, String description) {
            this.name = name;
            this.value = value;
            this.valueType = valueType;
            this.description = description;
        }
    }
}
