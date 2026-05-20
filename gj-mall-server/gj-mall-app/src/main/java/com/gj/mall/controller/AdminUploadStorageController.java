package com.gj.mall.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.service.UploadStorageService;
import com.gj.mall.vo.UploadCleanupResultVO;
import com.gj.mall.vo.UploadStorageSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-上传存储运维")
@RestController
@RequestMapping("/api/admin/upload-storage")
@RequiredArgsConstructor
public class AdminUploadStorageController {

    private final UploadStorageService uploadStorageService;

    @Operation(summary = "上传文件存储概览")
    @GetMapping("/summary")
    public Result<UploadStorageSummaryVO> summary(@RequestParam(required = false) Integer retainDays) {
        return Result.success(uploadStorageService.summary(retainDays));
    }

    @Operation(summary = "清理未引用上传文件")
    @PostMapping("/cleanup")
    public Result<UploadCleanupResultVO> cleanup(@RequestParam(defaultValue = "7") Integer retainDays,
                                                 @RequestParam(defaultValue = "true") Boolean dryRun) {
        return Result.success(uploadStorageService.cleanup(retainDays, dryRun));
    }
}
