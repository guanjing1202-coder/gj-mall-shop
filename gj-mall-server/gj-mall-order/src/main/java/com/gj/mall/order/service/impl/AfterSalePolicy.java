package com.gj.mall.order.service.impl;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.vo.AfterSaleEligibilityVO;

import java.time.LocalDateTime;

public final class AfterSalePolicy {

    public static final int TYPE_REFUND_ONLY = 1;
    public static final int TYPE_RETURN_REFUND = 2;
    public static final int DEFAULT_WINDOW_DAYS = 7;

    private AfterSalePolicy() {
    }

    public static AfterSaleEligibilityVO evaluate(OmsOrder order, Integer requestedType, int windowDays, LocalDateTime now) {
        if (order == null) {
            return unavailable(requestedType, "订单不存在", false, false, windowDays, null, null);
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null) {
            return unavailable(requestedType, "订单状态异常，暂不能申请售后", false, false, windowDays, null, null);
        }
        if (OrderStatus.PENDING_PAY.equals(status)) {
            return unavailable(requestedType, "待付款订单暂不能申请售后", false, false, windowDays, null, null);
        }
        if (OrderStatus.CANCELED.equals(status)) {
            return unavailable(requestedType, "已取消订单不能申请售后", false, false, windowDays, null, null);
        }
        if (OrderStatus.REFUNDING.equals(status)) {
            return unavailable(requestedType, "订单售后处理中，请勿重复申请", false, false, windowDays, null, null);
        }
        if (OrderStatus.REFUNDED.equals(status)) {
            return unavailable(requestedType, "订单已退款，不能再次申请售后", false, false, windowDays, null, null);
        }

        boolean refundOnlyAllowed = true;
        boolean returnRefundAllowed = !OrderStatus.PENDING_DELIVERY.equals(status);
        if (Integer.valueOf(TYPE_RETURN_REFUND).equals(requestedType) && !returnRefundAllowed) {
            return unavailable(requestedType, "待发货订单仅支持仅退款", refundOnlyAllowed, false, windowDays, null, null);
        }

        LocalDateTime startTime = afterSaleStartTime(order, status);
        LocalDateTime deadline = afterSaleDeadline(order, status, windowDays);
        if (deadline != null && now != null && now.isAfter(deadline)) {
            return unavailable(
                    requestedType,
                    "售后申请已超过 " + positiveWindowDays(windowDays) + " 天，如需帮助请联系商家客服",
                    refundOnlyAllowed,
                    returnRefundAllowed,
                    positiveWindowDays(windowDays),
                    startTime,
                    deadline);
        }

        return AfterSaleEligibilityVO.available(
                requestedType,
                refundOnlyAllowed,
                returnRefundAllowed,
                positiveWindowDays(windowDays),
                startTime,
                deadline);
    }

    public static void validateApply(OmsOrder order, Integer requestedType, int windowDays, LocalDateTime now) {
        AfterSaleEligibilityVO eligibility = evaluate(order, requestedType, windowDays, now);
        if (!Boolean.TRUE.equals(eligibility.getAvailable())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, eligibility.getUnavailableReason());
        }
    }

    private static AfterSaleEligibilityVO unavailable(Integer requestedType, String reason,
                                                     boolean refundOnlyAllowed, boolean returnRefundAllowed,
                                                     int windowDays, LocalDateTime startTime,
                                                     LocalDateTime deadline) {
        return AfterSaleEligibilityVO.unavailable(
                requestedType,
                reason,
                refundOnlyAllowed,
                returnRefundAllowed,
                positiveWindowDays(windowDays),
                startTime,
                deadline);
    }

    private static LocalDateTime afterSaleStartTime(OmsOrder order, OrderStatus status) {
        if (!OrderStatus.COMPLETED.equals(status)) {
            return null;
        }
        if (order.getReceiveTime() != null) {
            return order.getReceiveTime();
        }
        if (order.getUpdateTime() != null) {
            return order.getUpdateTime();
        }
        return order.getCreateTime();
    }

    private static LocalDateTime afterSaleDeadline(OmsOrder order, OrderStatus status, int windowDays) {
        LocalDateTime startTime = afterSaleStartTime(order, status);
        return startTime == null ? null : startTime.plusDays(positiveWindowDays(windowDays));
    }

    private static int positiveWindowDays(int windowDays) {
        return windowDays <= 0 ? DEFAULT_WINDOW_DAYS : windowDays;
    }
}
