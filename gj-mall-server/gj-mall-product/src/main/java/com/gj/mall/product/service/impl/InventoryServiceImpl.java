package com.gj.mall.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.AdminInventoryAdjustDTO;
import com.gj.mall.product.dto.AdminInventoryBatchAdjustDTO;
import com.gj.mall.product.dto.AdminInventoryLogQueryDTO;
import com.gj.mall.product.dto.AdminInventoryQueryDTO;
import com.gj.mall.product.dto.AdminInventoryWarnStockDTO;
import com.gj.mall.product.entity.PmsInventoryLog;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsInventoryLogMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.InventoryService;
import com.gj.mall.product.vo.AdminInventoryLogVO;
import com.gj.mall.product.vo.AdminInventorySummaryVO;
import com.gj.mall.product.vo.AdminInventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int MAX_DELTA = 999999;

    private final PmsSkuMapper skuMapper;
    private final PmsSpuMapper spuMapper;
    private final PmsInventoryLogMapper inventoryLogMapper;

    @Override
    public PageResult<AdminInventoryVO> page(AdminInventoryQueryDTO query) {
        query = normalizeQuery(query);
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        IPage<AdminInventoryVO> result = skuMapper.selectInventoryPage(new Page<>(pageNum, pageSize), query);
        List<AdminInventoryVO> records = result.getRecords().stream()
                .peek(this::fillComputedFields)
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public AdminInventorySummaryVO summary(AdminInventoryQueryDTO query) {
        AdminInventorySummaryVO summary = skuMapper.selectInventorySummary(normalizeQuery(query));
        if (summary == null) {
            summary = new AdminInventorySummaryVO();
        }
        summary.setTotalSkuCount(safeLong(summary.getTotalSkuCount()));
        summary.setEmptySkuCount(safeLong(summary.getEmptySkuCount()));
        summary.setLowSkuCount(safeLong(summary.getLowSkuCount()));
        summary.setLockedSkuCount(safeLong(summary.getLockedSkuCount()));
        summary.setTotalAvailableStock(safeLong(summary.getTotalAvailableStock()));
        summary.setTotalLockedStock(safeLong(summary.getTotalLockedStock()));
        summary.setTotalStock(safeLong(summary.getTotalStock()));
        summary.setStockAmount(summary.getStockAmount() == null ? BigDecimal.ZERO : summary.getStockAmount());
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminInventoryVO adjustStock(Long skuId, AdminInventoryAdjustDTO dto) {
        if (dto == null || dto.getStockDelta() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "调整数量不能为空");
        }
        return adjustStockInternal(skuId, dto.getStockDelta(), dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AdminInventoryVO> batchAdjustStock(AdminInventoryBatchAdjustDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, "调整明细不能为空");
        }
        if (dto.getItems().size() > 50) {
            throw new BizException(ResultCode.PARAM_ERROR, "单次最多调整 50 个 SKU");
        }
        Set<Long> skuIds = new HashSet<>();
        List<AdminInventoryVO> result = new ArrayList<>();
        for (AdminInventoryBatchAdjustDTO.Item item : dto.getItems()) {
            if (item == null || item.getSkuId() == null) {
                throw new BizException(ResultCode.PARAM_MISSING, "调整明细缺少 SKU ID");
            }
            if (!skuIds.add(item.getSkuId())) {
                throw new BizException(ResultCode.PARAM_ERROR, "批量调整中存在重复 SKU");
            }
            if (item.getStockDelta() == null) {
                throw new BizException(ResultCode.PARAM_MISSING, "调整数量不能为空");
            }
            String remark = StrUtil.isBlank(item.getRemark()) ? dto.getRemark() : item.getRemark();
            result.add(adjustStockInternal(item.getSkuId(), item.getStockDelta(), remark));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminInventoryVO updateWarnStock(Long skuId, AdminInventoryWarnStockDTO dto) {
        if (skuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少 SKU ID");
        }
        if (dto == null || dto.getWarnStock() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "预警库存不能为空");
        }
        if (dto.getWarnStock() < 0 || dto.getWarnStock() > MAX_DELTA) {
            throw new BizException(ResultCode.PARAM_ERROR, "预警库存范围应为 0-999999");
        }
        PmsSku sku = skuMapper.selectByIdForUpdate(skuId);
        if (sku == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SKU 不存在");
        }
        int rows = skuMapper.updateWarnStock(skuId, dto.getWarnStock());
        if (rows <= 0) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SKU 不存在");
        }
        AdminInventoryVO vo = skuMapper.selectInventoryBySkuId(skuId);
        fillComputedFields(vo);
        return vo;
    }

    private AdminInventoryVO adjustStockInternal(Long skuId, Integer stockDelta, String remark) {
        if (skuId == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少 SKU ID");
        }
        int delta = stockDelta;
        if (delta == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "调整数量不能为 0");
        }
        long absDelta = Math.abs((long) delta);
        if (absDelta > MAX_DELTA) {
            throw new BizException(ResultCode.PARAM_ERROR, "单次调整数量过大");
        }

        PmsSku before = skuMapper.selectByIdForUpdate(skuId);
        if (before == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SKU 不存在");
        }

        int rows = delta > 0
                ? skuMapper.increaseStock(skuId, delta)
                : skuMapper.decreaseStock(skuId, (int) absDelta);
        if (rows <= 0) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "可用库存不足");
        }

        PmsSku after = skuMapper.selectById(skuId);
        saveLog(before, after, delta, remark);
        AdminInventoryVO vo = skuMapper.selectInventoryBySkuId(skuId);
        if (vo == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SKU 不存在");
        }
        fillComputedFields(vo);
        return vo;
    }

    @Override
    public PageResult<AdminInventoryLogVO> logPage(AdminInventoryLogQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        IPage<PmsInventoryLog> result = inventoryLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<PmsInventoryLog>lambdaQuery()
                        .eq(query.getSkuId() != null, PmsInventoryLog::getSkuId, query.getSkuId())
                        .eq(query.getSpuId() != null, PmsInventoryLog::getSpuId, query.getSpuId())
                        .orderByDesc(PmsInventoryLog::getCreateTime));
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                enrichLogs(result.getRecords()));
    }

    private void saveLog(PmsSku before, PmsSku after, int delta, String remark) {
        PmsInventoryLog log = new PmsInventoryLog();
        log.setSpuId(before.getSpuId());
        log.setSkuId(before.getId());
        log.setChangeType(delta > 0 ? 1 : 2);
        log.setChangeQuantity(Math.abs(delta));
        log.setStockBefore(safeInt(before.getStock()));
        log.setStockAfter(safeInt(after.getStock()));
        log.setLockedStockBefore(safeInt(before.getLockedStock()));
        log.setLockedStockAfter(safeInt(after.getLockedStock()));
        log.setRemark(StrUtil.isBlank(remark) ? null : StrUtil.sub(remark.trim(), 0, 255));
        inventoryLogMapper.insert(log);
    }

    private List<AdminInventoryLogVO> enrichLogs(List<PmsInventoryLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> skuIds = logs.stream()
                .map(PmsInventoryLog::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> spuIds = logs.stream()
                .map(PmsInventoryLog::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PmsSku> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuMapper.selectBatchIds(skuIds).stream()
                        .collect(Collectors.toMap(PmsSku::getId, Function.identity(), (a, b) -> a));
        Map<Long, PmsSpu> spuMap = spuIds.isEmpty()
                ? Collections.emptyMap()
                : spuMapper.selectBatchIds(spuIds).stream()
                        .collect(Collectors.toMap(PmsSpu::getId, Function.identity(), (a, b) -> a));

        return logs.stream().map(log -> {
            PmsSku sku = skuMap.get(log.getSkuId());
            PmsSpu spu = spuMap.get(log.getSpuId());
            AdminInventoryLogVO vo = new AdminInventoryLogVO();
            vo.setId(log.getId());
            vo.setSpuId(log.getSpuId());
            vo.setSpuName(spu == null ? null : spu.getName());
            vo.setSkuId(log.getSkuId());
            vo.setSkuName(sku == null ? null : sku.getName());
            vo.setSkuCode(sku == null ? null : sku.getSkuCode());
            vo.setChangeType(log.getChangeType());
            vo.setChangeTypeDesc(log.getChangeType() != null && log.getChangeType() == 1 ? "增加" : "减少");
            vo.setChangeQuantity(log.getChangeQuantity());
            vo.setStockBefore(log.getStockBefore());
            vo.setStockAfter(log.getStockAfter());
            vo.setLockedStockBefore(log.getLockedStockBefore());
            vo.setLockedStockAfter(log.getLockedStockAfter());
            vo.setRemark(log.getRemark());
            vo.setCreateTime(log.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private void fillComputedFields(AdminInventoryVO vo) {
        int stock = safeInt(vo.getStock());
        int lockedStock = safeInt(vo.getLockedStock());
        int warnStock = vo.getWarnStock() == null ? LOW_STOCK_THRESHOLD : vo.getWarnStock();
        vo.setStock(stock);
        vo.setLockedStock(lockedStock);
        vo.setWarnStock(warnStock);
        vo.setTotalStock(stock + lockedStock);
        vo.setAlertGap(Math.max(warnStock - stock, 0));
        vo.setSaleCount(safeInt(vo.getSaleCount()));
        vo.setSpecData(parseSpecData(vo.getSpecDataJson()));
        if (stock == 0) {
            vo.setStockStatus("empty");
            vo.setStockStatusDesc("无可用库存");
        } else if (stock <= LOW_STOCK_THRESHOLD) {
            vo.setStockStatus("low");
            vo.setStockStatusDesc("低库存");
        } else {
            vo.setStockStatus("normal");
            vo.setStockStatusDesc("库存正常");
        }
    }

    private Map<String, String> parseSpecData(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private AdminInventoryQueryDTO normalizeQuery(AdminInventoryQueryDTO query) {
        if (query == null) {
            query = new AdminInventoryQueryDTO();
        }
        query.setKeyword(StrUtil.trim(query.getKeyword()));
        return query;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }
}
