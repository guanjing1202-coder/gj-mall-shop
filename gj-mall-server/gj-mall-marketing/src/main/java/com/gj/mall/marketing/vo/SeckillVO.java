package com.gj.mall.marketing.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SeckillVO {
    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusDesc;
    private List<SeckillSkuVO> skus;
}
