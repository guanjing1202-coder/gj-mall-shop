package com.gj.mall.pay.support;

import com.gj.mall.order.enums.PayChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCallbackEventSupportTest {

    @Test
    void recognizesSuccessfulEventsByChannel() {
        PayCallbackEventSupport support = new PayCallbackEventSupport();

        assertTrue(support.isPaidEvent(PayChannel.WECHAT, "SUCCESS"));
        assertTrue(support.isPaidEvent(PayChannel.ALIPAY, "TRADE_SUCCESS"));
        assertTrue(support.isPaidEvent(PayChannel.ALIPAY, "TRADE_FINISHED"));
        assertTrue(support.isPaidEvent(PayChannel.MOCK, "SUCCESS"));
        assertTrue(support.isPaidEvent(PayChannel.MOCK, null));
    }

    @Test
    void rejectsNonSuccessfulEventsByChannel() {
        PayCallbackEventSupport support = new PayCallbackEventSupport();

        assertFalse(support.isPaidEvent(PayChannel.WECHAT, "NOTPAY"));
        assertFalse(support.isPaidEvent(PayChannel.WECHAT, "CLOSED"));
        assertFalse(support.isPaidEvent(PayChannel.ALIPAY, "TRADE_CLOSED"));
        assertFalse(support.isPaidEvent(PayChannel.ALIPAY, "WAIT_BUYER_PAY"));
        assertFalse(support.isPaidEvent(PayChannel.MOCK, "FAILED"));
    }
}
