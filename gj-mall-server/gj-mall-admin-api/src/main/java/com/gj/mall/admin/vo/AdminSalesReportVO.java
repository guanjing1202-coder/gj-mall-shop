package com.gj.mall.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminSalesReportVO {

    private LocalDate startDate;
    private LocalDate endDate;
    private String granularity;

    private Overview overview = new Overview();
    private List<TrendItem> salesTrend = new ArrayList<>();
    private List<ProductRankItem> productRanks = new ArrayList<>();
    private List<CategoryRankItem> categoryRanks = new ArrayList<>();
    private List<RefundTrendItem> refundTrend = new ArrayList<>();
    private List<MemberGrowthItem> memberGrowth = new ArrayList<>();

    @Data
    public static class Overview {
        private Long orderCount = 0L;
        private Long paidOrderCount = 0L;
        private Long refundOrderCount = 0L;
        private Long newMemberCount = 0L;
        private BigDecimal grossAmount = BigDecimal.ZERO;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal refundAmount = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;
        private BigDecimal averageOrderAmount = BigDecimal.ZERO;
        private BigDecimal paidConversionRate = BigDecimal.ZERO;
        private BigDecimal refundRate = BigDecimal.ZERO;
    }

    @Data
    public static class TrendItem {
        private LocalDate date;
        private Long orderCount = 0L;
        private Long paidOrderCount = 0L;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;
    }

    @Data
    public static class ProductRankItem {
        private Long spuId;
        private String productName;
        private Long saleQuantity = 0L;
        private Long orderCount = 0L;
        private BigDecimal salesAmount = BigDecimal.ZERO;
        private BigDecimal averagePrice = BigDecimal.ZERO;
    }

    @Data
    public static class CategoryRankItem {
        private Long categoryId;
        private String categoryName;
        private Long saleQuantity = 0L;
        private Long orderCount = 0L;
        private BigDecimal salesAmount = BigDecimal.ZERO;
    }

    @Data
    public static class RefundTrendItem {
        private LocalDate date;
        private Long refundCount = 0L;
        private BigDecimal refundAmount = BigDecimal.ZERO;
    }

    @Data
    public static class MemberGrowthItem {
        private LocalDate date;
        private Long newMemberCount = 0L;
    }
}
