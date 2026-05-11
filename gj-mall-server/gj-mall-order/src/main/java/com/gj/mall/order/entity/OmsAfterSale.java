package com.gj.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单
 */
@Data
@TableName("oms_after_sale")
public class OmsAfterSale implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long userId;

    /** 1仅退款 2退货退款 */
    private Integer type;
    private BigDecimal amount;
    private String reason;
    private String description;
    private String images;

    /** 创建售后时订单原状态，拒绝/取消时用于恢复 */
    private Integer orderStatusSnapshot;

    /** 0待审核 1待退货 2待退款 3已拒绝 4已完成 5已取消 */
    private Integer status;
    private String auditRemark;
    private String rejectReason;
    private String returnCompany;
    private String returnNo;
    private Long refundPaymentId;
    private LocalDateTime auditTime;
    private LocalDateTime receiveTime;
    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
