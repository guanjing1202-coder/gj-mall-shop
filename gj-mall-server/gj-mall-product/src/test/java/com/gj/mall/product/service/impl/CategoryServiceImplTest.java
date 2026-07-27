package com.gj.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.product.dto.CategoryDTO;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.vo.CategoryTreeVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceImplTest {

    @Test
    void saveRejectsNullDtoBeforeTouchingMapper() {
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        CategoryServiceImpl service = service(categoryMapper, mock(PmsSpuMapper.class));

        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("分类信息不能为空");
        verify(categoryMapper, never()).insert(any(PmsCategory.class));
    }

    @Test
    void saveTrimsNameDefaultsRootFieldsAndInfersRootLevel() {
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        CategoryServiceImpl service = service(categoryMapper, mock(PmsSpuMapper.class));
        CategoryDTO dto = new CategoryDTO();
        dto.setParentId(0L);
        dto.setName("  手机数码  ");

        service.save(dto);

        ArgumentCaptor<PmsCategory> captor = ArgumentCaptor.forClass(PmsCategory.class);
        verify(categoryMapper).insert(captor.capture());
        PmsCategory saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("手机数码");
        assertThat(saved.getLevel()).isEqualTo(1);
        assertThat(saved.getSort()).isZero();
        assertThat(saved.getShowStatus()).isEqualTo(1);
    }

    @Test
    void updateRecomputesLevelWhenParentChanges() {
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        CategoryServiceImpl service = service(categoryMapper, mock(PmsSpuMapper.class));
        when(categoryMapper.selectById(10L)).thenReturn(category(10L, 0L, 1));
        when(categoryMapper.selectById(20L)).thenReturn(category(20L, 0L, 1));
        CategoryDTO dto = new CategoryDTO();
        dto.setId(10L);
        dto.setParentId(20L);
        dto.setName("  手机配件  ");

        service.update(dto);

        ArgumentCaptor<PmsCategory> captor = ArgumentCaptor.forClass(PmsCategory.class);
        verify(categoryMapper).updateById(captor.capture());
        PmsCategory updated = captor.getValue();
        assertThat(updated.getName()).isEqualTo("手机配件");
        assertThat(updated.getParentId()).isEqualTo(20L);
        assertThat(updated.getLevel()).isEqualTo(2);
    }

    @Test
    void updateRejectsMovingCategoryUnderDescendant() {
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        CategoryServiceImpl service = service(categoryMapper, mock(PmsSpuMapper.class));
        when(categoryMapper.selectById(10L)).thenReturn(category(10L, 0L, 1));
        when(categoryMapper.selectById(30L)).thenReturn(category(30L, 20L, 3));
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                category(20L, 10L, 2),
                category(30L, 20L, 3)
        ));
        CategoryDTO dto = new CategoryDTO();
        dto.setId(10L);
        dto.setParentId(30L);
        dto.setName("手机");

        assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("父分类不能是当前分类的子分类");
        verify(categoryMapper, never()).updateById(any(PmsCategory.class));
    }

    @Test
    void deleteRejectsCategoryReferencedByProducts() {
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        CategoryServiceImpl service = service(categoryMapper, spuMapper);
        when(categoryMapper.selectById(10L)).thenReturn(category(10L, 0L, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(spuMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("分类下存在商品，不能删除");
        verify(categoryMapper, never()).deleteById(10L);
    }

    @Test
    void treeSortsChildrenBySortThenId() {
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        CategoryServiceImpl service = service(categoryMapper, mock(PmsSpuMapper.class));
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                category(1L, 0L, 1, 1),
                category(12L, 1L, 2, 20),
                category(11L, 1L, 2, 10),
                category(13L, 1L, 2, 10)
        ));

        List<CategoryTreeVO> tree = service.tree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren())
                .extracting(CategoryTreeVO::getId)
                .containsExactly(11L, 13L, 12L);
    }

    private CategoryServiceImpl service(PmsCategoryMapper categoryMapper, PmsSpuMapper spuMapper) {
        return new CategoryServiceImpl(categoryMapper, spuMapper);
    }

    private PmsCategory category(Long id, Long parentId, Integer level) {
        return category(id, parentId, level, 0);
    }

    private PmsCategory category(Long id, Long parentId, Integer level, Integer sort) {
        PmsCategory category = new PmsCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName("分类" + id);
        category.setLevel(level);
        category.setSort(sort);
        category.setShowStatus(1);
        return category;
    }
}
