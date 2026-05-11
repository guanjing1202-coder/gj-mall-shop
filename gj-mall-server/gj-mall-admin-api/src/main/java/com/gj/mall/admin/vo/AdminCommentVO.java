package com.gj.mall.admin.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.user.entity.UmsUser;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class AdminCommentVO {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private Long spuId;
    private String spuName;
    private String subTitle;
    private String mainImage;
    private Long skuId;
    private String skuName;
    private String skuCode;
    private String skuImage;
    private Map<String, String> specData;
    private BigDecimal skuPrice;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private Integer score;
    private String content;
    private List<String> images;
    private Integer status;
    private String statusDesc;
    private String auditRemark;
    private LocalDateTime auditTime;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminCommentVO from(
            PmsProductComment comment,
            UmsUser user,
            PmsSpu spu,
            PmsSku sku,
            PmsBrand brand,
            PmsCategory category,
            OmsOrderItem orderItem) {
        AdminCommentVO vo = new AdminCommentVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setOrderNo(comment.getOrderNo());
        vo.setOrderItemId(comment.getOrderItemId());
        vo.setUserId(comment.getUserId());
        vo.setSpuId(comment.getSpuId());
        vo.setSkuId(comment.getSkuId());
        vo.setScore(comment.getScore());
        vo.setContent(comment.getContent());
        vo.setImages(parseImages(comment.getImages()));
        vo.setStatus(comment.getStatus());
        vo.setStatusDesc(statusDesc(comment.getStatus()));
        vo.setAuditRemark(comment.getAuditRemark());
        vo.setAuditTime(comment.getAuditTime());
        vo.setReplyContent(comment.getReplyContent());
        vo.setReplyTime(comment.getReplyTime());
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
            vo.setAvatar(user.getAvatar());
        }
        if (spu != null) {
            vo.setSpuName(spu.getName());
            vo.setSubTitle(spu.getSubTitle());
            vo.setMainImage(spu.getMainImage());
            vo.setBrandId(spu.getBrandId());
            vo.setCategoryId(spu.getCategoryId());
        }
        if (sku != null) {
            vo.setSkuName(sku.getName());
            vo.setSkuCode(sku.getSkuCode());
            vo.setSkuImage(sku.getImage());
            vo.setSkuPrice(sku.getPrice());
            vo.setSpecData(parseSpecData(sku.getSpecData()));
        }
        if (orderItem != null) {
            vo.setSkuName(firstNotBlank(vo.getSkuName(), orderItem.getSkuName()));
            vo.setSkuImage(firstNotBlank(vo.getSkuImage(), orderItem.getSkuImage()));
            if (vo.getSkuPrice() == null) {
                vo.setSkuPrice(orderItem.getPrice());
            }
            if (vo.getSpecData() == null || vo.getSpecData().isEmpty()) {
                vo.setSpecData(parseSpecData(orderItem.getSpecData()));
            }
        }
        if (brand != null) {
            vo.setBrandName(brand.getName());
        }
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }

    private static String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "已通过";
            case 2:
                return "已驳回";
            case 3:
                return "已隐藏";
            default:
                return "未知";
        }
    }

    private static List<String> parseImages(String images) {
        if (images == null || images.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseObject(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static Map<String, String> parseSpecData(String specData) {
        if (specData == null || specData.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(specData, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String firstNotBlank(String first, String second) {
        return first == null || first.trim().isEmpty() ? second : first;
    }
}
