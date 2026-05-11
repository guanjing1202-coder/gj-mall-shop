package com.gj.mall.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "后台保存优惠券")
public class AdminCouponSaveDTO {

    @Schema(description = "优惠券ID，新增时为空")
    private Long id;

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @NotNull
    @Schema(description = "类型：1满减 2折扣 3新人")
    private Integer type;

    @Schema(description = "满减/新人券减免金额")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率，0.85=85折")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛，0=无门槛")
    private BigDecimal minAmount;

    @NotNull
    @Min(1)
    @Schema(description = "发行总量")
    private Integer totalCount;

    @NotNull
    @Schema(description = "有效期开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull
    @Schema(description = "有效期结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "状态：0关闭 1开启")
    private Integer status;
}
