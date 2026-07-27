package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminCommentActionDTO;
import com.gj.mall.admin.dto.AdminCommentQueryDTO;
import com.gj.mall.admin.dto.AdminCommentReplyDTO;
import com.gj.mall.admin.service.AdminCommentService;
import com.gj.mall.admin.vo.AdminCommentVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCommentServiceImpl implements AdminCommentService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_HIDDEN = 3;
    private static final long MAX_PAGE_SIZE = 100L;

    private final PmsProductCommentMapper commentMapper;
    private final UmsUserMapper userMapper;
    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;
    private final OmsOrderItemMapper orderItemMapper;

    @Override
    public PageResult<AdminCommentVO> page(AdminCommentQueryDTO query) {
        AdminCommentQueryDTO actualQuery = normalizeQuery(query);
        long pageNum = normalizePageNum(actualQuery.getPageNum());
        long pageSize = normalizePageSize(actualQuery.getPageSize());
        String keyword = actualQuery.getKeyword();
        List<Long> matchedUserIds = Collections.emptyList();
        List<Long> matchedSpuIds = Collections.emptyList();
        List<Long> matchedSkuIds = Collections.emptyList();
        if (StrUtil.isNotBlank(keyword)) {
            matchedUserIds = searchUserIds(keyword);
            matchedSpuIds = searchSpuIds(keyword);
            matchedSkuIds = searchSkuIds(keyword);
        }
        final List<Long> keywordUserIds = matchedUserIds;
        final List<Long> keywordSpuIds = matchedSpuIds;
        final List<Long> keywordSkuIds = matchedSkuIds;

        IPage<PmsProductComment> result = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<PmsProductComment>lambdaQuery()
                        .eq(actualQuery.getUserId() != null, PmsProductComment::getUserId, actualQuery.getUserId())
                        .eq(actualQuery.getSpuId() != null, PmsProductComment::getSpuId, actualQuery.getSpuId())
                        .eq(actualQuery.getSkuId() != null, PmsProductComment::getSkuId, actualQuery.getSkuId())
                        .eq(actualQuery.getScore() != null, PmsProductComment::getScore, actualQuery.getScore())
                        .eq(actualQuery.getStatus() != null, PmsProductComment::getStatus, actualQuery.getStatus())
                        .and(Boolean.TRUE.equals(actualQuery.getHasImage()),
                                w -> w.isNotNull(PmsProductComment::getImages).ne(PmsProductComment::getImages, "[]"))
                        .and(Boolean.FALSE.equals(actualQuery.getHasImage()),
                                w -> w.isNull(PmsProductComment::getImages).or().eq(PmsProductComment::getImages, "[]"))
                        .isNotNull(Boolean.TRUE.equals(actualQuery.getHasReply()), PmsProductComment::getReplyContent)
                        .isNull(Boolean.FALSE.equals(actualQuery.getHasReply()), PmsProductComment::getReplyContent)
                        .and(StrUtil.isNotBlank(keyword), w -> {
                            w.like(PmsProductComment::getContent, keyword)
                                    .or()
                                    .like(PmsProductComment::getOrderNo, keyword)
                                    .or()
                                    .like(PmsProductComment::getReplyContent, keyword);
                            if (CollUtil.isNotEmpty(keywordUserIds)) {
                                w.or().in(PmsProductComment::getUserId, keywordUserIds);
                            }
                            if (CollUtil.isNotEmpty(keywordSpuIds)) {
                                w.or().in(PmsProductComment::getSpuId, keywordSpuIds);
                            }
                            if (CollUtil.isNotEmpty(keywordSkuIds)) {
                                w.or().in(PmsProductComment::getSkuId, keywordSkuIds);
                            }
                        })
                        .orderByAsc(PmsProductComment::getStatus)
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
    public AdminCommentVO detail(Long id) {
        return enrich(Collections.singletonList(getByIdOrThrow(id))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, AdminCommentActionDTO dto) {
        updateStatus(id, STATUS_APPROVED, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, AdminCommentActionDTO dto) {
        updateStatus(id, STATUS_REJECTED, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hide(Long id, AdminCommentActionDTO dto) {
        updateStatus(id, STATUS_HIDDEN, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void show(Long id, AdminCommentActionDTO dto) {
        updateStatus(id, STATUS_APPROVED, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, AdminCommentReplyDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getReplyContent())) {
            throw new BizException(ResultCode.PARAM_MISSING, "回复内容不能为空");
        }
        getByIdOrThrow(id);
        PmsProductComment update = new PmsProductComment();
        update.setId(id);
        update.setReplyContent(StrUtil.sub(dto.getReplyContent().trim(), 0, 1000));
        update.setReplyTime(LocalDateTime.now());
        commentMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getByIdOrThrow(id);
        commentMapper.deleteById(id);
    }

    private void updateStatus(Long id, int status, AdminCommentActionDTO dto) {
        getByIdOrThrow(id);
        PmsProductComment update = new PmsProductComment();
        update.setId(id);
        update.setStatus(status);
        update.setAuditRemark(dto == null || StrUtil.isBlank(dto.getAuditRemark())
                ? null
                : StrUtil.sub(dto.getAuditRemark().trim(), 0, 500));
        update.setAuditTime(LocalDateTime.now());
        commentMapper.updateById(update);
    }

    private PmsProductComment getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少评价ID");
        }
        PmsProductComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "评价不存在");
        }
        return comment;
    }

    private List<AdminCommentVO> enrich(List<PmsProductComment> comments) {
        List<Long> userIds = comments.stream()
                .map(PmsProductComment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> spuIds = comments.stream()
                .map(PmsProductComment::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> skuIds = comments.stream()
                .map(PmsProductComment::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> orderItemIds = comments.stream()
                .map(PmsProductComment::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UmsUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : loadMap(userMapper.selectBatchIds(userIds), UmsUser::getId);
        Map<Long, PmsSpu> spuMap = spuIds.isEmpty()
                ? Collections.emptyMap()
                : loadMap(spuMapper.selectBatchIds(spuIds), PmsSpu::getId);
        Map<Long, PmsSku> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : loadMap(skuMapper.selectBatchIds(skuIds), PmsSku::getId);
        Map<Long, OmsOrderItem> orderItemMap = orderItemIds.isEmpty()
                ? Collections.emptyMap()
                : loadMap(orderItemMapper.selectBatchIds(orderItemIds), OmsOrderItem::getId);
        Map<Long, PmsBrand> brandMap = loadBrandMap(spuMap.values());
        Map<Long, PmsCategory> categoryMap = loadCategoryMap(spuMap.values());

        return comments.stream()
                .map(comment -> {
                    PmsSpu spu = spuMap.get(comment.getSpuId());
                    return AdminCommentVO.from(
                            comment,
                            userMap.get(comment.getUserId()),
                            spu,
                            skuMap.get(comment.getSkuId()),
                            spu == null ? null : brandMap.get(spu.getBrandId()),
                            spu == null ? null : categoryMap.get(spu.getCategoryId()),
                            orderItemMap.get(comment.getOrderItemId()));
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

    private List<Long> searchSkuIds(String keyword) {
        return skuMapper.selectList(Wrappers.<PmsSku>lambdaQuery()
                        .and(w -> w.like(PmsSku::getName, keyword)
                                .or()
                                .like(PmsSku::getSkuCode, keyword))
                        .last("LIMIT 500"))
                .stream()
                .map(PmsSku::getId)
                .collect(Collectors.toList());
    }

    private Map<Long, PmsBrand> loadBrandMap(Collection<PmsSpu> spus) {
        List<Long> ids = spus.stream()
                .map(PmsSpu::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return ids.isEmpty() ? Collections.emptyMap() : loadMap(brandMapper.selectBatchIds(ids), PmsBrand::getId);
    }

    private Map<Long, PmsCategory> loadCategoryMap(Collection<PmsSpu> spus) {
        List<Long> ids = spus.stream()
                .map(PmsSpu::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return ids.isEmpty() ? Collections.emptyMap() : loadMap(categoryMapper.selectBatchIds(ids), PmsCategory::getId);
    }

    private <T> Map<Long, T> loadMap(List<T> list, Function<T, Long> keyGetter) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(keyGetter, Function.identity(), (a, b) -> a));
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private AdminCommentQueryDTO normalizeQuery(AdminCommentQueryDTO query) {
        AdminCommentQueryDTO actualQuery = query == null ? new AdminCommentQueryDTO() : query;
        actualQuery.setKeyword(StrUtil.trim(actualQuery.getKeyword()));
        return actualQuery;
    }
}
