package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(description = "订单商品评价请求")
public class OrderCommentCreateDTO {

    @Schema(description = "订单项 ID")
    @NotNull
    private Long orderItemId;

    @Schema(description = "评分 1-5")
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    @Schema(description = "评价内容")
    @NotBlank
    private String content;

    @Schema(description = "图片 URL 列表")
    private List<String> images;
}
