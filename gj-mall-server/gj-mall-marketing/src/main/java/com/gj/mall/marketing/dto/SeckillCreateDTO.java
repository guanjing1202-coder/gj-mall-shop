package com.gj.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建秒杀活动")
public class SeckillCreateDTO {

    @NotBlank
    @Schema(description = "活动名称")
    private String name;

    @NotNull
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @NotNull
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
