package com.gj.mall.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.user.entity.UmsUserFavorite;
import com.gj.mall.user.mapper.UmsUserFavoriteMapper;
import com.gj.mall.user.service.UserFavoriteService;
import com.gj.mall.user.vo.UserFavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFavoriteServiceImpl implements UserFavoriteService {

    private final UmsUserFavoriteMapper favoriteMapper;
    private final PmsSpuMapper spuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;

    @Override
    public PageResult<UserFavoriteVO> page(Long userId, Long current, Long size) {
        long pageNum = normalizePageNum(current);
        long pageSize = normalizePageSize(size);
        IPage<UmsUserFavorite> result = favoriteMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<UmsUserFavorite>lambdaQuery()
                        .eq(UmsUserFavorite::getUserId, userId)
                        .eq(UmsUserFavorite::getDeleted, 0)
                        .orderByDesc(UmsUserFavorite::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                enrich(result.getRecords()));
    }

    @Override
    public Boolean exists(Long userId, Long spuId) {
        if (spuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少商品ID");
        }
        Long count = favoriteMapper.selectCount(Wrappers.<UmsUserFavorite>lambdaQuery()
                .eq(UmsUserFavorite::getUserId, userId)
                .eq(UmsUserFavorite::getSpuId, spuId)
                .eq(UmsUserFavorite::getDeleted, 0));
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long userId, Long spuId) {
        checkProductCanFavorite(spuId);
        UmsUserFavorite existing = findActive(userId, spuId);
        if (existing != null) {
            return existing.getId();
        }
        if (favoriteMapper.restoreByUserIdAndSpuId(userId, spuId) > 0) {
            UmsUserFavorite restored = findActive(userId, spuId);
            if (restored != null) {
                return restored.getId();
            }
        }

        UmsUserFavorite favorite = new UmsUserFavorite();
        favorite.setUserId(userId);
        favorite.setSpuId(spuId);
        try {
            favoriteMapper.insert(favorite);
            return favorite.getId();
        } catch (DuplicateKeyException ex) {
            UmsUserFavorite duplicated = findActive(userId, spuId);
            if (duplicated != null) {
                return duplicated.getId();
            }
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long spuId) {
        if (spuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少商品ID");
        }
        favoriteMapper.physicalDeleteByUserIdAndSpuId(userId, spuId);
    }

    private void checkProductCanFavorite(Long spuId) {
        if (spuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少商品ID");
        }
        PmsSpu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(spu.getPublishStatus())) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
        }
    }

    private UmsUserFavorite findActive(Long userId, Long spuId) {
        return favoriteMapper.selectOne(Wrappers.<UmsUserFavorite>lambdaQuery()
                .eq(UmsUserFavorite::getUserId, userId)
                .eq(UmsUserFavorite::getSpuId, spuId)
                .eq(UmsUserFavorite::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private List<UserFavoriteVO> enrich(List<UmsUserFavorite> favorites) {
        List<Long> spuIds = favorites.stream()
                .map(UmsUserFavorite::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PmsSpu> spuMap = loadSpuMap(spuIds);
        Map<Long, PmsBrand> brandMap = loadBrandMap(spuMap.values());
        Map<Long, PmsCategory> categoryMap = loadCategoryMap(spuMap.values());
        return favorites.stream()
                .map(favorite -> {
                    PmsSpu spu = spuMap.get(favorite.getSpuId());
                    PmsBrand brand = spu == null ? null : brandMap.get(spu.getBrandId());
                    PmsCategory category = spu == null ? null : categoryMap.get(spu.getCategoryId());
                    return UserFavoriteVO.from(favorite, spu, brand, category);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, PmsSpu> loadSpuMap(Collection<Long> spuIds) {
        if (CollUtil.isEmpty(spuIds)) {
            return Collections.emptyMap();
        }
        return spuMapper.selectList(Wrappers.<PmsSpu>lambdaQuery().in(PmsSpu::getId, spuIds))
                .stream()
                .collect(Collectors.toMap(PmsSpu::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, PmsBrand> loadBrandMap(Collection<PmsSpu> spus) {
        List<Long> brandIds = spus.stream()
                .map(PmsSpu::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(brandIds)) {
            return Collections.emptyMap();
        }
        return brandMapper.selectList(Wrappers.<PmsBrand>lambdaQuery().in(PmsBrand::getId, brandIds))
                .stream()
                .collect(Collectors.toMap(PmsBrand::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, PmsCategory> loadCategoryMap(Collection<PmsSpu> spus) {
        List<Long> categoryIds = spus.stream()
                .map(PmsSpu::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectList(Wrappers.<PmsCategory>lambdaQuery().in(PmsCategory::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(PmsCategory::getId, Function.identity(), (a, b) -> a));
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }
}
