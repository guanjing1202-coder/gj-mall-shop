package com.gj.mall.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogisticsTraceVO {
    private String title;
    private String description;
    private LocalDateTime time;
    private Boolean active;
}
