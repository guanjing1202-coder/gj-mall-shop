package com.gj.mall.order.service.impl;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.vo.AfterSaleEligibilityVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AfterSalePolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 12, 0);

    @Test
    void pendingDeliveryAllowsRefundOnlyButRejectsReturnRefund() {
        OmsOrder order = order(OrderStatus.PENDING_DELIVERY, null);

        AfterSaleEligibilityVO eligibility = AfterSalePolicy.evaluate(order, 1, 7, NOW);

        assertThat(eligibility.getAvailable()).isTrue();
        assertThat(eligibility.getUnavailableReason()).isNull();
        assertThatThrownBy(() -> AfterSalePolicy.validateApply(order, 2, 7, NOW))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("待发货订单仅支持仅退款");
    }

    @Test
    void completedOrderInsideWindowAllowsAfterSaleAndReportsDeadline() {
        LocalDateTime receiveTime = NOW.minusDays(6);
        OmsOrder order = order(OrderStatus.COMPLETED, receiveTime);

        AfterSaleEligibilityVO eligibility = AfterSalePolicy.evaluate(order, 2, 7, NOW);

        assertThat(eligibility.getAvailable()).isTrue();
        assertThat(eligibility.getWindowDays()).isEqualTo(7);
        assertThat(eligibility.getDeadline()).isEqualTo(receiveTime.plusDays(7));
    }

    @Test
    void completedOrderOutsideWindowRejectsAfterSale() {
        OmsOrder order = order(OrderStatus.COMPLETED, NOW.minusDays(8));

        AfterSaleEligibilityVO eligibility = AfterSalePolicy.evaluate(order, 2, 7, NOW);

        assertThat(eligibility.getAvailable()).isFalse();
        assertThat(eligibility.getUnavailableReason()).contains("售后申请已超过 7 天");
        assertThatThrownBy(() -> AfterSalePolicy.validateApply(order, 2, 7, NOW))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("售后申请已超过 7 天");
    }

    @Test
    void refundedOrderRejectsAfterSale() {
        OmsOrder order = order(OrderStatus.REFUNDED, NOW.minusDays(1));

        AfterSaleEligibilityVO eligibility = AfterSalePolicy.evaluate(order, 1, 7, NOW);

        assertThat(eligibility.getAvailable()).isFalse();
        assertThat(eligibility.getUnavailableReason()).contains("已退款");
    }

    private OmsOrder order(OrderStatus status, LocalDateTime receiveTime) {
        OmsOrder order = new OmsOrder();
        order.setStatus(status.getCode());
        order.setCreateTime(NOW.minusDays(10));
        order.setPayTime(NOW.minusDays(9));
        order.setUpdateTime(receiveTime == null ? NOW.minusDays(1) : receiveTime);
        order.setReceiveTime(receiveTime);
        return order;
    }
}
