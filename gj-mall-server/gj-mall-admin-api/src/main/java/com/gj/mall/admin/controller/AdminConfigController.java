package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminConfigQueryDTO;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.service.AdminConfigService;
import com.gj.mall.admin.vo.AdminConfigVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
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

@Tag(name = "系统配置", description = "商城运行参数管理")
@RestController
@RequestMapping("/api/admin/sys/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService configService;

    @Operation(summary = "系统配置分页")
    @GetMapping("/page")
    public Result<PageResult<AdminConfigVO>> page(AdminConfigQueryDTO query) {
        return Result.success(configService.page(query));
    }

    @Operation(summary = "系统配置详情")
    @GetMapping("/{id}")
    public Result<AdminConfigVO> detail(@PathVariable Long id) {
        return Result.success(configService.detail(id));
    }

    @Operation(summary = "新增系统配置")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminConfigSaveDTO dto) {
        return Result.success(configService.create(dto));
    }

    @Operation(summary = "修改系统配置")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AdminConfigSaveDTO dto) {
        configService.update(dto);
        return Result.success();
    }

    @Operation(summary = "启停系统配置")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        configService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除系统配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.success();
    }
}
