package com.gj.mall.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道
 */
@Getter
@AllArgsConstructor
public enum PayChannel {

    WECHAT(1, "wechat", "微信支付"),
    ALIPAY(2, "alipay", "支付宝"),
    BALANCE(3, "balance", "余额"),
    MOCK(9, "mock", "MOCK 模拟支付");

    private final Integer code;
    private final String name;
    private final String desc;

    public static PayChannel of(Integer code) {
        if (code == null) return null;
        for (PayChannel c : values()) {
            if (c.code.equals(code)) return c;
        }
        return null;
    }

    public static PayChannel ofName(String name) {
        if (name == null) return null;
        for (PayChannel c : values()) {
            if (c.name.equalsIgnoreCase(name)) return c;
        }
        return null;
    }
}
