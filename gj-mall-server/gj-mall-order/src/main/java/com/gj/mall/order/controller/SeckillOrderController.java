package com.gj.mall.order.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.marketing.entity.SmsSeckillSku;
import com.gj.mall.marketing.service.impl.SeckillServiceImpl;
import com.gj.mall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "18-秒杀下单", description = "秒杀抢购（Redis Lua 原子扣减）")
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillOrderController {

    private final SeckillServiceImpl seckillService;   // 直接用 impl 以访问 lockStock/releaseStock
    private final OrderService orderService;

    @Operation(summary = "秒杀下单")
    @PostMapping("/{seckillSkuId}/order")
    public Result<String> placeOrder(
            @PathVariable Long seckillSkuId,
            @RequestParam Long addressId) {
        Long userId = UserContext.getUserId();
        // 1. Redis Lua 原子抢购（超卖保护 + 限购）
        SmsSeckillSku sku = seckillService.lockStock(seckillSkuId, userId);
        try {
            // 2. 落订单
            String orderNo = orderService.createSeckillOrder(userId, sku, addressId);
            return Result.success(orderNo);
        } catch (Exception e) {
            // 3. 回滚 Redis 库存
            seckillService.releaseStock(seckillSkuId, userId);
            throw e;
        }
    }
}
