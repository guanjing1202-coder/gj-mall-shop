package com.gj.mall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.doc.ProductDetailDoc;
import com.gj.mall.product.dto.AdminSpuOperationDTO;
import com.gj.mall.product.dto.SkuDTO;
import com.gj.mall.product.dto.SpuQueryDTO;
import com.gj.mall.product.dto.SpuSaveDTO;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.repo.ProductDetailRepository;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.product.service.SpuService;
import com.gj.mall.product.vo.SkuVO;
import com.gj.mall.product.vo.SpuDetailVO;
import com.gj.mall.product.vo.SpuListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsCategoryMapper categoryMapper;
    private final PmsBrandMapper brandMapper;
    private final ProductDetailRepository detailRepo;
    private final SkuService skuService;

    @Override
    public PageResult<SpuListVO> pagePublic(SpuQueryDTO query) {
        SpuQueryDTO actualQuery = normalizeQuery(query);
        actualQuery.setPublishStatus(1);
        return doPage(actualQuery);
    }

    @Override
    public PageResult<SpuListVO> pageAdmin(SpuQueryDTO query) {
        return doPage(normalizeQuery(query));
    }

    private PageResult<SpuListVO> doPage(SpuQueryDTO q) {
        long pageNum = normalizePageNum(q.getCurrent());
        long pageSize = normalizePageSize(q.getSize());
        Page<PmsSpu> page = new Page<>(pageNum, pageSize);
        List<String> keywordTerms = splitKeyword(q.getKeyword());
        Map<String, List<Long>> keywordRelation = resolveKeywordRelations(keywordTerms);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsSpu> wrapper =
                Wrappers.<PmsSpu>lambdaQuery()
                        .eq(q.getCategoryId() != null, PmsSpu::getCategoryId, q.getCategoryId())
                        .eq(q.getBrandId() != null, PmsSpu::getBrandId, q.getBrandId())
                        .eq(q.getPublishStatus() != null, PmsSpu::getPublishStatus, q.getPublishStatus())
                        .eq(q.getNewStatus() != null, PmsSpu::getNewStatus, q.getNewStatus())
                        .eq(q.getRecommendStatus() != null, PmsSpu::getRecommendStatus, q.getRecommendStatus());
        applyKeyword(wrapper, keywordTerms, keywordRelation);

        switch (q.getSort() == null ? "default" : q.getSort()) {
            case "operation":
                wrapper.orderByDesc(PmsSpu::getSort).orderByDesc(PmsSpu::getCreateTime);
                break;
            case "sales":
                wrapper.orderByDesc(PmsSpu::getSaleCount);
                break;
            case "price_asc":
                wrapper.orderByAsc(PmsSpu::getPrice);
                break;
            case "price_desc":
                wrapper.orderByDesc(PmsSpu::getPrice);
                break;
            default:
                wrapper.orderByDesc(PmsSpu::getSort).orderByDesc(PmsSpu::getCreateTime);
        }

        IPage<PmsSpu> p = spuMapper.selectPage(page, wrapper);
        List<SpuListVO> list = p.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    private void applyKeyword(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsSpu> wrapper,
            List<String> terms,
            Map<String, List<Long>> relations) {
        if (CollUtil.isEmpty(terms)) {
            return;
        }
        for (String term : terms) {
            List<Long> categoryIds = relations.get("category:" + term);
            List<Long> brandIds = relations.get("brand:" + term);
            List<Long> directSpuIds = relations.get("spu:" + term);
            List<Long> skuSpuIds = relations.get("sku:" + term);
            String normalized = normalizeKeyword(term);
            wrapper.and(w -> {
                w.like(PmsSpu::getName, term)
                        .or()
                        .like(PmsSpu::getSubTitle, term);
                if (StrUtil.isNotBlank(normalized) && !Objects.equals(normalized, term.toLowerCase(Locale.ROOT))) {
                    String like = "%" + normalized + "%";
                    w.or()
                            .apply("REPLACE(REPLACE(LOWER(name), ' ', ''), '-', '') LIKE {0}", like)
                            .or()
                            .apply("REPLACE(REPLACE(LOWER(sub_title), ' ', ''), '-', '') LIKE {0}", like);
                }
                if (CollUtil.isNotEmpty(categoryIds)) {
                    w.or().in(PmsSpu::getCategoryId, categoryIds);
                }
                if (CollUtil.isNotEmpty(brandIds)) {
                    w.or().in(PmsSpu::getBrandId, brandIds);
                }
                if (CollUtil.isNotEmpty(directSpuIds)) {
                    w.or().in(PmsSpu::getId, directSpuIds);
                }
                if (CollUtil.isNotEmpty(skuSpuIds)) {
                    w.or().in(PmsSpu::getId, skuSpuIds);
                }
            });
        }
    }

    private Map<String, List<Long>> resolveKeywordRelations(List<String> terms) {
        if (CollUtil.isEmpty(terms)) {
            return Collections.emptyMap();
        }
        Map<String, List<Long>> result = new HashMap<>();
        for (String term : terms) {
            LinkedHashSet<Long> categoryIds = categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                            .eq(PmsCategory::getShowStatus, 1)
                            .like(PmsCategory::getName, term))
                    .stream()
                    .map(PmsCategory::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (String categoryName : canonicalCategoryNames(term)) {
                categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                                .eq(PmsCategory::getShowStatus, 1)
                                .like(PmsCategory::getName, categoryName))
                        .forEach(category -> categoryIds.add(category.getId()));
            }
            result.put("category:" + term, expandCategoryIds(categoryIds));

            LinkedHashSet<Long> brandIds = brandMapper.selectList(Wrappers.<PmsBrand>lambdaQuery()
                            .eq(PmsBrand::getShowStatus, 1)
                            .like(PmsBrand::getName, term))
                    .stream()
                    .map(PmsBrand::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            String canonicalBrand = canonicalBrandName(term);
            if (shouldMatchBrandByTerm(term) && StrUtil.isNotBlank(canonicalBrand)) {
                brandMapper.selectList(Wrappers.<PmsBrand>lambdaQuery()
                                .eq(PmsBrand::getShowStatus, 1)
                                .eq(PmsBrand::getName, canonicalBrand))
                        .forEach(brand -> brandIds.add(brand.getId()));
            }
            result.put("brand:" + term, new ArrayList<>(brandIds));

            result.put("spu:" + term, searchDirectSpuIds(term));
            result.put("sku:" + term, searchSkuSpuIds(term));
        }
        return result;
    }

    private List<String> splitKeyword(String keyword) {
        String raw = StrUtil.trim(keyword);
        if (StrUtil.isBlank(raw)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> splitTerms = new LinkedHashSet<>();
        Arrays.stream(raw.split("[\\s,，、/|]+"))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .forEach(splitTerms::add);

        String compact = raw.replaceAll("[\\s,，、/|]+", "");
        LinkedHashSet<String> knownTerms = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(compact)) {
            addKnownWordTerms(compact, knownTerms);
        }

        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (splitTerms.size() > 1) {
            terms.addAll(splitTerms);
        }
        terms.addAll(knownTerms);
        String remainder = removeKnownWords(compact, knownTerms);
        if (StrUtil.isNotBlank(remainder) && !isGenericSearchModifier(remainder)) {
            terms.add(remainder);
        }
        if (terms.isEmpty() && StrUtil.isNotBlank(compact)) {
            terms.add(compact);
        }
        return new ArrayList<>(terms);
    }

    private void addKnownWordTerms(String keyword, Set<String> terms) {
        List<PmsBrand> brands = brandMapper.selectList(Wrappers.<PmsBrand>lambdaQuery().eq(PmsBrand::getShowStatus, 1));
        for (PmsBrand brand : brands) {
            addContainedTerm(keyword, brand.getName(), terms);
            brandAliases(brand.getName()).forEach(alias -> addContainedTerm(keyword, alias, terms));
        }
        List<PmsCategory> categories = categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery().eq(PmsCategory::getShowStatus, 1));
        for (PmsCategory category : categories) {
            addContainedTerm(keyword, category.getName(), terms);
        }
    }

    private void addContainedTerm(String keyword, String candidate, Set<String> terms) {
        if (StrUtil.isBlank(candidate)) {
            return;
        }
        String lowerKeyword = normalizeKeyword(keyword);
        String lowerCandidate = normalizeKeyword(candidate);
        if (!lowerKeyword.equals(lowerCandidate) && lowerKeyword.contains(lowerCandidate)) {
            terms.add(candidate);
        }
    }

    private String removeKnownWords(String keyword, Collection<String> knownTerms) {
        if (StrUtil.isBlank(keyword) || CollUtil.isEmpty(knownTerms)) {
            return StrUtil.trim(keyword);
        }
        String remainder = normalizeKeyword(keyword);
        List<String> words = knownTerms.stream()
                .map(this::normalizeKeyword)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
        for (String word : words) {
            remainder = remainder.replace(word, "");
        }
        return remainder;
    }

    private String normalizeKeyword(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return value.replaceAll("[\\s,，、/|_\\-]+", "").toLowerCase(Locale.ROOT);
    }

    private List<String> brandAliases(String brandName) {
        if (StrUtil.isBlank(brandName)) {
            return Collections.emptyList();
        }
        switch (brandName.toLowerCase(Locale.ROOT)) {
            case "apple":
                return Arrays.asList("苹果", "iPhone", "iPad", "Mac", "MacBook", "AirPods", "Apple Watch");
            case "huawei":
                return Arrays.asList("华为", "Mate", "Pura");
            case "xiaomi":
                return Arrays.asList("小米", "米家", "Redmi", "红米");
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

    private String canonicalBrandName(String term) {
        String normalized = normalizeKeyword(term);
        switch (normalized) {
            case "apple":
            case "苹果":
                return "Apple";
            case "huawei":
            case "华为":
                return "Huawei";
            case "xiaomi":
            case "mi":
            case "小米":
            case "redmi":
            case "红米":
                return "小米";
            case "samsung":
            case "三星":
                return "Samsung";
            case "oppo":
                return "OPPO";
            case "lenovo":
            case "联想":
                return "Lenovo";
            case "sony":
            case "索尼":
                return "Sony";
            case "nike":
            case "耐克":
                return "Nike";
            case "adidas":
            case "阿迪达斯":
                return "Adidas";
            case "dyson":
            case "戴森":
                return "Dyson";
            default:
                return null;
        }
    }

    private boolean shouldMatchBrandByTerm(String term) {
        String normalized = normalizeKeyword(term);
        if (StrUtil.isBlank(normalized)) {
            return false;
        }
        return Arrays.asList(
                "apple", "苹果",
                "huawei", "华为",
                "xiaomi", "mi", "小米", "redmi", "红米",
                "samsung", "三星",
                "oppo",
                "lenovo", "联想",
                "sony", "索尼",
                "nike", "耐克",
                "adidas", "阿迪达斯",
                "dyson", "戴森"
        ).contains(normalized);
    }

    private List<Long> expandCategoryIds(Collection<Long> seedIds) {
        if (CollUtil.isEmpty(seedIds)) {
            return Collections.emptyList();
        }
        List<PmsCategory> categories = categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                .eq(PmsCategory::getShowStatus, 1));
        Map<Long, List<PmsCategory>> childrenMap = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(PmsCategory::getParentId));
        LinkedHashSet<Long> expanded = new LinkedHashSet<>(seedIds);
        Deque<Long> queue = new ArrayDeque<>(seedIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            for (PmsCategory child : childrenMap.getOrDefault(parentId, Collections.emptyList())) {
                if (expanded.add(child.getId())) {
                    queue.offer(child.getId());
                }
            }
        }
        return new ArrayList<>(expanded);
    }

    private List<Long> searchDirectSpuIds(String term) {
        if (StrUtil.isBlank(term)) {
            return Collections.emptyList();
        }
        String normalized = normalizeKeyword(term);
        String like = "%" + normalized + "%";
        return spuMapper.selectList(Wrappers.<PmsSpu>lambdaQuery()
                        .and(w -> w.like(PmsSpu::getName, term)
                                .or()
                                .like(PmsSpu::getSubTitle, term)
                                .or()
                                .apply("REPLACE(REPLACE(LOWER(name), ' ', ''), '-', '') LIKE {0}", like)
                                .or()
                                .apply("REPLACE(REPLACE(LOWER(sub_title), ' ', ''), '-', '') LIKE {0}", like))
                        .last("LIMIT 500"))
                .stream()
                .map(PmsSpu::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> searchSkuSpuIds(String term) {
        if (StrUtil.isBlank(term)) {
            return Collections.emptyList();
        }
        String normalized = normalizeKeyword(term);
        String like = "%" + normalized + "%";
        return skuMapper.selectList(Wrappers.<PmsSku>lambdaQuery()
                        .and(w -> w.like(PmsSku::getName, term)
                                .or()
                                .like(PmsSku::getSkuCode, term)
                                .or()
                                .like(PmsSku::getSpecData, term)
                                .or()
                                .apply("REPLACE(REPLACE(LOWER(name), ' ', ''), '-', '') LIKE {0}", like)
                                .or()
                                .apply("REPLACE(REPLACE(LOWER(sku_code), ' ', ''), '-', '') LIKE {0}", like)
                                .or()
                                .apply("REPLACE(REPLACE(LOWER(spec_data), ' ', ''), '-', '') LIKE {0}", like))
                        .last("LIMIT 500"))
                .stream()
                .map(PmsSku::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> canonicalCategoryNames(String term) {
        String normalized = normalizeKeyword(term);
        switch (normalized) {
            case "phone":
            case "mobile":
            case "smartphone":
            case "智能手机":
            case "手机":
                return Collections.singletonList("手机");
            case "earphone":
            case "earphones":
            case "headphone":
            case "headphones":
            case "bluetoothheadset":
            case "蓝牙耳机":
            case "耳机":
                return Collections.singletonList("耳机");
            case "computer":
            case "pc":
            case "laptop":
            case "notebook":
            case "macbook":
            case "笔记本":
            case "笔记本电脑":
            case "电脑":
                return Collections.singletonList("电脑");
            default:
                return Collections.emptyList();
        }
    }

    private boolean isGenericSearchModifier(String value) {
        String normalized = normalizeKeyword(value);
        if (StrUtil.isBlank(normalized)) {
            return true;
        }
        return Arrays.asList(
                "智能", "数码", "商品", "产品", "好物", "精选", "新款", "新品",
                "热卖", "热销", "正品", "官方", "旗舰", "旗舰店", "限时", "秒杀",
                "笔记本", "蓝牙", "无线", "高清", "专业", "高端", "便携"
        ).contains(normalized);
    }

    private SpuListVO toListVO(PmsSpu spu) {
        SpuListVO vo = new SpuListVO();
        BeanUtil.copyProperties(spu, vo);
        return vo;
    }

    @Override
    public SpuDetailVO detail(Long id) {
        PmsSpu spu = spuMapper.selectById(id);
        if (spu == null) throw new BizException(ResultCode.PRODUCT_NOT_FOUND);

        SpuDetailVO vo = new SpuDetailVO();
        BeanUtil.copyProperties(spu, vo, "images");
        vo.setImages(parseImages(spu.getImages()));

        // 分类/品牌名
        if (spu.getCategoryId() != null) {
            PmsCategory c = categoryMapper.selectById(spu.getCategoryId());
            if (c != null) vo.setCategoryName(c.getName());
        }
        if (spu.getBrandId() != null) {
            PmsBrand b = brandMapper.selectById(spu.getBrandId());
            if (b != null) vo.setBrandName(b.getName());
        }

        // SKU 列表
        List<SkuVO> skus = skuService.listBySpuId(spu.getId());
        vo.setSkus(skus);

        // Mongo 详情
        if (StrUtil.isNotBlank(spu.getDetailId())) {
            try {
                detailRepo.findById(spu.getDetailId()).ifPresent(d -> {
                    vo.setDetailHtml(d.getHtml());
                    vo.setDetailImages(d.getImages());
                    vo.setPackingList(d.getPackingList());
                    vo.setAfterSale(d.getAfterSale());
                });
            } catch (Exception ignored) {
                // Mongo 详情不可用时，仍返回 MySQL 商品基础资料。
            }
        }
        return vo;
    }

    @Override
    @Transactional
    public Long create(SpuSaveDTO dto) {
        validateSpuSaveDTO(dto, false);
        PmsSpu spu = new PmsSpu();
        BeanUtil.copyProperties(dto, spu, "images", "skus");
        spu.setId(null);
        spu.setName(StrUtil.trim(dto.getName()));
        spu.setImages(JSON.toJSONString(dto.getImages() == null ? Collections.emptyList() : dto.getImages()));
        if (spu.getPublishStatus() == null) spu.setPublishStatus(0);
        spu.setNewStatus(0);
        spu.setRecommendStatus(0);
        spu.setSort(0);

        // 计算最低价
        BigDecimal minPrice = dto.getSkus().stream()
                .map(SkuDTO::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        spu.setPrice(minPrice);
        spu.setSaleCount(0);

        // 先存详情到 Mongo，拿回 detailId
        ProductDetailDoc doc = new ProductDetailDoc();
        doc.setHtml(dto.getDetailHtml());
        doc.setImages(dto.getDetailImages());
        doc.setPackingList(dto.getPackingList());
        doc.setAfterSale(dto.getAfterSale());
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());
        ProductDetailDoc saved = detailRepo.save(doc);
        spu.setDetailId(saved.getId());

        spuMapper.insert(spu);

        // 反向写入 spuId
        saved.setSpuId(spu.getId());
        detailRepo.save(saved);

        // 插入 SKU
        for (SkuDTO sd : dto.getSkus()) {
            PmsSku sku = buildSku(sd);
            sku.setSpuId(spu.getId());
            sku.setLockedStock(0);
            sku.setSaleCount(0);
            skuMapper.insert(sku);
        }

        return spu.getId();
    }

    @Override
    @Transactional
    public void update(SpuSaveDTO dto) {
        validateSpuSaveDTO(dto, true);
        PmsSpu old = spuMapper.selectById(dto.getId());
        if (old == null) throw new BizException(ResultCode.PRODUCT_NOT_FOUND);

        PmsSpu spu = new PmsSpu();
        BeanUtil.copyProperties(dto, spu, "images", "skus");
        spu.setName(StrUtil.trim(dto.getName()));
        if (dto.getImages() != null) {
            spu.setImages(JSON.toJSONString(dto.getImages()));
        }

        // 重新计算最低价
        if (CollUtil.isNotEmpty(dto.getSkus())) {
            BigDecimal minPrice = dto.getSkus().stream()
                    .map(SkuDTO::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            spu.setPrice(minPrice);
        }

        spuMapper.updateById(spu);

        // 更新 Mongo 详情
        ProductDetailDoc doc;
        if (StrUtil.isNotBlank(old.getDetailId())) {
            doc = detailRepo.findById(old.getDetailId()).orElse(new ProductDetailDoc());
        } else {
            doc = new ProductDetailDoc();
            doc.setCreateTime(LocalDateTime.now());
        }
        doc.setSpuId(old.getId());
        if (dto.getDetailHtml() != null) doc.setHtml(dto.getDetailHtml());
        if (dto.getDetailImages() != null) doc.setImages(dto.getDetailImages());
        if (dto.getPackingList() != null) doc.setPackingList(dto.getPackingList());
        if (dto.getAfterSale() != null) doc.setAfterSale(dto.getAfterSale());
        doc.setUpdateTime(LocalDateTime.now());
        ProductDetailDoc saved = detailRepo.save(doc);
        if (!Objects.equals(old.getDetailId(), saved.getId())) {
            PmsSpu upd = new PmsSpu();
            upd.setId(old.getId());
            upd.setDetailId(saved.getId());
            spuMapper.updateById(upd);
        }

        // SKU：简化策略 — 全删全插（业务规模可控时最简单可靠）
        if (CollUtil.isNotEmpty(dto.getSkus())) {
            skuMapper.delete(Wrappers.<PmsSku>lambdaQuery().eq(PmsSku::getSpuId, old.getId()));
            for (SkuDTO sd : dto.getSkus()) {
                PmsSku sku = buildSku(sd);
                sku.setSpuId(old.getId());
                if (sku.getLockedStock() == null) sku.setLockedStock(0);
                if (sku.getSaleCount() == null) sku.setSaleCount(0);
                skuMapper.insert(sku);
            }
        }
    }

    @Override
    public void publish(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "上下架状态非法");
        }
        PmsSpu spu = spuMapper.selectById(id);
        if (spu == null) throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        PmsSpu upd = new PmsSpu();
        upd.setId(id);
        upd.setPublishStatus(status);
        spuMapper.updateById(upd);
    }

    @Override
    public void updateOperation(Long id, AdminSpuOperationDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少运营设置");
        }
        PmsSpu spu = spuMapper.selectById(id);
        if (spu == null) throw new BizException(ResultCode.PRODUCT_NOT_FOUND);

        PmsSpu upd = new PmsSpu();
        upd.setId(id);
        if (dto.getPublishStatus() != null) {
            upd.setPublishStatus(normalizeFlag(dto.getPublishStatus(), "上下架状态"));
        }
        if (dto.getNewStatus() != null) {
            upd.setNewStatus(normalizeFlag(dto.getNewStatus(), "新品状态"));
        }
        if (dto.getRecommendStatus() != null) {
            upd.setRecommendStatus(normalizeFlag(dto.getRecommendStatus(), "推荐状态"));
        }
        if (dto.getSort() != null) {
            if (dto.getSort() < 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "排序值不能小于 0");
            }
            upd.setSort(dto.getSort());
        }
        spuMapper.updateById(upd);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PmsSpu spu = spuMapper.selectById(id);
        if (spu == null) return;
        spuMapper.deleteById(id);
        skuMapper.delete(Wrappers.<PmsSku>lambdaQuery().eq(PmsSku::getSpuId, id));
        // Mongo 详情保留作为审计，不强删
    }

    private List<String> parseImages(String json) {
        if (StrUtil.isBlank(json)) return Collections.emptyList();
        try {
            return JSON.parseObject(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Integer normalizeFlag(Integer value, String fieldName) {
        if (value != 0 && value != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, fieldName + "非法");
        }
        return value;
    }

    private SpuQueryDTO normalizeQuery(SpuQueryDTO query) {
        SpuQueryDTO actualQuery = query == null ? new SpuQueryDTO() : query;
        actualQuery.setKeyword(StrUtil.trim(actualQuery.getKeyword()));
        actualQuery.setSort(StrUtil.trim(actualQuery.getSort()));
        return actualQuery;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private void validateSpuSaveDTO(SpuSaveDTO dto, boolean requireId) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "商品信息不能为空");
        }
        if (requireId && dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少 SPU ID");
        }
        if (StrUtil.isBlank(dto.getName())) {
            throw new BizException(ResultCode.PARAM_MISSING, "商品名不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "分类不能为空");
        }
        if (categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        if (dto.getBrandId() != null && brandMapper.selectById(dto.getBrandId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "品牌不存在");
        }
        if (CollUtil.isEmpty(dto.getSkus())) {
            throw new BizException(ResultCode.PARAM_MISSING, "至少需要一个 SKU");
        }
        for (SkuDTO sku : dto.getSkus()) {
            validateSkuDTO(sku);
        }
    }

    private void validateSkuDTO(SkuDTO sku) {
        if (sku == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "SKU 信息不能为空");
        }
        if (sku.getPrice() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "SKU 售价不能为空");
        }
        if (sku.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "SKU 售价必须大于 0");
        }
        if (sku.getStock() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "SKU 库存不能为空");
        }
        if (sku.getStock() < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "SKU 库存不能为负");
        }
    }

    private PmsSku buildSku(SkuDTO dto) {
        PmsSku sku = new PmsSku();
        BeanUtil.copyProperties(dto, sku, "specData");
        sku.setId(null);
        sku.setName(StrUtil.trim(dto.getName()));
        sku.setSkuCode(StrUtil.trim(dto.getSkuCode()));
        sku.setSpecData(dto.getSpecData() == null ? null : JSON.toJSONString(dto.getSpecData()));
        return sku;
    }
}
