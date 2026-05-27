package com.gj.mall.admin.support;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;

public final class AdminConfigSecurity {

    private static final List<String> SENSITIVE_PAYMENT_TOKENS = Arrays.asList(
            "secret",
            "api-v3-key",
            "private-key",
            "alipay-public-key");

    private AdminConfigSecurity() {
    }

    public static boolean isSensitive(String configKey) {
        String key = StrUtil.trimToEmpty(configKey).toLowerCase();
        if (!key.startsWith("mall.pay.")) {
            return false;
        }
        return SENSITIVE_PAYMENT_TOKENS.stream().anyMatch(key::contains);
    }

    public static String maskValue(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized)) {
            return normalized;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        int starCount = Math.min(Math.max(normalized.length() - 4, 6), 15);
        return normalized.substring(0, 2) + repeatStars(starCount) + normalized.substring(normalized.length() - 2);
    }

    public static boolean isMaskedPlaceholder(String value) {
        String normalized = StrUtil.trimToEmpty(value);
        return normalized.contains("***");
    }

    public static boolean matchesMaskedValue(String rawValue, String submittedValue) {
        return StrUtil.equals(maskValue(rawValue), StrUtil.trim(submittedValue));
    }

    private static String repeatStars(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append('*');
        }
        return builder.toString();
    }
}
