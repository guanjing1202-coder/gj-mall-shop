package com.gj.mall.product.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.product.dto.SpuQueryDTO;
import com.gj.mall.product.service.SpuService;
import com.gj.mall.product.vo.SpuDetailVO;
import com.gj.mall.product.vo.SpuListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "11-商品(C端)", description = "前台商品查询")
@RestController
@RequestMapping("/api/product/spu")
@RequiredArgsConstructor
@AuthExclude
public class ProductSpuController {

    private final SpuService spuService;

    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    public Result<PageResult<SpuListVO>> page(SpuQueryDTO query) {
        return Result.success(spuService.pagePublic(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<SpuDetailVO> detail(@PathVariable Long id) {
        return Result.success(spuService.detail(id));
    }
}
