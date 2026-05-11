package com.gj.mall.order.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.order.dto.AdminDeliveryCompanyQueryDTO;
import com.gj.mall.order.dto.AdminDeliveryCompanySaveDTO;
import com.gj.mall.order.dto.AdminLogisticsOrderQueryDTO;
import com.gj.mall.order.dto.AdminOrderDeliverDTO;
import com.gj.mall.order.service.LogisticsService;
import com.gj.mall.order.vo.AdminDeliveryCompanyVO;
import com.gj.mall.order.vo.AdminLogisticsSummaryVO;
import com.gj.mall.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "15-物流发货-后台", description = "物流/发货管理")
@RestController
@RequestMapping("/api/admin/logistics")
@RequiredArgsConstructor
public class LogisticsAdminController {

    private final LogisticsService logisticsService;

    @Operation(summary = "物流履约汇总")
    @GetMapping("/summary")
    public Result<AdminLogisticsSummaryVO> summary() {
        return Result.success(logisticsService.summary());
    }

    @Operation(summary = "物流订单分页")
    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> orders(AdminLogisticsOrderQueryDTO query) {
        return Result.success(logisticsService.orderPage(query));
    }

    @Operation(summary = "物流发货")
    @PostMapping("/orders/{id}/deliver")
    public Result<OrderVO> deliver(@PathVariable Long id,
                                   @RequestBody(required = false) AdminOrderDeliverDTO dto) {
        return Result.success(logisticsService.deliver(id, dto));
    }

    @Operation(summary = "物流公司选项")
    @GetMapping("/companies/options")
    public Result<List<AdminDeliveryCompanyVO>> companyOptions() {
        return Result.success(logisticsService.enabledCompanies());
    }

    @Operation(summary = "物流公司分页")
    @GetMapping("/companies/page")
    public Result<PageResult<AdminDeliveryCompanyVO>> companyPage(AdminDeliveryCompanyQueryDTO query) {
        return Result.success(logisticsService.companyPage(query));
    }

    @Operation(summary = "新增物流公司")
    @PostMapping("/companies")
    public Result<Long> createCompany(@Valid @RequestBody AdminDeliveryCompanySaveDTO dto) {
        return Result.success(logisticsService.createCompany(dto));
    }

    @Operation(summary = "修改物流公司")
    @PutMapping("/companies")
    public Result<Void> updateCompany(@Valid @RequestBody AdminDeliveryCompanySaveDTO dto) {
        logisticsService.updateCompany(dto);
        return Result.success();
    }

    @Operation(summary = "启停物流公司")
    @PutMapping("/companies/{id}/status")
    public Result<Void> updateCompanyStatus(@PathVariable Long id, @RequestParam Integer status) {
        logisticsService.updateCompanyStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除物流公司")
    @DeleteMapping("/companies/{id}")
    public Result<Void> deleteCompany(@PathVariable Long id) {
        logisticsService.deleteCompany(id);
        return Result.success();
    }
}
