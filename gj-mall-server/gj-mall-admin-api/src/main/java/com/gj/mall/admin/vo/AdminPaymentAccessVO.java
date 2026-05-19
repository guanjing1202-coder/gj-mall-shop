package com.gj.mall.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminPaymentAccessVO {
    private String mode;
    private Boolean callbackRequireSignature;
    private String callbackPath;
    private String devSignatureAlgorithm;
    private List<ChannelItem> channels = new ArrayList<>();

    @Data
    public static class ChannelItem {
        private Integer channel;
        private String name;
        private String desc;
        private Boolean enabled;
        private String status;

        public static ChannelItem of(Integer channel, String name, String desc, Boolean enabled, String status) {
            ChannelItem item = new ChannelItem();
            item.setChannel(channel);
            item.setName(name);
            item.setDesc(desc);
            item.setEnabled(enabled);
            item.setStatus(status);
            return item;
        }
    }
}
