package com.gj.mall.cart.service.impl;

import com.alibaba.fastjson2.JSON;
import com.gj.mall.cart.dto.AddCartDTO;
import com.gj.mall.cart.dto.MergeCartDTO;
import com.gj.mall.cart.vo.CartItemVO;
import com.gj.mall.cart.vo.CartVO;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.product.vo.SkuVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final String CART_KEY = "mall:cart:" + USER_ID;

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private HashOperations<String, String, String> hash;
    @Mock
    private SkuService skuService;
    @Mock
    private PmsSpuMapper spuMapper;

    private CartServiceImpl service;

    @BeforeEach
    void setUp() {
        doReturn(hash).when(redis).opsForHash();
        service = new CartServiceImpl(redis, skuService, spuMapper);
    }

    @Test
    void getMarksInvalidItemsAndExcludesThemFromSelectedTotals() {
        Map<String, String> raw = new HashMap<>();
        raw.put("1", lineJson(1L, 2, 1, 100L));
        raw.put("2", lineJson(2L, 3, 1, 200L));
        when(hash.entries(CART_KEY)).thenReturn(raw);
        PmsSku validSku = sku(1L, 101L, "有效SKU", "valid.png", "19.90", 10);
        when(skuService.listByIds(anyCollection())).thenReturn(Collections.singletonList(validSku));
        when(spuMapper.selectBatchIds(Collections.singleton(101L)))
                .thenReturn(Collections.singletonList(spu(101L, "有效商品", 1, "spu.png")));
        when(skuService.toVO(validSku)).thenReturn(skuVO(Collections.singletonMap("颜色", "黑色")));

        CartVO cart = service.get(USER_ID);

        assertThat(cart.getTotalCount()).isEqualTo(5);
        assertThat(cart.getSelectedCount()).isEqualTo(2);
        assertThat(cart.getSelectedAmount()).isEqualByComparingTo("39.80");
        assertThat(cart.getItems()).hasSize(2);
        CartItemVO deletedItem = cart.getItems().stream()
                .filter(item -> Long.valueOf(2L).equals(item.getSkuId()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        assertThat(deletedItem.getInvalid()).isTrue();
        assertThat(deletedItem.getSelected()).isEqualTo(1);
        assertThat(deletedItem.getInvalidReason()).isEqualTo("商品已删除或下架");
    }

    @Test
    void selectAllOnlySelectsCurrentlyValidItems() {
        Map<String, String> raw = new HashMap<>();
        raw.put("1", lineJson(1L, 2, 0, 100L));
        raw.put("2", lineJson(2L, 1, 0, 200L));
        raw.put("3", lineJson(3L, 4, 0, 300L));
        when(hash.entries(CART_KEY)).thenReturn(raw);
        PmsSku validSku = sku(1L, 101L, "有效SKU", "valid.png", "19.90", 10);
        PmsSku stockShortSku = sku(2L, 102L, "缺货SKU", "short.png", "9.90", 0);
        when(skuService.listByIds(anyCollection())).thenReturn(Arrays.asList(validSku, stockShortSku));
        when(spuMapper.selectBatchIds(anyCollection()))
                .thenReturn(Arrays.asList(
                        spu(101L, "有效商品", 1, "valid-spu.png"),
                        spu(102L, "缺货商品", 1, "short-spu.png")
                ));

        service.selectAll(USER_ID, true);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hash).putAll(eq(CART_KEY), captor.capture());
        Map<String, String> updated = captor.getValue();
        assertThat(selectedOf(updated, "1")).isEqualTo(1);
        assertThat(selectedOf(updated, "2")).isEqualTo(0);
        assertThat(selectedOf(updated, "3")).isEqualTo(0);
    }

    @Test
    void selectAllCanUnselectEveryItemWithoutProductLookup() {
        Map<String, String> raw = new HashMap<>();
        raw.put("1", lineJson(1L, 2, 1, 100L));
        raw.put("2", lineJson(2L, 1, 1, 200L));
        when(hash.entries(CART_KEY)).thenReturn(raw);

        service.selectAll(USER_ID, false);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hash).putAll(eq(CART_KEY), captor.capture());
        assertThat(selectedOf(captor.getValue(), "1")).isEqualTo(0);
        assertThat(selectedOf(captor.getValue(), "2")).isEqualTo(0);
        verify(skuService, never()).listByIds(anyCollection());
    }

    @Test
    void mergeKeepsValidItemsWhenAnotherItemCannotBeAdded() {
        AddCartDTO valid = addDto(1L, 2);
        AddCartDTO offShelf = addDto(2L, 1);
        MergeCartDTO dto = new MergeCartDTO();
        dto.setItems(Arrays.asList(valid, offShelf));

        when(skuService.getByIdOrThrow(1L)).thenReturn(sku(1L, 101L, "有效SKU", "valid.png", "19.90", 10));
        when(spuMapper.selectById(101L)).thenReturn(spu(101L, "有效商品", 1, "valid-spu.png"));
        when(hash.get(CART_KEY, "1")).thenReturn(null);
        when(hash.size(CART_KEY)).thenReturn(0L);
        when(skuService.getByIdOrThrow(2L)).thenReturn(sku(2L, 102L, "下架SKU", "off.png", "9.90", 10));
        when(spuMapper.selectById(102L)).thenReturn(spu(102L, "下架商品", 0, "off-spu.png"));

        service.merge(USER_ID, dto);

        verify(hash).put(eq(CART_KEY), eq("1"), any(String.class));
        verify(hash, never()).put(eq(CART_KEY), eq("2"), any(String.class));
    }

    private static String lineJson(Long skuId, Integer quantity, Integer selected, Long addTime) {
        Map<String, Object> line = new HashMap<>();
        line.put("skuId", skuId);
        line.put("quantity", quantity);
        line.put("selected", selected);
        line.put("addTime", addTime);
        return JSON.toJSONString(line);
    }

    private static Integer selectedOf(Map<String, String> updated, String field) {
        return JSON.parseObject(updated.get(field)).getInteger("selected");
    }

    private static AddCartDTO addDto(Long skuId, Integer quantity) {
        AddCartDTO dto = new AddCartDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(quantity);
        return dto;
    }

    private static PmsSku sku(Long id, Long spuId, String name, String image, String price, Integer stock) {
        PmsSku sku = new PmsSku();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setName(name);
        sku.setImage(image);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        return sku;
    }

    private static PmsSpu spu(Long id, String name, Integer publishStatus, String mainImage) {
        PmsSpu spu = new PmsSpu();
        spu.setId(id);
        spu.setName(name);
        spu.setPublishStatus(publishStatus);
        spu.setMainImage(mainImage);
        return spu;
    }

    private static SkuVO skuVO(Map<String, String> specData) {
        SkuVO vo = new SkuVO();
        vo.setSpecData(specData);
        return vo;
    }
}
