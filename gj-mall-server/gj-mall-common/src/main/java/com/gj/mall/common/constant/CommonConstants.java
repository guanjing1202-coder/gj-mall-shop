package com.gj.mall.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;
    /** 默认每页大小 */
    public static final long DEFAULT_PAGE_SIZE = 10L;
    /** 最大每页大小 */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 客户端类型请求头 */
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";
    /** Token 请求头 */
    public static final String HEADER_TOKEN = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Redis Key 前缀 */
    public static final String REDIS_PREFIX = "mall:";
    public static final String REDIS_USER_TOKEN = REDIS_PREFIX + "user:token:";
    public static final String REDIS_ADMIN_TOKEN = REDIS_PREFIX + "admin:token:";
    public static final String REDIS_CART = REDIS_PREFIX + "cart:";
    public static final String REDIS_PRODUCT_DETAIL = REDIS_PREFIX + "product:detail:";
    public static final String REDIS_VERIFY_CODE = REDIS_PREFIX + "verify:code:";
    public static final String REDIS_STOCK = REDIS_PREFIX + "stock:";

    /** 客户端类型枚举 */
    public static final class ClientType {
        public static final String PC = "pc";
        public static final String H5 = "h5";
        public static final String MP_WEIXIN = "mp-weixin";
        public static final String ADMIN = "admin";
    }
}
