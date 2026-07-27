package com.gj.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminCommentActionDTO;
import com.gj.mall.admin.dto.AdminCommentQueryDTO;
import com.gj.mall.admin.dto.AdminCommentReplyDTO;
import com.gj.mall.admin.vo.AdminCommentVO;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCommentServiceImplTest {

    @Test
    void approveStoresApprovedStatusAndTrimmedAuditRemark() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        AdminCommentServiceImpl service = service(commentMapper);
        when(commentMapper.selectById(10L)).thenReturn(comment());
        AdminCommentActionDTO dto = new AdminCommentActionDTO();
        dto.setAuditRemark("  内容真实  ");

        service.approve(10L, dto);

        ArgumentCaptor<PmsProductComment> captor = ArgumentCaptor.forClass(PmsProductComment.class);
        verify(commentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getAuditRemark()).isEqualTo("内容真实");
        assertThat(captor.getValue().getAuditTime()).isNotNull();
    }

    @Test
    void replyRejectsBlankContentAndTrimsValidContent() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        AdminCommentServiceImpl service = service(commentMapper);
        when(commentMapper.selectById(10L)).thenReturn(comment());
        AdminCommentReplyDTO blank = new AdminCommentReplyDTO();
        blank.setReplyContent("  ");

        assertThatThrownBy(() -> service.reply(10L, blank))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("回复内容不能为空");

        AdminCommentReplyDTO dto = new AdminCommentReplyDTO();
        dto.setReplyContent("  感谢反馈  ");
        service.reply(10L, dto);

        ArgumentCaptor<PmsProductComment> captor = ArgumentCaptor.forClass(PmsProductComment.class);
        verify(commentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getReplyContent()).isEqualTo("感谢反馈");
        assertThat(captor.getValue().getReplyTime()).isNotNull();
    }

    @Test
    void detailEnrichesUserProductSkuBrandCategoryAndOrderItemFallbacks() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        PmsBrandMapper brandMapper = mock(PmsBrandMapper.class);
        PmsCategoryMapper categoryMapper = mock(PmsCategoryMapper.class);
        OmsOrderItemMapper orderItemMapper = mock(OmsOrderItemMapper.class);
        AdminCommentServiceImpl service = new AdminCommentServiceImpl(commentMapper, userMapper, spuMapper, skuMapper,
                brandMapper, categoryMapper, orderItemMapper);
        when(commentMapper.selectById(10L)).thenReturn(comment());
        when(userMapper.selectBatchIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(user()));
        when(spuMapper.selectBatchIds(Collections.singletonList(100L))).thenReturn(Collections.singletonList(spu()));
        when(skuMapper.selectBatchIds(Collections.singletonList(200L))).thenReturn(Collections.singletonList(sku()));
        when(orderItemMapper.selectBatchIds(Collections.singletonList(300L))).thenReturn(Collections.singletonList(orderItem()));
        when(brandMapper.selectBatchIds(Collections.singletonList(400L))).thenReturn(Collections.singletonList(brand()));
        when(categoryMapper.selectBatchIds(Collections.singletonList(500L))).thenReturn(Collections.singletonList(category()));

        AdminCommentVO detail = service.detail(10L);

        assertThat(detail.getUsername()).isEqualTo("alice");
        assertThat(detail.getSpuName()).isEqualTo("iPhone 16 Pro");
        assertThat(detail.getSkuName()).isEqualTo("黑色 / 256GB");
        assertThat(detail.getBrandName()).isEqualTo("Apple");
        assertThat(detail.getCategoryName()).isEqualTo("手机");
        assertThat(detail.getSpecData()).containsEntry("颜色", "黑色");
        assertThat(detail.getImages()).containsExactly("https://example.com/comment.png");
    }

    @Test
    void detailRejectsMissingComment() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        AdminCommentServiceImpl service = service(commentMapper);

        assertThatThrownBy(() -> service.detail(10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("评价不存在");
    }

    @Test
    void pageUsesDefaultsWhenQueryIsNull() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        AdminCommentServiceImpl service = service(commentMapper);
        Page<PmsProductComment> page = new Page<>(1, 10, 0);
        page.setRecords(Collections.emptyList());
        when(commentMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);

        PageResult<AdminCommentVO> result = service.page(null);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(commentMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10L);
        assertThat(result.getPageNum()).isEqualTo(1L);
        assertThat(result.getPageSize()).isEqualTo(10L);
    }

    @Test
    void pageNormalizesPageBoundsAndKeyword() {
        PmsProductCommentMapper commentMapper = mock(PmsProductCommentMapper.class);
        UmsUserMapper userMapper = mock(UmsUserMapper.class);
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        AdminCommentServiceImpl service = new AdminCommentServiceImpl(commentMapper, userMapper, spuMapper, skuMapper,
                mock(PmsBrandMapper.class), mock(PmsCategoryMapper.class), mock(OmsOrderItemMapper.class));
        Page<PmsProductComment> page = new Page<>(1, 100, 0);
        page.setRecords(Collections.emptyList());
        when(commentMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(spuMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(skuMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        AdminCommentQueryDTO query = new AdminCommentQueryDTO();
        query.setPageNum(-1L);
        query.setPageSize(500L);
        query.setKeyword("  iPhone  ");

        service.page(query);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(commentMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100L);
        assertThat(query.getKeyword()).isEqualTo("iPhone");
    }

    private AdminCommentServiceImpl service(PmsProductCommentMapper commentMapper) {
        return new AdminCommentServiceImpl(commentMapper, mock(UmsUserMapper.class), mock(PmsSpuMapper.class),
                mock(PmsSkuMapper.class), mock(PmsBrandMapper.class), mock(PmsCategoryMapper.class),
                mock(OmsOrderItemMapper.class));
    }

    private PmsProductComment comment() {
        PmsProductComment comment = new PmsProductComment();
        comment.setId(10L);
        comment.setUserId(1L);
        comment.setSpuId(100L);
        comment.setSkuId(200L);
        comment.setOrderItemId(300L);
        comment.setScore(5);
        comment.setContent("体验不错");
        comment.setImages("[\"https://example.com/comment.png\"]");
        comment.setStatus(0);
        return comment;
    }

    private UmsUser user() {
        UmsUser user = new UmsUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setPhone("13800000000");
        return user;
    }

    private PmsSpu spu() {
        PmsSpu spu = new PmsSpu();
        spu.setId(100L);
        spu.setName("iPhone 16 Pro");
        spu.setBrandId(400L);
        spu.setCategoryId(500L);
        return spu;
    }

    private PmsSku sku() {
        PmsSku sku = new PmsSku();
        sku.setId(200L);
        sku.setName("黑色 / 256GB");
        sku.setSkuCode("IP16P-BK-256");
        sku.setPrice(new BigDecimal("8999.00"));
        sku.setSpecData("{\"颜色\":\"黑色\",\"容量\":\"256GB\"}");
        return sku;
    }

    private OmsOrderItem orderItem() {
        OmsOrderItem item = new OmsOrderItem();
        item.setId(300L);
        item.setSkuName("订单快照 SKU");
        item.setPrice(new BigDecimal("8999.00"));
        return item;
    }

    private PmsBrand brand() {
        PmsBrand brand = new PmsBrand();
        brand.setId(400L);
        brand.setName("Apple");
        return brand;
    }

    private PmsCategory category() {
        PmsCategory category = new PmsCategory();
        category.setId(500L);
        category.setName("手机");
        return category;
    }
}
