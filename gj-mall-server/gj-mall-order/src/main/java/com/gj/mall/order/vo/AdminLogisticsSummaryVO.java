package com.gj.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminLogisticsSummaryVO {
    private Long pendingDeliveryCount;
    private Long pendingReceiveCount;
    private Long shippedTodayCount;
    private Long receivedTodayCount;
    private Long activeCompanyCount;
    private BigDecimal pendingDeliveryAmount;
    private BigDecimal pendingReceiveAmount;
    private BigDecimal shippedTodayAmount;
    private BigDecimal receivedTodayAmount;
    private Long overdueDeliveryCount;
    private Long overdueReceiveCount;
    private BigDecimal overdueDeliveryAmount;
    private BigDecimal overdueReceiveAmount;
}
