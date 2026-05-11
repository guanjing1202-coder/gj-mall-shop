package com.gj.mall.product.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.product.service.CategoryService;
import com.gj.mall.product.vo.CategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "10-商品分类(C端)", description = "前台公开访问")
@RestController
@RequestMapping("/api/product/category")
@RequiredArgsConstructor
@AuthExclude
public class ProductCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树（仅展示）")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        return Result.success(categoryService.visibleTree());
    }
}
