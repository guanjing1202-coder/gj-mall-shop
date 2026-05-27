package com.gj.mall.admin.service.impl;

import com.gj.mall.admin.dto.AdminConfigQueryDTO;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.entity.SysConfig;
import com.gj.mall.admin.mapper.SysConfigMapper;
import com.gj.mall.admin.vo.AdminConfigVO;
import com.gj.mall.admin.vo.AdminConfigInitResultVO;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminConfigServiceImplTest {

    private SysConfigMapper configMapper;
    private AdminConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        configMapper = mock(SysConfigMapper.class);
        service = new AdminConfigServiceImpl(configMapper);
    }

    @Test
    void pageMasksSensitivePaymentConfigValues() {
        SysConfig config = config("mall.pay.wechat.api-v3-key", "wechat-api-v3-secret");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysConfig> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(config));
        when(configMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<AdminConfigVO> result = service.page(new AdminConfigQueryDTO());

        AdminConfigVO vo = result.getList().get(0);
        assertTrue(vo.getSensitive());
        assertTrue(vo.getMasked());
        assertEquals("we***************et", vo.getConfigValue());
    }

    @Test
    void updateKeepsOriginalSensitiveValueWhenMaskedPlaceholderSubmitted() {
        SysConfig exists = config("mall.pay.callback.secret", "prod-callback-secret");
        exists.setId(10L);
        when(configMapper.selectById(10L)).thenReturn(exists);
        when(configMapper.selectCount(any())).thenReturn(0L);

        AdminConfigSaveDTO dto = saveDto("mall.pay.callback.secret", "pr***************et");
        dto.setId(10L);
        service.update(dto);

        verify(configMapper).updateById(argThat(update ->
                Long.valueOf(10L).equals(update.getId())
                        && "prod-callback-secret".equals(update.getConfigValue())));
    }

    @Test
    void updateRejectsUnknownMaskedSensitivePlaceholder() {
        SysConfig exists = config("mall.pay.callback.secret", "prod-callback-secret");
        exists.setId(10L);
        when(configMapper.selectById(10L)).thenReturn(exists);
        when(configMapper.selectCount(any())).thenReturn(0L);

        AdminConfigSaveDTO dto = saveDto("mall.pay.callback.secret", "wrong****mask");
        dto.setId(10L);
        BizException error = assertThrows(BizException.class, () -> service.update(dto));

        assertTrue(error.getMessage().contains("敏感配置值不能使用脱敏占位符"));
    }

    @Test
    void updateRejectsInvalidAlipayNotifyUrl() {
        SysConfig exists = config("mall.pay.alipay.notify-url", "https://shop.guanjing.cloud/api/pay/callback/alipay");
        exists.setId(11L);
        when(configMapper.selectById(11L)).thenReturn(exists);

        AdminConfigSaveDTO dto = saveDto("mall.pay.alipay.notify-url", "ftp://shop.guanjing.cloud/callback");
        dto.setId(11L);
        BizException error = assertThrows(BizException.class, () -> service.update(dto));

        assertTrue(error.getMessage().contains("回调地址必须以 http:// 或 https:// 开头"));
    }

    @Test
    void paymentSecretMetadataMarksValuesAsSensitive() {
        SysConfig config = config("mall.pay.alipay.private-key", "private-key-value");

        AdminConfigVO vo = AdminConfigVO.from(config);

        assertTrue(vo.getSensitive());
        assertTrue(vo.getMasked());
        assertFalse(vo.getConfigValue().contains("private-key-value"));
    }

    @Test
    void initializePaymentConfigsCreatesOnlyMissingBaselineRows() {
        when(configMapper.selectByConfigKeysIncludingDeleted(any()))
                .thenReturn(Collections.singletonList(config("mall.pay.mode", "real")));
        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);

        AdminConfigInitResultVO result = service.initializePaymentConfigs();

        assertEquals("payment", result.getGroupCode());
        assertEquals(12, result.getCreatedCount());
        assertEquals(0, result.getRestoredCount());
        assertEquals(1, result.getExistingCount());
        assertFalse(result.getCreatedKeys().contains("mall.pay.mode"));
        assertTrue(result.getCreatedKeys().contains("mall.pay.callback.secret"));
        verify(configMapper, times(12)).insert(captor.capture());
        List<SysConfig> inserted = captor.getAllValues();
        assertTrue(inserted.stream().noneMatch(item -> "mall.pay.mode".equals(item.getConfigKey())));
        assertTrue(inserted.stream()
                .filter(item -> "mall.pay.callback.secret".equals(item.getConfigKey()))
                .allMatch(item -> "gj-mall-dev-pay-callback-secret".equals(item.getConfigValue())));
    }

    @Test
    void initializePaymentConfigsRestoresDeletedBaselineRows() {
        SysConfig deletedMode = config("mall.pay.mode", "real");
        deletedMode.setId(20L);
        deletedMode.setDeleted(1);
        when(configMapper.selectByConfigKeysIncludingDeleted(any()))
                .thenReturn(Collections.singletonList(deletedMode));
        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);

        AdminConfigInitResultVO result = service.initializePaymentConfigs();

        assertEquals(12, result.getCreatedCount());
        assertEquals(1, result.getRestoredCount());
        assertEquals(0, result.getExistingCount());
        assertTrue(result.getRestoredKeys().contains("mall.pay.mode"));
        verify(configMapper).restoreDeletedConfig(captor.capture());
        SysConfig restored = captor.getValue();
        assertEquals(20L, restored.getId());
        assertEquals("mall.pay.mode", restored.getConfigKey());
        assertEquals("mock", restored.getConfigValue());
        assertEquals(1, restored.getStatus());
        assertEquals(0, restored.getDeleted());
        verify(configMapper, times(12)).insert(any(SysConfig.class));
    }

    private SysConfig config(String key, String value) {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey(key);
        config.setConfigName(key);
        config.setConfigValue(value);
        config.setValueType("text");
        config.setGroupCode("payment");
        config.setDescription("payment config");
        config.setEditable(1);
        config.setStatus(1);
        return config;
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
