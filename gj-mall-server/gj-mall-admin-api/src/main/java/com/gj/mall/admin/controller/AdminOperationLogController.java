package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminOperationLogQueryDTO;
import com.gj.mall.admin.service.AdminOperationLogService;
import com.gj.mall.admin.vo.AdminOperationLogVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台操作日志", description = "后台操作审计查询")
@RestController
@RequestMapping("/api/admin/operation-log")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService operationLogService;

    @Operation(summary = "操作日志分页")
    @GetMapping("/page")
    public Result<PageResult<AdminOperationLogVO>> page(AdminOperationLogQueryDTO query) {
        return Result.success(operationLogService.page(query));
    }
}
