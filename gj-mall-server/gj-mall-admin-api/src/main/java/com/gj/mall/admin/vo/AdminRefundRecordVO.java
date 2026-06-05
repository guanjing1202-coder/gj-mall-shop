package com.gj.mall.admin.vo;

import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.enums.PayChannel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "后台退款记录")
public class AdminRefundRecordVO {
    private Long id;
    private String refundNo;
    private Long paymentId;
    private String payNo;
    private String thirdPayNo;
    private Long afterSaleId;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Integer channel;
    private String channelDesc;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private String reason;
    private String operatorType;
    private String callbackData;
    private LocalDateTime successTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @JsonIgnore
    private PayRefundRecord rawRecord;

    public static AdminRefundRecordVO from(PayRefundRecord record) {
        if (record == null) {
            return null;
        }
        AdminRefundRecordVO vo = new AdminRefundRecordVO();
        vo.setId(record.getId());
        vo.setRefundNo(record.getRefundNo());
        vo.setPaymentId(record.getPaymentId());
        vo.setPayNo(record.getPayNo());
        vo.setThirdPayNo(record.getThirdPayNo());
        vo.setAfterSaleId(record.getAfterSaleId());
        vo.setAfterSaleNo(record.getAfterSaleNo());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(record.getOrderNo());
        vo.setUserId(record.getUserId());
        vo.setChannel(record.getChannel());
        vo.setChannelDesc(channelDesc(record.getChannel()));
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(statusDesc(record.getStatus()));
        vo.setReason(record.getReason());
        vo.setOperatorType(record.getOperatorType());
        vo.setCallbackData(record.getCallbackData());
        vo.setSuccessTime(record.getSuccessTime());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        vo.setRawRecord(record);
        return vo;
    }

    private static String channelDesc(Integer channel) {
        PayChannel payChannel = PayChannel.of(channel);
        return payChannel == null ? "未知渠道" : payChannel.getDesc();
    }

    private static String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "退款中";
            case 1:
                return "退款成功";
            case 2:
                return "退款失败";
            default:
                return "未知";
        }
    }
}
