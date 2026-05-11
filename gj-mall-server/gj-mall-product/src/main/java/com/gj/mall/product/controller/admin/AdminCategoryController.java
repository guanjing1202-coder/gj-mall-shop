package com.gj.mall.product.controller.admin;

import com.gj.mall.common.result.Result;
import com.gj.mall.product.dto.CategoryDTO;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.service.CategoryService;
import com.gj.mall.product.vo.CategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "20-后台-分类管理", description = "管理员维护商品分类")
@RestController
@RequestMapping("/api/admin/product/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "全量分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        return Result.success(categoryService.tree());
    }

    @Operation(summary = "按父分类列出")
    @GetMapping
    public Result<List<PmsCategory>> list(@RequestParam(defaultValue = "0") Long parentId) {
        return Result.success(categoryService.listByParent(parentId));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody CategoryDTO dto) {
        return Result.success(categoryService.save(dto));
    }

    @Operation(summary = "修改分类")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CategoryDTO dto) {
        categoryService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
