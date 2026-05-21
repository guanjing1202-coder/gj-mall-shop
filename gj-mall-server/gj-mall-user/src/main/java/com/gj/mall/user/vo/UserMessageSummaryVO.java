package com.gj.mall.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserMessageSummaryVO {
    private Long total;
    private Long unreadTotal;
    private Long readTotal;
    private List<TypeItem> typeItems;

    @Data
    public static class TypeItem {
        private String type;
        private String typeDesc;
        private Long total;
        private Long unreadTotal;
        private Long readTotal;

        public static TypeItem empty(String type, String typeDesc) {
            TypeItem item = new TypeItem();
            item.setType(type);
            item.setTypeDesc(typeDesc);
            item.setTotal(0L);
            item.setUnreadTotal(0L);
            item.setReadTotal(0L);
            return item;
        }
    }
}
