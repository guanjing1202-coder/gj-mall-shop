package com.gj.mall.product.dto;

import lombok.Data;

/**
 * 后台商品运营位设置。
 */
@Data
public class AdminSpuOperationDTO {

    /** 0下架 1上架 */
    private Integer publishStatus;

    /** 0否 1是 */
    private Integer newStatus;

    /** 0否 1是 */
    private Integer recommendStatus;

    /** 排序值，越大越靠前 */
    private Integer sort;
}
