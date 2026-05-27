package com.gj.mall.admin.support;

import com.gj.mall.admin.entity.SysConfig;
import com.gj.mall.admin.vo.AdminConfigGroupSummaryVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminConfigSummarySupportTest {

    @Test
    void reportsMissingPaymentBaselineWhenRowsAreNotInitialized() {
        AdminConfigGroupSummaryVO summary = AdminConfigSummarySupport.buildGroupSummary("payment", Collections.emptyList());

        assertEquals("payment", summary.getGroupCode());
        assertEquals("支付", summary.getGroupName());
        assertEquals(13, summary.getRequiredCount());
        assertEquals(0, summary.getReadyCount());
        assertEquals(13, summary.getMissingCount());
        assertEquals(13, summary.getUninitializedCount());
        assertEquals(0, summary.getCompletenessPercent());
        assertTrue(summary.getMissingKeys().contains("mall.pay.mode"));
        assertTrue(summary.getUninitializedKeys().contains("mall.pay.mode"));
        assertTrue(summary.getMissingKeys().contains("mall.pay.alipay.notify-url"));
    }

    @Test
    void summarizesPaymentConfigCompletenessAndSensitiveCoverage() {
        List<SysConfig> configs = Arrays.asList(
                config("mall.pay.mode", "payment", "mock", 1),
                config("mall.pay.callback.secret", "payment", "prod-secret", 1),
                config("mall.pay.wechat.app-id", "payment", "wx-app", 1),
                config("mall.pay.wechat.private-key-path", "payment", "", 1),
                config("mall.pay.alipay.notify-url", "payment", "https://shop.guanjing.cloud/callback", 0),
                config("mall.site.name", "basic", "GJ Mall", 1));

        AdminConfigGroupSummaryVO summary = AdminConfigSummarySupport.buildGroupSummary("payment", configs);

        assertEquals("payment", summary.getGroupCode());
        assertEquals("支付", summary.getGroupName());
        assertEquals(5, summary.getTotalCount());
        assertEquals(13, summary.getRequiredCount());
        assertEquals(3, summary.getReadyCount());
        assertEquals(4, summary.getEnabledCount());
        assertEquals(4, summary.getFilledCount());
        assertEquals(2, summary.getSensitiveCount());
        assertEquals(10, summary.getMissingCount());
        assertEquals(8, summary.getUninitializedCount());
        assertEquals(23, summary.getCompletenessPercent());
        assertTrue(summary.getMissingKeys().contains("mall.pay.callback.require-signature"));
        assertTrue(summary.getUninitializedKeys().contains("mall.pay.callback.require-signature"));
        assertTrue(summary.getMissingKeys().contains("mall.pay.wechat.private-key-path"));
        assertTrue(summary.getMissingKeys().contains("mall.pay.alipay.notify-url"));
    }

    @Test
    void buildsChangeSummaryWithoutLeakingSensitiveValue() {
        SysConfig before = config("mall.pay.callback.secret", "payment", "old-callback-secret", 1);
        before.setConfigName("回调密钥");
        SysConfig after = config("mall.pay.callback.secret", "payment", "new-callback-secret", 1);
        after.setConfigName("回调密钥");

        String summary = AdminConfigSummarySupport.buildChangeSummary(before, after, "update");

        assertTrue(summary.contains("回调密钥"));
        assertTrue(summary.contains("敏感值已更新"));
        assertTrue(summary.contains("支付"));
        assertTrue(summary.contains("状态:启用"));
        assertTrue(summary.contains("类型:text"));
    }

    private SysConfig config(String key, String groupCode, String value, int status) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigName(key);
        config.setGroupCode(groupCode);
        config.setConfigValue(value);
        config.setValueType("text");
        config.setStatus(status);
        config.setEditable(1);
        return config;
    }
}
