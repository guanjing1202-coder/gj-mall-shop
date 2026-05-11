package com.gj.mall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.product.dto.CategoryDTO;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.service.CategoryService;
import com.gj.mall.product.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final PmsCategoryMapper mapper;

    @Override
    public List<CategoryTreeVO> tree() {
        return buildTree(null);
    }

    @Override
    public List<CategoryTreeVO> visibleTree() {
        return buildTree(1);
    }

    private List<CategoryTreeVO> buildTree(Integer showStatusFilter) {
        List<PmsCategory> all = mapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                .eq(showStatusFilter != null, PmsCategory::getShowStatus, showStatusFilter)
                .orderByAsc(PmsCategory::getSort));

        Map<Long, CategoryTreeVO> nodes = new HashMap<>();
        for (PmsCategory c : all) {
            CategoryTreeVO vo = new CategoryTreeVO();
            BeanUtil.copyProperties(c, vo, "children");
            nodes.put(vo.getId(), vo);
        }
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryTreeVO vo : nodes.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0L) {
                roots.add(vo);
            } else {
                CategoryTreeVO parent = nodes.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo); // 父节点已被删除，挂到顶层兜底
                }
            }
        }
        roots.sort(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()));
        return roots;
    }

    @Override
    public List<PmsCategory> listByParent(Long parentId) {
        return mapper.selectList(Wrappers.<PmsCategory>lambdaQuery()
                .eq(PmsCategory::getParentId, parentId)
                .orderByAsc(PmsCategory::getSort));
    }

    @Override
    public Long save(CategoryDTO dto) {
        PmsCategory entity = new PmsCategory();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(null);
        // level 自动推导
        if (entity.getLevel() == null) {
            if (dto.getParentId() == null || dto.getParentId() == 0L) {
                entity.setLevel(1);
            } else {
                PmsCategory parent = mapper.selectById(dto.getParentId());
                if (parent == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "父分类不存在");
                entity.setLevel(parent.getLevel() == null ? 2 : parent.getLevel() + 1);
            }
        }
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getShowStatus() == null) entity.setShowStatus(1);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(CategoryDTO dto) {
        if (dto.getId() == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少分类ID");
        PmsCategory exists = mapper.selectById(dto.getId());
        if (exists == null) throw new BizException(ResultCode.DATA_NOT_FOUND);
        if (Objects.equals(dto.getId(), dto.getParentId())) {
            throw new BizException(ResultCode.PARAM_ERROR, "父分类不能是自身");
        }
        PmsCategory entity = new PmsCategory();
        BeanUtil.copyProperties(dto, entity);
        mapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        // 有子分类不允许删
        Long childCount = mapper.selectCount(Wrappers.<PmsCategory>lambdaQuery()
                .eq(PmsCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "存在子分类，不能删除");
        }
        mapper.deleteById(id);
    }
}
