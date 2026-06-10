package com.gj.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.BrandDTO;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrandServiceImplTest {

    @Test
    void pageNormalizesPageAndKeyword() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        BrandServiceImpl service = service(brandMapper, mock(PmsSpuMapper.class));
        Page<PmsBrand> page = new Page<>(1, 100, 1);
        page.setRecords(Collections.singletonList(brand(10L, "Apple")));
        when(brandMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);

        PageResult<PmsBrand> result = service.page("  Apple  ", -1L, 500L);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(brandMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100L);
        assertThat(result.getPageSize()).isEqualTo(100L);
        assertThat(result.getList()).extracting(PmsBrand::getName).containsExactly("Apple");
    }

    @Test
    void saveRejectsNullDtoBeforeTouchingMapper() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        BrandServiceImpl service = service(brandMapper, mock(PmsSpuMapper.class));

        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("品牌信息不能为空");
        verify(brandMapper, never()).insert(any(PmsBrand.class));
    }

    @Test
    void saveTrimsNameAndDefaultsFields() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        BrandServiceImpl service = service(brandMapper, mock(PmsSpuMapper.class));
        BrandDTO dto = new BrandDTO();
        dto.setName("  Apple  ");

        service.save(dto);

        ArgumentCaptor<PmsBrand> captor = ArgumentCaptor.forClass(PmsBrand.class);
        verify(brandMapper).insert(captor.capture());
        PmsBrand saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Apple");
        assertThat(saved.getSort()).isZero();
        assertThat(saved.getShowStatus()).isEqualTo(1);
    }

    @Test
    void updateRejectsMissingBrandBeforeUpdating() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        BrandServiceImpl service = service(brandMapper, mock(PmsSpuMapper.class));
        BrandDTO dto = new BrandDTO();
        dto.setId(10L);
        dto.setName("Apple");

        assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(BizException.class);
        verify(brandMapper, never()).updateById(any(PmsBrand.class));
    }

    @Test
    void updateTrimsNameAndKeepsId() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        BrandServiceImpl service = service(brandMapper, mock(PmsSpuMapper.class));
        when(brandMapper.selectById(10L)).thenReturn(brand(10L, "Apple"));
        BrandDTO dto = new BrandDTO();
        dto.setId(10L);
        dto.setName("  Apple 官方  ");

        service.update(dto);

        ArgumentCaptor<PmsBrand> captor = ArgumentCaptor.forClass(PmsBrand.class);
        verify(brandMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getName()).isEqualTo("Apple 官方");
    }

    @Test
    void deleteRejectsBrandReferencedByProducts() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        BrandServiceImpl service = service(brandMapper, spuMapper);
        when(brandMapper.selectById(10L)).thenReturn(brand(10L, "Apple"));
        when(spuMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("品牌下存在商品，不能删除");
        verify(brandMapper, never()).deleteById(10L);
    }

    @Test
    void deleteMissingBrandIsIdempotent() {
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        BrandServiceImpl service = service(brandMapper, mock(PmsSpuMapper.class));

        service.delete(10L);

        verify(brandMapper, never()).deleteById(10L);
    }

    private BrandServiceImpl service(PmsBrandMapper brandMapper, PmsSpuMapper spuMapper) {
        return new BrandServiceImpl(brandMapper, spuMapper);
    }

    private PmsBrand brand(Long id, String name) {
        PmsBrand brand = new PmsBrand();
        brand.setId(id);
        brand.setName(name);
        return brand;
    }
}
