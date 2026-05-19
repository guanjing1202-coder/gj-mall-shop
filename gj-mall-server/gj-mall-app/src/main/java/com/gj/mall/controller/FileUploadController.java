package com.gj.mall.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.service.UploadService;
import com.gj.mall.vo.UploadFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "00-文件上传", description = "通用图片上传")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final UploadService uploadService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result<UploadFileVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "scene", required = false) String scene) {
        return Result.success(uploadService.uploadImage(file, scene));
    }
}
