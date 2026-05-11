package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminSeckillQueryDTO;
import com.gj.mall.admin.dto.AdminSeckillSaveDTO;
import com.gj.mall.admin.dto.AdminSeckillSkuSaveDTO;
import com.gj.mall.admin.service.AdminSeckillService;
import com.gj.mall.admin.vo.AdminSeckillVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "后台营销-秒杀", description = "后台秒杀活动管理")
@RestController
@RequestMapping("/api/admin/marketing/seckill")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final AdminSeckillService seckillService;

    @Operation(summary = "秒杀活动分页")
    @GetMapping("/page")
    public Result<PageResult<AdminSeckillVO>> page(AdminSeckillQueryDTO query) {
        return Result.success(seckillService.page(query));
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{id}")
    public Result<AdminSeckillVO> detail(@PathVariable Long id) {
        return Result.success(seckillService.detail(id));
    }

    @Operation(summary = "新增秒杀活动")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminSeckillSaveDTO dto) {
        return Result.success(seckillService.create(dto));
    }

    @Operation(summary = "修改秒杀活动")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AdminSeckillSaveDTO dto) {
        seckillService.update(dto);
        return Result.success();
    }

    @Operation(summary = "设置秒杀活动状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        seckillService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除秒杀活动")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seckillService.delete(id);
        return Result.success();
    }

    @Operation(summary = "保存秒杀 SKU")
    @PostMapping("/sku")
    public Result<Long> saveSku(@Valid @RequestBody AdminSeckillSkuSaveDTO dto) {
        return Result.success(seckillService.saveSku(dto));
    }

    @Operation(summary = "删除秒杀 SKU")
    @DeleteMapping("/sku/{skuId}")
    public Result<Void> deleteSku(@PathVariable Long skuId) {
        seckillService.deleteSku(skuId);
        return Result.success();
    }

    @Operation(summary = "预热秒杀库存")
    @PostMapping("/{id}/warm-up")
    public Result<Void> warmUp(@PathVariable Long id) {
        seckillService.warmUp(id);
        return Result.success();
    }
}
