package com.gj.mall.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderLogisticsVO {
    private Long orderId;
    private String orderNo;
    private Integer status;
    private String statusDesc;
    private String deliveryCompany;
    private String deliveryNo;
    private String deliveryRemark;
    private String currentAction;
    private String nextHint;
    private List<OrderLogisticsTraceVO> traces;
}
