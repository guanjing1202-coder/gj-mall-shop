package com.gj.mall.order.vo;

import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.enums.PayChannel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AfterSaleRefundRecordVO {
    private Long id;
    private String refundNo;
    private Long paymentId;
    private String payNo;
    private String thirdPayNo;
    private Long afterSaleId;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Integer channel;
    private String channelDesc;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private String reason;
    private String operatorType;
    private LocalDateTime successTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AfterSaleRefundRecordVO from(PayRefundRecord record) {
        if (record == null) {
            return null;
        }
        AfterSaleRefundRecordVO vo = new AfterSaleRefundRecordVO();
        vo.setId(record.getId());
        vo.setRefundNo(record.getRefundNo());
        vo.setPaymentId(record.getPaymentId());
        vo.setPayNo(record.getPayNo());
        vo.setThirdPayNo(record.getThirdPayNo());
        vo.setAfterSaleId(record.getAfterSaleId());
        vo.setAfterSaleNo(record.getAfterSaleNo());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(record.getOrderNo());
        vo.setChannel(record.getChannel());
        vo.setChannelDesc(channelDesc(record.getChannel()));
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(statusDesc(record.getStatus()));
        vo.setReason(record.getReason());
        vo.setOperatorType(record.getOperatorType());
        vo.setSuccessTime(record.getSuccessTime());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
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
