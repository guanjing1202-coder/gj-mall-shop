package com.gj.mall.pay.vo;

import lombok.Data;

@Data
public class PayCallbackResultVO {
    private String callbackNo;
    private String payNo;
    private String thirdPayNo;
    private String channel;
    private boolean processed;
    private boolean duplicate;
    private String message;
}
