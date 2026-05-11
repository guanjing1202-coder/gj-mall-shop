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

    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsCategoryMapper categoryMapper;
    private final PmsBrandMapper brandMapper;
    private final ProductDetailRepository detailRepo;
    private final SkuService skuService;

    @Override
    public PageResult<SpuListVO> pagePublic(SpuQueryDTO query) {
        if (query.getPublishStatus() == null) {
            query.setPublishStatus(1);
        }
        return doPage(query);
    }

    @Override
    public PageResult<SpuListVO> pageAdmin(SpuQueryDTO query) {
        return doPage(query);
    }

    private PageResult<SpuListVO> doPage(SpuQueryDTO q) {
        long pageNum = q.getCurrent() == null ? 1 : q.getCurrent();
        long pageSize = q.getSize() == null ? 20 : q.getSize();
        Page<PmsSpu> page = new Page<>(pageNum, pageSize);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsSpu> wrapper =
                Wrappers.<PmsSpu>lambdaQuery()
                        .eq(q.getCategoryId() != null, PmsSpu::getCategoryId, q.getCategoryId())
                        .eq(q.getBrandId() != null, PmsSpu::getBrandId, q.getBrandId())
                        .eq(q.getPublishStatus() != null, PmsSpu::getPublishStatus, q.getPublishStatus())
                        .eq(q.getNewStatus() != null, PmsSpu::getNewStatus, q.getNewStatus())
                        .eq(q.getRecommendStatus() != null, PmsSpu::getRecommendStatus, q.getRecommendStatus())
                        .like(StrUtil.isNotBlank(q.getKeyword()), PmsSpu::getName, q.getKeyword());

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
        PmsSpu spu = new PmsSpu();
        BeanUtil.copyProperties(dto, spu, "images", "skus");
        spu.setId(null);
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
            PmsSku sku = new PmsSku();
            BeanUtil.copyProperties(sd, sku, "specData");
            sku.setId(null);
            sku.setSpuId(spu.getId());
            sku.setSpecData(sd.getSpecData() == null ? null : JSON.toJSONString(sd.getSpecData()));
            sku.setLockedStock(0);
            sku.setSaleCount(0);
            skuMapper.insert(sku);
        }

        return spu.getId();
    }

    @Override
    @Transactional
    public void update(SpuSaveDTO dto) {
        if (dto.getId() == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少 SPU ID");
        PmsSpu old = spuMapper.selectById(dto.getId());
        if (old == null) throw new BizException(ResultCode.PRODUCT_NOT_FOUND);

        PmsSpu spu = new PmsSpu();
        BeanUtil.copyProperties(dto, spu, "images", "skus");
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
                PmsSku sku = new PmsSku();
                BeanUtil.copyProperties(sd, sku, "specData");
                sku.setId(null);
                sku.setSpuId(old.getId());
                sku.setSpecData(sd.getSpecData() == null ? null : JSON.toJSONString(sd.getSpecData()));
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
}
