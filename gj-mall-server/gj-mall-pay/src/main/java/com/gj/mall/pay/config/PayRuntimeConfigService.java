package com.gj.mall.pay.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayRuntimeConfigService {

    private static final String CONFIG_SQL = "select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1";
    private static final String DEFAULT_MODE = "mock";
    private static final String DEFAULT_CALLBACK_SECRET = "gj-mall-dev-pay-callback-secret";

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public String mode() {
        return text("mall.pay.mode", DEFAULT_MODE);
    }

    public boolean callbackRequireSignature() {
        return bool("mall.pay.callback.require-signature", false);
    }

    public String callbackSecret() {
        return text("mall.pay.callback.secret", DEFAULT_CALLBACK_SECRET);
    }

    public boolean usingDefaultCallbackSecret() {
        return DEFAULT_CALLBACK_SECRET.equals(callbackSecret());
    }

    public String wechatAppId() {
        return text("mall.pay.wechat.app-id", "");
    }

    public String wechatMchId() {
        return text("mall.pay.wechat.mch-id", "");
    }

    public String wechatApiV3Key() {
        return text("mall.pay.wechat.api-v3-key", "");
    }

    public String wechatMerchantSerialNo() {
        return text("mall.pay.wechat.merchant-serial-no", "");
    }

    public String wechatPrivateKeyPath() {
        return text("mall.pay.wechat.private-key-path", "");
    }

    public String wechatNotifyUrl() {
        return text("mall.pay.wechat.notify-url", "");
    }

    public boolean wechatPrivateKeyFileReadable() {
        String path = wechatPrivateKeyPath();
        if (StrUtil.isBlank(path)) {
            return false;
        }
        try {
            Path keyPath = Paths.get(StrUtil.trim(path));
            return Files.isRegularFile(keyPath) && Files.isReadable(keyPath);
        } catch (Exception ex) {
            return false;
        }
    }

    public String alipayAppId() {
        return text("mall.pay.alipay.app-id", "");
    }

    public String alipayPrivateKey() {
        return text("mall.pay.alipay.private-key", "");
    }

    public String alipayPublicKey() {
        return text("mall.pay.alipay.alipay-public-key", "");
    }

    public String alipayNotifyUrl() {
        return text("mall.pay.alipay.notify-url", "");
    }

    public String text(String key, String defaultValue) {
        String value = systemConfigValue(key);
        if (StrUtil.isNotBlank(value)) {
            return StrUtil.trim(value);
        }
        return StrUtil.blankToDefault(environment.getProperty(key), defaultValue);
    }

    public boolean bool(String key, boolean defaultValue) {
        String value = text(key, String.valueOf(defaultValue));
        String normalized = StrUtil.trim(value).toLowerCase();
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }

    private String systemConfigValue(String key) {
        try {
            return jdbcTemplate.queryForObject(CONFIG_SQL, String.class, key);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        } catch (DataAccessException ex) {
            log.warn("[pay-config] read runtime config failed key={}", key, ex);
            return null;
        }
    }
}
