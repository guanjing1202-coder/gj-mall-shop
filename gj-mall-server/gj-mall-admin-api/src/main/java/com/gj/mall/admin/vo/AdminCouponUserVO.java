package com.gj.mall.admin.vo;

import com.gj.mall.marketing.entity.SmsCouponUser;
import com.gj.mall.user.entity.UmsUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台优惠券领取记录")
public class AdminCouponUserVO {
    private Long id;
    private Long couponId;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    private String statusDesc;
    private LocalDateTime usedAt;
    private Long orderId;
    private LocalDateTime createTime;

    public static AdminCouponUserVO from(SmsCouponUser couponUser, UmsUser user) {
        AdminCouponUserVO vo = new AdminCouponUserVO();
        vo.setId(couponUser.getId());
        vo.setCouponId(couponUser.getCouponId());
        vo.setUserId(couponUser.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
        }
        vo.setStatus(couponUser.getStatus());
        vo.setStatusDesc(statusDesc(couponUser.getStatus()));
        vo.setUsedAt(couponUser.getUsedAt());
        vo.setOrderId(couponUser.getOrderId());
        vo.setCreateTime(couponUser.getCreateTime());
        return vo;
    }

    private static String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "未使用";
            case 1:
                return "已使用";
            case 2:
                return "已过期";
            default:
                return "未知";
        }
    }
}
