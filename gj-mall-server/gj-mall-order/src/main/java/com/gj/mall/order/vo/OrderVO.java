package com.gj.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;

    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal freightAmount;
    private BigDecimal couponAmount;
    private Long couponUserId;

    private Integer status;
    private String statusDesc;

    private Integer payType;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private String deliveryCompany;
    private String deliveryNo;
    private String deliveryRemark;

    private ReceiverVO receiver;
    private InvoiceVO invoice;
    private String remark;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;
}
