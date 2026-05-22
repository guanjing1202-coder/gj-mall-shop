package com.gj.mall.admin.vo;

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
}
