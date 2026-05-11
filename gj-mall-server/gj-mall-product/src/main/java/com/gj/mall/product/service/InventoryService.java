package com.gj.mall.product.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.AdminInventoryAdjustDTO;
import com.gj.mall.product.dto.AdminInventoryBatchAdjustDTO;
import com.gj.mall.product.dto.AdminInventoryLogQueryDTO;
import com.gj.mall.product.dto.AdminInventoryQueryDTO;
import com.gj.mall.product.dto.AdminInventoryWarnStockDTO;
import com.gj.mall.product.vo.AdminInventoryLogVO;
import com.gj.mall.product.vo.AdminInventorySummaryVO;
import com.gj.mall.product.vo.AdminInventoryVO;

import java.util.List;

public interface InventoryService {

    PageResult<AdminInventoryVO> page(AdminInventoryQueryDTO query);

    AdminInventorySummaryVO summary(AdminInventoryQueryDTO query);

    AdminInventoryVO adjustStock(Long skuId, AdminInventoryAdjustDTO dto);

    List<AdminInventoryVO> batchAdjustStock(AdminInventoryBatchAdjustDTO dto);

    AdminInventoryVO updateWarnStock(Long skuId, AdminInventoryWarnStockDTO dto);

    PageResult<AdminInventoryLogVO> logPage(AdminInventoryLogQueryDTO query);
}
