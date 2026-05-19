package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;

@Data
@Schema(description = "发票信息")
public class InvoiceInfoDTO {
    @Schema(description = "发票类型：0不开发票 1个人 2企业")
    private Integer type;

    @Schema(description = "发票抬头")
    private String title;

    @Schema(description = "企业税号")
    private String taxNo;

    @Email(message = "接收邮箱格式不正确")
    @Schema(description = "接收邮箱")
    private String email;

    @Schema(description = "发票内容")
    private String content;
}
