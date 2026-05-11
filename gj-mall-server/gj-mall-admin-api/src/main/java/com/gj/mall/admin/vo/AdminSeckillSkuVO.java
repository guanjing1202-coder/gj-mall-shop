package com.gj.mall.admin.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gj.mall.marketing.entity.SmsSeckillSku;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "后台秒杀 SKU")
public class AdminSeckillSkuVO {
    private Long id;
    private Long seckillId;
    private Long spuId;
    private String spuName;
    private Long skuId;
    private String skuName;
    private String skuImage;
    private Map<String, String> specData;
    private BigDecimal originalPrice;
    private Integer productStock;
    private Integer lockedStock;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer seckillLimit;
    private Integer soldCount;
    private Integer remainStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminSeckillSkuVO from(
            SmsSeckillSku seckillSku,
            PmsSpu spu,
            PmsSku sku,
            Integer remainStock) {
        AdminSeckillSkuVO vo = new AdminSeckillSkuVO();
        vo.setId(seckillSku.getId());
        vo.setSeckillId(seckillSku.getSeckillId());
        vo.setSpuId(seckillSku.getSpuId());
        vo.setSkuId(seckillSku.getSkuId());
        vo.setSeckillPrice(seckillSku.getSeckillPrice());
        vo.setSeckillStock(defaultInt(seckillSku.getSeckillStock()));
        vo.setSeckillLimit(defaultInt(seckillSku.getSeckillLimit()));
        vo.setSoldCount(defaultInt(seckillSku.getSoldCount()));
        vo.setRemainStock(remainStock == null
                ? Math.max(vo.getSeckillStock() - vo.getSoldCount(), 0)
                : remainStock);
        vo.setCreateTime(seckillSku.getCreateTime());
        vo.setUpdateTime(seckillSku.getUpdateTime());
        if (spu != null) {
            vo.setSpuName(spu.getName());
        }
        if (sku != null) {
            vo.setSkuName(sku.getName());
            vo.setSkuImage(sku.getImage());
            vo.setOriginalPrice(sku.getPrice());
            vo.setProductStock(sku.getStock());
            vo.setLockedStock(sku.getLockedStock());
            if (sku.getSpecData() != null) {
                try {
                    vo.setSpecData(JSON.parseObject(sku.getSpecData(),
                            new TypeReference<Map<String, String>>() {}));
                } catch (Exception ignored) {
                }
            }
        }
        return vo;
    }

    private static Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
