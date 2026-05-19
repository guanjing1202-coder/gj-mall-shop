package com.gj.mall.order.vo;

import lombok.Data;

@Data
public class InvoiceVO {
    /** 0不开发票 1个人 2企业 */
    private Integer type;
    private String title;
    private String taxNo;
    private String email;
    private String content;
}
