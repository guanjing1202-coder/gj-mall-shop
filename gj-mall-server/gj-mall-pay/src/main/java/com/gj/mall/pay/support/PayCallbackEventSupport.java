package com.gj.mall.pay.support;

import com.gj.mall.order.enums.PayChannel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class PayCallbackEventSupport {

    private static final Set<String> WECHAT_PAID_EVENTS = new HashSet<>(Arrays.asList("SUCCESS"));
    private static final Set<String> ALIPAY_PAID_EVENTS = new HashSet<>(Arrays.asList("TRADE_SUCCESS", "TRADE_FINISHED"));
    private static final Set<String> MOCK_PAID_EVENTS = new HashSet<>(Arrays.asList("SUCCESS", "PAID"));

    public boolean isPaidEvent(PayChannel channel, String eventType) {
        if (channel == null) {
            return false;
        }
        String normalized = normalize(eventType);
        if (PayChannel.MOCK.equals(channel)) {
            return normalized == null || MOCK_PAID_EVENTS.contains(normalized);
        }
        if (PayChannel.WECHAT.equals(channel)) {
            return WECHAT_PAID_EVENTS.contains(normalized);
        }
        if (PayChannel.ALIPAY.equals(channel)) {
            return ALIPAY_PAID_EVENTS.contains(normalized);
        }
        return "SUCCESS".equals(normalized);
    }

    private String normalize(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            return null;
        }
        return eventType.trim().toUpperCase(Locale.ROOT);
    }
}
