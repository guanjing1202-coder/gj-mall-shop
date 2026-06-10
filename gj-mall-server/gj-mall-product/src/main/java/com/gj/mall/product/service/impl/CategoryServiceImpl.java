package com.gj.mall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.product.dto.CategoryDTO;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
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
    private final PmsSpuMapper spuMapper;

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
        sortTree(roots);
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
        validateSaveDTO(dto);
        PmsCategory entity = new PmsCategory();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(null);
        entity.setName(StrUtil.trim(dto.getName()));
        // level 自动推导
        if (entity.getLevel() == null) {
            entity.setLevel(resolveLevel(dto.getParentId()));
        }
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getShowStatus() == null) entity.setShowStatus(1);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(CategoryDTO dto) {
        validateUpdateDTO(dto);
        PmsCategory exists = mapper.selectById(dto.getId());
        if (exists == null) throw new BizException(ResultCode.DATA_NOT_FOUND);
        if (Objects.equals(dto.getId(), dto.getParentId())) {
            throw new BizException(ResultCode.PARAM_ERROR, "父分类不能是自身");
        }
        if (dto.getParentId() != null && dto.getParentId() != 0L) {
            PmsCategory parent = mapper.selectById(dto.getParentId());
            if (parent == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "父分类不存在");
            if (isDescendant(dto.getId(), dto.getParentId())) {
                throw new BizException(ResultCode.PARAM_ERROR, "父分类不能是当前分类的子分类");
            }
            dto.setLevel(parent.getLevel() == null ? 2 : parent.getLevel() + 1);
        } else if (dto.getParentId() != null) {
            dto.setLevel(1);
        }
        PmsCategory entity = new PmsCategory();
        BeanUtil.copyProperties(dto, entity);
        entity.setName(StrUtil.trim(dto.getName()));
        mapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        if (id == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少分类ID");
        PmsCategory exists = mapper.selectById(id);
        if (exists == null) return;
        // 有子分类不允许删
        Long childCount = mapper.selectCount(Wrappers.<PmsCategory>lambdaQuery()
                .eq(PmsCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "存在子分类，不能删除");
        }
        Long productCount = spuMapper.selectCount(Wrappers.<PmsSpu>lambdaQuery()
                .eq(PmsSpu::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "分类下存在商品，不能删除");
        }
        mapper.deleteById(id);
    }

    private void validateSaveDTO(CategoryDTO dto) {
        validateDTO(dto);
    }

    private void validateUpdateDTO(CategoryDTO dto) {
        validateDTO(dto);
        if (dto.getId() == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少分类ID");
    }

    private void validateDTO(CategoryDTO dto) {
        if (dto == null) throw new BizException(ResultCode.PARAM_MISSING, "分类信息不能为空");
        if (dto.getParentId() == null) throw new BizException(ResultCode.PARAM_MISSING, "父分类ID不能为空");
        if (StrUtil.isBlank(dto.getName())) throw new BizException(ResultCode.PARAM_MISSING, "分类名不能为空");
    }

    private Integer resolveLevel(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return 1;
        }
        PmsCategory parent = mapper.selectById(parentId);
        if (parent == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "父分类不存在");
        return parent.getLevel() == null ? 2 : parent.getLevel() + 1;
    }

    private boolean isDescendant(Long currentId, Long candidateParentId) {
        List<PmsCategory> all = mapper.selectList(Wrappers.<PmsCategory>lambdaQuery());
        Map<Long, Long> parentMap = all.stream()
                .filter(category -> category.getId() != null)
                .collect(Collectors.toMap(PmsCategory::getId, PmsCategory::getParentId, (a, b) -> a));
        Long parentId = candidateParentId;
        while (parentId != null && parentId != 0L) {
            if (Objects.equals(parentId, currentId)) {
                return true;
            }
            parentId = parentMap.get(parentId);
        }
        return false;
    }

    private void sortTree(List<CategoryTreeVO> nodes) {
        nodes.sort(Comparator
                .comparing((CategoryTreeVO c) -> c.getSort() == null ? 0 : c.getSort())
                .thenComparing(c -> c.getId() == null ? 0L : c.getId()));
        for (CategoryTreeVO node : nodes) {
            sortTree(node.getChildren());
        }
    }
}
