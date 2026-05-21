package com.gj.mall.user.vo;

import com.gj.mall.user.entity.UmsUserMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserMessageVO {
    private Long id;
    private String type;
    private String typeDesc;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;

    public static UserMessageVO from(UmsUserMessage message) {
        UserMessageVO vo = new UserMessageVO();
        vo.setId(message.getId());
        vo.setType(message.getType());
        vo.setTypeDesc(typeDesc(message.getType()));
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setBizType(message.getBizType());
        vo.setBizId(message.getBizId());
        vo.setBizNo(message.getBizNo());
        vo.setReadStatus(message.getReadStatus());
        vo.setReadTime(message.getReadTime());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    public static String typeDesc(String type) {
        if ("order".equals(type)) return "订单通知";
        if ("logistics".equals(type)) return "物流通知";
        if ("after_sale".equals(type)) return "售后通知";
        if ("payment".equals(type)) return "支付通知";
        return "系统通知";
    }
}
