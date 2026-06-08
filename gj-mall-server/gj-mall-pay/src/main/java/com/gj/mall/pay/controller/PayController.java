package com.gj.mall.pay.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.framework.security.AuthExclude;
import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.service.PayService;
import com.gj.mall.pay.support.WechatPayCallbackParser;
import com.gj.mall.pay.vo.PayCallbackResultVO;
import com.gj.mall.pay.vo.PayChannelVO;
import com.gj.mall.pay.vo.PayResultVO;
import com.gj.mall.pay.vo.PayStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "15-支付", description = "支付（mock/wechat/alipay）")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;
    private final WechatPayCallbackParser wechatPayCallbackParser;

    @Operation(summary = "发起支付")
    @PostMapping
    public Result<PayResultVO> pay(@Valid @RequestBody PayDTO dto) {
        return Result.success(payService.pay(UserContext.getUserId(), dto));
    }

    @Operation(summary = "查询用户侧支付渠道")
    @GetMapping("/channels")
    public Result<List<PayChannelVO>> channels() {
        return Result.success(payService.listChannels());
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/{payNo}/status")
    public Result<PayStatusVO> status(@PathVariable String payNo) {
        return Result.success(payService.getStatus(UserContext.getUserId(), payNo));
    }

    @Operation(summary = "支付回调（开发用，模拟第三方异步通知）")
    @AuthExclude
    @PostMapping("/notify/{payNo}")
    public Result<Void> notifyPaid(@PathVariable String payNo,
                                   @RequestParam(required = false) String thirdPayNo) {
        payService.notifyPaid(payNo, thirdPayNo == null ? "DEV-CB-" + payNo : thirdPayNo, "manual-trigger");
        return Result.success();
    }

    @Operation(summary = "支付渠道回调统一入口")
    @AuthExclude
    @PostMapping("/callback/wechat")
    public Result<PayCallbackResultVO> wechatCallback(
            @RequestBody(required = false) String rawBody,
            @RequestHeader Map<String, String> headers) {
        Map<String, Object> payload = wechatPayCallbackParser.parse(rawBody, headers).toMap();
        return Result.success(payService.handleVerifiedCallback("wechat", payload, headers));
    }

    @Operation(summary = "支付渠道回调统一入口")
    @AuthExclude
    @PostMapping("/callback/{channel}")
    public Result<PayCallbackResultVO> callback(
            @PathVariable String channel,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam Map<String, String> params,
            @RequestHeader Map<String, String> headers) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (params != null) {
            payload.putAll(params);
        }
        if (body != null) {
            payload.putAll(body);
        }
        return Result.success(payService.handleCallback(channel, payload, headers));
    }
}
