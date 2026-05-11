package com.gj.mall.marketing.service;

import com.gj.mall.marketing.dto.SeckillCreateDTO;
import com.gj.mall.marketing.dto.SeckillSkuAddDTO;
import com.gj.mall.marketing.vo.SeckillVO;

import java.util.List;

public interface SeckillService {

    // -------- C 端 --------
    /** 当前进行中（status=1，时间内）的活动列表 */
    List<SeckillVO> activeList();

    /** 活动详情（含 SKU 剩余库存） */
    SeckillVO detail(Long seckillId);

    /**
     * 秒杀下单：Redis Lua 原子扣减库存 → 写入订单
     * @return orderNo
     */
    String placeOrder(Long userId, Long seckillSkuId, Long addressId);

    // -------- Admin 端 --------
    Long adminCreate(SeckillCreateDTO dto);

    void adminAddSku(SeckillSkuAddDTO dto);

    void adminSetStatus(Long seckillId, Integer status);

    /** 活动上线时：将 seckill_stock 预热到 Redis */
    void warmUpStock(Long seckillId);
}
