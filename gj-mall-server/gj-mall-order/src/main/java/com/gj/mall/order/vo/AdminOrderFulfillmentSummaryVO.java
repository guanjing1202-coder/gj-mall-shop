package com.gj.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderFulfillmentSummaryVO {

    private Long totalOrderCount;
    private Long pendingPayCount;
    private Long pendingDeliveryCount;
    private Long pendingReceiveCount;
    private Long completedCount;
    private Long canceledCount;
    private Long refundingCount;
    private Long refundedCount;
    private Long todayOrderCount;
    private BigDecimal pendingDeliveryAmount;
    private BigDecimal totalPayAmount;
}
