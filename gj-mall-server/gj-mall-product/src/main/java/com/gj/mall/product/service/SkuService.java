package com.gj.mall.product.service;

import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.vo.SkuVO;

import java.util.Collection;
import java.util.List;

public interface SkuService {

    PmsSku getByIdOrThrow(Long id);

    List<SkuVO> listBySpuId(Long spuId);

    List<PmsSku> listByIds(Collection<Long> ids);

    /** SKU VO 转换（含规格反序列化） */
    SkuVO toVO(PmsSku sku);

    /**
     * 预扣库存（下单）：stock -= qty，locked_stock += qty。
     * 失败抛 STOCK_NOT_ENOUGH。
     */
    boolean lockStock(Long skuId, int quantity);

    /**
     * 释放库存（取消/超时）：stock += qty，locked_stock -= qty。
     */
    boolean releaseStock(Long skuId, int quantity);

    /**
     * 真正扣减（支付成功）：locked_stock -= qty，sale_count += qty。
     */
    boolean consumeStock(Long skuId, int quantity);
}
