package com.gj.mall.product.controller.admin;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.product.dto.BrandDTO;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "21-后台-品牌管理", description = "管理员维护品牌")
@RestController
@RequestMapping("/api/admin/product/brand")
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService brandService;

    @Operation(summary = "分页")
    @GetMapping("/page")
    public Result<PageResult<PmsBrand>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return Result.success(brandService.page(keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody BrandDTO dto) {
        return Result.success(brandService.save(dto));
    }

    @Operation(summary = "修改品牌")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody BrandDTO dto) {
        brandService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.success();
    }
}
