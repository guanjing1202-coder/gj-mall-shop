package com.gj.mall.pay.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayRuntimeConfigServiceTest {

    @Test
    void readsEnabledSystemConfigBeforeEnvironmentProperty() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment().withProperty("mall.pay.mode", "mock");
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.mode"))).thenReturn("real");

        assertEquals("real", service.mode());
    }

    @Test
    void fallsBackToEnvironmentWhenSystemConfigIsMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment()
                .withProperty("mall.pay.callback.require-signature", "true");
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.callback.require-signature"))).thenThrow(new EmptyResultDataAccessException(1));

        assertTrue(service.callbackRequireSignature());
    }

    @Test
    void detectsDefaultDevelopmentCallbackSecret() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment();
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.callback.secret"))).thenThrow(new EmptyResultDataAccessException(1));

        assertTrue(service.usingDefaultCallbackSecret());
    }

    @Test
    void detectsCustomCallbackSecret() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment();
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.callback.secret"))).thenReturn("prod-secret");

        assertFalse(service.usingDefaultCallbackSecret());
    }

    @Test
    void detectsReadableWechatPrivateKeyFile() throws Exception {
        Path keyFile = Files.createTempFile("gj-mall-wx-pay-", ".pem");
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment();
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.wechat.private-key-path"))).thenReturn(keyFile.toString());

        assertTrue(service.wechatPrivateKeyFileReadable());
    }

    @Test
    void rejectsMissingWechatPrivateKeyFile() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment();
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.wechat.private-key-path"))).thenReturn("Z:/missing/wx-pay.pem");

        assertFalse(service.wechatPrivateKeyFileReadable());
    }

    @Test
    void readsWechatNotifyUrlFromRuntimeConfig() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = new MockEnvironment()
                .withProperty("mall.pay.wechat.notify-url", "https://env.example.com/api/pay/callback/wechat");
        PayRuntimeConfigService service = new PayRuntimeConfigService(jdbcTemplate, environment);

        when(jdbcTemplate.queryForObject(
                eq("select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1"),
                eq(String.class),
                eq("mall.pay.wechat.notify-url"))).thenReturn("https://shop.guanjing.cloud/api/pay/callback/wechat");

        assertEquals("https://shop.guanjing.cloud/api/pay/callback/wechat", service.wechatNotifyUrl());
    }
}
