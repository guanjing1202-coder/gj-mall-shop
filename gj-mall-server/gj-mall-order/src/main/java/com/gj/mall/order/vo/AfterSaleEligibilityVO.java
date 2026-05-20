package com.gj.mall.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSaleEligibilityVO {
    private Boolean available;
    private String unavailableReason;
    private Integer requestedType;
    private Boolean refundOnlyAllowed;
    private Boolean returnRefundAllowed;
    private Integer windowDays;
    private LocalDateTime startTime;
    private LocalDateTime deadline;

    public static AfterSaleEligibilityVO available(Integer requestedType, boolean refundOnlyAllowed,
                                                   boolean returnRefundAllowed, Integer windowDays,
                                                   LocalDateTime startTime, LocalDateTime deadline) {
        AfterSaleEligibilityVO vo = new AfterSaleEligibilityVO();
        vo.setAvailable(true);
        vo.setRequestedType(requestedType);
        vo.setRefundOnlyAllowed(refundOnlyAllowed);
        vo.setReturnRefundAllowed(returnRefundAllowed);
        vo.setWindowDays(windowDays);
        vo.setStartTime(startTime);
        vo.setDeadline(deadline);
        return vo;
    }

    public static AfterSaleEligibilityVO unavailable(Integer requestedType, String reason,
                                                     boolean refundOnlyAllowed, boolean returnRefundAllowed,
                                                     Integer windowDays, LocalDateTime startTime,
                                                     LocalDateTime deadline) {
        AfterSaleEligibilityVO vo = available(requestedType, refundOnlyAllowed, returnRefundAllowed, windowDays, startTime, deadline);
        vo.setAvailable(false);
        vo.setUnavailableReason(reason);
        return vo;
    }
}
