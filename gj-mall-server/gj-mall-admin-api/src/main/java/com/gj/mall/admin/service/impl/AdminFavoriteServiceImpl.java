package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminFavoriteBatchDeleteDTO;
import com.gj.mall.admin.dto.AdminFavoriteQueryDTO;
import com.gj.mall.admin.service.AdminFavoriteService;
import com.gj.mall.admin.vo.AdminFavoriteVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.entity.UmsUserFavorite;
import com.gj.mall.user.mapper.UmsUserFavoriteMapper;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
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
public class AdminFavoriteServiceImpl implements AdminFavoriteService {

    private final UmsUserFavoriteMapper favoriteMapper;
    private final UmsUserMapper userMapper;
    private final PmsSpuMapper spuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;

    @Override
    public PageResult<AdminFavoriteVO> page(AdminFavoriteQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        List<Long> matchedUserIds = Collections.emptyList();
        List<Long> matchedSpuIds = Collections.emptyList();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            matchedUserIds = searchUserIds(query.getKeyword());
            matchedSpuIds = searchSpuIds(query.getKeyword());
            if (CollUtil.isEmpty(matchedUserIds) && CollUtil.isEmpty(matchedSpuIds)) {
                return PageResult.empty(pageNum, pageSize);
            }
        }
        final List<Long> keywordUserIds = matchedUserIds;
        final List<Long> keywordSpuIds = matchedSpuIds;

        IPage<UmsUserFavorite> result = favoriteMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<UmsUserFavorite>lambdaQuery()
                        .eq(UmsUserFavorite::getDeleted, 0)
                        .eq(query.getUserId() != null, UmsUserFavorite::getUserId, query.getUserId())
                        .eq(query.getSpuId() != null, UmsUserFavorite::getSpuId, query.getSpuId())
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> {
                            if (CollUtil.isNotEmpty(keywordUserIds)) {
                                w.in(UmsUserFavorite::getUserId, keywordUserIds);
                            }
                            if (CollUtil.isNotEmpty(keywordUserIds) && CollUtil.isNotEmpty(keywordSpuIds)) {
                                w.or();
                            }
                            if (CollUtil.isNotEmpty(keywordSpuIds)) {
                                w.in(UmsUserFavorite::getSpuId, keywordSpuIds);
                            }
                        })
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
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少收藏记录ID");
        }
        UmsUserFavorite exists = favoriteMapper.selectOne(Wrappers.<UmsUserFavorite>lambdaQuery()
                .eq(UmsUserFavorite::getId, id)
                .eq(UmsUserFavorite::getDeleted, 0));
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "收藏记录不存在");
        }
        favoriteMapper.physicalDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(AdminFavoriteBatchDeleteDTO dto) {
        List<Long> ids = dto.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            throw new BizException(ResultCode.PARAM_MISSING, "请选择收藏记录");
        }
        int rows = favoriteMapper.physicalDeleteBatchIds(ids);
        if (rows == 0) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "收藏记录不存在");
        }
    }

    private List<AdminFavoriteVO> enrich(List<UmsUserFavorite> favorites) {
        List<Long> userIds = favorites.stream()
                .map(UmsUserFavorite::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> spuIds = favorites.stream()
                .map(UmsUserFavorite::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UmsUser> userMap = loadUserMap(userIds);
        Map<Long, PmsSpu> spuMap = loadSpuMap(spuIds);
        Map<Long, PmsBrand> brandMap = loadBrandMap(spuMap.values());
        Map<Long, PmsCategory> categoryMap = loadCategoryMap(spuMap.values());

        return favorites.stream()
                .map(favorite -> {
                    PmsSpu spu = spuMap.get(favorite.getSpuId());
                    PmsBrand brand = spu == null ? null : brandMap.get(spu.getBrandId());
                    PmsCategory category = spu == null ? null : categoryMap.get(spu.getCategoryId());
                    return AdminFavoriteVO.from(favorite, userMap.get(favorite.getUserId()), spu, brand, category);
                })
                .collect(Collectors.toList());
    }

    private List<Long> searchUserIds(String keyword) {
        return userMapper.selectList(Wrappers.<UmsUser>lambdaQuery()
                        .and(w -> w.like(UmsUser::getUsername, keyword)
                                .or()
                                .like(UmsUser::getNickname, keyword)
                                .or()
                                .like(UmsUser::getPhone, keyword))
                        .last("LIMIT 500"))
                .stream()
                .map(UmsUser::getId)
                .collect(Collectors.toList());
    }

    private List<Long> searchSpuIds(String keyword) {
        return spuMapper.selectList(Wrappers.<PmsSpu>lambdaQuery()
                        .and(w -> w.like(PmsSpu::getName, keyword)
                                .or()
                                .like(PmsSpu::getSubTitle, keyword))
                        .last("LIMIT 500"))
                .stream()
                .map(PmsSpu::getId)
                .collect(Collectors.toList());
    }

    private Map<Long, UmsUser> loadUserMap(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(Wrappers.<UmsUser>lambdaQuery().in(UmsUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UmsUser::getId, Function.identity(), (a, b) -> a));
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
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }
}
