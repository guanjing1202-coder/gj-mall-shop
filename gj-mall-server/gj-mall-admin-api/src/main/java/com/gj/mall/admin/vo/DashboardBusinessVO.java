package com.gj.mall.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DashboardBusinessVO {

    private Long productTotal;
    private Long productOnSale;
    private Long lowStockSkuCount;
    private Long emptyStockSkuCount;
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
    private Long refundingCount;
    private Long refundedCount;

    private BigDecimal paidAmountTotal = BigDecimal.ZERO;
    private BigDecimal todayPaidAmount = BigDecimal.ZERO;
    private BigDecimal pendingDeliveryAmount = BigDecimal.ZERO;

    private Long pendingAfterSaleCount;
    private Long pendingCommentCount;
    private Long activeCouponCount;
    private Long activeSeckillCount;

    private List<TrendItem> orderTrend = new ArrayList<>();
    private List<HotProductItem> hotProducts = new ArrayList<>();
    private List<LowStockItem> lowStockSkus = new ArrayList<>();
    private List<LatestOrderItem> latestOrders = new ArrayList<>();

    @Data
    public static class TrendItem {
        private LocalDate date;
        private Long orderCount;
        private BigDecimal paidAmount = BigDecimal.ZERO;
    }

    @Data
    public static class HotProductItem {
        private Long id;
        private String name;
        private String mainImage;
        private BigDecimal price;
        private Integer saleCount;
    }

    @Data
    public static class LowStockItem {
        private Long skuId;
        private Long spuId;
        private String spuName;
        private String skuName;
        private String skuCode;
        private Integer stock;
        private Integer warnStock;
        private Integer alertGap;
    }

    @Data
    public static class LatestOrderItem {
        private Long id;
        private String orderNo;
        private Long userId;
        private BigDecimal payAmount;
        private Integer status;
        private String statusDesc;
        private LocalDateTime createTime;
    }
}
