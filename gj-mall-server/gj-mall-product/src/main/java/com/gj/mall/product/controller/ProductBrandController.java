package com.gj.mall.product.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "12-品牌(C端)", description = "前台品牌列表")
@RestController
@RequestMapping("/api/product/brand")
@RequiredArgsConstructor
@AuthExclude
public class ProductBrandController {

    private final BrandService brandService;

    @Operation(summary = "全部品牌")
    @GetMapping
    public Result<List<PmsBrand>> all() {
        return Result.success(brandService.listAll());
    }
}
