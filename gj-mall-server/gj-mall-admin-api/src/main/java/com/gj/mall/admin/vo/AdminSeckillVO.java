package com.gj.mall.admin.vo;

import com.gj.mall.marketing.entity.SmsSeckill;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Schema(description = "后台秒杀活动详情")
public class AdminSeckillVO {
    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusDesc;
    private String timeStatus;
    private Integer skuCount;
    private Integer totalStock;
    private Integer soldCount;
    private Integer remainStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<AdminSeckillSkuVO> skus;

    public static AdminSeckillVO from(SmsSeckill seckill, List<AdminSeckillSkuVO> skus) {
        List<AdminSeckillSkuVO> skuList = skus == null ? Collections.emptyList() : skus;
        AdminSeckillVO vo = new AdminSeckillVO();
        vo.setId(seckill.getId());
        vo.setName(seckill.getName());
        vo.setStartTime(seckill.getStartTime());
        vo.setEndTime(seckill.getEndTime());
        vo.setStatus(seckill.getStatus());
        vo.setStatusDesc(statusDesc(seckill.getStatus()));
        vo.setTimeStatus(timeStatus(seckill));
        vo.setSkuCount(skuList.size());
        vo.setTotalStock(skuList.stream().mapToInt(AdminSeckillVO::skuTotalStock).sum());
        vo.setSoldCount(skuList.stream().mapToInt(AdminSeckillVO::skuSoldCount).sum());
        vo.setRemainStock(skuList.stream().mapToInt(AdminSeckillVO::skuRemainStock).sum());
        vo.setCreateTime(seckill.getCreateTime());
        vo.setUpdateTime(seckill.getUpdateTime());
        vo.setSkus(skuList);
        return vo;
    }

    private static int skuTotalStock(AdminSeckillSkuVO sku) {
        return sku.getSeckillStock() == null ? 0 : sku.getSeckillStock();
    }

    private static int skuSoldCount(AdminSeckillSkuVO sku) {
        return sku.getSoldCount() == null ? 0 : sku.getSoldCount();
    }

    private static int skuRemainStock(AdminSeckillSkuVO sku) {
        return sku.getRemainStock() == null ? 0 : sku.getRemainStock();
    }

    private static String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "草稿";
            case 1:
                return "上线";
            case 2:
                return "结束";
            default:
                return "未知";
        }
    }

    private static String timeStatus(SmsSeckill seckill) {
        LocalDateTime now = LocalDateTime.now();
        if (seckill.getStartTime() != null && now.isBefore(seckill.getStartTime())) {
            return "未开始";
        }
        if (seckill.getEndTime() != null && now.isAfter(seckill.getEndTime())) {
            return "已结束";
        }
        return "进行中";
    }
}
