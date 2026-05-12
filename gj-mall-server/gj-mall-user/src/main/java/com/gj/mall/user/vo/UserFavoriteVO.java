package com.gj.mall.user.vo;

import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.user.entity.UmsUserFavorite;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserFavoriteVO {

    private Long id;
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
    private LocalDateTime createTime;

    public static UserFavoriteVO from(
            UmsUserFavorite favorite,
            PmsSpu spu,
            PmsBrand brand,
            PmsCategory category) {
        UserFavoriteVO vo = new UserFavoriteVO();
        vo.setId(favorite.getId());
        vo.setSpuId(favorite.getSpuId());
        vo.setCreateTime(favorite.getCreateTime());
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
