package com.gj.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.gj.mall.product.vo.SpuListVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpuServiceImplTest {

    @Test
    void pagePublicUsesDefaultPublishedQueryWhenQueryIsNull() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));
        Page<PmsSpu> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(spu(100L, "iPhone 16 Pro")));
        when(spuMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);

        PageResult<SpuListVO> result = service.pagePublic(null);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(spuMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
        assertThat(result.getPageNum()).isEqualTo(1L);
        assertThat(result.getPageSize()).isEqualTo(20L);
        assertThat(result.getList()).extracting(SpuListVO::getName).containsExactly("iPhone 16 Pro");
    }

    @Test
    void pageAdminNormalizesPageSizeAndTrimsKeywordAndSort() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));
        Page<PmsSpu> page = new Page<>(1, 100, 0);
        page.setRecords(Collections.emptyList());
        when(spuMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        SpuQueryDTO query = new SpuQueryDTO();
        query.setCurrent(-1L);
        query.setSize(500L);
        query.setKeyword("  iPhone  ");
        query.setSort("  sales  ");

        PageResult<SpuListVO> result = service.pageAdmin(query);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(spuMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100L);
        assertThat(query.getKeyword()).isEqualTo("iPhone");
        assertThat(query.getSort()).isEqualTo("sales");
        assertThat(result.getPageSize()).isEqualTo(100L);
    }

    @Test
    void pagePublicAlwaysForcesPublishedStatus() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));
        Page<PmsSpu> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(spuMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        SpuQueryDTO query = new SpuQueryDTO();
        query.setPublishStatus(0);

        service.pagePublic(query);

        assertThat(query.getPublishStatus()).isEqualTo(1);
    }

    @Test
    void createRejectsNullDtoBeforeTouchingRepositories() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        ProductDetailRepository detailRepo = mock(ProductDetailRepository.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), detailRepo);

        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("商品信息不能为空");
        verify(spuMapper, never()).insert(any(PmsSpu.class));
        verify(detailRepo, never()).save(any(ProductDetailDoc.class));
    }

    @Test
    void createRejectsMissingCategoryBeforeSavingDetail() {
        ProductDetailRepository detailRepo = mock(ProductDetailRepository.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        SpuServiceImpl service = service(mock(PmsSpuMapper.class), mock(PmsSkuMapper.class),
                categoryMapper, mock(PmsBrandMapper.class), detailRepo);

        assertThatThrownBy(() -> service.create(validDto()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("分类不存在");
        verify(detailRepo, never()).save(any(ProductDetailDoc.class));
    }

    @Test
    void createRejectsMissingBrandBeforeSavingDetail() {
        ProductDetailRepository detailRepo = mock(ProductDetailRepository.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        SpuSaveDTO dto = validDto();
        dto.setBrandId(20L);
        when(categoryMapper.selectById(10L)).thenReturn(category());
        SpuServiceImpl service = service(mock(PmsSpuMapper.class), mock(PmsSkuMapper.class),
                categoryMapper, brandMapper, detailRepo);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("品牌不存在");
        verify(detailRepo, never()).save(any(ProductDetailDoc.class));
    }

    @Test
    void createRejectsInvalidSkuBeforeSavingDetail() {
        ProductDetailRepository detailRepo = mock(ProductDetailRepository.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        SpuSaveDTO dto = validDto();
        dto.getSkus().get(0).setPrice(BigDecimal.ZERO);
        when(categoryMapper.selectById(10L)).thenReturn(category());
        SpuServiceImpl service = service(mock(PmsSpuMapper.class), mock(PmsSkuMapper.class),
                categoryMapper, mock(PmsBrandMapper.class), detailRepo);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("SKU 售价必须大于 0");
        verify(detailRepo, never()).save(any(ProductDetailDoc.class));
    }

    @Test
    void createTrimsNamesAndDefaultsSkuFields() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        ProductDetailRepository detailRepo = mock(ProductDetailRepository.class);
        when(categoryMapper.selectById(10L)).thenReturn(category());
        when(detailRepo.save(any(ProductDetailDoc.class))).thenAnswer(invocation -> {
            ProductDetailDoc doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId("detail-1");
            }
            return doc;
        });
        SpuSaveDTO dto = validDto();
        dto.setName("  iPhone 16 Pro  ");
        dto.getSkus().get(0).setName("  黑色 256GB  ");
        dto.getSkus().get(0).setSkuCode("  IP16P-BK-256  ");
        SpuServiceImpl service = service(spuMapper, skuMapper, categoryMapper, mock(PmsBrandMapper.class), detailRepo);

        service.create(dto);

        ArgumentCaptor<PmsSpu> spuCaptor = ArgumentCaptor.forClass(PmsSpu.class);
        ArgumentCaptor<PmsSku> skuCaptor = ArgumentCaptor.forClass(PmsSku.class);
        verify(spuMapper).insert(spuCaptor.capture());
        verify(skuMapper).insert(skuCaptor.capture());
        assertThat(spuCaptor.getValue().getName()).isEqualTo("iPhone 16 Pro");
        assertThat(spuCaptor.getValue().getPublishStatus()).isZero();
        assertThat(spuCaptor.getValue().getPrice()).isEqualByComparingTo("6999.00");
        assertThat(skuCaptor.getValue().getName()).isEqualTo("黑色 256GB");
        assertThat(skuCaptor.getValue().getSkuCode()).isEqualTo("IP16P-BK-256");
        assertThat(skuCaptor.getValue().getLockedStock()).isZero();
        assertThat(skuCaptor.getValue().getSaleCount()).isZero();
    }

    @Test
    void updateRejectsNullDtoBeforeTouchingMapper() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));

        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("商品信息不能为空");
        verify(spuMapper, never()).selectById(any());
    }

    @Test
    void publishRejectsMissingIdBeforeTouchingMapper() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));

        assertThatThrownBy(() -> service.publish(null, 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺少 SPU ID");
        verify(spuMapper, never()).selectById(any());
        verify(spuMapper, never()).updateById(any(PmsSpu.class));
    }

    @Test
    void updateOperationRejectsMissingIdBeforeTouchingMapper() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));

        assertThatThrownBy(() -> service.updateOperation(null, new AdminSpuOperationDTO()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺少 SPU ID");
        verify(spuMapper, never()).selectById(any());
        verify(spuMapper, never()).updateById(any(PmsSpu.class));
    }

    @Test
    void updateOperationRejectsEmptyOperationBeforeUpdating() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));

        assertThatThrownBy(() -> service.updateOperation(10L, new AdminSpuOperationDTO()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("运营设置不能为空");
        verify(spuMapper, never()).updateById(any(PmsSpu.class));
    }

    @Test
    void deleteRejectsMissingIdBeforeTouchingMapper() {
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        SpuServiceImpl service = service(spuMapper, mock(PmsSkuMapper.class),
                mock(PmsCategoryMapper.class), mock(PmsBrandMapper.class), mock(ProductDetailRepository.class));

        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺少 SPU ID");
        verify(spuMapper, never()).selectById(any());
        verify(spuMapper, never()).deleteById(any());
    }

    private SpuServiceImpl service(PmsSpuMapper spuMapper,
                                   PmsSkuMapper skuMapper,
                                   PmsCategoryMapper categoryMapper,
                                   PmsBrandMapper brandMapper,
                                   ProductDetailRepository detailRepo) {
        return new SpuServiceImpl(spuMapper, skuMapper, categoryMapper, brandMapper, detailRepo, mock(SkuService.class));
    }

    private SpuSaveDTO validDto() {
        SpuSaveDTO dto = new SpuSaveDTO();
        dto.setName("iPhone 16 Pro");
        dto.setCategoryId(10L);
        dto.setSkus(Collections.singletonList(validSku()));
        return dto;
    }

    private SkuDTO validSku() {
        SkuDTO sku = new SkuDTO();
        sku.setName("黑色 256GB");
        sku.setSkuCode("IP16P-BK-256");
        sku.setPrice(new BigDecimal("6999.00"));
        sku.setStock(10);
        return sku;
    }

    private PmsCategory category() {
        PmsCategory category = new PmsCategory();
        category.setId(10L);
        category.setName("手机");
        return category;
    }

    private PmsSpu spu(Long id, String name) {
        PmsSpu spu = new PmsSpu();
        spu.setId(id);
        spu.setName(name);
        spu.setPublishStatus(1);
        return spu;
    }

    @SuppressWarnings("unused")
    private PmsBrand brand() {
        PmsBrand brand = new PmsBrand();
        brand.setId(20L);
        brand.setName("Apple");
        return brand;
    }
}
