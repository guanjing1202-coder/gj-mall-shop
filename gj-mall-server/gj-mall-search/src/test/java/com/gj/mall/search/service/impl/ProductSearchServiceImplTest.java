package com.gj.mall.search.service.impl;

import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.search.vo.SearchHotWordVO;
import com.gj.mall.search.vo.SearchSuggestVO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductSearchServiceImplTest {

    @Test
    void hotWordsCombinesPresetWordsAndPopularProducts() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        ProductSearchServiceImpl service = new ProductSearchServiceImpl(spuMapper, skuMapper, brandMapper, categoryMapper);
        when(spuMapper.selectList(any())).thenReturn(Collections.singletonList(spu("iPhone 16 Pro", 1000)));
        when(categoryMapper.selectList(any())).thenReturn(Collections.singletonList(category("手机")));
        when(brandMapper.selectList(any())).thenReturn(Collections.singletonList(brand("Apple")));

        List<SearchHotWordVO> hotWords = service.hotWords(8);

        assertEquals(8, hotWords.size());
        assertEquals("手机", hotWords.get(0).getKeyword());
        assertTrue(hotWords.stream().anyMatch(item -> "iPhone 16 Pro".equals(item.getKeyword())));
    }

    @Test
    void suggestExpandsKeywordSynonymsAndQueriesProducts() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        ProductSearchServiceImpl service = new ProductSearchServiceImpl(spuMapper, skuMapper, brandMapper, categoryMapper);
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(brandMapper.selectList(any())).thenReturn(Collections.singletonList(brand("Apple")));
        when(spuMapper.selectList(any())).thenReturn(Collections.singletonList(spu("iPhone 16 Pro", 1000)));
        when(skuMapper.selectList(any())).thenReturn(Collections.singletonList(sku("黑色钛金属 / 256GB")));

        List<SearchSuggestVO> suggestions = service.suggest("苹果", 6);

        assertTrue(suggestions.stream().anyMatch(item -> "Apple".equals(item.getKeyword()) && "brand".equals(item.getType())));
        assertTrue(suggestions.stream().anyMatch(item -> "iPhone 16 Pro".equals(item.getKeyword()) && "product".equals(item.getType())));
        assertTrue(suggestions.size() <= 6);
    }

    private PmsSpu spu(String name, Integer saleCount) {
        PmsSpu spu = new PmsSpu();
        spu.setId(1L);
        spu.setName(name);
        spu.setSaleCount(saleCount);
        spu.setMainImage("https://example.com/main.png");
        return spu;
    }

    private PmsSku sku(String name) {
        PmsSku sku = new PmsSku();
        sku.setSpuId(1L);
        sku.setName(name);
        sku.setImage("https://example.com/sku.png");
        return sku;
    }

    private PmsBrand brand(String name) {
        PmsBrand brand = new PmsBrand();
        brand.setId(2L);
        brand.setName(name);
        brand.setLogo("https://example.com/logo.png");
        return brand;
    }

    private PmsCategory category(String name) {
        PmsCategory category = new PmsCategory();
        category.setId(3L);
        category.setName(name);
        category.setIcon("phone");
        return category;
    }
}
