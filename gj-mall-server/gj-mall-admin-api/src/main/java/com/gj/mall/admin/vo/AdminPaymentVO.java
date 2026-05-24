package com.gj.mall.admin.vo;

import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.user.entity.UmsUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "后台支付记录")
public class AdminPaymentVO {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String payNo;
    private String thirdPayNo;
    private Integer channel;
    private String channelDesc;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime payTime;
    private String callbackData;
    private Integer orderStatus;
    private String orderStatusDesc;
    private BigDecimal orderPayAmount;
    private LocalDateTime orderPayTime;
    private AdminRefundRecordVO refundRecord;
    private Boolean syncStatusAllowed;
    private String syncStatusReason;
    private Long syncCallbackId;
    private String syncCallbackNo;
    private String syncThirdPayNo;
    private LocalDateTime syncCallbackTime;
    private Boolean refundAllowed;
    private String refundReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminPaymentVO from(PayPaymentRecord record, OmsOrder order, UmsUser user) {
        return from(record, order, user, null);
    }

    public static AdminPaymentVO from(PayPaymentRecord record, OmsOrder order, UmsUser user, PayRefundRecord refundRecord) {
        AdminPaymentVO vo = new AdminPaymentVO();
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(record.getOrderNo());
        vo.setUserId(record.getUserId());
        vo.setPayNo(record.getPayNo());
        vo.setThirdPayNo(record.getThirdPayNo());
        vo.setChannel(record.getChannel());
        vo.setChannelDesc(channelDesc(record.getChannel()));
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(paymentStatusDesc(record.getStatus()));
        vo.setPayTime(record.getPayTime());
        vo.setCallbackData(record.getCallbackData());
        vo.setRefundRecord(AdminRefundRecordVO.from(refundRecord));
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        if (order != null) {
            vo.setOrderStatus(order.getStatus());
            OrderStatus status = OrderStatus.of(order.getStatus());
            vo.setOrderStatusDesc(status == null ? "未知" : status.getDesc());
            vo.setOrderPayAmount(order.getPayAmount());
            vo.setOrderPayTime(order.getPayTime());
        }
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
        }
        return vo;
    }

    private static String channelDesc(Integer channel) {
        PayChannel payChannel = PayChannel.of(channel);
        return payChannel == null ? "未知渠道" : payChannel.getDesc();
    }

    private static String paymentStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "已支付";
            case 2:
                return "支付失败";
            case 3:
                return "已退款";
            default:
                return "未知";
        }
    }
}
