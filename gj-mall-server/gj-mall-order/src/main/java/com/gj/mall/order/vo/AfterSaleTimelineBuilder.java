package com.gj.mall.order.vo;

import cn.hutool.core.util.StrUtil;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.PayRefundRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AfterSaleTimelineBuilder {

    private AfterSaleTimelineBuilder() {
    }

    public static List<AfterSaleTimelineItemVO> build(OmsAfterSale afterSale, PayRefundRecord refundRecord) {
        List<AfterSaleTimelineItemVO> timeline = new ArrayList<>();
        timeline.add(item("提交申请", StrUtil.blankToDefault(afterSale.getReason(), "售后申请已提交"),
                afterSale.getCreateTime(), true, "default"));
        addAudit(timeline, afterSale);
        addReturn(timeline, afterSale);
        addRefund(timeline, afterSale, refundRecord);
        return timeline;
    }

    private static void addAudit(List<AfterSaleTimelineItemVO> timeline, OmsAfterSale afterSale) {
        Integer status = afterSale.getStatus();
        if (Integer.valueOf(3).equals(status)) {
            timeline.add(item("商家审核", "审核未通过：" + StrUtil.blankToDefault(afterSale.getRejectReason(), "-"),
                    afterSale.getAuditTime(), true, "danger"));
            return;
        }
        if (Integer.valueOf(5).equals(status)) {
            timeline.add(item("售后关闭", "售后申请已取消", afterSale.getUpdateTime(), true, "muted"));
            return;
        }
        if (status != null && status >= 1) {
            timeline.add(item("商家审核", StrUtil.blankToDefault(afterSale.getAuditRemark(), "审核已通过"),
                    afterSale.getAuditTime(), true, "success"));
        } else {
            timeline.add(item("商家审核", "商家会尽快审核申请", null, false, "default"));
        }
    }

    private static void addReturn(List<AfterSaleTimelineItemVO> timeline, OmsAfterSale afterSale) {
        if (!Integer.valueOf(2).equals(afterSale.getType())) {
            return;
        }
        boolean hasLogistics = StrUtil.isNotBlank(afterSale.getReturnCompany()) || StrUtil.isNotBlank(afterSale.getReturnNo());
        if (hasLogistics) {
            timeline.add(item("退货物流",
                    StrUtil.blankToDefault(afterSale.getReturnCompany(), "-") + " "
                            + StrUtil.blankToDefault(afterSale.getReturnNo(), ""),
                    afterSale.getReceiveTime(), true, "success"));
            return;
        }
        if (Integer.valueOf(1).equals(afterSale.getStatus())) {
            timeline.add(item("退货物流", "请寄回商品并填写物流", null, false, "default"));
        }
    }

    private static void addRefund(List<AfterSaleTimelineItemVO> timeline, OmsAfterSale afterSale, PayRefundRecord refundRecord) {
        if (refundRecord != null) {
            addRefundRetry(timeline, refundRecord);
            if (Integer.valueOf(1).equals(refundRecord.getStatus())) {
                timeline.add(item("退款处理", "退款成功：" + refundRecord.getRefundNo(),
                        firstNonNull(refundRecord.getSuccessTime(), afterSale.getRefundTime(), refundRecord.getCreateTime()),
                        true, "success"));
                return;
            }
            if (Integer.valueOf(2).equals(refundRecord.getStatus())) {
                timeline.add(item("退款失败", StrUtil.blankToDefault(refundRecord.getReason(), "退款失败，请等待商家处理"),
                        refundRecord.getUpdateTime(), true, "danger"));
                return;
            }
            timeline.add(item("退款中", "退款流水已生成：" + refundRecord.getRefundNo(),
                    refundRecord.getCreateTime(), true, "warning"));
            return;
        }
        if (Integer.valueOf(2).equals(afterSale.getStatus())) {
            timeline.add(item("退款处理", "商家正在处理退款", null, false, "default"));
        }
    }

    private static void addRefundRetry(List<AfterSaleTimelineItemVO> timeline, PayRefundRecord refundRecord) {
        String callbackData = refundRecord.getCallbackData();
        if (StrUtil.isBlank(callbackData)) {
            return;
        }
        if (callbackData.contains("refund failed") && callbackData.contains("admin-retry-refund success")) {
            timeline.add(item("退款失败后重试", "商家已重新发起退款，等待重试结果",
                    firstNonNull(refundRecord.getUpdateTime(), refundRecord.getCreateTime()), true, "warning"));
        }
    }

    private static LocalDateTime firstNonNull(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static AfterSaleTimelineItemVO item(String title, String description, LocalDateTime time, boolean active, String tone) {
        return new AfterSaleTimelineItemVO(title, description, time, active, tone);
    }
}
