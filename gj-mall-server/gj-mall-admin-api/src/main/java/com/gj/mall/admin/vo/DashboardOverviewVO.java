package com.gj.mall.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台首页统计概览")
public class DashboardOverviewVO {

    private Long productTotal;
    private Long productOnSale;
    private Long brandTotal;
    private Long categoryTotal;

    private Long userTotal;
    private Long todayNewUsers;

    private Long orderTotal;
    private Long todayOrderCount;
    private Long pendingPayCount;
    private Long pendingDeliveryCount;
    private Long pendingReceiveCount;
    private Long completedCount;
    private Long canceledCount;

    private BigDecimal paidAmountTotal;
    private BigDecimal todayPaidAmount;
}
