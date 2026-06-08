package com.gj.mall.pay.gateway;

public class AlipayPagePayRequest {

    private final String appId;
    private final String privateKey;
    private final String alipayPublicKey;
    private final String notifyUrl;
    private final String outTradeNo;
    private final String totalAmount;
    private final String subject;

    public AlipayPagePayRequest(String appId,
                                String privateKey,
                                String alipayPublicKey,
                                String notifyUrl,
                                String outTradeNo,
                                String totalAmount,
                                String subject) {
        this.appId = appId;
        this.privateKey = privateKey;
        this.alipayPublicKey = alipayPublicKey;
        this.notifyUrl = notifyUrl;
        this.outTradeNo = outTradeNo;
        this.totalAmount = totalAmount;
        this.subject = subject;
    }

    public String getAppId() {
        return appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getSubject() {
        return subject;
    }
}
