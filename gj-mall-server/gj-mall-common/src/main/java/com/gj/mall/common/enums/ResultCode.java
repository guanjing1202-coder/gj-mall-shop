package com.gj.mall.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务返回码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 通用错误 1xxx
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "参数缺失"),
    DATA_NOT_FOUND(1003, "数据不存在"),
    DATA_EXISTS(1004, "数据已存在"),
    OPERATION_FORBIDDEN(1005, "操作被禁止"),

    // 用户/鉴权 2xxx
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2002, "无权限访问"),
    LOGIN_FAIL(2003, "用户名或密码错误"),
    USER_NOT_FOUND(2004, "用户不存在"),
    USER_DISABLED(2005, "用户已被禁用"),
    TOKEN_INVALID(2006, "Token 无效"),
    TOKEN_EXPIRED(2007, "Token 已过期"),
    CAPTCHA_ERROR(2008, "验证码错误"),

    // 商品 3xxx
    PRODUCT_NOT_FOUND(3001, "商品不存在"),
    PRODUCT_OFF_SHELF(3002, "商品已下架"),
    STOCK_NOT_ENOUGH(3003, "库存不足"),

    // 订单 4xxx
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态异常"),
    ORDER_PAID(4003, "订单已支付"),
    ORDER_CANCELED(4004, "订单已取消"),
    ORDER_EMPTY_ITEMS(4005, "下单商品为空"),
    ORDER_AMOUNT_ERROR(4006, "订单金额异常"),

    // 支付 5xxx
    PAY_FAIL(5001, "支付失败"),
    PAY_CHANNEL_NOT_SUPPORT(5002, "不支持的支付渠道"),
    PAY_RECORD_NOT_FOUND(5003, "支付记录不存在"),
    PAY_AMOUNT_MISMATCH(5004, "支付金额不一致"),

    // 营销 6xxx
    COUPON_NOT_FOUND(6001, "优惠券不存在"),
    COUPON_EXPIRED(6002, "优惠券已过期"),
    COUPON_USED(6003, "优惠券已使用"),
    COUPON_STOCK_OUT(6004, "优惠券已领完"),
    COUPON_ALREADY_RECEIVED(6005, "已领取过该优惠券"),
    COUPON_NOT_MATCH(6006, "订单金额不满足优惠券门槛"),
    SECKILL_NOT_FOUND(6101, "秒杀活动不存在"),
    SECKILL_NOT_ACTIVE(6102, "秒杀活动未开始或已结束"),
    SECKILL_STOCK_OUT(6103, "秒杀商品已售罄"),
    SECKILL_LIMIT_EXCEEDED(6104, "超过秒杀限购数量"),

    // 系统 9xxx
    SYSTEM_ERROR(9001, "系统异常"),
    THIRD_PARTY_ERROR(9002, "第三方服务异常"),
    RATE_LIMIT(9003, "请求过于频繁，请稍后再试");

    private final Integer code;
    private final String message;
}
