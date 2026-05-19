package com.gj.mall.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mall.pay.callback")
public class PayCallbackProperties {

    /** 开发环境默认不强制验签；真实支付建议开启。 */
    private boolean requireSignature = false;

    /** 开发验签密钥。真实微信/支付宝接入后应替换为 SDK 验签配置。 */
    private String secret = "gj-mall-dev-pay-callback-secret";
}
