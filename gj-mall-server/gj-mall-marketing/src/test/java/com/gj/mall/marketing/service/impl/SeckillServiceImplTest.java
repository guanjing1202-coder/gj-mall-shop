package com.gj.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.marketing.entity.SmsSeckill;
import com.gj.mall.marketing.entity.SmsSeckillSku;
import com.gj.mall.marketing.mapper.SmsSeckillMapper;
import com.gj.mall.marketing.mapper.SmsSeckillSkuMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillServiceImplTest {

    @Test
    void lockStockRejectsInactiveSeckillBeforeTouchingRedis() {
        SmsSeckillMapper seckillMapper = mock(SmsSeckillMapper.class);
        SmsSeckillSkuMapper seckillSkuMapper = mock(SmsSeckillSkuMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SeckillServiceImpl service = service(seckillMapper, seckillSkuMapper, redis);
        when(seckillSkuMapper.selectById(10L)).thenReturn(seckillSku(10L, 20L, 5, 1));
        SmsSeckill seckill = seckill(20L);
        seckill.setStatus(0);
        when(seckillMapper.selectById(20L)).thenReturn(seckill);

        assertThatThrownBy(() -> service.lockStock(10L, 1L))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getCode(), Throwable::getMessage)
                .containsExactly(ResultCode.SECKILL_NOT_ACTIVE.getCode(), "活动未上线");
    }

    @Test
    void lockStockMapsLuaLimitAndStockResultsToBusinessErrors() {
        SmsSeckillMapper seckillMapper = mock(SmsSeckillMapper.class);
        SmsSeckillSkuMapper seckillSkuMapper = mock(SmsSeckillSkuMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SeckillServiceImpl service = service(seckillMapper, seckillSkuMapper, redis);
        when(seckillSkuMapper.selectById(10L)).thenReturn(seckillSku(10L, 20L, 5, 1));
        when(seckillMapper.selectById(20L)).thenReturn(seckill(20L));
        when(redis.execute(any(RedisScript.class), eq(Arrays.asList("mall:seckill:stock:10", "mall:seckill:ordered:20:1")),
                eq("1"), eq("1"), any())).thenReturn(-1L);

        assertThatThrownBy(() -> service.lockStock(10L, 1L))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getCode())
                .isEqualTo(ResultCode.SECKILL_LIMIT_EXCEEDED.getCode());

        when(redis.execute(any(RedisScript.class), eq(Arrays.asList("mall:seckill:stock:10", "mall:seckill:ordered:20:1")),
                eq("1"), eq("1"), any())).thenReturn(-2L);

        assertThatThrownBy(() -> service.lockStock(10L, 1L))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getCode())
                .isEqualTo(ResultCode.SECKILL_STOCK_OUT.getCode());
    }

    @Test
    void lockStockReturnsSnapshotWhenLuaDeductsStock() {
        SmsSeckillMapper seckillMapper = mock(SmsSeckillMapper.class);
        SmsSeckillSkuMapper seckillSkuMapper = mock(SmsSeckillSkuMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SeckillServiceImpl service = service(seckillMapper, seckillSkuMapper, redis);
        SmsSeckillSku sku = seckillSku(10L, 20L, 5, 1);
        when(seckillSkuMapper.selectById(10L)).thenReturn(sku);
        when(seckillMapper.selectById(20L)).thenReturn(seckill(20L));
        when(redis.execute(any(RedisScript.class), eq(Arrays.asList("mall:seckill:stock:10", "mall:seckill:ordered:20:1")),
                eq("1"), eq("1"), any())).thenReturn(4L);

        SmsSeckillSku locked = service.lockStock(10L, 1L);

        assertThat(locked).isSameAs(sku);
    }

    @Test
    void releaseStockUsesAtomicScriptSoOrderFlagCannotGoNegative() {
        SmsSeckillMapper seckillMapper = mock(SmsSeckillMapper.class);
        SmsSeckillSkuMapper seckillSkuMapper = mock(SmsSeckillSkuMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SeckillServiceImpl service = service(seckillMapper, seckillSkuMapper, redis);
        when(seckillSkuMapper.selectById(10L)).thenReturn(seckillSku(10L, 20L, 5, 1));
        when(redis.execute(any(RedisScript.class), eq(Arrays.asList("mall:seckill:stock:10", "mall:seckill:ordered:20:1")),
                eq("1"))).thenReturn(4L);

        service.releaseStock(10L, 1L);

        verify(redis).execute(any(RedisScript.class), eq(Arrays.asList("mall:seckill:stock:10", "mall:seckill:ordered:20:1")), eq("1"));
    }

    @Test
    void warmUpStockStoresRemainingStockWithActivityTtl() {
        SmsSeckillMapper seckillMapper = mock(SmsSeckillMapper.class);
        SmsSeckillSkuMapper seckillSkuMapper = mock(SmsSeckillSkuMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SeckillServiceImpl service = service(seckillMapper, seckillSkuMapper, redis);
        SmsSeckill seckill = seckill(20L);
        seckill.setEndTime(LocalDateTime.now().plusHours(2));
        when(seckillMapper.selectById(20L)).thenReturn(seckill);
        when(seckillSkuMapper.selectList(any())).thenReturn(Collections.singletonList(seckillSku(10L, 20L, 5, 1, 2)));
        when(redis.opsForValue()).thenReturn(valueOperations);

        service.warmUpStock(20L);

        verify(valueOperations).set(eq("mall:seckill:stock:10"), eq("3"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void onPaidIncrementsSoldCountByQuantity() {
        SmsSeckillSkuMapper seckillSkuMapper = mock(SmsSeckillSkuMapper.class);
        SeckillServiceImpl service = service(mock(SmsSeckillMapper.class), seckillSkuMapper, mock(StringRedisTemplate.class));

        service.onPaid(10L, 2);

        verify(seckillSkuMapper).update(eq(null), any(Wrapper.class));
    }

    private SeckillServiceImpl service(SmsSeckillMapper seckillMapper, SmsSeckillSkuMapper seckillSkuMapper, StringRedisTemplate redis) {
        return new SeckillServiceImpl(seckillMapper, seckillSkuMapper, mock(PmsSkuMapper.class),
                mock(PmsSpuMapper.class), redis, mock(RedissonClient.class));
    }

    private SmsSeckill seckill(Long id) {
        SmsSeckill seckill = new SmsSeckill();
        seckill.setId(id);
        seckill.setName("限时秒杀");
        seckill.setStatus(1);
        seckill.setStartTime(LocalDateTime.now().minusMinutes(5));
        seckill.setEndTime(LocalDateTime.now().plusMinutes(30));
        return seckill;
    }

    private SmsSeckillSku seckillSku(Long id, Long seckillId, Integer stock, Integer limit) {
        return seckillSku(id, seckillId, stock, limit, 0);
    }

    private SmsSeckillSku seckillSku(Long id, Long seckillId, Integer stock, Integer limit, Integer soldCount) {
        SmsSeckillSku sku = new SmsSeckillSku();
        sku.setId(id);
        sku.setSeckillId(seckillId);
        sku.setSpuId(100L);
        sku.setSkuId(200L);
        sku.setSeckillStock(stock);
        sku.setSeckillLimit(limit);
        sku.setSoldCount(soldCount);
        return sku;
    }
}
