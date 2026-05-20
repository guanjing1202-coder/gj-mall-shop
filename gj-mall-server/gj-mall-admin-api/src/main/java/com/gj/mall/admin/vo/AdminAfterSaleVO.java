package com.gj.mall.admin.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.user.entity.UmsUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Schema(description = "后台售后单")
public class AdminAfterSaleVO {
    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private Integer type;
    private String typeDesc;
    private BigDecimal amount;
    private String reason;
    private String description;
    private List<String> images;
    private Integer orderStatusSnapshot;
    private String orderStatusSnapshotDesc;
    private Integer status;
    private String statusDesc;
    private String auditRemark;
    private String rejectReason;
    private String returnCompany;
    private String returnNo;
    private Long refundPaymentId;
    private LocalDateTime auditTime;
    private LocalDateTime receiveTime;
    private LocalDateTime refundTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private BigDecimal orderPayAmount;
    private Integer currentOrderStatus;
    private String currentOrderStatusDesc;
    private AdminRefundRecordVO refundRecord;
    private List<OmsOrderItem> items;

    public static AdminAfterSaleVO from(
            OmsAfterSale afterSale,
            OmsOrder order,
            UmsUser user,
            List<OmsOrderItem> items) {
        return from(afterSale, order, user, items, null);
    }

    public static AdminAfterSaleVO from(
            OmsAfterSale afterSale,
            OmsOrder order,
            UmsUser user,
            List<OmsOrderItem> items,
            AdminRefundRecordVO refundRecord) {
        AdminAfterSaleVO vo = new AdminAfterSaleVO();
        vo.setId(afterSale.getId());
        vo.setAfterSaleNo(afterSale.getAfterSaleNo());
        vo.setOrderId(afterSale.getOrderId());
        vo.setOrderNo(afterSale.getOrderNo());
        vo.setUserId(afterSale.getUserId());
        vo.setType(afterSale.getType());
        vo.setTypeDesc(typeDesc(afterSale.getType()));
        vo.setAmount(afterSale.getAmount());
        vo.setReason(afterSale.getReason());
        vo.setDescription(afterSale.getDescription());
        vo.setImages(parseImages(afterSale.getImages()));
        vo.setOrderStatusSnapshot(afterSale.getOrderStatusSnapshot());
        vo.setOrderStatusSnapshotDesc(orderStatusDesc(afterSale.getOrderStatusSnapshot()));
        vo.setStatus(afterSale.getStatus());
        vo.setStatusDesc(statusDesc(afterSale.getStatus()));
        vo.setAuditRemark(afterSale.getAuditRemark());
        vo.setRejectReason(afterSale.getRejectReason());
        vo.setReturnCompany(afterSale.getReturnCompany());
        vo.setReturnNo(afterSale.getReturnNo());
        vo.setRefundPaymentId(afterSale.getRefundPaymentId());
        vo.setAuditTime(afterSale.getAuditTime());
        vo.setReceiveTime(afterSale.getReceiveTime());
        vo.setRefundTime(afterSale.getRefundTime());
        vo.setCreateTime(afterSale.getCreateTime());
        vo.setUpdateTime(afterSale.getUpdateTime());
        vo.setItems(items == null ? Collections.emptyList() : items);
        vo.setRefundRecord(refundRecord);
        if (order != null) {
            vo.setOrderPayAmount(order.getPayAmount());
            vo.setCurrentOrderStatus(order.getStatus());
            vo.setCurrentOrderStatusDesc(orderStatusDesc(order.getStatus()));
        }
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
        }
        return vo;
    }

    private static String typeDesc(Integer type) {
        if (Integer.valueOf(1).equals(type)) {
            return "仅退款";
        }
        if (Integer.valueOf(2).equals(type)) {
            return "退货退款";
        }
        return "未知";
    }

    private static String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "待退货";
            case 2:
                return "待退款";
            case 3:
                return "已拒绝";
            case 4:
                return "已完成";
            case 5:
                return "已取消";
            default:
                return "未知";
        }
    }

    private static String orderStatusDesc(Integer status) {
        OrderStatus orderStatus = OrderStatus.of(status);
        return orderStatus == null ? "未知" : orderStatus.getDesc();
    }

    private static List<String> parseImages(String images) {
        if (images == null || images.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseObject(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
