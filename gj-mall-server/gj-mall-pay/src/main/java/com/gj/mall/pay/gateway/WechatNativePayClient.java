package com.gj.mall.pay.gateway;

public interface WechatNativePayClient {

    WechatNativePayResponse prepay(WechatNativePayRequest request);
}
