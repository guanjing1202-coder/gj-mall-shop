package com.gj.mall.product.controller;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.product.service.ProductCommentService;
import com.gj.mall.product.vo.ProductCommentSummaryVO;
import com.gj.mall.product.vo.ProductCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "15-商品评价(C端)", description = "前台商品评价展示")
@RestController
@RequestMapping("/api/product/comment")
@RequiredArgsConstructor
@AuthExclude
public class ProductCommentController {

    private final ProductCommentService commentService;

    @Operation(summary = "商品评价分页")
    @GetMapping("/spu/{spuId}/page")
    public Result<PageResult<ProductCommentVO>> page(
            @PathVariable Long spuId,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Boolean hasImage) {
        return Result.success(commentService.page(spuId, current, size, hasImage));
    }

    @Operation(summary = "商品评价统计")
    @GetMapping("/spu/{spuId}/summary")
    public Result<ProductCommentSummaryVO> summary(@PathVariable Long spuId) {
        return Result.success(commentService.summary(spuId));
    }
}
