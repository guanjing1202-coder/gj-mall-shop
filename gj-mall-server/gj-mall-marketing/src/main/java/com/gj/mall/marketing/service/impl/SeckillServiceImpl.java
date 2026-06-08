package com.gj.mall.marketing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.marketing.dto.SeckillCreateDTO;
import com.gj.mall.marketing.dto.SeckillSkuAddDTO;
import com.gj.mall.marketing.entity.SmsSeckill;
import com.gj.mall.marketing.entity.SmsSeckillSku;
import com.gj.mall.marketing.mapper.SmsSeckillMapper;
import com.gj.mall.marketing.mapper.SmsSeckillSkuMapper;
import com.gj.mall.marketing.service.SeckillService;
import com.gj.mall.marketing.vo.SeckillSkuVO;
import com.gj.mall.marketing.vo.SeckillVO;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    /** Redis key: mall:seckill:stock:{seckillSkuId} */
    private static final String STOCK_KEY_PREFIX = "mall:seckill:stock:";
    /** Redis key: mall:seckill:order:{seckillId}:{userId} — 防重购  */
    private static final String ORDER_FLAG_PREFIX = "mall:seckill:ordered:";
    private static final String LOCK_PREFIX       = "mall:seckill:lock:";

    /**
     * Lua 脚本：原子检测限购 + 扣减库存
     * KEYS[1] = stock key
     * KEYS[2] = order flag key (seckillId + userId)
     * ARGV[1] = 扣减数量 (qty)
     * ARGV[2] = 限购上限 (limit)
     * ARGV[3] = flag TTL 秒（活动结束后自动过期）
     * 返回: >=0 剩余库存  -1 已超限购  -2 库存不足
     */
    private static final String SECKILL_LUA =
            "local ordered = redis.call('get', KEYS[2])\n" +
            "local already = ordered and tonumber(ordered) or 0\n" +
            "local qty     = tonumber(ARGV[1])\n" +
            "local lim     = tonumber(ARGV[2])\n" +
            "if already + qty > lim then return -1 end\n" +
            "local stock = tonumber(redis.call('get', KEYS[1]) or '0')\n" +
            "if stock < qty then return -2 end\n" +
            "redis.call('decrby', KEYS[1], qty)\n" +
            "redis.call('incrby', KEYS[2], qty)\n" +
            "redis.call('expire', KEYS[2], tonumber(ARGV[3]))\n" +
            "return stock - qty\n";

    /**
     * 释放秒杀库存：只有已锁定数量大于 0 时才回补库存，避免重复释放把购买标记扣成负数。
     * KEYS[1] = stock key
     * KEYS[2] = order flag key
     * ARGV[1] = 释放数量
     * 返回释放后的已锁定数量；-1 表示没有可释放的锁定数量
     */
    private static final String RELEASE_LUA =
            "local ordered = tonumber(redis.call('get', KEYS[2]) or '0')\n" +
            "local qty = tonumber(ARGV[1])\n" +
            "if ordered <= 0 then return -1 end\n" +
            "local release = qty\n" +
            "if ordered < qty then release = ordered end\n" +
            "redis.call('incrby', KEYS[1], release)\n" +
            "local remain = redis.call('decrby', KEYS[2], release)\n" +
            "if remain <= 0 then redis.call('del', KEYS[2]) end\n" +
            "return remain\n";

    private final SmsSeckillMapper seckillMapper;
    private final SmsSeckillSkuMapper seckillSkuMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsSpuMapper spuMapper;
    private final StringRedisTemplate redis;
    private final RedissonClient redissonClient;

    // ============================== C 端 ==============================

    @Override
    public List<SeckillVO> activeList() {
        LocalDateTime now = LocalDateTime.now();
        List<SmsSeckill> list = seckillMapper.selectList(
                Wrappers.<SmsSeckill>lambdaQuery()
                        .eq(SmsSeckill::getStatus, 1)
                        .le(SmsSeckill::getStartTime, now)
                        .ge(SmsSeckill::getEndTime, now)
                        .orderByAsc(SmsSeckill::getEndTime));
        return list.stream().map(s -> toVO(s, false)).collect(Collectors.toList());
    }

    @Override
    public SeckillVO detail(Long seckillId) {
        // MyBatis-Plus @TableLogic 已过滤 deleted=1，只需判 null
        SmsSeckill s = seckillMapper.selectById(seckillId);
        if (s == null) {
            throw new BizException(ResultCode.SECKILL_NOT_FOUND);
        }
        return toVO(s, true);
    }

    /**
     * 秒杀抢购核心：Redis Lua 原子扣减，成功返回 seckillSku 快照供 OrderService 使用。
     * placeOrder 不直接创建订单（避免 marketing → order 循环依赖），由调用方组合。
     */
    @Override
    public String placeOrder(Long userId, Long seckillSkuId, Long addressId) {
        throw new UnsupportedOperationException("Use SeckillOrderController in gj-mall-order instead");
    }

    /**
     * 对外暴露：Lua 原子抢购，成功返回 seckillSku 快照
     */
    public SmsSeckillSku lockStock(Long seckillSkuId, Long userId) {
        SmsSeckillSku sku = seckillSkuMapper.selectById(seckillSkuId);
        if (sku == null) throw new BizException(ResultCode.SECKILL_NOT_FOUND);

        // 验证活动状态和时间
        SmsSeckill seckill = seckillMapper.selectById(sku.getSeckillId());
        validateActive(seckill);

        String stockKey     = STOCK_KEY_PREFIX + seckillSkuId;
        String orderFlagKey = ORDER_FLAG_PREFIX + sku.getSeckillId() + ":" + userId;
        long ttlSeconds     = computeTtl(seckill.getEndTime());

        RedisScript<Long> script = RedisScript.of(SECKILL_LUA, Long.class);
        Long result = redis.execute(script,
                Arrays.asList(stockKey, orderFlagKey),
                "1", String.valueOf(sku.getSeckillLimit()), String.valueOf(ttlSeconds));

        if (result == null || result == -1L) {
            throw new BizException(ResultCode.SECKILL_LIMIT_EXCEEDED);
        }
        if (result == -2L) {
            throw new BizException(ResultCode.SECKILL_STOCK_OUT);
        }
        log.info("[seckill] locked stock seckillSkuId={} userId={} remain={}", seckillSkuId, userId, result);
        return sku;
    }

    /** 秒杀库存回滚（下单失败时调用） */
    public void releaseStock(Long seckillSkuId, Long userId) {
        SmsSeckillSku sku = seckillSkuMapper.selectById(seckillSkuId);
        if (sku == null) return;
        String orderFlagKey = ORDER_FLAG_PREFIX + sku.getSeckillId() + ":" + userId;
        RedisScript<Long> script = RedisScript.of(RELEASE_LUA, Long.class);
        Long result = redis.execute(script,
                Arrays.asList(STOCK_KEY_PREFIX + seckillSkuId, orderFlagKey),
                "1");
        if (result != null && result >= 0L) {
            log.info("[seckill] released stock seckillSkuId={} userId={} lockedRemain={}", seckillSkuId, userId, result);
        }
    }

    /** 支付成功后更新 DB sold_count */
    @Transactional
    public void onPaid(Long seckillSkuId, int qty) {
        seckillSkuMapper.update(null, Wrappers.<SmsSeckillSku>lambdaUpdate()
                .setSql("sold_count = sold_count + " + qty)
                .eq(SmsSeckillSku::getId, seckillSkuId));
    }

    // ============================== Admin 端 ==============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(SeckillCreateDTO dto) {
        SmsSeckill s = new SmsSeckill();
        BeanUtil.copyProperties(dto, s);
        s.setStatus(0);
        seckillMapper.insert(s);
        return s.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAddSku(SeckillSkuAddDTO dto) {
        SmsSeckillSku sku = new SmsSeckillSku();
        BeanUtil.copyProperties(dto, sku);
        sku.setSoldCount(0);
        seckillSkuMapper.insert(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSetStatus(Long seckillId, Integer status) {
        SmsSeckill upd = new SmsSeckill();
        upd.setId(seckillId);
        upd.setStatus(status);
        seckillMapper.updateById(upd);
        if (Integer.valueOf(1).equals(status)) {
            warmUpStock(seckillId);
        }
    }

    @Override
    public void warmUpStock(Long seckillId) {
        List<SmsSeckillSku> skus = seckillSkuMapper.selectList(
                Wrappers.<SmsSeckillSku>lambdaQuery().eq(SmsSeckillSku::getSeckillId, seckillId));
        SmsSeckill seckill = seckillMapper.selectById(seckillId);
        long ttl = seckill != null ? computeTtl(seckill.getEndTime()) : 3600L * 24;
        for (SmsSeckillSku sku : skus) {
            String key = STOCK_KEY_PREFIX + sku.getId();
            int stock = Math.max(
                    (sku.getSeckillStock() == null ? 0 : sku.getSeckillStock())
                            - (sku.getSoldCount() == null ? 0 : sku.getSoldCount()),
                    0);
            redis.opsForValue().set(key, String.valueOf(stock), ttl, TimeUnit.SECONDS);
            log.info("[seckill] warm up stock key={} stock={}", key, stock);
        }
    }

    // ============================== 私有 ==============================

    private void validateActive(SmsSeckill s) {
        if (s == null || !Integer.valueOf(1).equals(s.getStatus())) {
            throw new BizException(ResultCode.SECKILL_NOT_ACTIVE, "活动未上线");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(s.getStartTime()) || now.isAfter(s.getEndTime())) {
            throw new BizException(ResultCode.SECKILL_NOT_ACTIVE, "活动未在有效期内");
        }
    }

    private long computeTtl(LocalDateTime endTime) {
        if (endTime == null) return 3600L;
        java.time.Duration dur = java.time.Duration.between(LocalDateTime.now(), endTime);
        return Math.max(dur.getSeconds(), 60L);
    }

    private SeckillVO toVO(SmsSeckill s, boolean withSkus) {
        SeckillVO vo = new SeckillVO();
        BeanUtil.copyProperties(s, vo);
        vo.setStatusDesc(seckillStatusDesc(s.getStatus()));
        if (withSkus) {
            List<SmsSeckillSku> skus = seckillSkuMapper.selectList(
                    Wrappers.<SmsSeckillSku>lambdaQuery().eq(SmsSeckillSku::getSeckillId, s.getId()));
            vo.setSkus(skus.stream().map(this::toSkuVO).collect(Collectors.toList()));
        }
        return vo;
    }

    private SeckillSkuVO toSkuVO(SmsSeckillSku sk) {
        SeckillSkuVO vo = new SeckillSkuVO();
        BeanUtil.copyProperties(sk, vo);
        // 剩余库存从 Redis 读
        String stockStr = redis.opsForValue().get(STOCK_KEY_PREFIX + sk.getId());
        vo.setRemainStock(stockStr != null ? Integer.parseInt(stockStr) : sk.getSeckillStock());
        // 补充 SPU / SKU 信息
        PmsSku sku = skuMapper.selectById(sk.getSkuId());
        if (sku != null) {
            vo.setSkuName(sku.getName());
            vo.setImage(sku.getImage());
            vo.setOriginalPrice(sku.getPrice());
            if (sku.getSpecData() != null) {
                try {
                    vo.setSpecData(JSON.parseObject(sku.getSpecData(),
                            new TypeReference<Map<String, String>>() {}));
                } catch (Exception ignored) {}
            }
        }
        PmsSpu spu = spuMapper.selectById(sk.getSpuId());
        if (spu != null) vo.setSpuName(spu.getName());
        return vo;
    }

    private String seckillStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "草稿";
            case 1: return "进行中";
            case 2: return "已结束";
            default: return "未知";
        }
    }
}
