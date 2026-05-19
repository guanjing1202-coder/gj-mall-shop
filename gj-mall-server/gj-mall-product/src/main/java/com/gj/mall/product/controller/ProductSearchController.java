package com.gj.mall.product.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.product.service.ProductSearchService;
import com.gj.mall.product.vo.SearchHotWordVO;
import com.gj.mall.product.vo.SearchSuggestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "15-搜索体验(C端)", description = "热搜词与搜索建议")
@RestController
@RequestMapping("/api/product/search")
@RequiredArgsConstructor
@AuthExclude
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @Operation(summary = "热搜词")
    @GetMapping("/hot")
    public Result<List<SearchHotWordVO>> hot(@RequestParam(required = false) Integer limit) {
        return Result.success(productSearchService.hotWords(limit));
    }

    @Operation(summary = "搜索建议")
    @GetMapping("/suggest")
    public Result<List<SearchSuggestVO>> suggest(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.success(productSearchService.suggest(keyword, limit));
    }
}
