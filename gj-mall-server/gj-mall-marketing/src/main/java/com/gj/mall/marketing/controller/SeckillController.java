package com.gj.mall.marketing.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.marketing.service.SeckillService;
import com.gj.mall.marketing.vo.SeckillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "17-秒杀", description = "秒杀（C 端浏览）")
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @AuthExclude
    @Operation(summary = "当前进行中的秒杀活动列表")
    @GetMapping("/active")
    public Result<List<SeckillVO>> activeList() {
        return Result.success(seckillService.activeList());
    }

    @AuthExclude
    @Operation(summary = "秒杀活动详情（含 SKU 剩余库存）")
    @GetMapping("/{seckillId}")
    public Result<SeckillVO> detail(@PathVariable Long seckillId) {
        return Result.success(seckillService.detail(seckillId));
    }
}
