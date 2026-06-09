package com.gj.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.AdminInventoryLogQueryDTO;
import com.gj.mall.product.dto.AdminInventoryQueryDTO;
import com.gj.mall.product.entity.PmsInventoryLog;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsInventoryLogMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.vo.AdminInventoryLogVO;
import com.gj.mall.product.vo.AdminInventoryVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceImplTest {

    @Test
    void pageCapsPageSizeAndUsesWarnStockForLowStatus() {
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        InventoryServiceImpl service = service(skuMapper, mock(PmsSpuMapper.class), mock(PmsInventoryLogMapper.class));
        Page<AdminInventoryVO> page = new Page<>(1, 100, 1);
        AdminInventoryVO item = inventoryItem(15, 0, 20);
        page.setRecords(Collections.singletonList(item));
        when(skuMapper.selectInventoryPage(any(Page.class), any(AdminInventoryQueryDTO.class))).thenReturn(page);
        AdminInventoryQueryDTO query = new AdminInventoryQueryDTO();
        query.setPageNum(-1L);
        query.setPageSize(500L);
        query.setKeyword("  SKU-100  ");

        PageResult<AdminInventoryVO> result = service.page(query);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<AdminInventoryQueryDTO> queryCaptor = ArgumentCaptor.forClass(AdminInventoryQueryDTO.class);
        verify(skuMapper).selectInventoryPage(pageCaptor.capture(), queryCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100L);
        assertThat(queryCaptor.getValue().getKeyword()).isEqualTo("SKU-100");
        assertThat(result.getPageSize()).isEqualTo(100L);
        AdminInventoryVO vo = result.getList().get(0);
        assertThat(vo.getStockStatus()).isEqualTo("low");
        assertThat(vo.getStockStatusDesc()).isEqualTo("低库存");
        assertThat(vo.getAlertGap()).isEqualTo(5);
        assertThat(vo.getTotalStock()).isEqualTo(15);
    }

    @Test
    void logPageUsesDefaultQueryWhenQueryIsNullAndCapsPageSize() {
        PmsInventoryLogMapper logMapper = mock(PmsInventoryLogMapper.class);
        PmsSkuMapper skuMapper = mock(PmsSkuMapper.class);
        PmsSpuMapper spuMapper = mock(PmsSpuMapper.class);
        InventoryServiceImpl service = service(skuMapper, spuMapper, logMapper);
        Page<PmsInventoryLog> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(inventoryLog()));
        when(logMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        when(skuMapper.selectBatchIds(Collections.singletonList(200L))).thenReturn(Collections.singletonList(sku()));
        when(spuMapper.selectBatchIds(Collections.singletonList(100L))).thenReturn(Collections.singletonList(spu()));

        PageResult<AdminInventoryLogVO> result = service.logPage(null);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(logMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getSpuName()).isEqualTo("iPhone 16 Pro");
        assertThat(result.getList().get(0).getSkuName()).isEqualTo("黑色 256GB");
    }

    @Test
    void logPageCapsOversizedPageSize() {
        PmsInventoryLogMapper logMapper = mock(PmsInventoryLogMapper.class);
        InventoryServiceImpl service = service(mock(PmsSkuMapper.class), mock(PmsSpuMapper.class), logMapper);
        Page<PmsInventoryLog> page = new Page<>(1, 100, 0);
        page.setRecords(Collections.emptyList());
        when(logMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        AdminInventoryLogQueryDTO query = new AdminInventoryLogQueryDTO();
        query.setPageNum(-1L);
        query.setPageSize(500L);

        PageResult<AdminInventoryLogVO> result = service.logPage(query);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(logMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100L);
        assertThat(result.getPageSize()).isEqualTo(100L);
    }

    private InventoryServiceImpl service(
            PmsSkuMapper skuMapper,
            PmsSpuMapper spuMapper,
            PmsInventoryLogMapper inventoryLogMapper) {
        return new InventoryServiceImpl(skuMapper, spuMapper, inventoryLogMapper);
    }

    private AdminInventoryVO inventoryItem(Integer stock, Integer lockedStock, Integer warnStock) {
        AdminInventoryVO vo = new AdminInventoryVO();
        vo.setSkuId(200L);
        vo.setSpuId(100L);
        vo.setSkuCode("SKU-100");
        vo.setSkuName("黑色 256GB");
        vo.setStock(stock);
        vo.setLockedStock(lockedStock);
        vo.setWarnStock(warnStock);
        vo.setSaleCount(null);
        vo.setSpecDataJson("{\"颜色\":\"黑色\"}");
        return vo;
    }

    private PmsInventoryLog inventoryLog() {
        PmsInventoryLog log = new PmsInventoryLog();
        log.setId(1L);
        log.setSpuId(100L);
        log.setSkuId(200L);
        log.setChangeType(1);
        log.setChangeQuantity(5);
        log.setStockBefore(10);
        log.setStockAfter(15);
        return log;
    }

    private PmsSku sku() {
        PmsSku sku = new PmsSku();
        sku.setId(200L);
        sku.setName("黑色 256GB");
        sku.setSkuCode("SKU-100");
        return sku;
    }

    private PmsSpu spu() {
        PmsSpu spu = new PmsSpu();
        spu.setId(100L);
        spu.setName("iPhone 16 Pro");
        return spu;
    }
}
