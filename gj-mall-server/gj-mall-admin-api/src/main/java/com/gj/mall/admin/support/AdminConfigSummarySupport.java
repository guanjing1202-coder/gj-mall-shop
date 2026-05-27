package com.gj.mall.admin.support;

import cn.hutool.core.util.StrUtil;
import com.gj.mall.admin.entity.SysConfig;
import com.gj.mall.admin.vo.AdminConfigGroupSummaryVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AdminConfigSummarySupport {

    private static final List<String> PAYMENT_REQUIRED_KEYS = Collections.unmodifiableList(Arrays.asList(
            "mall.pay.mode",
            "mall.pay.callback.require-signature",
            "mall.pay.callback.secret",
            "mall.pay.wechat.app-id",
            "mall.pay.wechat.mch-id",
            "mall.pay.wechat.api-v3-key",
            "mall.pay.wechat.merchant-serial-no",
            "mall.pay.wechat.private-key-path",
            "mall.pay.wechat.notify-url",
            "mall.pay.alipay.app-id",
            "mall.pay.alipay.private-key",
            "mall.pay.alipay.alipay-public-key",
            "mall.pay.alipay.notify-url"));

    private AdminConfigSummarySupport() {
    }

    public static AdminConfigGroupSummaryVO buildGroupSummary(String groupCode, List<SysConfig> configs) {
        List<SysConfig> groupConfigs = configs == null ? Collections.emptyList() : configs.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.equals(groupCode, item.getGroupCode()))
                .collect(Collectors.toList());
        List<String> requiredKeys = requiredKeys(groupCode);
        Map<String, SysConfig> configByKey = groupConfigs.stream()
                .filter(item -> StrUtil.isNotBlank(item.getConfigKey()))
                .collect(Collectors.toMap(SysConfig::getConfigKey, Function.identity(), (left, right) -> left));
        AdminConfigGroupSummaryVO vo = new AdminConfigGroupSummaryVO();
        vo.setGroupCode(groupCode);
        vo.setGroupName(groupName(groupCode));
        vo.setTotalCount(groupConfigs.size());
        vo.setEnabledCount((int) groupConfigs.stream().filter(item -> Integer.valueOf(1).equals(item.getStatus())).count());
        vo.setFilledCount((int) groupConfigs.stream().filter(item -> StrUtil.isNotBlank(item.getConfigValue())).count());
        vo.setSensitiveCount((int) groupConfigs.stream().filter(item -> AdminConfigSecurity.isSensitive(item.getConfigKey())).count());
        Set<String> missingKeySet = new LinkedHashSet<>();
        Set<String> uninitializedKeySet = new LinkedHashSet<>();
        if (requiredKeys.isEmpty()) {
            groupConfigs.stream()
                    .filter(item -> StrUtil.isBlank(item.getConfigValue()) || !Integer.valueOf(1).equals(item.getStatus()))
                    .map(SysConfig::getConfigKey)
                    .filter(StrUtil::isNotBlank)
                    .forEach(missingKeySet::add);
        } else {
            requiredKeys.forEach(key -> {
                SysConfig config = configByKey.get(key);
                if (config == null) {
                    uninitializedKeySet.add(key);
                    missingKeySet.add(key);
                } else if (StrUtil.isBlank(config.getConfigValue()) || !Integer.valueOf(1).equals(config.getStatus())) {
                    missingKeySet.add(key);
                }
            });
        }
        List<String> missingKeys = missingKeySet.stream().collect(Collectors.toList());
        List<String> uninitializedKeys = uninitializedKeySet.stream().collect(Collectors.toList());
        vo.setMissingKeys(missingKeys);
        vo.setMissingCount(missingKeys.size());
        vo.setUninitializedKeys(uninitializedKeys);
        vo.setUninitializedCount(uninitializedKeys.size());
        int total = requiredKeys.isEmpty() ? groupConfigs.size() : requiredKeys.size();
        int complete = total - missingKeys.size();
        vo.setRequiredCount(total);
        vo.setReadyCount(Math.max(0, complete));
        vo.setCompletenessPercent(total <= 0 ? 0 : Math.max(0, Math.min(100, complete * 100 / total)));
        return vo;
    }

    public static List<String> paymentRequiredKeys() {
        return PAYMENT_REQUIRED_KEYS;
    }

    public static String buildChangeSummary(SysConfig before, SysConfig after, String action) {
        SysConfig target = after != null ? after : before;
        if (target == null) {
            return null;
        }
        StringBuilder summary = new StringBuilder();
        summary.append(actionText(action)).append("配置 ");
        summary.append(StrUtil.blankToDefault(target.getConfigName(), target.getConfigKey()));
        summary.append(" (").append(StrUtil.blankToDefault(target.getConfigKey(), "-")).append(")");
        summary.append(" / 分组:").append(groupName(target.getGroupCode()));
        summary.append(" / 类型:").append(StrUtil.blankToDefault(target.getValueType(), "text"));
        summary.append(" / 状态:").append(Integer.valueOf(1).equals(target.getStatus()) ? "启用" : "停用");
        if (AdminConfigSecurity.isSensitive(target.getConfigKey())) {
            summary.append(" / 敏感值已更新");
        } else if (after != null && StrUtil.isNotBlank(after.getConfigValue())) {
            summary.append(" / 值:").append(StrUtil.trim(after.getConfigValue()));
        }
        return summary.toString();
    }

    private static String actionText(String action) {
        if ("create".equalsIgnoreCase(action)) {
            return "新增";
        }
        if ("delete".equalsIgnoreCase(action)) {
            return "删除";
        }
        return "更新";
    }

    private static String groupName(String groupCode) {
        if ("product".equals(groupCode)) {
            return "商品";
        }
        if ("order".equals(groupCode)) {
            return "订单";
        }
        if ("payment".equals(groupCode)) {
            return "支付";
        }
        if ("marketing".equals(groupCode)) {
            return "营销";
        }
        if ("after_sale".equals(groupCode)) {
            return "售后";
        }
        return "basic".equals(groupCode) ? "基础" : StrUtil.blankToDefault(groupCode, "未分组");
    }

    private static List<String> requiredKeys(String groupCode) {
        if ("payment".equals(groupCode)) {
            return PAYMENT_REQUIRED_KEYS;
        }
        return Collections.emptyList();
    }
}
