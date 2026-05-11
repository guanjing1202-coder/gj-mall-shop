package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Schema(description = "后台发放优惠券")
public class AdminCouponIssueDTO {

    @NotEmpty
    @Schema(description = "会员ID列表")
    private List<Long> userIds;
}
