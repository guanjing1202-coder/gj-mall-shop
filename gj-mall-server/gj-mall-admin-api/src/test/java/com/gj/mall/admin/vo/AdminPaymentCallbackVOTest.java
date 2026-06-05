package com.gj.mall.admin.vo;

import com.gj.mall.order.entity.PayCallbackRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminPaymentCallbackVOTest {

    @Test
    void mapsIgnoredCallbackStatusToBroadIgnoredLabel() {
        PayCallbackRecord record = new PayCallbackRecord();
        record.setChannel(1);
        record.setSignatureStatus(0);
        record.setProcessStatus(2);

        AdminPaymentCallbackVO vo = AdminPaymentCallbackVO.from(record);

        assertEquals("已忽略", vo.getProcessStatusDesc());
    }

    @Test
    void extractsAlipayPayloadFieldsForOperationsInspection() {
        PayCallbackRecord record = new PayCallbackRecord();
        record.setChannel(2);
        record.setPayNo("P202605210001");
        record.setThirdPayNo("2026052222001400000001");
        record.setNotifyId("2026052200222000000001");
        record.setEventType("TRADE_SUCCESS");
        record.setSignatureStatus(1);
        record.setProcessStatus(3);
        record.setErrorMessage("支付宝回调 AppID 与当前配置不一致");
        record.setRawData("{\"app_id\":\"2021000123456789\",\"out_trade_no\":\"P202605210001\",\"trade_no\":\"2026052222001400000001\",\"notify_id\":\"2026052200222000000001\",\"trade_status\":\"TRADE_SUCCESS\",\"total_amount\":\"99.00\"}");

        AdminPaymentCallbackVO vo = AdminPaymentCallbackVO.from(record);

        assertEquals("2021000123456789", vo.getChannelAppId());
        assertEquals("P202605210001", vo.getChannelOrderNo());
        assertEquals("2026052222001400000001", vo.getChannelTradeNo());
        assertEquals("2026052200222000000001", vo.getChannelNotifyId());
        assertEquals("TRADE_SUCCESS", vo.getChannelTradeStatus());
        assertEquals("99.00", vo.getChannelAmount());
        assertEquals("验签通过 / 处理失败 / 原因：支付宝回调 AppID 与当前配置不一致", vo.getValidationSummary());
    }
}
