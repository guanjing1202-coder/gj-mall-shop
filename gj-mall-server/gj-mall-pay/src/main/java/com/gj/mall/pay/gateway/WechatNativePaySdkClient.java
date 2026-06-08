package com.gj.mall.pay.gateway;

import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import org.springframework.stereotype.Component;

@Component
public class WechatNativePaySdkClient implements WechatNativePayClient {

    @Override
    public WechatNativePayResponse prepay(WechatNativePayRequest request) {
        try {
            PrepayResponse response = service(request).prepay(toSdkRequest(request));
            return new WechatNativePayResponse(response == null ? null : response.getCodeUrl());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.PAY_FAIL, "微信 Native Pay 下单失败：" + ex.getMessage());
        }
    }

    private NativePayService service(WechatNativePayRequest request) {
        Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(request.getMchId())
                .privateKeyFromPath(request.getPrivateKeyPath())
                .merchantSerialNumber(request.getMerchantSerialNo())
                .apiV3Key(request.getApiV3Key())
                .build();
        return new NativePayService.Builder()
                .config(config)
                .build();
    }

    private PrepayRequest toSdkRequest(WechatNativePayRequest request) {
        PrepayRequest sdkRequest = new PrepayRequest();
        sdkRequest.setAppid(request.getAppId());
        sdkRequest.setMchid(request.getMchId());
        sdkRequest.setDescription(request.getDescription());
        sdkRequest.setOutTradeNo(request.getOutTradeNo());
        sdkRequest.setNotifyUrl(request.getNotifyUrl());

        Amount amount = new Amount();
        amount.setTotal(request.getTotalFee());
        amount.setCurrency(request.getCurrency());
        sdkRequest.setAmount(amount);
        return sdkRequest;
    }
}
