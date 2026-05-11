package com.gj.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项
 */
@Data
@TableName(value = "oms_order_item", autoResultMap = true)
public class OmsOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;

    private Long spuId;
    private Long skuId;
    private String skuName;
    private String skuImage;

    /** 规格 JSON：例如 {"颜色":"黑"} */
    private String specData;

    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
