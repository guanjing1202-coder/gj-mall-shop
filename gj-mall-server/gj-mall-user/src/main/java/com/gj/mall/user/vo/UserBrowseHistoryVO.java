package com.gj.mall.user.vo;

import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserBrowseHistoryVO {
    private Long spuId;
    private String spuName;
    private String subTitle;
    private String mainImage;
    private BigDecimal price;
    private Integer saleCount;
    private Integer publishStatus;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime browseTime;

    public static UserBrowseHistoryVO from(
            Long spuId,
            LocalDateTime browseTime,
            PmsSpu spu,
            PmsBrand brand,
            PmsCategory category) {
        UserBrowseHistoryVO vo = new UserBrowseHistoryVO();
        vo.setSpuId(spuId);
        vo.setBrowseTime(browseTime);
        if (spu != null) {
            vo.setSpuName(spu.getName());
            vo.setSubTitle(spu.getSubTitle());
            vo.setMainImage(spu.getMainImage());
            vo.setPrice(spu.getPrice());
            vo.setSaleCount(spu.getSaleCount());
            vo.setPublishStatus(spu.getPublishStatus());
            vo.setBrandId(spu.getBrandId());
            vo.setCategoryId(spu.getCategoryId());
        }
        if (brand != null) {
            vo.setBrandName(brand.getName());
        }
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }
}
