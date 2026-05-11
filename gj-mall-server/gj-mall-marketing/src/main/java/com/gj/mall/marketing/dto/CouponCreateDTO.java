package com.gj.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建优惠券")
public class CouponCreateDTO {

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @NotNull
    @Schema(description = "类型：1满减 2折扣 3新人")
    private Integer type;

    @Schema(description = "满减金额（type=1时必填）")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率 0.85=85折（type=2时必填）")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛，0=无门槛")
    private BigDecimal minAmount;

    @NotNull
    @Schema(description = "发行总量")
    private Integer totalCount;

    @NotNull
    @Schema(description = "有效期开始")
    private LocalDateTime startTime;

    @NotNull
    @Schema(description = "有效期结束")
    private LocalDateTime endTime;
}
