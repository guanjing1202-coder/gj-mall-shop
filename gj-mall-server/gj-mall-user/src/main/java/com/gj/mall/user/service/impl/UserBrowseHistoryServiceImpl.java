package com.gj.mall.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.user.service.UserBrowseHistoryService;
import com.gj.mall.user.vo.UserBrowseHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserBrowseHistoryServiceImpl implements UserBrowseHistoryService {

    private static final String KEY_PREFIX = "mall:user:history:";
    private static final int MAX_HISTORY = 200;
    private static final long HISTORY_TTL_DAYS = 90L;

    private final StringRedisTemplate redis;
    private final PmsSpuMapper spuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;

    @Override
    public PageResult<UserBrowseHistoryVO> page(Long userId, Long current, Long size) {
        long pageNum = normalizePageNum(current);
        long pageSize = normalizePageSize(size);
        String key = key(userId);
        Long total = redis.opsForZSet().zCard(key);
        if (total == null || total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }
        long start = (pageNum - 1) * pageSize;
        long end = start + pageSize - 1;
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key, start, end);
        if (CollUtil.isEmpty(tuples)) {
            return PageResult.empty(pageNum, pageSize);
        }
        return new PageResult<>(total, pageNum, pageSize, enrich(tuples));
    }

    @Override
    public void record(Long userId, Long spuId) {
        PmsSpu spu = checkProductCanRecord(spuId);
        String key = key(userId);
        redis.opsForZSet().add(key, String.valueOf(spu.getId()), System.currentTimeMillis());
        redis.opsForZSet().removeRange(key, 0, -MAX_HISTORY - 1L);
        redis.expire(key, HISTORY_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void remove(Long userId, Long spuId) {
        if (spuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少商品ID");
        }
        redis.opsForZSet().remove(key(userId), String.valueOf(spuId));
    }

    @Override
    public void clear(Long userId) {
        redis.delete(key(userId));
    }

    private List<UserBrowseHistoryVO> enrich(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<Long> spuIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(this::parseLong)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PmsSpu> spuMap = loadSpuMap(spuIds);
        Map<Long, PmsBrand> brandMap = loadBrandMap(spuMap.values());
        Map<Long, PmsCategory> categoryMap = loadCategoryMap(spuMap.values());
        return tuples.stream()
                .map(tuple -> {
                    Long spuId = parseLong(tuple.getValue());
                    PmsSpu spu = spuId == null ? null : spuMap.get(spuId);
                    PmsBrand brand = spu == null ? null : brandMap.get(spu.getBrandId());
                    PmsCategory category = spu == null ? null : categoryMap.get(spu.getCategoryId());
                    return UserBrowseHistoryVO.from(spuId, toLocalDateTime(tuple.getScore()), spu, brand, category);
                })
                .filter(item -> item.getSpuId() != null)
                .collect(Collectors.toList());
    }

    private PmsSpu checkProductCanRecord(Long spuId) {
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
        return spu;
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

    private Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Double score) {
        if (score == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(score.longValue()), ZoneId.systemDefault());
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
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
