package com.gj.mall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.product.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final PmsSkuMapper mapper;

    @Override
    public PmsSku getByIdOrThrow(Long id) {
        PmsSku sku = mapper.selectById(id);
        if (sku == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "SKU 不存在");
        }
        return sku;
    }

    @Override
    public List<SkuVO> listBySpuId(Long spuId) {
        List<PmsSku> list = mapper.selectList(Wrappers.<PmsSku>lambdaQuery()
                .eq(PmsSku::getSpuId, spuId)
                .orderByAsc(PmsSku::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<PmsSku> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return mapper.selectBatchIds(ids);
    }

    @Override
    public boolean lockStock(Long skuId, int quantity) {
        if (quantity <= 0) return true;
        // UPDATE ... WHERE id=? AND stock >= ?
        int rows = mapper.update(null, Wrappers.<PmsSku>lambdaUpdate()
                .setSql("stock = stock - " + quantity)
                .setSql("locked_stock = IFNULL(locked_stock,0) + " + quantity)
                .eq(PmsSku::getId, skuId)
                .ge(PmsSku::getStock, quantity));
        return rows > 0;
    }

    @Override
    public boolean releaseStock(Long skuId, int quantity) {
        if (quantity <= 0) return true;
        int rows = mapper.update(null, Wrappers.<PmsSku>lambdaUpdate()
                .setSql("stock = stock + " + quantity)
                .setSql("locked_stock = GREATEST(IFNULL(locked_stock,0) - " + quantity + ", 0)")
                .eq(PmsSku::getId, skuId));
        return rows > 0;
    }

    @Override
    public boolean consumeStock(Long skuId, int quantity) {
        if (quantity <= 0) return true;
        int rows = mapper.update(null, Wrappers.<PmsSku>lambdaUpdate()
                .setSql("locked_stock = GREATEST(IFNULL(locked_stock,0) - " + quantity + ", 0)")
                .setSql("sale_count = IFNULL(sale_count,0) + " + quantity)
                .eq(PmsSku::getId, skuId));
        return rows > 0;
    }

    @Override
    public SkuVO toVO(PmsSku sku) {
        SkuVO vo = new SkuVO();
        BeanUtil.copyProperties(sku, vo, "specData");
        if (StrUtil.isNotBlank(sku.getSpecData())) {
            try {
                Map<String, String> spec = JSON.parseObject(sku.getSpecData(),
                        new TypeReference<Map<String, String>>() {});
                vo.setSpecData(spec);
            } catch (Exception ignored) {
            }
        }
        return vo;
    }
}
