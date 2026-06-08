package com.gj.mall.pay.gateway;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class AlipayPagePaySdkClient implements AlipayPagePayClient {

    private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String FORMAT = "json";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";

    @Override
    public String pagePay(AlipayPagePayRequest request) {
        try {
            AlipayTradePagePayResponse response = client(request).pageExecute(toSdkRequest(request), "POST");
            String form = firstNonBlank(response == null ? null : response.getBody(),
                    response == null ? null : response.getPageRedirectionData());
            if (form == null) {
                throw new BizException(ResultCode.PAY_FAIL, "支付宝 Page Pay 返回表单为空");
            }
            return form;
        } catch (BizException ex) {
            throw ex;
        } catch (AlipayApiException ex) {
            throw new BizException(ResultCode.PAY_FAIL, "支付宝 Page Pay 下单失败：" + ex.getMessage());
        }
    }

    private AlipayClient client(AlipayPagePayRequest request) throws AlipayApiException {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(GATEWAY_URL);
        config.setAppId(request.getAppId());
        config.setPrivateKey(request.getPrivateKey());
        config.setAlipayPublicKey(request.getAlipayPublicKey());
        config.setFormat(FORMAT);
        config.setCharset(CHARSET);
        config.setSignType(SIGN_TYPE);
        return new DefaultAlipayClient(config);
    }

    private com.alipay.api.request.AlipayTradePagePayRequest toSdkRequest(AlipayPagePayRequest request) {
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(request.getOutTradeNo());
        model.setTotalAmount(request.getTotalAmount());
        model.setSubject(request.getSubject());
        model.setProductCode(PRODUCT_CODE);

        com.alipay.api.request.AlipayTradePagePayRequest sdkRequest =
                new com.alipay.api.request.AlipayTradePagePayRequest();
        sdkRequest.setNotifyUrl(request.getNotifyUrl());
        sdkRequest.setBizModel(model);
        return sdkRequest;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
