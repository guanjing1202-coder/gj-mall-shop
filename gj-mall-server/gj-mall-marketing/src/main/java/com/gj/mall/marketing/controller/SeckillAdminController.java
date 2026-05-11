package com.gj.mall.marketing.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.marketing.dto.SeckillCreateDTO;
import com.gj.mall.marketing.dto.SeckillSkuAddDTO;
import com.gj.mall.marketing.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "17-秒杀-后台", description = "秒杀（后台管理）")
@RestController
@RequestMapping("/api/admin/seckill")
@RequiredArgsConstructor
public class SeckillAdminController {

    private final SeckillService seckillService;

    @Operation(summary = "创建秒杀活动")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SeckillCreateDTO dto) {
        return Result.success(seckillService.adminCreate(dto));
    }

    @Operation(summary = "添加秒杀 SKU")
    @PostMapping("/sku")
    public Result<Void> addSku(@Valid @RequestBody SeckillSkuAddDTO dto) {
        seckillService.adminAddSku(dto);
        return Result.success();
    }

    @Operation(summary = "设置活动状态（0草稿 1上线 2结束）")
    @PutMapping("/{seckillId}/status")
    public Result<Void> setStatus(@PathVariable Long seckillId, @RequestParam Integer status) {
        seckillService.adminSetStatus(seckillId, status);
        return Result.success();
    }

    @Operation(summary = "手动预热库存到 Redis")
    @PostMapping("/{seckillId}/warm-up")
    public Result<Void> warmUp(@PathVariable Long seckillId) {
        seckillService.warmUpStock(seckillId);
        return Result.success();
    }
}
