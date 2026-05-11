package com.gj.mall.admin.vo;

import com.gj.mall.marketing.entity.SmsCoupon;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "后台优惠券详情")
public class AdminCouponVO {
    private Long id;
    private String name;
    private Integer type;
    private String typeDesc;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal minAmount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer remainCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusDesc;
    private String validityStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminCouponVO from(SmsCoupon coupon) {
        AdminCouponVO vo = new AdminCouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setTypeDesc(typeDesc(coupon.getType()));
        vo.setDiscountAmount(coupon.getDiscountAmount());
        vo.setDiscountRate(coupon.getDiscountRate());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setTotalCount(defaultInt(coupon.getTotalCount()));
        vo.setReceivedCount(defaultInt(coupon.getReceivedCount()));
        vo.setUsedCount(defaultInt(coupon.getUsedCount()));
        vo.setRemainCount(Math.max(vo.getTotalCount() - vo.getReceivedCount(), 0));
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        vo.setStatus(coupon.getStatus());
        vo.setStatusDesc(statusDesc(coupon.getStatus()));
        vo.setValidityStatus(validityStatus(coupon));
        vo.setCreateTime(coupon.getCreateTime());
        vo.setUpdateTime(coupon.getUpdateTime());
        return vo;
    }

    private static Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String typeDesc(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case 1:
                return "满减券";
            case 2:
                return "折扣券";
            case 3:
                return "新人券";
            default:
                return "未知";
        }
    }

    private static String statusDesc(Integer status) {
        return Integer.valueOf(1).equals(status) ? "开启" : "关闭";
    }

    private static String validityStatus(SmsCoupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            return "未开始";
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            return "已结束";
        }
        return "进行中";
    }
}
