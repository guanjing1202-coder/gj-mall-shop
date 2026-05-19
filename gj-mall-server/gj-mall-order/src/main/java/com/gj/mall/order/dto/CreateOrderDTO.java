package com.gj.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 下单请求：从购物车选中项下单
 */
@Data
@Schema(description = "下单请求")
public class CreateOrderDTO {

    @Schema(description = "收货地址 ID")
    @NotNull
    private Long addressId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "优惠券 ID（可选）")
    private Long couponId;

    @Schema(description = "发票信息（可选）")
    @Valid
    private InvoiceInfoDTO invoiceInfo;

    @Schema(description = "直购商品列表（可选，传入时不读取购物车）")
    private List<CreateOrderItemDTO> items;
}
