package com.gj.mall.pay.support;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class WechatPayCallbackPayload {

    private final String eventId;
    private final Transaction transaction;

    private WechatPayCallbackPayload(String eventId, Transaction transaction) {
        this.eventId = eventId;
        this.transaction = transaction;
    }

    public static WechatPayCallbackPayload from(String eventId, Transaction transaction) {
        if (transaction == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "微信支付回调交易数据为空");
        }
        return new WechatPayCallbackPayload(eventId, transaction);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfNotBlank(payload, "payNo", transaction.getOutTradeNo());
        putIfNotBlank(payload, "out_trade_no", transaction.getOutTradeNo());
        putIfNotBlank(payload, "thirdPayNo", transaction.getTransactionId());
        putIfNotBlank(payload, "transaction_id", transaction.getTransactionId());
        putIfNotBlank(payload, "notifyId", eventId);
        putIfNotBlank(payload, "id", eventId);
        String tradeState = transaction.getTradeState() == null ? null : transaction.getTradeState().name();
        putIfNotBlank(payload, "eventType", tradeState);
        putIfNotBlank(payload, "tradeState", tradeState);
        BigDecimal amount = amount(transaction.getAmount());
        if (amount != null) {
            payload.put("amount", amount);
            payload.put("payer_total", amount);
        }
        return payload;
    }

    private BigDecimal amount(TransactionAmount amount) {
        if (amount == null) {
            return null;
        }
        Integer cents = amount.getPayerTotal() == null ? amount.getTotal() : amount.getPayerTotal();
        return cents == null ? null : BigDecimal.valueOf(cents, 2);
    }

    private void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            payload.put(key, value.trim());
        }
    }
}
