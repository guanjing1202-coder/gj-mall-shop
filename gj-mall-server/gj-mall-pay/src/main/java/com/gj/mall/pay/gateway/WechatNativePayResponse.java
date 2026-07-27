package com.gj.mall.pay.gateway;

public class WechatNativePayResponse {

    private final String codeUrl;

    public WechatNativePayResponse(String codeUrl) {
        this.codeUrl = codeUrl;
    }

    public String getCodeUrl() {
        return codeUrl;
    }
}
