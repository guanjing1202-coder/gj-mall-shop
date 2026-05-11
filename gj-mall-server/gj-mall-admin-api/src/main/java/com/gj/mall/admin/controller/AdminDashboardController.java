package com.gj.mall.admin.controller;

import com.gj.mall.admin.service.AdminDashboardService;
import com.gj.mall.admin.vo.DashboardBusinessVO;
import com.gj.mall.admin.vo.DashboardOverviewVO;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台首页", description = "管理后台统计看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @Operation(summary = "首页统计概览")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @Operation(summary = "经营数据看板")
    @GetMapping("/business")
    public Result<DashboardBusinessVO> business() {
        return Result.success(dashboardService.business());
    }
}
