package com.gj.mall.admin.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.enums.PayChannel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminPaymentCallbackVO {
    private Long id;
    private String callbackNo;
    private Integer channel;
    private String channelDesc;
    private String channelName;
    private String payNo;
    private String thirdPayNo;
    private String notifyId;
    private String eventType;
    private BigDecimal amount;
    private Integer signatureStatus;
    private String signatureStatusDesc;
    private Integer processStatus;
    private String processStatusDesc;
    private Integer retryCount;
    private String errorMessage;
    private String rawData;
    private String requestHeaders;
    private String channelAppId;
    private String channelOrderNo;
    private String channelTradeNo;
    private String channelNotifyId;
    private String channelTradeStatus;
    private String channelAmount;
    private String validationSummary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminPaymentCallbackVO from(PayCallbackRecord record) {
        AdminPaymentCallbackVO vo = new AdminPaymentCallbackVO();
        vo.setId(record.getId());
        vo.setCallbackNo(record.getCallbackNo());
        vo.setChannel(record.getChannel());
        PayChannel channel = PayChannel.of(record.getChannel());
        vo.setChannelDesc(channel == null ? "未知渠道" : channel.getDesc());
        vo.setChannelName(record.getChannelName());
        vo.setPayNo(record.getPayNo());
        vo.setThirdPayNo(record.getThirdPayNo());
        vo.setNotifyId(record.getNotifyId());
        vo.setEventType(record.getEventType());
        vo.setAmount(record.getAmount());
        vo.setSignatureStatus(record.getSignatureStatus());
        vo.setSignatureStatusDesc(signatureStatusDesc(record.getSignatureStatus()));
        vo.setProcessStatus(record.getProcessStatus());
        vo.setProcessStatusDesc(processStatusDesc(record.getProcessStatus()));
        vo.setRetryCount(record.getRetryCount());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setRawData(record.getRawData());
        vo.setRequestHeaders(record.getRequestHeaders());
        JSONObject rawPayload = parseRawPayload(record.getRawData());
        vo.setChannelAppId(firstText(rawPayload, "app_id", "appid", "appId", "mchid"));
        vo.setChannelOrderNo(firstText(rawPayload, "out_trade_no", "outTradeNo", "payNo"));
        vo.setChannelTradeNo(firstText(rawPayload, "trade_no", "transaction_id", "thirdPayNo", "transactionId"));
        vo.setChannelNotifyId(firstText(rawPayload, "notify_id", "notifyId", "eventId", "id"));
        vo.setChannelTradeStatus(firstText(rawPayload, "trade_status", "tradeState", "eventType", "event_type"));
        vo.setChannelAmount(firstText(rawPayload, "total_amount", "amount", "totalAmount", "payer_total"));
        vo.setValidationSummary(validationSummary(vo));
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private static String signatureStatusDesc(Integer status) {
        if (Integer.valueOf(0).equals(status)) return "跳过验签";
        if (Integer.valueOf(1).equals(status)) return "验签通过";
        if (Integer.valueOf(2).equals(status)) return "验签失败";
        return "未知";
    }

    private static String processStatusDesc(Integer status) {
        if (Integer.valueOf(0).equals(status)) return "已接收";
        if (Integer.valueOf(1).equals(status)) return "已处理";
        if (Integer.valueOf(2).equals(status)) return "已忽略";
        if (Integer.valueOf(3).equals(status)) return "处理失败";
        return "未知";
    }

    private static JSONObject parseRawPayload(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(rawData);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static String firstText(JSONObject payload, String... keys) {
        for (String key : keys) {
            String value = payload.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String validationSummary(AdminPaymentCallbackVO vo) {
        StringBuilder builder = new StringBuilder();
        builder.append(vo.getSignatureStatusDesc());
        builder.append(" / ");
        builder.append(vo.getProcessStatusDesc());
        if (vo.getErrorMessage() != null && !vo.getErrorMessage().trim().isEmpty()) {
            builder.append(" / 原因：").append(vo.getErrorMessage().trim());
        }
        return builder.toString();
    }
}
