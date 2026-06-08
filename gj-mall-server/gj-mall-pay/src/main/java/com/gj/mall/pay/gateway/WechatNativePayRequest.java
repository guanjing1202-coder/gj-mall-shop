package com.gj.mall.pay.gateway;

public class WechatNativePayRequest {

    private final String appId;
    private final String mchId;
    private final String apiV3Key;
    private final String merchantSerialNo;
    private final String privateKeyPath;
    private final String notifyUrl;
    private final String description;
    private final String outTradeNo;
    private final Integer totalFee;
    private final String currency;

    public WechatNativePayRequest(String appId,
                                  String mchId,
                                  String apiV3Key,
                                  String merchantSerialNo,
                                  String privateKeyPath,
                                  String notifyUrl,
                                  String description,
                                  String outTradeNo,
                                  Integer totalFee,
                                  String currency) {
        this.appId = appId;
        this.mchId = mchId;
        this.apiV3Key = apiV3Key;
        this.merchantSerialNo = merchantSerialNo;
        this.privateKeyPath = privateKeyPath;
        this.notifyUrl = notifyUrl;
        this.description = description;
        this.outTradeNo = outTradeNo;
        this.totalFee = totalFee;
        this.currency = currency;
    }

    public String getAppId() {
        return appId;
    }

    public String getMchId() {
        return mchId;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public String getMerchantSerialNo() {
        return merchantSerialNo;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public Integer getTotalFee() {
        return totalFee;
    }

    public String getCurrency() {
        return currency;
    }
}
