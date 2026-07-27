package com.gj.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.vo.ProductCommentSummaryVO;
import com.gj.mall.product.vo.ProductCommentVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCommentServiceImplTest {

    @Test
    void summaryIgnoresNullScoresWhenCalculatingAverageButCountsApprovedComments() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        ProductCommentServiceImpl service = service(commentMapper, mock(PmsSkuMapper.class), spuMapperWithProduct());
        when(commentMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                comment(1L, 5, "[]"),
                comment(2L, 4, "[\"https://example.com/1.png\"]"),
                comment(3L, null, "")
        ));

        ProductCommentSummaryVO summary = service.summary(100L);

        assertThat(summary.getTotal()).isEqualTo(3L);
        assertThat(summary.getAverageScore()).isEqualByComparingTo("4.5");
        assertThat(summary.getGoodCount()).isEqualTo(2L);
        assertThat(summary.getGoodRate()).isEqualByComparingTo("66.7");
        assertThat(summary.getImageCount()).isEqualTo(1L);
    }

    @Test
    void pageReturnsOnlyApprovedCommentsAndEnrichesSkuSpecData() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        ProductCommentServiceImpl service = service(commentMapper, skuMapper, spuMapperWithProduct());
        Page<PmsProductComment> page = new Page<>(1, 50, 1);
        page.setRecords(Collections.singletonList(comment(1L, 5, "[\"https://example.com/1.png\"]")));
        when(commentMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        when(skuMapper.selectBatchIds(Collections.singletonList(200L))).thenReturn(Collections.singletonList(sku()));

        PageResult<ProductCommentVO> result = service.page(100L, -1L, 1000L, true);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        org.mockito.Mockito.verify(commentMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(50L);
        assertThat(result.getPageNum()).isEqualTo(1L);
        assertThat(result.getPageSize()).isEqualTo(50L);
        assertThat(result.getList()).hasSize(1);
        ProductCommentVO vo = result.getList().get(0);
        assertThat(vo.getSkuName()).isEqualTo("黑色 / 256GB");
        assertThat(vo.getSpecData()).containsEntry("颜色", "黑色");
        assertThat(vo.getImages()).containsExactly("https://example.com/1.png");
    }

    @Test
    void pageKeepsTotalWhenRequestedPageHasNoRecords() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        ProductCommentServiceImpl service = service(commentMapper, mock(PmsSkuMapper.class), spuMapperWithProduct());
        Page<PmsProductComment> page = new Page<>(3, 10, 27);
        page.setRecords(Collections.emptyList());
        when(commentMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);

        PageResult<ProductCommentVO> result = service.page(100L, 3L, 10L, null);

        assertThat(result.getTotal()).isEqualTo(27L);
        assertThat(result.getPageNum()).isEqualTo(3L);
        assertThat(result.getPageSize()).isEqualTo(10L);
        assertThat(result.getList()).isEmpty();
    }

    @Test
    void pageRejectsMissingOrUnknownProduct() {
        ProductCommentServiceImpl service = service(mock(PmsProductCommentMapper.class), mock(PmsSkuMapper.class), mock(PmsSpuMapper.class));

        assertThatThrownBy(() -> service.page(null, 1L, 10L, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺少商品ID");
        assertThatThrownBy(() -> service.page(100L, 1L, 10L, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("商品不存在");
    }

    private ProductCommentServiceImpl service(PmsProductCommentMapper commentMapper, PmsSkuMapper skuMapper, PmsSpuMapper spuMapper) {
        return new ProductCommentServiceImpl(commentMapper, skuMapper, spuMapper);
    }

    private PmsSpuMapper spuMapperWithProduct() {
        PmsSpuMapper mapper = mock(PmsSpuMapper.class);
        PmsSpu spu = new PmsSpu();
        spu.setId(100L);
        when(mapper.selectById(100L)).thenReturn(spu);
        return mapper;
    }

    private PmsProductComment comment(Long id, Integer score, String images) {
        PmsProductComment comment = new PmsProductComment();
        comment.setId(id);
        comment.setUserId(1L);
        comment.setSpuId(100L);
        comment.setSkuId(200L);
        comment.setScore(score);
        comment.setContent("体验不错");
        comment.setImages(images);
        comment.setStatus(1);
        return comment;
    }

    private PmsSku sku() {
        PmsSku sku = new PmsSku();
        sku.setId(200L);
        sku.setName("黑色 / 256GB");
        sku.setSpecData("{\"颜色\":\"黑色\",\"容量\":\"256GB\"}");
        return sku;
    }
}
