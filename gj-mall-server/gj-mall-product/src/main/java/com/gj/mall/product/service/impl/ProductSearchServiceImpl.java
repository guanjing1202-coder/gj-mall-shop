package com.gj.mall.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.ProductSearchService;
import com.gj.mall.product.vo.SearchHotWordVO;
import com.gj.mall.product.vo.SearchSuggestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final int DEFAULT_HOT_LIMIT = 12;
    private static final int DEFAULT_SUGGEST_LIMIT = 10;

    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;

    @Override
    public List<SearchHotWordVO> hotWords(Integer limit) {
        int size = normalizeLimit(limit, DEFAULT_HOT_LIMIT, 20);
        List<SearchHotWordVO> result = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        addPresetHotWords(result, seen);
        List<PmsSpu> hotProducts = spuMapper.selectList(Wrappers.<PmsSpu>lambdaQuery()
                .eq(PmsSpu::getPublishStatus, 1)
                .orderByDesc(PmsSpu::getSaleCount)
                .orderByDesc(PmsSpu::getRecommendStatus)
                .orderByDesc(PmsSpu::getSort)
                .last("LIMIT 20"));
        for (PmsSpu spu : hotProducts) {
            if (result.size() >= size) {
                break;
            }
            addHot(result, seen, spu.getName(), "product", "热卖商品", spu.getId(), spu.getSaleCount());
        }

        List<PmsCategory> categories = categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                .eq(PmsCategory::getShowStatus, 1)
                .orderByAsc(PmsCategory::getSort)
                .last("LIMIT 20"));
        for (PmsCategory category : categories) {
            if (result.size() >= size) {
                break;
            }
            addHot(result, seen, category.getName(), "category", "热门类目", category.getId(), 0);
        }

        List<PmsBrand> brands = brandMapper.selectList(Wrappers.<PmsBrand>lambdaQuery()
                .eq(PmsBrand::getShowStatus, 1)
                .orderByAsc(PmsBrand::getSort)
                .last("LIMIT 20"));
        for (PmsBrand brand : brands) {
            if (result.size() >= size) {
                break;
            }
            addHot(result, seen, brand.getName(), "brand", "热门品牌", brand.getId(), 0);
        }
        return result.size() > size ? result.subList(0, size) : result;
    }

    @Override
    public List<SearchSuggestVO> suggest(String keyword, Integer limit) {
        String term = StrUtil.trim(keyword);
        int size = normalizeLimit(limit, DEFAULT_SUGGEST_LIMIT, 20);
        if (StrUtil.isBlank(term)) {
            return hotWords(size).stream()
                    .map(item -> new SearchSuggestVO(
                            item.getKeyword(),
                            item.getType(),
                            item.getLabel(),
                            item.getTargetId(),
                            null))
                    .collect(Collectors.toList());
        }

        List<SearchSuggestVO> result = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        addSynonymSuggestions(result, seen, term, size);
        addCategorySuggestions(result, seen, term, size);
        addBrandSuggestions(result, seen, term, size);
        addProductSuggestions(result, seen, term, size);
        addSkuSuggestions(result, seen, term, size);

        return result.size() > size ? result.subList(0, size) : result;
    }

    private void addPresetHotWords(List<SearchHotWordVO> result, LinkedHashSet<String> seen) {
        addHot(result, seen, "手机", "keyword", "热搜词", null, 1000);
        addHot(result, seen, "华为手机", "keyword", "热搜词", null, 940);
        addHot(result, seen, "iPhone 16", "keyword", "热搜词", null, 920);
        addHot(result, seen, "蓝牙耳机", "keyword", "热搜词", null, 860);
        addHot(result, seen, "笔记本电脑", "keyword", "热搜词", null, 820);
        addHot(result, seen, "运动鞋", "keyword", "热搜词", null, 780);
    }

    private void addSynonymSuggestions(List<SearchSuggestVO> result, LinkedHashSet<String> seen, String term, int limit) {
        int added = 0;
        for (String suggestion : expandSynonyms(term)) {
            if (result.size() >= limit || added >= 4) {
                return;
            }
            addSuggest(result, seen, suggestion, "keyword", "相关搜索", null, null);
            added++;
        }
    }

    private void addCategorySuggestions(List<SearchSuggestVO> result, LinkedHashSet<String> seen, String term, int limit) {
        for (String candidate : searchTerms(term)) {
            List<PmsCategory> categories = categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                    .eq(PmsCategory::getShowStatus, 1)
                    .like(PmsCategory::getName, candidate)
                    .orderByAsc(PmsCategory::getSort)
                    .last("LIMIT 8"));
            for (PmsCategory category : categories) {
                if (result.size() >= limit) {
                    return;
                }
                addSuggest(result, seen, category.getName(), "category", "商品类目", category.getId(), category.getIcon());
            }
        }
    }

    private void addBrandSuggestions(List<SearchSuggestVO> result, LinkedHashSet<String> seen, String term, int limit) {
        for (String candidate : searchTerms(term)) {
            List<PmsBrand> brands = brandMapper.selectList(Wrappers.<PmsBrand>lambdaQuery()
                    .eq(PmsBrand::getShowStatus, 1)
                    .like(PmsBrand::getName, candidate)
                    .orderByAsc(PmsBrand::getSort)
                    .last("LIMIT 8"));
            for (PmsBrand brand : brands) {
                if (result.size() >= limit) {
                    return;
                }
                addSuggest(result, seen, brand.getName(), "brand", "品牌", brand.getId(), brand.getLogo());
                for (String alias : brandAliases(brand.getName())) {
                    if (result.size() >= limit) {
                        return;
                    }
                    addSuggest(result, seen, alias, "keyword", "品牌别名", brand.getId(), brand.getLogo());
                }
            }
        }
    }

    private void addProductSuggestions(List<SearchSuggestVO> result, LinkedHashSet<String> seen, String term, int limit) {
        for (String candidate : searchTerms(term)) {
            String normalized = "%" + normalize(candidate) + "%";
            List<PmsSpu> products = spuMapper.selectList(Wrappers.<PmsSpu>lambdaQuery()
                    .eq(PmsSpu::getPublishStatus, 1)
                    .and(w -> w.like(PmsSpu::getName, candidate)
                            .or()
                            .like(PmsSpu::getSubTitle, candidate)
                            .or()
                            .apply("REPLACE(REPLACE(LOWER(name), ' ', ''), '-', '') LIKE {0}", normalized)
                            .or()
                            .apply("REPLACE(REPLACE(LOWER(sub_title), ' ', ''), '-', '') LIKE {0}", normalized))
                    .orderByDesc(PmsSpu::getSaleCount)
                    .orderByDesc(PmsSpu::getSort)
                    .last("LIMIT 12"));
            for (PmsSpu product : products) {
                if (result.size() >= limit) {
                    return;
                }
                addSuggest(result, seen, product.getName(), "product", "商品", product.getId(), product.getMainImage());
            }
        }
    }

    private void addSkuSuggestions(List<SearchSuggestVO> result, LinkedHashSet<String> seen, String term, int limit) {
        for (String candidate : searchTerms(term)) {
            String normalized = "%" + normalize(candidate) + "%";
            List<PmsSku> skus = skuMapper.selectList(Wrappers.<PmsSku>lambdaQuery()
                    .and(w -> w.like(PmsSku::getName, candidate)
                            .or()
                            .like(PmsSku::getSkuCode, candidate)
                            .or()
                            .like(PmsSku::getSpecData, candidate)
                            .or()
                            .apply("REPLACE(REPLACE(LOWER(name), ' ', ''), '-', '') LIKE {0}", normalized)
                            .or()
                            .apply("REPLACE(REPLACE(LOWER(sku_code), ' ', ''), '-', '') LIKE {0}", normalized)
                            .or()
                            .apply("REPLACE(REPLACE(LOWER(spec_data), ' ', ''), '-', '') LIKE {0}", normalized))
                    .orderByDesc(PmsSku::getSaleCount)
                    .last("LIMIT 12"));
            for (PmsSku sku : skus) {
                if (result.size() >= limit) {
                    return;
                }
                addSuggest(result, seen, sku.getName(), "sku", "规格", sku.getSpuId(), sku.getImage());
            }
        }
    }

    private List<String> searchTerms(String term) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(term);
        terms.add(normalize(term));
        terms.addAll(expandSynonyms(term));
        return terms.stream().filter(StrUtil::isNotBlank).collect(Collectors.toList());
    }

    private List<String> expandSynonyms(String term) {
        String normalized = normalize(term);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (containsAny(normalized, "手机", "phone", "mobile", "smartphone")) {
            result.add("手机");
            result.add("iPhone");
            result.add("华为");
            result.add("华为手机");
            result.add("小米");
            result.add("小米手机");
            result.add("三星");
            result.add("OPPO");
        }
        if (containsAny(normalized, "华为", "huawei")) {
            result.add("华为");
            result.add("华为手机");
            result.add("Mate");
        }
        if (containsAny(normalized, "苹果", "apple", "iphone")) {
            result.add("Apple");
            result.add("iPhone");
            result.add("iPhone 16");
        }
        if (containsAny(normalized, "小米", "xiaomi", "redmi")) {
            result.add("小米");
            result.add("小米手机");
            result.add("Redmi");
        }
        if (containsAny(normalized, "耳机", "蓝牙耳机", "earphone", "headphone", "airpods")) {
            result.add("耳机");
            result.add("蓝牙耳机");
            result.add("AirPods");
        }
        if (containsAny(normalized, "电脑", "笔记本", "laptop", "notebook", "macbook")) {
            result.add("电脑");
            result.add("笔记本电脑");
            result.add("MacBook");
        }
        return new ArrayList<>(result);
    }

    private List<String> brandAliases(String brandName) {
        if (StrUtil.isBlank(brandName)) {
            return Collections.emptyList();
        }
        switch (brandName.toLowerCase(Locale.ROOT)) {
            case "apple":
                return Arrays.asList("苹果", "iPhone", "MacBook", "AirPods");
            case "huawei":
                return Arrays.asList("华为", "Mate", "Pura");
            case "xiaomi":
                return Arrays.asList("小米", "Redmi", "红米");
            case "samsung":
                return Arrays.asList("三星", "Galaxy");
            case "oppo":
                return Collections.singletonList("欧珀");
            case "lenovo":
                return Arrays.asList("联想", "ThinkPad", "小新");
            case "sony":
                return Collections.singletonList("索尼");
            case "nike":
                return Collections.singletonList("耐克");
            case "adidas":
                return Collections.singletonList("阿迪达斯");
            case "dyson":
                return Collections.singletonList("戴森");
            default:
                return Collections.emptyList();
        }
    }

    private void addHot(
            List<SearchHotWordVO> result,
            LinkedHashSet<String> seen,
            String keyword,
            String type,
            String label,
            Long targetId,
            Integer heat) {
        if (StrUtil.isBlank(keyword) || !seen.add(normalize(keyword))) {
            return;
        }
        result.add(new SearchHotWordVO(keyword, type, label, targetId, heat == null ? 0 : heat));
    }

    private void addSuggest(
            List<SearchSuggestVO> result,
            LinkedHashSet<String> seen,
            String keyword,
            String type,
            String label,
            Long targetId,
            String image) {
        if (StrUtil.isBlank(keyword) || !seen.add(normalize(keyword))) {
            return;
        }
        result.add(new SearchSuggestVO(keyword, type, label, targetId, image));
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return value.replaceAll("[\\s,，、/|_\\-]+", "").toLowerCase(Locale.ROOT);
    }

    private int normalizeLimit(Integer limit, int fallback, int max) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, max);
    }
}
