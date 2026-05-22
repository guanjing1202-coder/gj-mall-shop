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
}
