package com.gj.mall.product.controller.admin;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.product.dto.AdminInventoryAdjustDTO;
import com.gj.mall.product.dto.AdminInventoryBatchAdjustDTO;
import com.gj.mall.product.dto.AdminInventoryLogQueryDTO;
import com.gj.mall.product.dto.AdminInventoryQueryDTO;
import com.gj.mall.product.dto.AdminInventoryWarnStockDTO;
import com.gj.mall.product.service.InventoryService;
import com.gj.mall.product.vo.AdminInventoryLogVO;
import com.gj.mall.product.vo.AdminInventorySummaryVO;
import com.gj.mall.product.vo.AdminInventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "23-后台-库存管理", description = "库存查询、调整与调整记录")
@RestController
@RequestMapping("/api/admin/product/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "库存分页")
    @GetMapping("/page")
    public Result<PageResult<AdminInventoryVO>> page(AdminInventoryQueryDTO query) {
        return Result.success(inventoryService.page(query));
    }

    @Operation(summary = "库存运营汇总")
    @GetMapping("/summary")
    public Result<AdminInventorySummaryVO> summary(AdminInventoryQueryDTO query) {
        return Result.success(inventoryService.summary(query));
    }

    @Operation(summary = "调整 SKU 可用库存")
    @PutMapping("/{skuId}/adjust")
    public Result<AdminInventoryVO> adjust(@PathVariable Long skuId,
                                           @Valid @RequestBody AdminInventoryAdjustDTO dto) {
        return Result.success(inventoryService.adjustStock(skuId, dto));
    }

    @Operation(summary = "批量调整 SKU 可用库存")
    @PutMapping("/batch-adjust")
    public Result<List<AdminInventoryVO>> batchAdjust(@Valid @RequestBody AdminInventoryBatchAdjustDTO dto) {
        return Result.success(inventoryService.batchAdjustStock(dto));
    }

    @Operation(summary = "设置 SKU 预警库存")
    @PutMapping("/{skuId}/warn-stock")
    public Result<AdminInventoryVO> updateWarnStock(@PathVariable Long skuId,
                                                    @Valid @RequestBody AdminInventoryWarnStockDTO dto) {
        return Result.success(inventoryService.updateWarnStock(skuId, dto));
    }

    @Operation(summary = "库存调整记录分页")
    @GetMapping("/log/page")
    public Result<PageResult<AdminInventoryLogVO>> logPage(AdminInventoryLogQueryDTO query) {
        return Result.success(inventoryService.logPage(query));
    }
}
