package com.gj.mall.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态机
 *  0 PENDING_PAY  待付款
 *  1 PENDING_DELIVERY 已付款待发货
 *  2 PENDING_RECEIVE  已发货待收货
 *  3 COMPLETED        已完成
 *  4 CANCELED         已取消
 *  5 REFUNDING        退款中
 *  6 REFUNDED         已退款
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING_PAY(0, "待付款"),
    PENDING_DELIVERY(1, "待发货"),
    PENDING_RECEIVE(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final Integer code;
    private final String desc;

    public static OrderStatus of(Integer code) {
        if (code == null) return null;
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
