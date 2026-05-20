package com.gj.mall.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "运费试算结果")
public class FreightQuoteVO {

    @Schema(description = "商品金额")
    private BigDecimal orderAmount;

    @Schema(description = "最终运费")
    private BigDecimal freightAmount;

    @Schema(description = "基础运费")
    private BigDecimal baseFreight;

    @Schema(description = "实际收取基础运费")
    private BigDecimal chargedBaseFreight;

    @Schema(description = "偏远地区附加费配置")
    private BigDecimal remoteExtra;

    @Schema(description = "实际收取偏远地区附加费")
    private BigDecimal chargedRemoteExtra;

    @Schema(description = "免基础运费门槛")
    private BigDecimal freeThreshold;

    @Schema(description = "是否达到免基础运费门槛")
    private Boolean freeThresholdReached;

    @Schema(description = "是否整单免运费")
    private Boolean freeShipping;

    @Schema(description = "是否偏远地区")
    private Boolean remoteArea;

    @Schema(description = "收货省份")
    private String province;

    @Schema(description = "距离免基础运费还差金额")
    private BigDecimal nextFreeAmount;

    @Schema(description = "运费摘要")
    private String summary;

    @Schema(description = "运费提示")
    private String hint;
}
