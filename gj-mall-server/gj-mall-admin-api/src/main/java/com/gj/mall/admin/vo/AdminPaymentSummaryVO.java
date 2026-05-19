package com.gj.mall.admin.vo;

import com.gj.mall.order.enums.PayChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "后台支付汇总")
public class AdminPaymentSummaryVO {

    @Schema(description = "支付流水总数")
    private Long totalCount = 0L;

    @Schema(description = "支付流水总金额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Schema(description = "待支付笔数")
    private Long pendingCount = 0L;

    @Schema(description = "待支付金额")
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Schema(description = "已支付笔数")
    private Long paidCount = 0L;

    @Schema(description = "已支付金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Schema(description = "失败笔数")
    private Long failedCount = 0L;

    @Schema(description = "失败金额")
    private BigDecimal failedAmount = BigDecimal.ZERO;

    @Schema(description = "退款笔数")
    private Long refundedCount = 0L;

    @Schema(description = "退款金额")
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Schema(description = "今日实收金额")
    private BigDecimal todayPaidAmount = BigDecimal.ZERO;

    @Schema(description = "今日退款金额")
    private BigDecimal todayRefundedAmount = BigDecimal.ZERO;

    @Schema(description = "支付成功率")
    private BigDecimal successRate = BigDecimal.ZERO;

    @Schema(description = "退款率")
    private BigDecimal refundRate = BigDecimal.ZERO;

    @Schema(description = "渠道汇总")
    private List<ChannelItem> channels = new ArrayList<>();

    @Data
    @Schema(description = "支付渠道汇总")
    public static class ChannelItem {
        private Integer channel;
        private String channelDesc;
        private Long count = 0L;
        private BigDecimal amount = BigDecimal.ZERO;

        public static ChannelItem empty(PayChannel channel) {
            ChannelItem item = new ChannelItem();
            item.setChannel(channel.getCode());
            item.setChannelDesc(channel.getDesc());
            return item;
        }
    }
}
