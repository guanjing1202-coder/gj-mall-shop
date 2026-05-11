package com.gj.mall.admin.vo;

import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.entity.UmsUserFavorite;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "会员收藏记录")
public class AdminFavoriteVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private Long spuId;
    private String spuName;
    private String subTitle;
    private String mainImage;
    private BigDecimal price;
    private Integer publishStatus;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createTime;

    public static AdminFavoriteVO from(
            UmsUserFavorite favorite,
            UmsUser user,
            PmsSpu spu,
            PmsBrand brand,
            PmsCategory category) {
        AdminFavoriteVO vo = new AdminFavoriteVO();
        vo.setId(favorite.getId());
        vo.setUserId(favorite.getUserId());
        vo.setSpuId(favorite.getSpuId());
        vo.setCreateTime(favorite.getCreateTime());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
        }
        if (spu != null) {
            vo.setSpuName(spu.getName());
            vo.setSubTitle(spu.getSubTitle());
            vo.setMainImage(spu.getMainImage());
            vo.setPrice(spu.getPrice());
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
