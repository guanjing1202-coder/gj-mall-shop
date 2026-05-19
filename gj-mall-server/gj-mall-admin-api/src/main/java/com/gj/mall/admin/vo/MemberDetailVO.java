package com.gj.mall.admin.vo;

import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.vo.UserBrowseHistoryVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Schema(description = "会员详情")
public class MemberDetailVO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long addressCount;
    private Long orderCount;
    private BigDecimal paidAmount;
    private List<UmsUserAddress> addresses;
    private Long browseHistoryTotal;
    private List<UserBrowseHistoryVO> browseHistories;

    public static MemberDetailVO from(
            UmsUser user,
            Long addressCount,
            Long orderCount,
            BigDecimal paidAmount,
            List<UmsUserAddress> addresses) {
        return from(user, addressCount, orderCount, paidAmount, addresses, 0L, Collections.emptyList());
    }

    public static MemberDetailVO from(
            UmsUser user,
            Long addressCount,
            Long orderCount,
            BigDecimal paidAmount,
            List<UmsUserAddress> addresses,
            Long browseHistoryTotal,
            List<UserBrowseHistoryVO> browseHistories) {
        MemberDetailVO vo = new MemberDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setAddressCount(addressCount);
        vo.setOrderCount(orderCount);
        vo.setPaidAmount(paidAmount);
        vo.setAddresses(addresses);
        vo.setBrowseHistoryTotal(browseHistoryTotal);
        vo.setBrowseHistories(browseHistories);
        return vo;
    }
}
