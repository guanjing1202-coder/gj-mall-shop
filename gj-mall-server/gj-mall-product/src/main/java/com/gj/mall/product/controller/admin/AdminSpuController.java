package com.gj.mall.product.controller.admin;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.product.dto.AdminSpuOperationDTO;
import com.gj.mall.product.dto.SpuQueryDTO;
import com.gj.mall.product.dto.SpuSaveDTO;
import com.gj.mall.product.service.SpuService;
import com.gj.mall.product.vo.SpuDetailVO;
import com.gj.mall.product.vo.SpuListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "22-后台-商品管理", description = "管理员 SPU/SKU CRUD")
@RestController
@RequestMapping("/api/admin/product/spu")
@RequiredArgsConstructor
public class AdminSpuController {

    private final SpuService spuService;

    @Operation(summary = "分页（含下架）")
    @GetMapping("/page")
    public Result<PageResult<SpuListVO>> page(SpuQueryDTO query) {
        return Result.success(spuService.pageAdmin(query));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<SpuDetailVO> detail(@PathVariable Long id) {
        return Result.success(spuService.detail(id));
    }

    @Operation(summary = "新建商品")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SpuSaveDTO dto) {
        return Result.success(spuService.create(dto));
    }

    @Operation(summary = "修改商品")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SpuSaveDTO dto) {
        spuService.update(dto);
        return Result.success();
    }

    @Operation(summary = "上下架")
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id, @RequestParam Integer status) {
        spuService.publish(id, status);
        return Result.success();
    }

    @Operation(summary = "商品运营位设置")
    @PutMapping("/{id}/operation")
    public Result<Void> updateOperation(@PathVariable Long id, @RequestBody AdminSpuOperationDTO dto) {
        spuService.updateOperation(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        spuService.delete(id);
        return Result.success();
    }
}
