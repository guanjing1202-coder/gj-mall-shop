package com.gj.mall.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "后台售后汇总")
public class AdminAfterSaleSummaryVO {

    private Long totalCount = 0L;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private Long pendingCount = 0L;
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    private Long waitReturnCount = 0L;
    private BigDecimal waitReturnAmount = BigDecimal.ZERO;

    private Long waitRefundCount = 0L;
    private BigDecimal waitRefundAmount = BigDecimal.ZERO;

    private Long rejectedCount = 0L;
    private BigDecimal rejectedAmount = BigDecimal.ZERO;

    private Long completedCount = 0L;
    private BigDecimal completedAmount = BigDecimal.ZERO;

    private Long canceledCount = 0L;
    private BigDecimal canceledAmount = BigDecimal.ZERO;

    private Long processingCount = 0L;
    private BigDecimal processingAmount = BigDecimal.ZERO;

    private Long todayNewCount = 0L;
    private BigDecimal todayNewAmount = BigDecimal.ZERO;

    private Long todayRefundedCount = 0L;
    private BigDecimal todayRefundedAmount = BigDecimal.ZERO;

    private BigDecimal completionRate = BigDecimal.ZERO;
    private BigDecimal rejectionRate = BigDecimal.ZERO;

    private List<TypeItem> types = new ArrayList<>();

    @Data
    @Schema(description = "售后类型汇总")
    public static class TypeItem {
        private Integer type;
        private String typeDesc;
        private Long count = 0L;
        private BigDecimal amount = BigDecimal.ZERO;

        public static TypeItem empty(Integer type, String typeDesc) {
            TypeItem item = new TypeItem();
            item.setType(type);
            item.setTypeDesc(typeDesc);
            return item;
        }
    }
}
