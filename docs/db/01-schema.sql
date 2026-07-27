-- =====================================================
-- gj-mall-shop 数据库 schema
-- MySQL 8.0+, utf8mb4
-- =====================================================

USE gj_mall;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 用户中心 ums_*
-- =====================================================

DROP TABLE IF EXISTS ums_user;
CREATE TABLE ums_user (
    id              BIGINT          NOT NULL                COMMENT '用户ID(雪花)',
    username        VARCHAR(50)     NOT NULL                COMMENT '用户名/账号',
    password        VARCHAR(100)    DEFAULT NULL            COMMENT '密码(BCrypt)',
    phone           VARCHAR(20)     DEFAULT NULL            COMMENT '手机号',
    email           VARCHAR(100)    DEFAULT NULL            COMMENT '邮箱',
    nickname        VARCHAR(50)     DEFAULT NULL            COMMENT '昵称',
    avatar          VARCHAR(255)    DEFAULT NULL            COMMENT '头像URL',
    gender          TINYINT         DEFAULT 0               COMMENT '性别 0未知 1男 2女',
    birthday        DATE            DEFAULT NULL            COMMENT '生日',
    wx_openid       VARCHAR(64)     DEFAULT NULL            COMMENT '微信小程序 openid',
    wx_unionid      VARCHAR(64)     DEFAULT NULL            COMMENT '微信 unionid',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态 0禁用 1正常',
    last_login_at   DATETIME        DEFAULT NULL            COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)     DEFAULT NULL            COMMENT '最后登录IP',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_wx_openid (wx_openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

DROP TABLE IF EXISTS ums_user_address;
CREATE TABLE ums_user_address (
    id              BIGINT          NOT NULL                COMMENT '地址ID',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    receiver        VARCHAR(50)     NOT NULL                COMMENT '收件人',
    phone           VARCHAR(20)     NOT NULL                COMMENT '收件电话',
    province        VARCHAR(50)     NOT NULL                COMMENT '省',
    city            VARCHAR(50)     NOT NULL                COMMENT '市',
    district        VARCHAR(50)     NOT NULL                COMMENT '区/县',
    detail          VARCHAR(255)    NOT NULL                COMMENT '详细地址',
    post_code       VARCHAR(10)     DEFAULT NULL            COMMENT '邮编',
    is_default      TINYINT         NOT NULL DEFAULT 0      COMMENT '是否默认 0否 1是',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

DROP TABLE IF EXISTS ums_user_favorite;
CREATE TABLE ums_user_favorite (
    id              BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    spu_id          BIGINT          NOT NULL                COMMENT '商品SPU ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_spu (user_id, spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏';

DROP TABLE IF EXISTS ums_user_message;
CREATE TABLE ums_user_message (
    id              BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    type            VARCHAR(32)     NOT NULL DEFAULT 'system' COMMENT '消息类型 order/payment/logistics/after_sale/system',
    title           VARCHAR(100)    NOT NULL                COMMENT '消息标题',
    content         VARCHAR(1000)   DEFAULT NULL            COMMENT '消息内容',
    biz_type        VARCHAR(32)     DEFAULT NULL            COMMENT '业务类型 order/after_sale',
    biz_id          BIGINT          DEFAULT NULL            COMMENT '业务ID',
    biz_no          VARCHAR(64)     DEFAULT NULL            COMMENT '业务单号',
    read_status     TINYINT         NOT NULL DEFAULT 0      COMMENT '0未读 1已读',
    read_time       DATETIME        DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, read_status, create_time),
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户站内消息';

-- =====================================================
-- 商品 pms_*
-- =====================================================

DROP TABLE IF EXISTS pms_category;
CREATE TABLE pms_category (
    id              BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0      COMMENT '父分类ID 0=顶级',
    name            VARCHAR(100)    NOT NULL                COMMENT '分类名',
    icon            VARCHAR(255)    DEFAULT NULL            COMMENT '图标',
    level           TINYINT         NOT NULL DEFAULT 1      COMMENT '层级 1/2/3',
    sort            INT             NOT NULL DEFAULT 0      COMMENT '排序',
    show_status     TINYINT         NOT NULL DEFAULT 1      COMMENT '显示 0隐藏 1显示',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

DROP TABLE IF EXISTS pms_brand;
CREATE TABLE pms_brand (
    id              BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL                COMMENT '品牌名',
    logo            VARCHAR(255)    DEFAULT NULL,
    description     VARCHAR(500)    DEFAULT NULL,
    sort            INT             NOT NULL DEFAULT 0,
    show_status     TINYINT         NOT NULL DEFAULT 1,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌';

DROP TABLE IF EXISTS pms_spu;
CREATE TABLE pms_spu (
    id              BIGINT          NOT NULL                COMMENT 'SPU ID',
    name            VARCHAR(255)    NOT NULL                COMMENT '商品名',
    sub_title       VARCHAR(255)    DEFAULT NULL            COMMENT '副标题',
    category_id     BIGINT          NOT NULL                COMMENT '分类ID',
    brand_id        BIGINT          DEFAULT NULL            COMMENT '品牌ID',
    main_image      VARCHAR(500)    DEFAULT NULL            COMMENT '主图URL',
    images          JSON            DEFAULT NULL            COMMENT '图册JSON数组',
    detail_id       VARCHAR(64)     DEFAULT NULL            COMMENT 'MongoDB 详情文档ID',
    price           DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '展示价（最低SKU价）',
    sale_count      INT             NOT NULL DEFAULT 0      COMMENT '销量',
    publish_status  TINYINT         NOT NULL DEFAULT 0      COMMENT '上架 0下架 1上架',
    new_status      TINYINT         NOT NULL DEFAULT 0      COMMENT '新品 0否 1是',
    recommend_status TINYINT        NOT NULL DEFAULT 0      COMMENT '推荐 0否 1是',
    sort            INT             NOT NULL DEFAULT 0      COMMENT '运营排序，越大越靠前',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_brand (brand_id),
    KEY idx_publish (publish_status),
    KEY idx_operation (publish_status, recommend_status, new_status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU';

DROP TABLE IF EXISTS pms_sku;
CREATE TABLE pms_sku (
    id              BIGINT          NOT NULL,
    spu_id          BIGINT          NOT NULL,
    sku_code        VARCHAR(64)     DEFAULT NULL            COMMENT 'SKU编码',
    name            VARCHAR(255)    DEFAULT NULL,
    image           VARCHAR(500)    DEFAULT NULL,
    price           DECIMAL(10,2)   NOT NULL                COMMENT '售价',
    cost_price      DECIMAL(10,2)   DEFAULT NULL,
    stock           INT             NOT NULL DEFAULT 0      COMMENT '库存',
    locked_stock    INT             NOT NULL DEFAULT 0      COMMENT '锁定库存',
    warn_stock      INT             NOT NULL DEFAULT 10     COMMENT '预警库存阈值',
    spec_data       JSON            DEFAULT NULL            COMMENT '规格JSON {"颜色":"黑","尺寸":"L"}',
    sale_count      INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_spu_id (spu_id),
    KEY idx_stock_warn (stock, warn_stock, locked_stock),
    UNIQUE KEY uk_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU';

DROP TABLE IF EXISTS pms_inventory_log;
CREATE TABLE pms_inventory_log (
    id                   BIGINT        NOT NULL,
    spu_id               BIGINT        NOT NULL                COMMENT 'SPU ID',
    sku_id               BIGINT        NOT NULL                COMMENT 'SKU ID',
    change_type          TINYINT       NOT NULL                COMMENT '1增加 2减少',
    change_quantity      INT           NOT NULL                COMMENT '调整数量，正数',
    stock_before         INT           NOT NULL DEFAULT 0      COMMENT '调整前可用库存',
    stock_after          INT           NOT NULL DEFAULT 0      COMMENT '调整后可用库存',
    locked_stock_before  INT           NOT NULL DEFAULT 0      COMMENT '调整前锁定库存',
    locked_stock_after   INT           NOT NULL DEFAULT 0      COMMENT '调整后锁定库存',
    remark               VARCHAR(255)  DEFAULT NULL            COMMENT '调整备注',
    create_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sku_id (sku_id),
    KEY idx_spu_id (spu_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调整日志';

DROP TABLE IF EXISTS pms_product_comment;
CREATE TABLE pms_product_comment (
    id              BIGINT          NOT NULL,
    order_id        BIGINT          DEFAULT NULL                COMMENT '订单ID',
    order_no        VARCHAR(32)     DEFAULT NULL                COMMENT '订单号',
    order_item_id   BIGINT          DEFAULT NULL                COMMENT '订单项ID',
    user_id         BIGINT          NOT NULL                    COMMENT '评价用户ID',
    spu_id          BIGINT          NOT NULL                    COMMENT 'SPU ID',
    sku_id          BIGINT          NOT NULL                    COMMENT 'SKU ID',
    score           TINYINT         NOT NULL DEFAULT 5          COMMENT '评分 1-5',
    content         VARCHAR(1000)   DEFAULT NULL                COMMENT '评价内容',
    images          JSON            DEFAULT NULL                COMMENT '评价图片URL数组',
    status          TINYINT         NOT NULL DEFAULT 0          COMMENT '0待审核 1已通过 2已驳回 3已隐藏',
    audit_remark    VARCHAR(500)    DEFAULT NULL                COMMENT '审核备注',
    audit_time      DATETIME        DEFAULT NULL,
    reply_content   VARCHAR(1000)   DEFAULT NULL                COMMENT '商家回复',
    reply_time      DATETIME        DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_order_item (order_item_id),
    KEY idx_user (user_id),
    KEY idx_spu (spu_id),
    KEY idx_sku (sku_id),
    KEY idx_status (status),
    KEY idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价';

-- =====================================================
-- 购物车 oms_* (作为持久化备份，运行时主要走 Redis)
-- =====================================================

DROP TABLE IF EXISTS oms_cart_item;
CREATE TABLE oms_cart_item (
    id              BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    sku_id          BIGINT          NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    selected        TINYINT         NOT NULL DEFAULT 1      COMMENT '选中 0否 1是',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sku (user_id, sku_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车项';

-- =====================================================
-- 订单 oms_*
-- =====================================================

DROP TABLE IF EXISTS oms_order;
CREATE TABLE oms_order (
    id              BIGINT          NOT NULL,
    order_no        VARCHAR(32)     NOT NULL                COMMENT '订单号',
    user_id         BIGINT          NOT NULL,
    total_amount    DECIMAL(10,2)   NOT NULL,
    pay_amount      DECIMAL(10,2)   NOT NULL                COMMENT '实付',
    freight_amount  DECIMAL(10,2)   NOT NULL DEFAULT 0,
    coupon_amount   DECIMAL(10,2)   NOT NULL DEFAULT 0,
    coupon_user_id  BIGINT          DEFAULT NULL            COMMENT '用户优惠券ID',
    status          TINYINT         NOT NULL DEFAULT 0      COMMENT '0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款',
    pay_type        TINYINT         DEFAULT NULL            COMMENT '1微信 2支付宝 3余额 9MOCK',
    pay_time        DATETIME        DEFAULT NULL,
    delivery_time   DATETIME        DEFAULT NULL,
    receive_time    DATETIME        DEFAULT NULL,
    delivery_company VARCHAR(64)    DEFAULT NULL            COMMENT '物流公司',
    delivery_no      VARCHAR(64)    DEFAULT NULL            COMMENT '物流单号',
    delivery_remark  VARCHAR(255)   DEFAULT NULL            COMMENT '发货备注',
    receiver_info   JSON            DEFAULT NULL            COMMENT '收货地址快照',
    invoice_info    JSON            DEFAULT NULL            COMMENT '发票信息快照',
    remark          VARCHAR(500)    DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_status (status),
    KEY idx_delivery_no (delivery_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

DROP TABLE IF EXISTS oms_order_item;
CREATE TABLE oms_order_item (
    id              BIGINT          NOT NULL,
    order_id        BIGINT          NOT NULL,
    order_no        VARCHAR(32)     NOT NULL,
    spu_id          BIGINT          NOT NULL,
    sku_id          BIGINT          NOT NULL,
    seckill_sku_id  BIGINT          DEFAULT NULL            COMMENT '秒杀活动SKU ID',
    sku_name        VARCHAR(255)    NOT NULL,
    sku_image       VARCHAR(500)    DEFAULT NULL,
    spec_data       JSON            DEFAULT NULL,
    price           DECIMAL(10,2)   NOT NULL,
    quantity        INT             NOT NULL,
    total_amount    DECIMAL(10,2)   NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_seckill_sku (seckill_sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项';

DROP TABLE IF EXISTS oms_delivery_company;
CREATE TABLE oms_delivery_company (
    id              BIGINT          NOT NULL,
    code            VARCHAR(32)     NOT NULL                COMMENT '物流公司编码',
    name            VARCHAR(64)     NOT NULL                COMMENT '物流公司名称',
    contact_phone   VARCHAR(32)     DEFAULT NULL            COMMENT '联系电话',
    sort            INT             NOT NULL DEFAULT 100,
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态 0停用 1启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流公司';

DROP TABLE IF EXISTS oms_after_sale;
CREATE TABLE oms_after_sale (
    id                    BIGINT          NOT NULL,
    after_sale_no         VARCHAR(40)     NOT NULL                COMMENT '售后单号',
    order_id              BIGINT          NOT NULL,
    order_no              VARCHAR(32)     NOT NULL,
    user_id               BIGINT          NOT NULL,
    type                  TINYINT         NOT NULL                COMMENT '1仅退款 2退货退款',
    amount                DECIMAL(10,2)   NOT NULL                COMMENT '售后金额',
    reason                VARCHAR(255)    DEFAULT NULL,
    description           VARCHAR(1000)   DEFAULT NULL,
    images                JSON            DEFAULT NULL            COMMENT '凭证图片URL数组',
    order_status_snapshot TINYINT         NOT NULL                COMMENT '创建售后时订单状态',
    status                TINYINT         NOT NULL DEFAULT 0      COMMENT '0待审核 1待退货 2待退款 3已拒绝 4已完成 5已取消',
    audit_remark          VARCHAR(500)    DEFAULT NULL,
    reject_reason         VARCHAR(500)    DEFAULT NULL,
    return_company        VARCHAR(100)    DEFAULT NULL,
    return_no             VARCHAR(100)    DEFAULT NULL,
    refund_payment_id     BIGINT          DEFAULT NULL,
    audit_time            DATETIME        DEFAULT NULL,
    receive_time          DATETIME        DEFAULT NULL,
    refund_time           DATETIME        DEFAULT NULL,
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted               TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    KEY idx_order (order_id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后单';

-- =====================================================
-- 营销 sms_*
-- =====================================================

DROP TABLE IF EXISTS sms_coupon;
CREATE TABLE sms_coupon (
    id              BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    type            TINYINT         NOT NULL                COMMENT '1满减 2折扣 3新人',
    discount_amount DECIMAL(10,2)   DEFAULT NULL            COMMENT '减免金额',
    discount_rate   DECIMAL(5,2)    DEFAULT NULL            COMMENT '折扣率 0.85=85折',
    min_amount      DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '门槛',
    total_count     INT             NOT NULL                COMMENT '发行总量',
    received_count  INT             NOT NULL DEFAULT 0,
    used_count      INT             NOT NULL DEFAULT 0,
    start_time      DATETIME        NOT NULL,
    end_time        DATETIME        NOT NULL,
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '0关闭 1开启',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

DROP TABLE IF EXISTS sms_coupon_user;
CREATE TABLE sms_coupon_user (
    id              BIGINT          NOT NULL,
    coupon_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    status          TINYINT         NOT NULL DEFAULT 0      COMMENT '0未用 1已用 2过期',
    used_at         DATETIME        DEFAULT NULL,
    order_id        BIGINT          DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_coupon (coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

DROP TABLE IF EXISTS sms_seckill_sku;
DROP TABLE IF EXISTS sms_seckill;

CREATE TABLE sms_seckill (
    id              BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL            COMMENT '活动名',
    start_time      DATETIME        NOT NULL,
    end_time        DATETIME        NOT NULL,
    status          TINYINT         NOT NULL DEFAULT 0  COMMENT '0草稿 1上线 2结束',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

CREATE TABLE sms_seckill_sku (
    id              BIGINT          NOT NULL,
    seckill_id      BIGINT          NOT NULL,
    spu_id          BIGINT          NOT NULL,
    sku_id          BIGINT          NOT NULL,
    seckill_price   DECIMAL(10,2)   NOT NULL            COMMENT '秒杀价',
    seckill_stock   INT             NOT NULL            COMMENT '秒杀总库存',
    seckill_limit   INT             NOT NULL DEFAULT 1  COMMENT '每人限购',
    sold_count      INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_seckill (seckill_id),
    KEY idx_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动商品';

-- =====================================================
-- 支付 pay_*
-- =====================================================

DROP TABLE IF EXISTS pay_payment_record;
CREATE TABLE pay_payment_record (
    id              BIGINT          NOT NULL,
    order_id        BIGINT          NOT NULL,
    order_no        VARCHAR(32)     NOT NULL,
    user_id         BIGINT          NOT NULL,
    pay_no          VARCHAR(64)     NOT NULL                COMMENT '支付流水号',
    third_pay_no    VARCHAR(64)     DEFAULT NULL            COMMENT '第三方流水号',
    channel         TINYINT         NOT NULL                COMMENT '1微信 2支付宝 3余额 9MOCK',
    amount          DECIMAL(10,2)   NOT NULL,
    status          TINYINT         NOT NULL DEFAULT 0      COMMENT '0待支付 1已支付 2失败 3已退款',
    pay_time        DATETIME        DEFAULT NULL,
    callback_data   TEXT            DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录';

DROP TABLE IF EXISTS pay_refund_record;
CREATE TABLE pay_refund_record (
    id              BIGINT          NOT NULL,
    refund_no       VARCHAR(64)     NOT NULL                COMMENT '退款流水号',
    payment_id      BIGINT          NOT NULL                COMMENT '支付记录ID',
    pay_no          VARCHAR(64)     NOT NULL                COMMENT '内部支付流水号',
    third_pay_no    VARCHAR(64)     DEFAULT NULL            COMMENT '第三方支付流水号',
    after_sale_id   BIGINT          DEFAULT NULL            COMMENT '售后单ID',
    after_sale_no   VARCHAR(40)     DEFAULT NULL            COMMENT '售后单号',
    order_id        BIGINT          NOT NULL,
    order_no        VARCHAR(32)     NOT NULL,
    user_id         BIGINT          NOT NULL,
    channel         TINYINT         NOT NULL                COMMENT '1微信 2支付宝 3余额 9MOCK',
    amount          DECIMAL(10,2)   NOT NULL,
    status          TINYINT         NOT NULL DEFAULT 0      COMMENT '0退款中 1退款成功 2退款失败',
    reason          VARCHAR(255)    DEFAULT NULL            COMMENT '退款原因',
    operator_type   VARCHAR(32)     DEFAULT NULL            COMMENT '触发来源 admin_after_sale/admin_payment/system',
    callback_data   TEXT            DEFAULT NULL,
    success_time    DATETIME        DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_after_sale (after_sale_id),
    KEY idx_payment (payment_id),
    KEY idx_order (order_id),
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录';

DROP TABLE IF EXISTS pay_callback_record;
CREATE TABLE pay_callback_record (
    id                  BIGINT          NOT NULL,
    callback_no         VARCHAR(64)     NOT NULL                COMMENT '回调记录号',
    channel             TINYINT         NOT NULL                COMMENT '1微信 2支付宝 3余额 9MOCK',
    channel_name        VARCHAR(32)     NOT NULL                COMMENT '渠道名称',
    pay_no              VARCHAR(64)     DEFAULT NULL            COMMENT '内部支付流水号',
    third_pay_no        VARCHAR(64)     DEFAULT NULL            COMMENT '第三方流水号',
    notify_id           VARCHAR(128)    DEFAULT NULL            COMMENT '第三方通知ID/事件ID',
    event_type          VARCHAR(64)     DEFAULT NULL            COMMENT '事件类型',
    amount              DECIMAL(10,2)   DEFAULT NULL            COMMENT '回调金额',
    signature_status    TINYINT         NOT NULL DEFAULT 0      COMMENT '0跳过 1通过 2失败',
    process_status      TINYINT         NOT NULL DEFAULT 0      COMMENT '0接收 1处理 2幂等忽略 3失败',
    retry_count         INT             NOT NULL DEFAULT 0,
    error_message       VARCHAR(500)    DEFAULT NULL,
    raw_data            TEXT            DEFAULT NULL,
    request_headers     TEXT            DEFAULT NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_callback_no (callback_no),
    KEY idx_pay_no (pay_no),
    KEY idx_third_pay_no (third_pay_no),
    KEY idx_notify (notify_id),
    KEY idx_status (channel, process_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道回调记录';

-- =====================================================
-- 系统/RBAC sys_*
-- =====================================================

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL,
    username        VARCHAR(50)     NOT NULL,
    password        VARCHAR(100)    NOT NULL,
    nickname        VARCHAR(50)     DEFAULT NULL,
    avatar          VARCHAR(255)    DEFAULT NULL,
    email           VARCHAR(100)    DEFAULT NULL,
    phone           VARCHAR(20)     DEFAULT NULL,
    status          TINYINT         NOT NULL DEFAULT 1,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL,
    code            VARCHAR(50)     NOT NULL                COMMENT '角色编码',
    name            VARCHAR(50)     NOT NULL,
    description     VARCHAR(255)    DEFAULT NULL,
    status          TINYINT         NOT NULL DEFAULT 1,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台角色';

DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id              BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    code            VARCHAR(100)    NOT NULL                COMMENT '权限编码',
    name            VARCHAR(50)     NOT NULL,
    type            TINYINT         NOT NULL                COMMENT '1菜单 2按钮 3接口',
    path            VARCHAR(255)    DEFAULT NULL,
    icon            VARCHAR(50)     DEFAULT NULL,
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台权限';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系';

DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id         BIGINT          NOT NULL,
    permission_id   BIGINT          NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系';

DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id              BIGINT          NOT NULL,
    admin_id        BIGINT          DEFAULT NULL            COMMENT '后台用户ID',
    username        VARCHAR(50)     DEFAULT NULL            COMMENT '后台账号',
    module          VARCHAR(80)     DEFAULT NULL            COMMENT '模块',
    operation       VARCHAR(120)    DEFAULT NULL            COMMENT '操作名称',
    request_method  VARCHAR(10)     NOT NULL                COMMENT '请求方法',
    request_uri     VARCHAR(255)    NOT NULL                COMMENT '请求地址',
    request_params  TEXT            DEFAULT NULL            COMMENT '请求参数',
    request_summary VARCHAR(500)    DEFAULT NULL            COMMENT '变更摘要',
    ip              VARCHAR(64)     DEFAULT NULL            COMMENT '客户端IP',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态 0失败 1成功',
    error_message   VARCHAR(500)    DEFAULT NULL            COMMENT '失败原因',
    cost_time       BIGINT          DEFAULT NULL            COMMENT '耗时(ms)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_admin_time (admin_id, create_time),
    KEY idx_module_time (module, create_time),
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';

DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id              BIGINT          NOT NULL,
    config_key      VARCHAR(100)    NOT NULL                COMMENT '配置键',
    config_name     VARCHAR(100)    NOT NULL                COMMENT '配置名称',
    config_value    VARCHAR(1000)   DEFAULT NULL            COMMENT '配置值',
    value_type      VARCHAR(20)     NOT NULL DEFAULT 'text' COMMENT '值类型 text number boolean json',
    group_code      VARCHAR(50)     NOT NULL DEFAULT 'basic' COMMENT '配置分组',
    description     VARCHAR(255)    DEFAULT NULL            COMMENT '说明',
    editable        TINYINT         NOT NULL DEFAULT 1      COMMENT '是否可编辑 0否 1是',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态 0停用 1启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_group_status (group_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 初始数据
-- =====================================================

-- 默认后台账号 admin / 123456 (BCrypt)
INSERT INTO sys_user (id, username, password, nickname, status)
VALUES (1, 'admin', '$2a$10$8DXa6SIE4bRJdMvthGIlGe8fIOoXBVQSPOT44tiOuH0Od04mnpaCW', '超级管理员', 1);

INSERT INTO sys_role (id, code, name) VALUES (1, 'ROLE_ADMIN', '超级管理员');
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 演示用户  test / 123456
INSERT INTO ums_user (id, username, password, nickname, status)
VALUES (1, 'test', '$2a$10$8DXa6SIE4bRJdMvthGIlGe8fIOoXBVQSPOT44tiOuH0Od04mnpaCW', '测试用户', 1);

-- 一级分类
INSERT INTO pms_category (id, parent_id, name, level, sort, show_status) VALUES
 (1, 0, '数码电器', 1, 1, 1),
 (2, 0, '服饰鞋包', 1, 2, 1),
 (3, 0, '美妆个护', 1, 3, 1),
 (4, 0, '食品生鲜', 1, 4, 1);
