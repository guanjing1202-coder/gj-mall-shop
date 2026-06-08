package com.gj.mall.pay.support;

import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WechatPayCallbackParserTest {

    @Test
    void mapsDecryptedWechatTransactionToUnifiedCallbackPayload() {
        Transaction transaction = new Transaction();
        transaction.setOutTradeNo("P2026052700010001");
        transaction.setTransactionId("4200002222202605270000000001");
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);
        TransactionAmount amount = new TransactionAmount();
        amount.setPayerTotal(19990);
        amount.setTotal(20000);
        transaction.setAmount(amount);

        Map<String, Object> payload = WechatPayCallbackPayload.from("EVT202605270001", transaction).toMap();

        assertEquals("P2026052700010001", payload.get("payNo"));
        assertEquals("P2026052700010001", payload.get("out_trade_no"));
        assertEquals("4200002222202605270000000001", payload.get("thirdPayNo"));
        assertEquals("4200002222202605270000000001", payload.get("transaction_id"));
        assertEquals("EVT202605270001", payload.get("notifyId"));
        assertEquals("EVT202605270001", payload.get("id"));
        assertEquals("SUCCESS", payload.get("eventType"));
        assertEquals("SUCCESS", payload.get("tradeState"));
        assertEquals(new BigDecimal("199.90"), payload.get("amount"));
        assertEquals(new BigDecimal("199.90"), payload.get("payer_total"));
    }

    @Test
    void fallsBackToTotalAmountWhenPayerTotalIsMissing() {
        Transaction transaction = new Transaction();
        transaction.setOutTradeNo("P2026052700020001");
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(6600);
        transaction.setAmount(amount);

        Map<String, Object> payload = WechatPayCallbackPayload.from("EVT202605270002", transaction).toMap();

        assertEquals(new BigDecimal("66.00"), payload.get("amount"));
        assertEquals(new BigDecimal("66.00"), payload.get("payer_total"));
    }
}
