package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminReportQueryDTO;
import com.gj.mall.admin.service.AdminReportService;
import com.gj.mall.admin.vo.AdminSalesReportVO;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "后台经营报表", description = "销售趋势、商品排行、退款和会员增长")
@RestController
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService reportService;

    @Operation(summary = "销售经营报表")
    @GetMapping("/sales")
    public Result<AdminSalesReportVO> sales(AdminReportQueryDTO query) {
        return Result.success(reportService.salesReport(query));
    }

    @Operation(summary = "导出销售经营报表")
    @GetMapping(value = "/sales/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportSales(AdminReportQueryDTO query) {
        String filename = UriUtils.encode("GJ商城经营报表.csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(reportService.exportSalesReportCsv(query));
    }
}
