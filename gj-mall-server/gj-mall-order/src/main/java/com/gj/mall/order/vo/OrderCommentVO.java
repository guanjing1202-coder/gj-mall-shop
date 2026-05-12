package com.gj.mall.order.vo;

import com.alibaba.fastjson2.JSON;
import com.gj.mall.product.entity.PmsProductComment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class OrderCommentVO {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long spuId;
    private Long skuId;
    private Integer score;
    private String content;
    private List<String> images;
    private Integer status;
    private String statusDesc;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;

    public static OrderCommentVO from(PmsProductComment comment) {
        OrderCommentVO vo = new OrderCommentVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setOrderNo(comment.getOrderNo());
        vo.setOrderItemId(comment.getOrderItemId());
        vo.setSpuId(comment.getSpuId());
        vo.setSkuId(comment.getSkuId());
        vo.setScore(comment.getScore());
        vo.setContent(comment.getContent());
        vo.setImages(parseImages(comment.getImages()));
        vo.setStatus(comment.getStatus());
        vo.setStatusDesc(statusDesc(comment.getStatus()));
        vo.setReplyContent(comment.getReplyContent());
        vo.setReplyTime(comment.getReplyTime());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    private static List<String> parseImages(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(raw, String.class);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static String statusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已驳回";
            case 3: return "已隐藏";
            default: return "未知";
        }
    }
}
