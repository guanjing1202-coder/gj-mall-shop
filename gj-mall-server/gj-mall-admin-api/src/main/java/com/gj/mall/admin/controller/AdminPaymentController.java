package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.AdminPaymentQueryDTO;
import com.gj.mall.admin.dto.AdminPaymentRefundDTO;
import com.gj.mall.admin.service.AdminPaymentService;
import com.gj.mall.admin.vo.AdminPaymentVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台支付退款", description = "支付记录 / 退款管理")
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService paymentService;

    @Operation(summary = "支付记录分页")
    @GetMapping("/page")
    public Result<PageResult<AdminPaymentVO>> page(AdminPaymentQueryDTO query) {
        return Result.success(paymentService.page(query));
    }

    @Operation(summary = "支付记录详情")
    @GetMapping("/{id}")
    public Result<AdminPaymentVO> detail(@PathVariable Long id) {
        return Result.success(paymentService.detail(id));
    }

    @Operation(summary = "同步为已支付")
    @PutMapping("/{id}/paid")
    public Result<Void> markPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String thirdPayNo) {
        paymentService.markPaid(id, thirdPayNo);
        return Result.success();
    }

    @Operation(summary = "标记支付失败")
    @PutMapping("/{id}/failed")
    public Result<Void> markFailed(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        paymentService.markFailed(id, reason);
        return Result.success();
    }

    @Operation(summary = "全额退款")
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id, @RequestBody(required = false) AdminPaymentRefundDTO dto) {
        paymentService.refund(id, dto);
        return Result.success();
    }
}
