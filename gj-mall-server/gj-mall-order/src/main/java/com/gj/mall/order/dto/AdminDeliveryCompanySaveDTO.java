package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "物流公司保存")
public class AdminDeliveryCompanySaveDTO {

    private Long id;

    @NotBlank(message = "物流公司编码不能为空")
    @Size(max = 32, message = "物流公司编码不能超过32个字符")
    private String code;

    @NotBlank(message = "物流公司名称不能为空")
    @Size(max = 64, message = "物流公司名称不能超过64个字符")
    private String name;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String contactPhone;

    private Integer sort;
    private Integer status;
}
