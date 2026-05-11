package com.gj.mall.controller;

import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口
 */
@Tag(name = "0-健康检查", description = "服务可用性检查")
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @Operation(summary = "ping")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("app", "gj-mall-shop");
        data.put("ts", System.currentTimeMillis());
        return Result.success(data);
    }
}
