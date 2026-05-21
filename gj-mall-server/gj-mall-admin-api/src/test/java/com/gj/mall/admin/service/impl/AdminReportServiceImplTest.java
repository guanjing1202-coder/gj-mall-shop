package com.gj.mall.admin.service.impl;

import com.gj.mall.admin.dto.AdminReportQueryDTO;
import com.gj.mall.admin.mapper.AdminReportMapper;
import com.gj.mall.admin.vo.AdminSalesReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminReportServiceImplTest {

    private AdminReportMapper reportMapper;
    private AdminReportServiceImpl service;

    @BeforeEach
    void setUp() {
        reportMapper = mock(AdminReportMapper.class);
        service = new AdminReportServiceImpl(reportMapper);

        when(reportMapper.selectOverview(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new AdminSalesReportVO.Overview());
        when(reportMapper.selectProductRanks(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(Collections.emptyList());
        when(reportMapper.selectCategoryRanks(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(Collections.emptyList());
        when(reportMapper.selectRefundAmount(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void salesReportAggregatesDailyRowsIntoWeeklyBuckets() {
        AdminReportQueryDTO query = query("2026-05-10", "2026-05-24", "week");
        when(reportMapper.selectSalesTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList(
                        trend("2026-05-10", 2L, 1L, "100.00", "80.00"),
                        trend("2026-05-18", 1L, 1L, "50.00", "50.00")));
        when(reportMapper.selectRefundTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList(refund("2026-05-12", 2L, "30.00")));
        when(reportMapper.selectMemberGrowth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList(member("2026-05-24", 3L)));

        AdminSalesReportVO report = service.salesReport(query);

        assertEquals("week", report.getGranularity());
        assertEquals(Arrays.asList(
                LocalDate.parse("2026-05-10"),
                LocalDate.parse("2026-05-11"),
                LocalDate.parse("2026-05-18")
        ), report.getSalesTrend().stream().map(AdminSalesReportVO.TrendItem::getDate).collect(Collectors.toList()));
        assertEquals(2L, report.getSalesTrend().get(0).getOrderCount());
        assertEquals(new BigDecimal("100.00"), report.getSalesTrend().get(0).getPaidAmount());
        assertEquals(0L, report.getSalesTrend().get(1).getOrderCount());
        assertEquals(1L, report.getSalesTrend().get(2).getOrderCount());
        assertEquals(new BigDecimal("50.00"), report.getSalesTrend().get(2).getNetAmount());

        assertEquals(LocalDate.parse("2026-05-11"), report.getRefundTrend().get(1).getDate());
        assertEquals(2L, report.getRefundTrend().get(1).getRefundCount());
        assertEquals(new BigDecimal("30.00"), report.getRefundTrend().get(1).getRefundAmount());
        assertEquals(3L, report.getMemberGrowth().get(2).getNewMemberCount());
    }

    @Test
    void salesReportAggregatesDailyRowsIntoMonthlyBuckets() {
        AdminReportQueryDTO query = query("2026-04-20", "2026-06-10", "month");
        when(reportMapper.selectSalesTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList(
                        trend("2026-04-21", 1L, 1L, "88.00", "88.00"),
                        trend("2026-05-02", 4L, 3L, "300.00", "260.00")));
        when(reportMapper.selectRefundTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList(refund("2026-05-20", 1L, "40.00")));
        when(reportMapper.selectMemberGrowth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList());

        AdminSalesReportVO report = service.salesReport(query);

        assertEquals(Arrays.asList(
                LocalDate.parse("2026-04-20"),
                LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-06-01")
        ), report.getSalesTrend().stream().map(AdminSalesReportVO.TrendItem::getDate).collect(Collectors.toList()));
        assertEquals(1L, report.getSalesTrend().get(0).getOrderCount());
        assertEquals(4L, report.getSalesTrend().get(1).getOrderCount());
        assertEquals(new BigDecimal("300.00"), report.getSalesTrend().get(1).getPaidAmount());
        assertEquals(0L, report.getSalesTrend().get(2).getOrderCount());
        assertEquals(new BigDecimal("40.00"), report.getRefundTrend().get(1).getRefundAmount());
    }

    @Test
    void salesReportFallsBackToDayWhenGranularityIsUnsupported() {
        AdminReportQueryDTO query = query("2026-05-20", "2026-05-21", "quarter");
        when(reportMapper.selectSalesTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList());
        when(reportMapper.selectRefundTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList());
        when(reportMapper.selectMemberGrowth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList());

        AdminSalesReportVO report = service.salesReport(query);

        assertEquals("day", report.getGranularity());
        assertEquals(Arrays.asList(
                LocalDate.parse("2026-05-20"),
                LocalDate.parse("2026-05-21")
        ), report.getSalesTrend().stream().map(AdminSalesReportVO.TrendItem::getDate).collect(Collectors.toList()));
    }

    @Test
    void exportSalesReportCsvUsesSelectedGranularity() {
        AdminReportQueryDTO query = query("2026-05-01", "2026-05-31", "month");
        when(reportMapper.selectSalesTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList(
                        trend("2026-05-02", 1L, 1L, "88.00", "88.00"),
                        trend("2026-05-18", 2L, 2L, "120.00", "110.00")));
        when(reportMapper.selectRefundTrend(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList());
        when(reportMapper.selectMemberGrowth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mutableList());

        String csv = service.exportSalesReportCsv(query);

        assertFalse(csv.contains("2026-05-02,1,1,88.00"));
        assertFalse(csv.contains("2026-05-18,2,2,120.00"));
        assertEquals(1, countOccurrences(csv, "2026-05-01,3,3,208.00,198.00,0.00,0"));
    }

    private AdminReportQueryDTO query(String startDate, String endDate, String granularity) {
        AdminReportQueryDTO query = new AdminReportQueryDTO();
        query.setStartDate(LocalDate.parse(startDate));
        query.setEndDate(LocalDate.parse(endDate));
        query.setGranularity(granularity);
        return query;
    }

    @SafeVarargs
    private final <T> List<T> mutableList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private AdminSalesReportVO.TrendItem trend(
            String date,
            Long orderCount,
            Long paidOrderCount,
            String paidAmount,
            String netAmount) {
        AdminSalesReportVO.TrendItem item = new AdminSalesReportVO.TrendItem();
        item.setDate(LocalDate.parse(date));
        item.setOrderCount(orderCount);
        item.setPaidOrderCount(paidOrderCount);
        item.setPaidAmount(new BigDecimal(paidAmount));
        item.setNetAmount(new BigDecimal(netAmount));
        return item;
    }

    private AdminSalesReportVO.RefundTrendItem refund(String date, Long refundCount, String refundAmount) {
        AdminSalesReportVO.RefundTrendItem item = new AdminSalesReportVO.RefundTrendItem();
        item.setDate(LocalDate.parse(date));
        item.setRefundCount(refundCount);
        item.setRefundAmount(new BigDecimal(refundAmount));
        return item;
    }

    private AdminSalesReportVO.MemberGrowthItem member(String date, Long newMemberCount) {
        AdminSalesReportVO.MemberGrowthItem item = new AdminSalesReportVO.MemberGrowthItem();
        item.setDate(LocalDate.parse(date));
        item.setNewMemberCount(newMemberCount);
        return item;
    }

    private int countOccurrences(String source, String target) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
