package com.gj.mall.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "地址新增/修改请求")
public class AddressDTO {

    @Schema(description = "地址ID（新增时为空）")
    private Long id;

    @NotBlank(message = "收件人不能为空")
    private String receiver;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank private String province;
    @NotBlank private String city;
    @NotBlank private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private String postCode;
    private Integer isDefault;
}
