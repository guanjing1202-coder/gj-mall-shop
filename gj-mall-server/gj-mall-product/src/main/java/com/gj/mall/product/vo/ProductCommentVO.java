package com.gj.mall.product.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.entity.PmsSku;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class ProductCommentVO {

    private Long id;
    private Long userId;
    private Long spuId;
    private Long skuId;
    private String skuName;
    private Map<String, String> specData;
    private Integer score;
    private String content;
    private List<String> images;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;

    public static ProductCommentVO from(PmsProductComment comment, PmsSku sku) {
        ProductCommentVO vo = new ProductCommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setSpuId(comment.getSpuId());
        vo.setSkuId(comment.getSkuId());
        vo.setScore(comment.getScore());
        vo.setContent(comment.getContent());
        vo.setImages(parseImages(comment.getImages()));
        vo.setReplyContent(comment.getReplyContent());
        vo.setReplyTime(comment.getReplyTime());
        vo.setCreateTime(comment.getCreateTime());
        if (sku != null) {
            vo.setSkuName(sku.getName());
            vo.setSpecData(parseSpecData(sku.getSpecData()));
        }
        return vo;
    }

    private static List<String> parseImages(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseObject(raw, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static Map<String, String> parseSpecData(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(raw, new TypeReference<Map<String, String>>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
