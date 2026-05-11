package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminSeckillQueryDTO;
import com.gj.mall.admin.dto.AdminSeckillSaveDTO;
import com.gj.mall.admin.dto.AdminSeckillSkuSaveDTO;
import com.gj.mall.admin.service.AdminSeckillService;
import com.gj.mall.admin.vo.AdminSeckillSkuVO;
import com.gj.mall.admin.vo.AdminSeckillVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.marketing.entity.SmsSeckill;
import com.gj.mall.marketing.entity.SmsSeckillSku;
import com.gj.mall.marketing.mapper.SmsSeckillMapper;
import com.gj.mall.marketing.mapper.SmsSeckillSkuMapper;
import com.gj.mall.marketing.service.SeckillService;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSeckillServiceImpl implements AdminSeckillService {

    private static final String STOCK_KEY_PREFIX = "mall:seckill:stock:";

    private final SmsSeckillMapper seckillMapper;
    private final SmsSeckillSkuMapper seckillSkuMapper;
    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final SeckillService seckillService;
    private final StringRedisTemplate redis;

    @Override
    public PageResult<AdminSeckillVO> page(AdminSeckillQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        IPage<SmsSeckill> result = seckillMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<SmsSeckill>lambdaQuery()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SmsSeckill::getName, query.getKeyword())
                        .eq(query.getStatus() != null, SmsSeckill::getStatus, query.getStatus())
                        .orderByDesc(SmsSeckill::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        Map<Long, List<AdminSeckillSkuVO>> skuMap = loadSkuVOMap(
                result.getRecords().stream().map(SmsSeckill::getId).collect(Collectors.toList()));
        List<AdminSeckillVO> list = result.getRecords().stream()
                .map(item -> AdminSeckillVO.from(item, skuMap.get(item.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    public AdminSeckillVO detail(Long id) {
        SmsSeckill seckill = getByIdOrThrow(id);
        return AdminSeckillVO.from(seckill, loadSkuVOMap(Collections.singletonList(id)).get(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminSeckillSaveDTO dto) {
        validateSaveDTO(dto);
        SmsSeckill seckill = new SmsSeckill();
        fillSeckill(seckill, dto);
        seckill.setId(null);
        seckill.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        seckillMapper.insert(seckill);
        if (Integer.valueOf(1).equals(seckill.getStatus())) {
            validateCanOnline(seckill.getId(), seckill);
            seckillService.warmUpStock(seckill.getId());
        }
        return seckill.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AdminSeckillSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少秒杀活动ID");
        }
        SmsSeckill exists = getByIdOrThrow(dto.getId());
        validateSaveDTO(dto);
        SmsSeckill seckill = new SmsSeckill();
        seckill.setId(dto.getId());
        fillSeckill(seckill, dto);
        if (dto.getStatus() != null) {
            seckill.setStatus(dto.getStatus());
        }
        Integer finalStatus = dto.getStatus() == null ? exists.getStatus() : dto.getStatus();
        SmsSeckill merged = mergeForStatusCheck(exists, dto);
        if (Integer.valueOf(1).equals(finalStatus)) {
            validateCanOnline(dto.getId(), merged);
        }
        seckillMapper.updateById(seckill);
        if (Integer.valueOf(1).equals(dto.getStatus())) {
            seckillService.warmUpStock(dto.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        SmsSeckill exists = getByIdOrThrow(id);
        if (Integer.valueOf(1).equals(status)) {
            validateCanOnline(id, exists);
        }
        SmsSeckill seckill = new SmsSeckill();
        seckill.setId(id);
        seckill.setStatus(status);
        seckillMapper.updateById(seckill);
        if (Integer.valueOf(1).equals(status)) {
            seckillService.warmUpStock(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SmsSeckill seckill = getByIdOrThrow(id);
        if (Integer.valueOf(1).equals(seckill.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "上线中的活动不能删除");
        }
        List<SmsSeckillSku> skus = loadSkus(id);
        boolean hasSold = skus.stream().anyMatch(item -> defaultInt(item.getSoldCount()) > 0);
        if (hasSold) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "已有销量的活动不能删除，可改为结束");
        }
        seckillSkuMapper.delete(Wrappers.<SmsSeckillSku>lambdaQuery()
                .eq(SmsSeckillSku::getSeckillId, id));
        seckillMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSku(AdminSeckillSkuSaveDTO dto) {
        validateSkuDTO(dto);
        SmsSeckill seckill = getByIdOrThrow(dto.getSeckillId());
        if (Integer.valueOf(1).equals(seckill.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "上线中的活动不能修改秒杀 SKU");
        }
        PmsSpu spu = spuMapper.selectById(dto.getSpuId());
        if (spu == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SPU不存在");
        }
        PmsSku productSku = skuMapper.selectById(dto.getSkuId());
        if (productSku == null || !Objects.equals(productSku.getSpuId(), dto.getSpuId())) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SKU不存在或不属于该SPU");
        }
        if (productSku.getPrice() != null && dto.getSeckillPrice().compareTo(productSku.getPrice()) >= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "秒杀价需低于商品原价");
        }
        if (productSku.getStock() != null && dto.getSeckillStock() > productSku.getStock()) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "秒杀库存不能大于商品可用库存");
        }
        Long duplicateCount = seckillSkuMapper.selectCount(Wrappers.<SmsSeckillSku>lambdaQuery()
                .eq(SmsSeckillSku::getSeckillId, dto.getSeckillId())
                .eq(SmsSeckillSku::getSkuId, dto.getSkuId())
                .ne(dto.getId() != null, SmsSeckillSku::getId, dto.getId()));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "该SKU已加入当前秒杀活动");
        }

        SmsSeckillSku seckillSku = new SmsSeckillSku();
        fillSeckillSku(seckillSku, dto);
        if (dto.getId() == null) {
            seckillSku.setSoldCount(0);
            seckillSkuMapper.insert(seckillSku);
            return seckillSku.getId();
        }
        SmsSeckillSku exists = seckillSkuMapper.selectById(dto.getId());
        if (exists == null || !Objects.equals(exists.getSeckillId(), dto.getSeckillId())) {
            throw new BizException(ResultCode.SECKILL_NOT_FOUND, "秒杀 SKU 不存在");
        }
        if (dto.getSeckillStock() < defaultInt(exists.getSoldCount())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "秒杀库存不能小于已售数量");
        }
        seckillSku.setId(dto.getId());
        seckillSkuMapper.updateById(seckillSku);
        return dto.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long skuId) {
        SmsSeckillSku seckillSku = seckillSkuMapper.selectById(skuId);
        if (seckillSku == null) {
            throw new BizException(ResultCode.SECKILL_NOT_FOUND, "秒杀 SKU 不存在");
        }
        SmsSeckill seckill = getByIdOrThrow(seckillSku.getSeckillId());
        if (Integer.valueOf(1).equals(seckill.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "上线中的活动不能删除秒杀 SKU");
        }
        if (defaultInt(seckillSku.getSoldCount()) > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "已有销量的秒杀 SKU 不能删除");
        }
        seckillSkuMapper.deleteById(skuId);
    }

    @Override
    public void warmUp(Long id) {
        SmsSeckill seckill = getByIdOrThrow(id);
        if (seckill.getEndTime() != null && LocalDateTime.now().isAfter(seckill.getEndTime())) {
            throw new BizException(ResultCode.SECKILL_NOT_ACTIVE, "已结束活动不能预热库存");
        }
        if (CollUtil.isEmpty(loadSkus(id))) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "请先配置秒杀 SKU");
        }
        seckillService.warmUpStock(id);
    }

    private SmsSeckill getByIdOrThrow(Long id) {
        SmsSeckill seckill = seckillMapper.selectById(id);
        if (seckill == null) {
            throw new BizException(ResultCode.SECKILL_NOT_FOUND);
        }
        return seckill;
    }

    private void fillSeckill(SmsSeckill seckill, AdminSeckillSaveDTO dto) {
        seckill.setName(dto.getName().trim());
        seckill.setStartTime(dto.getStartTime());
        seckill.setEndTime(dto.getEndTime());
    }

    private void fillSeckillSku(SmsSeckillSku seckillSku, AdminSeckillSkuSaveDTO dto) {
        seckillSku.setSeckillId(dto.getSeckillId());
        seckillSku.setSpuId(dto.getSpuId());
        seckillSku.setSkuId(dto.getSkuId());
        seckillSku.setSeckillPrice(dto.getSeckillPrice());
        seckillSku.setSeckillStock(dto.getSeckillStock());
        seckillSku.setSeckillLimit(dto.getSeckillLimit());
    }

    private void validateSaveDTO(AdminSeckillSaveDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING);
        }
        validateStatus(dto.getStatus());
        if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BizException(ResultCode.PARAM_ERROR, "活动时间设置不正确");
        }
    }

    private void validateSkuDTO(AdminSeckillSkuSaveDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING);
        }
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "秒杀价必须大于0");
        }
        if (dto.getSeckillStock() == null || dto.getSeckillStock() <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "秒杀库存必须大于0");
        }
        if (dto.getSeckillLimit() == null || dto.getSeckillLimit() <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "限购数量必须大于0");
        }
        if (dto.getSeckillLimit() > dto.getSeckillStock()) {
            throw new BizException(ResultCode.PARAM_ERROR, "限购数量不能大于秒杀库存");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1 && status != 2) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
    }

    private void validateCanOnline(Long seckillId, SmsSeckill seckill) {
        LocalDateTime now = LocalDateTime.now();
        if (seckill.getEndTime() != null && !seckill.getEndTime().isAfter(now)) {
            throw new BizException(ResultCode.SECKILL_NOT_ACTIVE, "活动已结束，不能上线");
        }
        if (CollUtil.isEmpty(loadSkus(seckillId))) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "请先配置秒杀 SKU");
        }
    }

    private SmsSeckill mergeForStatusCheck(SmsSeckill exists, AdminSeckillSaveDTO dto) {
        SmsSeckill merged = new SmsSeckill();
        merged.setId(exists.getId());
        merged.setName(dto.getName() == null ? exists.getName() : dto.getName());
        merged.setStartTime(dto.getStartTime() == null ? exists.getStartTime() : dto.getStartTime());
        merged.setEndTime(dto.getEndTime() == null ? exists.getEndTime() : dto.getEndTime());
        merged.setStatus(dto.getStatus() == null ? exists.getStatus() : dto.getStatus());
        return merged;
    }

    private List<SmsSeckillSku> loadSkus(Long seckillId) {
        return seckillSkuMapper.selectList(Wrappers.<SmsSeckillSku>lambdaQuery()
                .eq(SmsSeckillSku::getSeckillId, seckillId)
                .orderByDesc(SmsSeckillSku::getCreateTime));
    }

    private Map<Long, List<AdminSeckillSkuVO>> loadSkuVOMap(Collection<Long> seckillIds) {
        if (CollUtil.isEmpty(seckillIds)) {
            return Collections.emptyMap();
        }
        List<SmsSeckillSku> seckillSkus = seckillSkuMapper.selectList(Wrappers.<SmsSeckillSku>lambdaQuery()
                .in(SmsSeckillSku::getSeckillId, seckillIds)
                .orderByDesc(SmsSeckillSku::getCreateTime));
        if (CollUtil.isEmpty(seckillSkus)) {
            return Collections.emptyMap();
        }

        Map<Long, PmsSpu> spuMap = loadSpuMap(seckillSkus);
        Map<Long, PmsSku> skuMap = loadProductSkuMap(seckillSkus);
        return seckillSkus.stream()
                .map(item -> AdminSeckillSkuVO.from(
                        item,
                        spuMap.get(item.getSpuId()),
                        skuMap.get(item.getSkuId()),
                        readRemainStock(item)))
                .collect(Collectors.groupingBy(AdminSeckillSkuVO::getSeckillId));
    }

    private Map<Long, PmsSpu> loadSpuMap(List<SmsSeckillSku> seckillSkus) {
        List<Long> spuIds = seckillSkus.stream().map(SmsSeckillSku::getSpuId).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(spuIds)) {
            return Collections.emptyMap();
        }
        return spuMapper.selectList(Wrappers.<PmsSpu>lambdaQuery().in(PmsSpu::getId, spuIds))
                .stream()
                .collect(Collectors.toMap(PmsSpu::getId, item -> item, (a, b) -> a));
    }

    private Map<Long, PmsSku> loadProductSkuMap(List<SmsSeckillSku> seckillSkus) {
        List<Long> skuIds = seckillSkus.stream().map(SmsSeckillSku::getSkuId).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(skuIds)) {
            return Collections.emptyMap();
        }
        return skuMapper.selectList(Wrappers.<PmsSku>lambdaQuery().in(PmsSku::getId, skuIds))
                .stream()
                .collect(Collectors.toMap(PmsSku::getId, item -> item, (a, b) -> a));
    }

    private Integer readRemainStock(SmsSeckillSku seckillSku) {
        String value = redis.opsForValue().get(STOCK_KEY_PREFIX + seckillSku.getId());
        if (StrUtil.isBlank(value)) {
            return Math.max(defaultInt(seckillSku.getSeckillStock()) - defaultInt(seckillSku.getSoldCount()), 0);
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return Math.max(defaultInt(seckillSku.getSeckillStock()) - defaultInt(seckillSku.getSoldCount()), 0);
        }
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
