package com.gj.mall.admin.service.impl;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import com.gj.mall.admin.dto.AdminReportQueryDTO;
import com.gj.mall.admin.mapper.AdminReportMapper;
import com.gj.mall.admin.service.AdminReportService;
import com.gj.mall.admin.vo.AdminSalesReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private static final int MAX_DAYS = 90;

    private final AdminReportMapper reportMapper;

    @Override
    public AdminSalesReportVO salesReport(AdminReportQueryDTO query) {
        ReportRange range = normalizeRange(query);
        LocalDateTime start = range.start.atStartOfDay();
        LocalDateTime end = range.end.plusDays(1).atStartOfDay();

        AdminSalesReportVO report = new AdminSalesReportVO();
        report.setStartDate(range.start);
        report.setEndDate(range.end);
        report.setGranularity(range.granularity);

        AdminSalesReportVO.Overview overview = reportMapper.selectOverview(start, end);
        if (overview == null) {
            overview = new AdminSalesReportVO.Overview();
        }
        overview.setNewMemberCount(safeLong(reportMapper.selectNewMemberCount(start, end)));
        overview.setRefundAmount(safeAmount(reportMapper.selectRefundAmount(start, end)));
        overview.setNetAmount(safeAmount(overview.getPaidAmount()).subtract(safeAmount(overview.getRefundAmount())));
        overview.setAverageOrderAmount(average(safeAmount(overview.getPaidAmount()), overview.getPaidOrderCount()));
        overview.setPaidConversionRate(percent(overview.getPaidOrderCount(), overview.getOrderCount()));
        overview.setRefundRate(percent(overview.getRefundOrderCount(), overview.getPaidOrderCount()));
        report.setOverview(overview);

        report.setSalesTrend(reportMapper.selectSalesTrend(start, end));
        report.setProductRanks(reportMapper.selectProductRanks(start, end, 10));
        report.setCategoryRanks(reportMapper.selectCategoryRanks(start, end, 8));
        report.setRefundTrend(reportMapper.selectRefundTrend(start, end));
        report.setMemberGrowth(reportMapper.selectMemberGrowth(start, end));
        fillTrendBuckets(report, range);
        return report;
    }

    @Override
    public String exportSalesReportCsv(AdminReportQueryDTO query) {
        AdminSalesReportVO report = salesReport(query);
        StringWriter stringWriter = new StringWriter();
        CsvWriter writer = CsvUtil.getWriter(stringWriter);
        writer.write(new String[] { "GJ Mall 经营报表" });
        writer.write(new String[] { "统计周期", report.getStartDate() + " 至 " + report.getEndDate() });
        writer.write(new String[] {});
        writer.write(new String[] { "概览指标", "数值" });
        AdminSalesReportVO.Overview overview = report.getOverview();
        writer.write(new String[] { "订单数", String.valueOf(safeLong(overview.getOrderCount())) });
        writer.write(new String[] { "支付订单数", String.valueOf(safeLong(overview.getPaidOrderCount())) });
        writer.write(new String[] { "实收金额", money(overview.getPaidAmount()) });
        writer.write(new String[] { "退款金额", money(overview.getRefundAmount()) });
        writer.write(new String[] { "净收入", money(overview.getNetAmount()) });
        writer.write(new String[] { "客单价", money(overview.getAverageOrderAmount()) });
        writer.write(new String[] { "支付转化率", overview.getPaidConversionRate() + "%" });
        writer.write(new String[] { "退款率", overview.getRefundRate() + "%" });
        writer.write(new String[] { "新增会员", String.valueOf(safeLong(overview.getNewMemberCount())) });
        writer.write(new String[] {});
        writer.write(new String[] { "周期", "订单数", "支付订单", "实收金额", "净收入", "退款金额", "新增会员" });
        Map<LocalDate, AdminSalesReportVO.RefundTrendItem> refundMap = report.getRefundTrend().stream()
                .collect(Collectors.toMap(AdminSalesReportVO.RefundTrendItem::getDate, Function.identity(), (a, b) -> a));
        Map<LocalDate, AdminSalesReportVO.MemberGrowthItem> memberMap = report.getMemberGrowth().stream()
                .collect(Collectors.toMap(AdminSalesReportVO.MemberGrowthItem::getDate, Function.identity(), (a, b) -> a));
        for (AdminSalesReportVO.TrendItem item : report.getSalesTrend()) {
            AdminSalesReportVO.RefundTrendItem refund = refundMap.get(item.getDate());
            AdminSalesReportVO.MemberGrowthItem member = memberMap.get(item.getDate());
            writer.write(new String[] {
                    String.valueOf(item.getDate()),
                    String.valueOf(safeLong(item.getOrderCount())),
                    String.valueOf(safeLong(item.getPaidOrderCount())),
                    money(item.getPaidAmount()),
                    money(item.getNetAmount()),
                    money(refund == null ? BigDecimal.ZERO : refund.getRefundAmount()),
                    String.valueOf(member == null ? 0L : safeLong(member.getNewMemberCount()))
            });
        }
        writer.write(new String[] {});
        writer.write(new String[] { "商品ID", "商品名称", "销量", "订单数", "销售额", "均价" });
        for (AdminSalesReportVO.ProductRankItem item : report.getProductRanks()) {
            writer.write(new String[] {
                    String.valueOf(item.getSpuId()),
                    item.getProductName(),
                    String.valueOf(safeLong(item.getSaleQuantity())),
                    String.valueOf(safeLong(item.getOrderCount())),
                    money(item.getSalesAmount()),
                    money(item.getAveragePrice())
            });
        }
        writer.close();
        return "\uFEFF" + stringWriter;
    }

    private void fillTrendBuckets(AdminSalesReportVO report, ReportRange range) {
        Map<LocalDate, AdminSalesReportVO.TrendItem> trendMap = report.getSalesTrend().stream()
                .collect(Collectors.toMap(item -> bucketStart(item.getDate(), range), Function.identity(), this::mergeTrend));
        Map<LocalDate, AdminSalesReportVO.RefundTrendItem> refundMap = report.getRefundTrend().stream()
                .collect(Collectors.toMap(item -> bucketStart(item.getDate(), range), Function.identity(), this::mergeRefund));
        Map<LocalDate, AdminSalesReportVO.MemberGrowthItem> memberMap = report.getMemberGrowth().stream()
                .collect(Collectors.toMap(item -> bucketStart(item.getDate(), range), Function.identity(), this::mergeMember));

        report.getSalesTrend().clear();
        report.getRefundTrend().clear();
        report.getMemberGrowth().clear();
        for (LocalDate date = firstBucketStart(range.start, range); !date.isAfter(range.end); date = nextBucketStart(date, range)) {
            LocalDate bucket = date.isBefore(range.start) ? range.start : date;
            AdminSalesReportVO.TrendItem trend = trendMap.getOrDefault(bucket, new AdminSalesReportVO.TrendItem());
            trend.setDate(bucket);
            report.getSalesTrend().add(trend);

            AdminSalesReportVO.RefundTrendItem refund = refundMap.getOrDefault(bucket, new AdminSalesReportVO.RefundTrendItem());
            refund.setDate(bucket);
            report.getRefundTrend().add(refund);

            AdminSalesReportVO.MemberGrowthItem member = memberMap.getOrDefault(bucket, new AdminSalesReportVO.MemberGrowthItem());
            member.setDate(bucket);
            report.getMemberGrowth().add(member);
        }
    }

    private LocalDate bucketStart(LocalDate date, ReportRange range) {
        LocalDate start;
        if ("week".equals(range.granularity)) {
            start = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        } else if ("month".equals(range.granularity)) {
            start = date.withDayOfMonth(1);
        } else {
            start = date;
        }
        return start.isBefore(range.start) ? range.start : start;
    }

    private LocalDate firstBucketStart(LocalDate start, ReportRange range) {
        if ("week".equals(range.granularity)) {
            return start.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        }
        if ("month".equals(range.granularity)) {
            return start.withDayOfMonth(1);
        }
        return start;
    }

    private LocalDate nextBucketStart(LocalDate date, ReportRange range) {
        if ("week".equals(range.granularity)) {
            return date.plusWeeks(1);
        }
        if ("month".equals(range.granularity)) {
            return date.plusMonths(1).withDayOfMonth(1);
        }
        return date.plusDays(1);
    }

    private AdminSalesReportVO.TrendItem mergeTrend(
            AdminSalesReportVO.TrendItem left,
            AdminSalesReportVO.TrendItem right) {
        left.setOrderCount(safeLong(left.getOrderCount()) + safeLong(right.getOrderCount()));
        left.setPaidOrderCount(safeLong(left.getPaidOrderCount()) + safeLong(right.getPaidOrderCount()));
        left.setPaidAmount(safeAmount(left.getPaidAmount()).add(safeAmount(right.getPaidAmount())));
        left.setNetAmount(safeAmount(left.getNetAmount()).add(safeAmount(right.getNetAmount())));
        return left;
    }

    private AdminSalesReportVO.RefundTrendItem mergeRefund(
            AdminSalesReportVO.RefundTrendItem left,
            AdminSalesReportVO.RefundTrendItem right) {
        left.setRefundCount(safeLong(left.getRefundCount()) + safeLong(right.getRefundCount()));
        left.setRefundAmount(safeAmount(left.getRefundAmount()).add(safeAmount(right.getRefundAmount())));
        return left;
    }

    private AdminSalesReportVO.MemberGrowthItem mergeMember(
            AdminSalesReportVO.MemberGrowthItem left,
            AdminSalesReportVO.MemberGrowthItem right) {
        left.setNewMemberCount(safeLong(left.getNewMemberCount()) + safeLong(right.getNewMemberCount()));
        return left;
    }

    private ReportRange normalizeRange(AdminReportQueryDTO query) {
        LocalDate today = LocalDate.now();
        LocalDate end = query == null || query.getEndDate() == null ? today : query.getEndDate();
        LocalDate start = query == null || query.getStartDate() == null ? end.minusDays(29) : query.getStartDate();
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        if (start.plusDays(MAX_DAYS - 1L).isBefore(end)) {
            start = end.minusDays(MAX_DAYS - 1L);
        }
        String granularity = normalizeGranularity(query == null ? null : query.getGranularity());
        return new ReportRange(start, end, granularity);
    }

    private String normalizeGranularity(String granularity) {
        String value = granularity == null ? "day" : granularity.trim().toLowerCase(Locale.ROOT);
        if ("week".equals(value) || "month".equals(value)) {
            return value;
        }
        return "day";
    }

    private BigDecimal percent(Long numerator, Long denominator) {
        long down = denominator == null ? 0L : denominator;
        if (down <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(safeLong(numerator))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(down), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(BigDecimal amount, Long count) {
        long safeCount = safeLong(count);
        if (safeCount <= 0) {
            return BigDecimal.ZERO;
        }
        return safeAmount(amount).divide(BigDecimal.valueOf(safeCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String money(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static class ReportRange {
        private final LocalDate start;
        private final LocalDate end;
        private final String granularity;

        private ReportRange(LocalDate start, LocalDate end, String granularity) {
            this.start = start;
            this.end = end;
            this.granularity = granularity;
        }
    }
}
