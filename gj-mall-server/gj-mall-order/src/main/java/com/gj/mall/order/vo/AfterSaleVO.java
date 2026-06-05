package com.gj.mall.order.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AfterSaleVO {
    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
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
    private LocalDateTime auditTime;
    private LocalDateTime receiveTime;
    private LocalDateTime refundTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private AfterSaleRefundRecordVO refundRecord;
    private List<AfterSaleTimelineItemVO> timeline;
    private List<OrderItemVO> items;

    public static AfterSaleVO from(OmsAfterSale afterSale, List<OmsOrderItem> items) {
        return from(afterSale, items, null);
    }

    public static AfterSaleVO from(OmsAfterSale afterSale, List<OmsOrderItem> items, PayRefundRecord refundRecord) {
        AfterSaleVO vo = new AfterSaleVO();
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
        vo.setStatusDesc(statusDesc(afterSale));
        vo.setAuditRemark(afterSale.getAuditRemark());
        vo.setRejectReason(afterSale.getRejectReason());
        vo.setReturnCompany(afterSale.getReturnCompany());
        vo.setReturnNo(afterSale.getReturnNo());
        vo.setAuditTime(afterSale.getAuditTime());
        vo.setReceiveTime(afterSale.getReceiveTime());
        vo.setRefundTime(afterSale.getRefundTime());
        vo.setCreateTime(afterSale.getCreateTime());
        vo.setUpdateTime(afterSale.getUpdateTime());
        vo.setRefundRecord(AfterSaleRefundRecordVO.from(refundRecord));
        vo.setTimeline(AfterSaleTimelineBuilder.build(afterSale, refundRecord));
        vo.setItems(items == null ? Collections.emptyList()
                : items.stream().map(AfterSaleVO::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    public static String typeDesc(Integer type) {
        if (Integer.valueOf(1).equals(type)) {
            return "仅退款";
        }
        if (Integer.valueOf(2).equals(type)) {
            return "退货退款";
        }
        return "未知";
    }

    public static String statusDesc(Integer status) {
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

    private static String statusDesc(OmsAfterSale afterSale) {
        if (Integer.valueOf(1).equals(afterSale.getStatus())
                && afterSale.getReturnNo() != null
                && !afterSale.getReturnNo().trim().isEmpty()) {
            return "待商家收货";
        }
        return statusDesc(afterSale.getStatus());
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

    private static OrderItemVO toItemVO(OmsOrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setSpuId(item.getSpuId());
        vo.setSkuId(item.getSkuId());
        vo.setSkuName(item.getSkuName());
        vo.setSkuImage(item.getSkuImage());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setTotalAmount(item.getTotalAmount());
        if (item.getSpecData() != null && !item.getSpecData().trim().isEmpty()) {
            try {
                vo.setSpecData(JSON.parseObject(item.getSpecData(), new TypeReference<java.util.Map<String, String>>() {}));
            } catch (Exception ignored) {
            }
        }
        return vo;
    }
}
