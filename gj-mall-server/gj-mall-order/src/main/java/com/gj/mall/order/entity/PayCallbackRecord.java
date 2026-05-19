package com.gj.mall.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付渠道回调记录
 */
@Data
@TableName("pay_callback_record")
public class PayCallbackRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String callbackNo;
    private Integer channel;
    private String channelName;
    private String payNo;
    private String thirdPayNo;
    private String notifyId;
    private String eventType;
    private BigDecimal amount;

    /** 0跳过 1有效 2无效 */
    private Integer signatureStatus;

    /** 0已接收 1已处理 2幂等忽略 3处理失败 */
    private Integer processStatus;

    private Integer retryCount;
    private String errorMessage;
    private String rawData;
    private String requestHeaders;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
