package com.gj.mall.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Schema(description = "后台保存秒杀活动")
public class AdminSeckillSaveDTO {

    @Schema(description = "秒杀活动ID，新增时为空")
    private Long id;

    @NotBlank
    @Schema(description = "活动名称")
    private String name;

    @NotNull
    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull
    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "状态：0草稿 1上线 2结束")
    private Integer status;
}
