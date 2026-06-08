package com.gj.mall.product.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.ProductCommentService;
import com.gj.mall.product.vo.ProductCommentSummaryVO;
import com.gj.mall.product.vo.ProductCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl implements ProductCommentService {

    private static final int STATUS_APPROVED = 1;

    private final PmsProductCommentMapper commentMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsSpuMapper spuMapper;

    @Override
    public PageResult<ProductCommentVO> page(Long spuId, Long current, Long size, Boolean hasImage) {
        checkProductExists(spuId);
        long pageNum = normalizePageNum(current);
        long pageSize = normalizePageSize(size);
        IPage<PmsProductComment> result = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<PmsProductComment>lambdaQuery()
                        .eq(PmsProductComment::getSpuId, spuId)
                        .eq(PmsProductComment::getStatus, STATUS_APPROVED)
                        .and(Boolean.TRUE.equals(hasImage),
                                w -> w.isNotNull(PmsProductComment::getImages).ne(PmsProductComment::getImages, "[]"))
                        .and(Boolean.FALSE.equals(hasImage),
                                w -> w.isNull(PmsProductComment::getImages).or().eq(PmsProductComment::getImages, "[]"))
                        .orderByDesc(PmsProductComment::getCreateTime));
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
    public ProductCommentSummaryVO summary(Long spuId) {
        checkProductExists(spuId);
        List<PmsProductComment> comments = commentMapper.selectList(Wrappers.<PmsProductComment>lambdaQuery()
                .eq(PmsProductComment::getSpuId, spuId)
                .eq(PmsProductComment::getStatus, STATUS_APPROVED));
        ProductCommentSummaryVO vo = new ProductCommentSummaryVO();
        long total = comments.size();
        vo.setTotal(total);
        if (total == 0) {
            vo.setAverageScore(BigDecimal.ZERO);
            vo.setGoodCount(0L);
            vo.setGoodRate(BigDecimal.ZERO);
            vo.setImageCount(0L);
            return vo;
        }
        long scoreSum = comments.stream()
                .map(PmsProductComment::getScore)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        long scoreCount = comments.stream()
                .map(PmsProductComment::getScore)
                .filter(Objects::nonNull)
                .count();
        long goodCount = comments.stream()
                .filter(comment -> comment.getScore() != null && comment.getScore() >= 4)
                .count();
        long imageCount = comments.stream()
                .filter(comment -> comment.getImages() != null
                        && !comment.getImages().trim().isEmpty()
                        && !"[]".equals(comment.getImages().trim()))
                .count();
        vo.setAverageScore(scoreCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(scoreSum).divide(BigDecimal.valueOf(scoreCount), 1, RoundingMode.HALF_UP));
        vo.setGoodCount(goodCount);
        vo.setGoodRate(BigDecimal.valueOf(goodCount * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
        vo.setImageCount(imageCount);
        return vo;
    }

    private List<ProductCommentVO> enrich(List<PmsProductComment> comments) {
        List<Long> skuIds = comments.stream()
                .map(PmsProductComment::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PmsSku> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuMapper.selectBatchIds(skuIds).stream()
                        .collect(Collectors.toMap(PmsSku::getId, Function.identity(), (a, b) -> a));
        return comments.stream()
                .map(comment -> ProductCommentVO.from(comment, skuMap.get(comment.getSkuId())))
                .collect(Collectors.toList());
    }

    private void checkProductExists(Long spuId) {
        if (spuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少商品ID");
        }
        PmsSpu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
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
