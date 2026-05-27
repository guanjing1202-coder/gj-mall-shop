-- ================================================================
-- GJ Mall 测试数据注入脚本
-- 执行前确保已运行 01-schema.sql 建好表结构
-- 可重复执行（全部使用 INSERT IGNORE）
-- ================================================================

USE gj_mall;

-- ================================================================
-- 1. 品牌数据
-- ================================================================
INSERT IGNORE INTO pms_brand (id, name, logo, description, sort, show_status) VALUES
(210000000000000001, 'Apple',    'https://picsum.photos/seed/applebrand/200/200',   '美国苹果公司，全球最具创新力的科技品牌，旗下 iPhone、Mac、iPad 享誉全球', 1, 1),
(210000000000000002, 'Huawei',   'https://picsum.photos/seed/huaweibrand/200/200',  '华为技术有限公司，全球领先的ICT基础设施和智能终端提供商', 2, 1),
(210000000000000003, 'Samsung',  'https://picsum.photos/seed/samsungbrand/200/200', '三星电子，韩国跨国消费电子巨头，全球最大智能手机及半导体制造商之一', 3, 1),
(210000000000000004, 'OPPO',     'https://picsum.photos/seed/oppobrand/200/200',    'OPPO，全球领先的智能设备品牌，以精湛工艺和创新影像技术享誉业界', 4, 1),
(210000000000000005, 'Lenovo',   'https://picsum.photos/seed/lenovobrand/200/200',  '联想集团，全球最大的PC制造商，ThinkPad系列深受商务人士青睐', 5, 1),
(210000000000000006, 'Sony',     'https://picsum.photos/seed/sonybrand/200/200',    '索尼，日本跨国企业，在消费电子、游戏、音乐和影视领域全球领先', 6, 1),
(210000000000000007, 'Nike',     'https://picsum.photos/seed/nikebrand/200/200',    '耐克，全球最大的运动品牌，Just Do It精神激励数亿运动爱好者', 7, 1),
(210000000000000008, 'Adidas',   'https://picsum.photos/seed/adidasbrand/200/200',  '阿迪达斯，德国运动品牌巨头，以三条杠标志享誉全球运动市场', 8, 1),
(210000000000000009, '李宁',     'https://picsum.photos/seed/liningbrand/200/200',  '李宁体育，中国最具代表性的国产运动品牌，"中国李宁"潮流风格深受年轻人喜爱', 9, 1),
(210000000000000010, '兰蔻',     'https://picsum.photos/seed/lancomebrand/200/200', 'Lancôme兰蔻，法国顶级化妆品品牌，以小黑瓶精华液享誉全球', 10, 1),
(210000000000000011, 'SK-II',    'https://picsum.photos/seed/skiibrand/200/200',    'SK-II，日本殿堂级护肤品牌，神仙水（Facial Treatment Essence）全球女性挚爱', 11, 1),
(210000000000000012, '雅诗兰黛', 'https://picsum.photos/seed/lauderbrand/200/200',  'Estée Lauder雅诗兰黛，美国高端美妆集团，小棕瓶精华全球热销', 12, 1),
(210000000000000013, '三只松鼠', 'https://picsum.photos/seed/squirrelbrand/200/200','三只松鼠，中国坚果零食领导品牌，主打高品质坚果炒货及休闲食品', 13, 1),
(210000000000000014, '元气森林', 'https://picsum.photos/seed/genkibrand/200/200',   '元气森林，中国新消费饮料品牌，气泡水等无糖饮料深受年轻消费者喜爱', 14, 1),
(210000000000000015, 'Dyson',    'https://picsum.photos/seed/dysonbrand/200/200',   '戴森，英国高端家电品牌，以吸尘器、吹风机、空气净化器等创新产品著称全球', 15, 1);

-- ================================================================
-- 2. 分类数据（二级子分类）
-- ================================================================
INSERT IGNORE INTO pms_category (id, parent_id, name, icon, level, sort, show_status) VALUES
-- 数码电器 (id=1) 子分类
(110000000000000001, 1, '平板电脑', 'tablet',     2, 3, 1),
(110000000000000002, 1, '耳机音频', 'headphones', 2, 4, 1),
(110000000000000003, 1, '智能穿戴', 'watch',      2, 5, 1),
(110000000000000004, 1, '数码配件', 'plug',       2, 6, 1),
-- 服饰鞋包 (id=2) 子分类
(110000000000000005, 2, '男装',    'shirt',       2, 1, 1),
(110000000000000006, 2, '女装',    'dress',       2, 2, 1),
(110000000000000007, 2, '运动鞋服','sneakers',    2, 3, 1),
-- 美妆个护 (id=3) 子分类
(110000000000000008, 3, '护肤精华','cream',       2, 1, 1),
(110000000000000009, 3, '彩妆香水','lipstick',    2, 2, 1),
(110000000000000010, 3, '美发护发','scissors',    2, 3, 1),
-- 食品生鲜 (id=4) 子分类
(110000000000000011, 4, '坚果零食','cookie',      2, 1, 1),
(110000000000000012, 4, '饮料酒水','bottle',      2, 2, 1),
(110000000000000013, 4, '生鲜水果','apple',       2, 3, 1);

-- ================================================================
-- 3. 更新现有小米14 Pro 数据
-- ================================================================
UPDATE pms_spu SET
  sub_title      = '第二代骁龙8 | 50MP徕卡光学镜头 | 4880mAh大电池',
  main_image     = 'https://picsum.photos/seed/mi14pro/600/600',
  images         = JSON_ARRAY(
    'https://picsum.photos/seed/mi14pro/600/600',
    'https://picsum.photos/seed/mi14pro2/600/600',
    'https://picsum.photos/seed/mi14pro3/600/600',
    'https://picsum.photos/seed/mi14pro4/600/600'
  ),
  detail_id      = 'detail_mi14pro',
  publish_status = 1,
  sale_count     = 2842,
  new_status     = 0,
  recommend_status = 1,
  sort           = 760
WHERE id = 2052032017358196737;

UPDATE pms_sku SET sale_count = 1854, cost_price = 3200 WHERE id = 2052032017421111298;
UPDATE pms_sku SET sale_count = 988,  cost_price = 3800 WHERE id = 2052032017488220161;

-- ================================================================
-- 4. 新增 SPU
-- ================================================================

INSERT IGNORE INTO pms_spu (id, name, sub_title, category_id, brand_id, main_image, images, detail_id, price, sale_count, publish_status) VALUES
-- 4-01 iPhone 16 Pro
(310000000000000001, 'iPhone 16 Pro',
  'A18 Pro芯片 | 钛金属边框 | 4K120fps超级视频录制',
  2052031956352045058, 210000000000000001,
  'https://picsum.photos/seed/iphone16pro/600/600',
  JSON_ARRAY('https://picsum.photos/seed/iphone16pro/600/600','https://picsum.photos/seed/iphone16proa/600/600','https://picsum.photos/seed/iphone16prob/600/600','https://picsum.photos/seed/iphone16proc/600/600'),
  'detail_iphone16pro', 7999.00, 5621, 1),

-- 4-02 华为 Mate 70 Pro
(310000000000000002, '华为 Mate 70 Pro',
  '麒麟9010 | 卫星通信 | 超级变焦影像系统',
  2052031956352045058, 210000000000000002,
  'https://picsum.photos/seed/mate70pro/600/600',
  JSON_ARRAY('https://picsum.photos/seed/mate70pro/600/600','https://picsum.photos/seed/mate70proa/600/600','https://picsum.photos/seed/mate70prob/600/600','https://picsum.photos/seed/mate70proc/600/600'),
  'detail_mate70pro', 5999.00, 3280, 1),

-- 4-03 三星 Galaxy S25 Ultra
(310000000000000003, '三星 Galaxy S25 Ultra',
  'Galaxy AI | 2亿像素 | S Pen手写笔 | 钛金属机身',
  2052031956352045058, 210000000000000003,
  'https://picsum.photos/seed/s25ultra/600/600',
  JSON_ARRAY('https://picsum.photos/seed/s25ultra/600/600','https://picsum.photos/seed/s25ultraa/600/600','https://picsum.photos/seed/s25ultrab/600/600','https://picsum.photos/seed/s25ultrac/600/600'),
  'detail_s25ultra', 8999.00, 2156, 1),

-- 4-04 OPPO Find X8 Pro
(310000000000000004, 'OPPO Find X8 Pro',
  '天玑9400 | 哈苏影像 | 5600mAh | 100W超级闪充',
  2052031956352045058, 210000000000000004,
  'https://picsum.photos/seed/findx8pro/600/600',
  JSON_ARRAY('https://picsum.photos/seed/findx8pro/600/600','https://picsum.photos/seed/findx8proa/600/600','https://picsum.photos/seed/findx8prob/600/600','https://picsum.photos/seed/findx8proc/600/600'),
  'detail_findx8pro', 5499.00, 1890, 1),

-- 4-05 MacBook Pro 16
(310000000000000005, 'MacBook Pro 16英寸 M4 Pro',
  'M4 Pro芯片 | Liquid Retina XDR显示屏 | 24GB统一内存',
  2052031957685833729, 210000000000000001,
  'https://picsum.photos/seed/mbp16m4/600/600',
  JSON_ARRAY('https://picsum.photos/seed/mbp16m4/600/600','https://picsum.photos/seed/mbp16m4a/600/600','https://picsum.photos/seed/mbp16m4b/600/600','https://picsum.photos/seed/mbp16m4c/600/600'),
  'detail_mbp16m4', 19999.00, 3210, 1),

-- 4-06 联想小新 Pro 16
(310000000000000006, '联想小新 Pro 16 2025',
  '酷睿Ultra 9 | 2.5K 165Hz | 32GB大内存 | 轻薄高性能',
  2052031957685833729, 210000000000000005,
  'https://picsum.photos/seed/lenovoxpro16/600/600',
  JSON_ARRAY('https://picsum.photos/seed/lenovoxpro16/600/600','https://picsum.photos/seed/lenovoxpro16a/600/600','https://picsum.photos/seed/lenovoxpro16b/600/600','https://picsum.photos/seed/lenovoxpro16c/600/600'),
  'detail_lenovoxpro16', 5999.00, 4120, 1),

-- 4-07 AirPods Pro 3
(310000000000000007, 'AirPods Pro 3',
  'H3芯片 | 主动降噪 | 空间音频 | 33小时续航',
  110000000000000002, 210000000000000001,
  'https://picsum.photos/seed/airpodspro3/600/600',
  JSON_ARRAY('https://picsum.photos/seed/airpodspro3/600/600','https://picsum.photos/seed/airpodspro3a/600/600','https://picsum.photos/seed/airpodspro3b/600/600'),
  'detail_airpodspro3', 1899.00, 8920, 1),

-- 4-08 索尼 WH-1000XM6
(310000000000000008, '索尼 WH-1000XM6',
  '行业领先降噪 | LDAC高解析 | 30小时续航 | 折叠便携',
  110000000000000002, 210000000000000006,
  'https://picsum.photos/seed/sonyxm6/600/600',
  JSON_ARRAY('https://picsum.photos/seed/sonyxm6/600/600','https://picsum.photos/seed/sonyxm6a/600/600','https://picsum.photos/seed/sonyxm6b/600/600'),
  'detail_sonyxm6', 2699.00, 3560, 1),

-- 4-09 Apple Watch Series 10
(310000000000000009, 'Apple Watch Series 10',
  '全新超薄机身 | 更大显示屏 | 睡眠呼吸暂停检测',
  110000000000000003, 210000000000000001,
  'https://picsum.photos/seed/applewatch10/600/600',
  JSON_ARRAY('https://picsum.photos/seed/applewatch10/600/600','https://picsum.photos/seed/applewatch10a/600/600','https://picsum.photos/seed/applewatch10b/600/600','https://picsum.photos/seed/applewatch10c/600/600'),
  'detail_applewatch10', 2999.00, 4230, 1),

-- 4-10 Dyson Airwrap
(310000000000000010, 'Dyson Airwrap 多功能造型器',
  '超音速气流 | 卷发/直发/吹发三合一 | 护发不伤发',
  110000000000000010, 210000000000000015,
  'https://picsum.photos/seed/dysonairwrap/600/600',
  JSON_ARRAY('https://picsum.photos/seed/dysonairwrap/600/600','https://picsum.photos/seed/dysonairwrapa/600/600','https://picsum.photos/seed/dysonairwrapb/600/600','https://picsum.photos/seed/dysonairwrapc/600/600'),
  'detail_dysonairwrap', 3590.00, 2860, 1),

-- 4-11 Nike Air Jordan 1
(310000000000000011, 'Nike Air Jordan 1 Retro High OG',
  '经典芝加哥配色 | 真皮鞋面 | Air-Sole气垫 | 限量复刻',
  110000000000000007, 210000000000000007,
  'https://picsum.photos/seed/airjordan1/600/600',
  JSON_ARRAY('https://picsum.photos/seed/airjordan1/600/600','https://picsum.photos/seed/airjordan1a/600/600','https://picsum.photos/seed/airjordan1b/600/600','https://picsum.photos/seed/airjordan1c/600/600'),
  'detail_airjordan1', 1299.00, 6810, 1),

-- 4-12 Adidas Ultraboost 24
(310000000000000012, 'Adidas Ultraboost 24',
  'BOOST中底回弹 | Primeknit编织鞋面 | 马拉松竞速跑鞋',
  110000000000000007, 210000000000000008,
  'https://picsum.photos/seed/ultraboost24/600/600',
  JSON_ARRAY('https://picsum.photos/seed/ultraboost24/600/600','https://picsum.photos/seed/ultraboost24a/600/600','https://picsum.photos/seed/ultraboost24b/600/600'),
  'detail_ultraboost24', 1099.00, 3420, 1),

-- 4-13 SK-II 神仙水
(310000000000000013, 'SK-II 神仙水精华露',
  '90%+ PITERA™精华 | 烟酰胺 | 改善肌肤纹理 | 日本进口',
  110000000000000008, 210000000000000011,
  'https://picsum.photos/seed/skiifacial/600/600',
  JSON_ARRAY('https://picsum.photos/seed/skiifacial/600/600','https://picsum.photos/seed/skiifaciala/600/600','https://picsum.photos/seed/skiifacialb/600/600'),
  'detail_skiifacial', 1090.00, 5620, 1),

-- 4-14 兰蔻小黑瓶
(310000000000000014, '兰蔻小黑瓶精华液',
  '夜间修护 | Pro-Xylane™ | 透明质酸 | 淡纹嫩肤 | 法国原装进口',
  110000000000000008, 210000000000000010,
  'https://picsum.photos/seed/lancomebb/600/600',
  JSON_ARRAY('https://picsum.photos/seed/lancomebb/600/600','https://picsum.photos/seed/lancomebbe/600/600','https://picsum.photos/seed/lancomebbf/600/600'),
  'detail_lancomebb', 1190.00, 4230, 1),

-- 4-15 三只松鼠
(310000000000000015, '三只松鼠 每日坚果大礼包',
  '8种坚果混合 | 碧根果+腰果+夏威夷果+巴旦木 | 独立小包装',
  110000000000000011, 210000000000000013,
  'https://picsum.photos/seed/squirrelnut/600/600',
  JSON_ARRAY('https://picsum.photos/seed/squirrelnut/600/600','https://picsum.photos/seed/squirrelnuta/600/600','https://picsum.photos/seed/squirrelnutb/600/600'),
  'detail_squirrelnut', 199.00, 12480, 1);

UPDATE pms_spu SET
  new_status = CASE id
    WHEN 310000000000000001 THEN 1
    WHEN 310000000000000003 THEN 1
    WHEN 310000000000000005 THEN 1
    WHEN 310000000000000009 THEN 1
    ELSE 0
  END,
  recommend_status = CASE id
    WHEN 310000000000000001 THEN 1
    WHEN 310000000000000005 THEN 1
    WHEN 310000000000000007 THEN 1
    WHEN 310000000000000011 THEN 1
    WHEN 310000000000000013 THEN 1
    WHEN 310000000000000015 THEN 1
    ELSE 0
  END,
  sort = CASE id
    WHEN 310000000000000001 THEN 980
    WHEN 310000000000000005 THEN 960
    WHEN 310000000000000007 THEN 930
    WHEN 310000000000000013 THEN 900
    WHEN 310000000000000015 THEN 880
    WHEN 310000000000000003 THEN 850
    WHEN 310000000000000009 THEN 820
    ELSE 100
  END
WHERE id BETWEEN 310000000000000001 AND 310000000000000015;

-- ================================================================
-- 5. 新增 SKU
-- ================================================================

-- iPhone 16 Pro SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000001, 310000000000000001, 'SKU-IP16P-BLK-128', '黑色钛金属 / 128GB', 'https://picsum.photos/seed/iphone16pro/600/600',   7999.00, 5200, 300, '{"颜色":"黑色钛金属","存储":"128GB"}', 2100),
(410000000000000002, 310000000000000001, 'SKU-IP16P-NAT-256', '原色钛金属 / 256GB', 'https://picsum.photos/seed/iphone16proa/600/600',  8999.00, 5800, 250, '{"颜色":"原色钛金属","存储":"256GB"}', 1800),
(410000000000000003, 310000000000000001, 'SKU-IP16P-WHT-512', '白色钛金属 / 512GB', 'https://picsum.photos/seed/iphone16prob/600/600', 10999.00, 7000, 120, '{"颜色":"白色钛金属","存储":"512GB"}',  980),
(410000000000000004, 310000000000000001, 'SKU-IP16P-DST-1TB', '沙漠色钛金属 / 1TB', 'https://picsum.photos/seed/iphone16proc/600/600', 13999.00, 9000,  60, '{"颜色":"沙漠色钛金属","存储":"1TB"}',   741);

-- 华为 Mate 70 Pro SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000005, 310000000000000002, 'SKU-MT70P-BLK-256', '砚黑 / 12+256G',  'https://picsum.photos/seed/mate70pro/600/600',  5999.00, 3800, 200, '{"颜色":"砚黑","内存":"12+256G"}',  1500),
(410000000000000006, 310000000000000002, 'SKU-MT70P-WHT-512', '宣白 / 12+512G',  'https://picsum.photos/seed/mate70proa/600/600', 6999.00, 4400, 150, '{"颜色":"宣白","内存":"12+512G"}',  1100),
(410000000000000007, 310000000000000002, 'SKU-MT70P-PUR-512', '龙晶紫 / 16+512G','https://picsum.photos/seed/mate70prob/600/600', 7499.00, 4800,  80, '{"颜色":"龙晶紫","内存":"16+512G"}',  680);

-- 三星 Galaxy S25 Ultra SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000008, 310000000000000003, 'SKU-S25U-BLK-256', '幻影黑 / 12+256G', 'https://picsum.photos/seed/s25ultra/600/600',  8999.00, 5800, 150, '{"颜色":"幻影黑","内存":"12+256G"}', 1200),
(410000000000000009, 310000000000000003, 'SKU-S25U-WHT-512', '皎月白 / 12+512G', 'https://picsum.photos/seed/s25ultraa/600/600',10499.00, 6800, 100, '{"颜色":"皎月白","内存":"12+512G"}',  956);

-- OPPO Find X8 Pro SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000010, 310000000000000004, 'SKU-FX8P-PUR-256', '星云紫 / 12+256G', 'https://picsum.photos/seed/findx8pro/600/600', 5499.00, 3500, 200, '{"颜色":"星云紫","内存":"12+256G"}', 1100),
(410000000000000011, 310000000000000004, 'SKU-FX8P-WHT-512', '流光白 / 16+512G', 'https://picsum.photos/seed/findx8proa/600/600',6499.00, 4100, 130, '{"颜色":"流光白","内存":"16+512G"}',  790);

-- MacBook Pro 16 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000012, 310000000000000005, 'SKU-MBP16-SLV-M4P-24-512', '银色 / M4 Pro / 24G / 512G',    'https://picsum.photos/seed/mbp16m4/600/600', 19999.00, 13000,  80, '{"颜色":"银色","芯片":"M4 Pro","内存":"24GB","存储":"512GB SSD"}', 1580),
(410000000000000013, 310000000000000005, 'SKU-MBP16-BLK-M4P-48-1T',  '深空黑色 / M4 Pro / 48G / 1TB', 'https://picsum.photos/seed/mbp16m4a/600/600',26999.00, 17500,  50, '{"颜色":"深空黑色","芯片":"M4 Pro","内存":"48GB","存储":"1TB SSD"}',  980),
(410000000000000014, 310000000000000005, 'SKU-MBP16-SLV-M4X-64-1T',  '银色 / M4 Max / 64G / 1TB',     'https://picsum.photos/seed/mbp16m4b/600/600',34999.00, 22000,  30, '{"颜色":"银色","芯片":"M4 Max","内存":"64GB","存储":"1TB SSD"}',    650);

-- 联想小新 Pro 16 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000015, 310000000000000006, 'SKU-XNP16-GRY-U7-16-512', '绅士灰 / Ultra 7 / 16G / 512G', 'https://picsum.photos/seed/lenovoxpro16/600/600', 5999.00, 3800, 250, '{"颜色":"绅士灰","处理器":"Core Ultra 7","内存":"16GB","存储":"512GB"}', 2200),
(410000000000000016, 310000000000000006, 'SKU-XNP16-BLU-U9-32-1T',  '远山蓝 / Ultra 9 / 32G / 1TB',  'https://picsum.photos/seed/lenovoxpro16a/600/600',8999.00, 5800, 150, '{"颜色":"远山蓝","处理器":"Core Ultra 9","内存":"32GB","存储":"1TB"}',  1920);

-- AirPods Pro 3 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000017, 310000000000000007, 'SKU-APP3-WHT-USBC',  '白色 / USB-C充电盒',  'https://picsum.photos/seed/airpodspro3/600/600', 1899.00, 1100, 500, '{"颜色":"白色","充电":"USB-C"}',   6200),
(410000000000000018, 310000000000000007, 'SKU-APP3-WHT-MSAFE', '白色 / MagSafe充电盒','https://picsum.photos/seed/airpodspro3a/600/600',2099.00, 1300, 300, '{"颜色":"白色","充电":"MagSafe"}', 2720);

-- 索尼 WH-1000XM6 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000019, 310000000000000008, 'SKU-XM6-SLV', '铂金银', 'https://picsum.photos/seed/sonyxm6/600/600',  2699.00, 1600, 200, '{"颜色":"铂金银"}', 1800),
(410000000000000020, 310000000000000008, 'SKU-XM6-BLK', '午夜黑', 'https://picsum.photos/seed/sonyxm6a/600/600', 2699.00, 1600, 200, '{"颜色":"午夜黑"}', 1760);

-- Apple Watch Series 10 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000021, 310000000000000009, 'SKU-AW10-STL-42',  '星光色铝金属 / 42mm', 'https://picsum.photos/seed/applewatch10/600/600',  2999.00, 1800, 300, '{"颜色":"星光色","材质":"铝金属","尺寸":"42mm"}', 2100),
(410000000000000022, 310000000000000009, 'SKU-AW10-BLK-46',  '午夜色铝金属 / 46mm', 'https://picsum.photos/seed/applewatch10a/600/600', 3499.00, 2100, 250, '{"颜色":"午夜色","材质":"铝金属","尺寸":"46mm"}', 1500),
(410000000000000023, 310000000000000009, 'SKU-AW10-SS-42',   '银色不锈钢 / 42mm',   'https://picsum.photos/seed/applewatch10b/600/600', 4999.00, 3000, 100, '{"颜色":"银色","材质":"不锈钢","尺寸":"42mm"}',   630);

-- Dyson Airwrap SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000024, 310000000000000010, 'SKU-DSAW-PUR', '紫红色 / 完整造型套装', 'https://picsum.photos/seed/dysonairwrap/600/600',  3590.00, 2200, 200, '{"颜色":"紫红色","套装":"完整造型套装"}', 1600),
(410000000000000025, 310000000000000010, 'SKU-DSAW-NKL', '镍银色 / 完整造型套装', 'https://picsum.photos/seed/dysonairwrapa/600/600', 3590.00, 2200, 150, '{"颜色":"镍银色","套装":"完整造型套装"}', 1260);

-- Nike Air Jordan 1 SKU（多尺码）
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000026, 310000000000000011, 'SKU-AJ1-RED-40', '黑红芝加哥 / 40码', 'https://picsum.photos/seed/airjordan1/600/600',  1299.00, 750,  60, '{"配色":"黑红芝加哥","尺码":"40"}',  780),
(410000000000000027, 310000000000000011, 'SKU-AJ1-RED-41', '黑红芝加哥 / 41码', 'https://picsum.photos/seed/airjordan1/600/600',  1299.00, 750,  80, '{"配色":"黑红芝加哥","尺码":"41"}', 1020),
(410000000000000028, 310000000000000011, 'SKU-AJ1-RED-42', '黑红芝加哥 / 42码', 'https://picsum.photos/seed/airjordan1/600/600',  1299.00, 750, 100, '{"配色":"黑红芝加哥","尺码":"42"}', 1380),
(410000000000000029, 310000000000000011, 'SKU-AJ1-RED-43', '黑红芝加哥 / 43码', 'https://picsum.photos/seed/airjordan1/600/600',  1299.00, 750,  80, '{"配色":"黑红芝加哥","尺码":"43"}', 1100),
(410000000000000030, 310000000000000011, 'SKU-AJ1-WHT-42', '黑白熊猫 / 42码',   'https://picsum.photos/seed/airjordan1a/600/600', 1399.00, 820,  80, '{"配色":"黑白熊猫","尺码":"42"}',  2530);

-- Adidas Ultraboost 24 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000031, 310000000000000012, 'SKU-UB24-BLK-41', '核心黑 / 41码', 'https://picsum.photos/seed/ultraboost24/600/600',  1099.00, 650, 100, '{"配色":"核心黑","尺码":"41"}',  780),
(410000000000000032, 310000000000000012, 'SKU-UB24-BLK-42', '核心黑 / 42码', 'https://picsum.photos/seed/ultraboost24/600/600',  1099.00, 650, 120, '{"配色":"核心黑","尺码":"42"}', 1020),
(410000000000000033, 310000000000000012, 'SKU-UB24-WHT-42', '跑步白 / 42码', 'https://picsum.photos/seed/ultraboost24a/600/600', 1099.00, 650, 100, '{"配色":"跑步白","尺码":"42"}',  920),
(410000000000000034, 310000000000000012, 'SKU-UB24-WHT-43', '跑步白 / 43码', 'https://picsum.photos/seed/ultraboost24a/600/600', 1099.00, 650,  80, '{"配色":"跑步白","尺码":"43"}',  700);

-- SK-II 神仙水 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000035, 310000000000000013, 'SKU-SKII-160', '神仙水精华露 / 160ml',       'https://picsum.photos/seed/skiifacial/600/600',  1090.00, 600, 200, '{"规格":"160ml"}', 3200),
(410000000000000036, 310000000000000013, 'SKU-SKII-230', '神仙水精华露 / 230ml',       'https://picsum.photos/seed/skiifaciala/600/600', 1690.00, 900, 150, '{"规格":"230ml"}', 1800),
(410000000000000037, 310000000000000013, 'SKU-SKII-330', '神仙水精华露 / 330ml 旅行套装','https://picsum.photos/seed/skiifacialb/600/600',2190.00,1200,  80, '{"规格":"330ml旅行套装"}', 620);

-- 兰蔻小黑瓶 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000038, 310000000000000014, 'SKU-LAN-BB-30',  '小黑瓶精华 / 30ml 体验装', 'https://picsum.photos/seed/lancomebb/600/600',   590.00, 320, 200, '{"规格":"30ml"}',         1800),
(410000000000000039, 310000000000000014, 'SKU-LAN-BB-50',  '小黑瓶精华 / 50ml',        'https://picsum.photos/seed/lancomebbe/600/600', 1190.00, 650, 150, '{"规格":"50ml"}',         1860),
(410000000000000040, 310000000000000014, 'SKU-LAN-BB-100', '小黑瓶精华 / 100ml 礼盒',  'https://picsum.photos/seed/lancomebbf/600/600', 1890.00,1050,  80, '{"规格":"100ml礼盒装"}',   570);

-- 三只松鼠 SKU
INSERT IGNORE INTO pms_sku (id, spu_id, sku_code, name, image, price, cost_price, stock, spec_data, sale_count) VALUES
(410000000000000041, 310000000000000015, 'SKU-SQN-STD-750',  '标准装 / 750g（30袋）',     'https://picsum.photos/seed/squirrelnut/600/600',  199.00, 110, 500, '{"规格":"750g","数量":"30袋"}',   7200),
(410000000000000042, 310000000000000015, 'SKU-SQN-LUX-1250', '豪华礼盒装 / 1250g（50袋）','https://picsum.photos/seed/squirrelnuta/600/600', 299.00, 170, 300, '{"规格":"1250g","数量":"50袋"}', 4280),
(410000000000000043, 310000000000000015, 'SKU-SQN-FAM-2500', '家庭超值装 / 2500g（100袋）','https://picsum.photos/seed/squirrelnutb/600/600',499.00, 280, 200, '{"规格":"2500g","数量":"100袋"}',1000);

UPDATE pms_sku SET
  warn_stock = CASE id
    WHEN 410000000000000001 THEN 350
    WHEN 410000000000000012 THEN 90
    WHEN 410000000000000014 THEN 45
    WHEN 410000000000000026 THEN 80
    WHEN 410000000000000037 THEN 100
    ELSE 10
  END
WHERE id BETWEEN 410000000000000001 AND 410000000000000043;

-- ================================================================
-- 6. 优惠券
-- ================================================================
INSERT IGNORE INTO sms_coupon (id, name, type, min_amount, discount_amount, discount_rate, start_time, end_time, total_count, received_count, used_count, status, deleted) VALUES
(510000000000000001, '新人专享50元券',  3,    0.00,  50.00,  NULL, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 99999, 0, 0, 1, 0),
(510000000000000002, '满500减80元',     1,  500.00,  80.00,  NULL, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 10000, 0, 0, 1, 0),
(510000000000000003, '满1000减150元',   1, 1000.00, 150.00,  NULL, '2026-01-01 00:00:00', '2027-12-31 23:59:59',  5000, 0, 0, 1, 0),
(510000000000000004, '全场九折优惠',    2,  100.00,   NULL,   0.9, '2026-01-01 00:00:00', '2027-12-31 23:59:59',  8000, 0, 0, 1, 0),
(510000000000000005, '数码专区85折',    2,  500.00,   NULL,  0.85, '2026-01-01 00:00:00', '2027-12-31 23:59:59',  3000, 0, 0, 1, 0);

-- ================================================================
-- 7. 会员收藏
-- ================================================================
INSERT INTO ums_user_favorite (id, user_id, spu_id, deleted) VALUES
(710000000000000001, 1, 310000000000000001, 0),
(710000000000000002, 1, 310000000000000002, 0),
(710000000000000003, 1, 310000000000000007, 0)
ON DUPLICATE KEY UPDATE deleted = VALUES(deleted);

-- ================================================================
-- 10. 商品评价数据
-- ================================================================
INSERT INTO pms_product_comment (
  id, order_id, order_no, order_item_id, user_id, spu_id, sku_id, score,
  content, images, status, audit_remark, audit_time, reply_content, reply_time, deleted
) VALUES
(720000000000000001, NULL, NULL, NULL, 1, 310000000000000001, 410000000000000002, 5,
 '手机质感很好，拍照和续航都比预期稳，物流也很快。', JSON_ARRAY('https://picsum.photos/seed/comment-ip16-1/600/600'), 1,
 '测试数据默认通过', NOW(), '感谢支持，后续使用有问题可以随时联系售后。', NOW(), 0),
(720000000000000002, NULL, NULL, NULL, 1, 310000000000000005, 410000000000000014, 4,
 '性能非常强，屏幕观感舒服，就是机身有点重。', NULL, 0,
 NULL, NULL, NULL, NULL, 0),
(720000000000000003, NULL, NULL, NULL, 1, 310000000000000007, 410000000000000017, 5,
 '降噪效果明显，通勤很好用，佩戴也轻。', JSON_ARRAY('https://picsum.photos/seed/comment-airpods-1/600/600','https://picsum.photos/seed/comment-airpods-2/600/600'), 3,
 '含晒单图，暂时隐藏用于后台演示', NOW(), NULL, NULL, 0),
(720000000000000004, NULL, NULL, NULL, 1, 310000000000000013, 410000000000000037, 2,
 '包装有压痕，希望改进发货包装。', NULL, 2,
 '测试驳回记录', NOW(), NULL, NULL, 0)
ON DUPLICATE KEY UPDATE
  order_id = VALUES(order_id),
  order_no = VALUES(order_no),
  order_item_id = VALUES(order_item_id),
  user_id = VALUES(user_id),
  spu_id = VALUES(spu_id),
  sku_id = VALUES(sku_id),
  score = VALUES(score),
  content = VALUES(content),
  images = VALUES(images),
  status = VALUES(status),
  audit_remark = VALUES(audit_remark),
  audit_time = VALUES(audit_time),
  reply_content = VALUES(reply_content),
  reply_time = VALUES(reply_time),
  deleted = VALUES(deleted);

-- ================================================================
-- 11. 后台账号 / 角色 / 权限
-- ================================================================
INSERT INTO sys_user (
  id, username, password, nickname, email, phone, status, deleted
) VALUES (
  1, 'admin', '$2a$10$8DXa6SIE4bRJdMvthGIlGe8fIOoXBVQSPOT44tiOuH0Od04mnpaCW',
  '超级管理员', 'admin@gj-mall.local', '13800000000', 1, 0
) ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  email = VALUES(email),
  phone = VALUES(phone),
  status = VALUES(status),
  deleted = VALUES(deleted);

INSERT INTO sys_role (id, code, name, description, status, deleted) VALUES
(1, 'ROLE_ADMIN', '超级管理员', '拥有全部后台权限', 1, 0),
(2, 'ROLE_OPERATOR', '运营专员', '商品、订单、营销和售后运营权限', 1, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  status = VALUES(status),
  deleted = VALUES(deleted);

INSERT INTO sys_permission (id, parent_id, code, name, type, path, icon, sort, deleted) VALUES
(910000000000000001, 0, 'menu.dashboard', '仪表盘', 1, '/dashboard', 'dashboard', 10, 0),
(910000000000000010, 0, 'menu.product', '商品中心', 1, '/product', 'shopping', 20, 0),
(910000000000000011, 910000000000000010, 'product.spu.manage', '商品管理', 2, '/product', NULL, 21, 0),
(910000000000000012, 910000000000000010, 'product.brand.manage', '品牌管理', 2, '/product-brand', NULL, 22, 0),
(910000000000000013, 910000000000000010, 'product.category.manage', '分类管理', 2, '/product-category', NULL, 23, 0),
(910000000000000014, 910000000000000010, 'product.inventory.manage', '库存管理', 2, '/product-inventory', NULL, 24, 0),
(910000000000000015, 910000000000000010, 'product.operation.manage', '商品运营', 2, '/product-operation', NULL, 25, 0),
(910000000000000016, 910000000000000010, 'product.comment.manage', '评价评论', 2, '/product-comment', NULL, 26, 0),
(910000000000000020, 0, 'menu.order', '订单中心', 1, '/order', 'order', 30, 0),
(910000000000000021, 910000000000000020, 'order.manage', '订单管理', 2, '/order', NULL, 31, 0),
(910000000000000022, 910000000000000020, 'payment.manage', '支付退款', 2, '/payment', NULL, 32, 0),
(910000000000000023, 910000000000000020, 'after.sale.manage', '售后退货', 2, '/after-sale', NULL, 33, 0),
(910000000000000024, 910000000000000020, 'logistics.manage', '物流发货', 2, '/logistics', NULL, 34, 0),
(910000000000000030, 0, 'menu.marketing', '营销中心', 1, '/marketing-coupon', 'gift', 40, 0),
(910000000000000031, 910000000000000030, 'coupon.manage', '优惠券管理', 2, '/marketing-coupon', NULL, 41, 0),
(910000000000000032, 910000000000000030, 'seckill.manage', '秒杀活动', 2, '/marketing-seckill', NULL, 42, 0),
(910000000000000040, 0, 'menu.member', '会员中心', 1, '/member', 'user', 50, 0),
(910000000000000041, 910000000000000040, 'member.manage', '会员管理', 2, '/member', NULL, 51, 0),
(910000000000000042, 910000000000000040, 'member.favorite.manage', '会员收藏', 2, '/member-favorite', NULL, 52, 0),
(910000000000000050, 0, 'menu.system', '系统管理', 1, '/user', 'setting', 90, 0),
(910000000000000051, 910000000000000050, 'sys.user.manage', '后台账号', 2, '/user', NULL, 91, 0),
(910000000000000052, 910000000000000050, 'sys.role.manage', '角色权限', 2, '/role-permission', NULL, 92, 0),
(910000000000000053, 910000000000000050, 'sys.operation.log.manage', '操作日志', 2, '/operation-log', NULL, 93, 0),
(910000000000000054, 910000000000000050, 'sys.config.manage', '系统配置', 2, '/system-config', NULL, 94, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  type = VALUES(type),
  path = VALUES(path),
  icon = VALUES(icon),
  sort = VALUES(sort),
  deleted = VALUES(deleted);

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE deleted = 0
  AND code IN (
    'menu.dashboard',
    'menu.product',
    'product.spu.manage',
    'product.brand.manage',
    'product.category.manage',
    'product.inventory.manage',
    'product.operation.manage',
    'product.comment.manage',
    'menu.order',
    'order.manage',
    'logistics.manage',
    'payment.manage',
    'after.sale.manage',
    'menu.marketing',
    'coupon.manage',
    'seckill.manage'
  );

INSERT INTO sys_config (id, config_key, config_name, config_value, value_type, group_code, description, editable, status, deleted) VALUES
(920000000000000001, 'mall.site.name', '商城名称', 'GJ Mall', 'text', 'basic', '展示在商城前台与后台的商城名称', 1, 1, 0),
(920000000000000002, 'mall.customer.service.phone', '客服电话', '400-800-2026', 'text', 'basic', '售后与订单咨询客服电话', 1, 1, 0),
(920000000000000003, 'mall.maintenance.enabled', '维护模式', 'false', 'boolean', 'basic', '开启后可用于前台维护提示', 1, 1, 0),
(920000000000000004, 'stock.warning.default', '默认库存预警阈值', '20', 'number', 'product', '商品库存预警默认阈值', 1, 1, 0),
(920000000000000005, 'order.auto.cancel.minutes', '未支付订单自动关闭分钟', '30', 'number', 'order', '订单超时未支付后的自动关闭时间', 1, 1, 0),
(920000000000000006, 'order.auto.receive.days', '自动确认收货天数', '7', 'number', 'order', '发货后自动确认收货的天数', 1, 1, 0),
(920000000000000009, 'order.freight.base', '基础运费', '12', 'number', 'order', '未满足免邮门槛时收取的基础运费', 1, 1, 0),
(920000000000000010, 'order.freight.free.threshold', '免基础运费门槛', '99', 'number', 'order', '商品金额达到该值后免基础运费', 1, 1, 0),
(920000000000000011, 'order.freight.remote.extra', '偏远地区附加运费', '18', 'number', 'order', '偏远地区在基础运费外加收的附加运费', 1, 1, 0),
(920000000000000012, 'order.freight.remote.provinces', '偏远地区省份', '["新疆维吾尔自治区","西藏自治区","青海省","宁夏回族自治区","内蒙古自治区","海南省"]', 'json', 'order', '收货省份匹配后加收偏远地区运费', 1, 1, 0),
(920000000000000013, 'mall.pay.mode', '支付模式', 'mock', 'text', 'payment', 'mock 为开发模拟支付，real 为真实渠道支付', 1, 1, 0),
(920000000000000014, 'mall.pay.callback.require-signature', '支付回调强制验签', 'false', 'boolean', 'payment', '真实支付模式建议开启，开发联调可关闭', 1, 1, 0),
(920000000000000015, 'mall.pay.callback.secret', '开发回调验签密钥', 'gj-mall-dev-pay-callback-secret', 'text', 'payment', 'Mock/开发回调用 HMAC 密钥，生产真实渠道以 SDK 验签为准', 1, 1, 0),
(920000000000000016, 'mall.pay.wechat.app-id', '微信支付 AppID', '', 'text', 'payment', '微信支付商户应用 AppID', 1, 1, 0),
(920000000000000017, 'mall.pay.wechat.mch-id', '微信支付商户号', '', 'text', 'payment', '微信支付商户号 MchID', 1, 1, 0),
(920000000000000018, 'mall.pay.wechat.api-v3-key', '微信支付 APIv3 密钥', '', 'text', 'payment', '微信支付 APIv3 密钥，生产环境请妥善保管', 1, 1, 0),
(920000000000000019, 'mall.pay.wechat.merchant-serial-no', '微信支付商户证书序列号', '', 'text', 'payment', '微信支付商户 API 证书序列号', 1, 1, 0),
(920000000000000020, 'mall.pay.wechat.private-key-path', '微信支付私钥路径', '', 'text', 'payment', '微信支付商户私钥文件路径', 1, 1, 0),
(920000000000000025, 'mall.pay.wechat.notify-url', '微信支付回调地址', '', 'text', 'payment', '微信支付支付结果通知地址', 1, 1, 0),
(920000000000000021, 'mall.pay.alipay.app-id', '支付宝 AppID', '', 'text', 'payment', '支付宝开放平台应用 AppID', 1, 1, 0),
(920000000000000022, 'mall.pay.alipay.private-key', '支付宝应用私钥', '', 'text', 'payment', '支付宝应用私钥，生产环境请妥善保管', 1, 1, 0),
(920000000000000023, 'mall.pay.alipay.alipay-public-key', '支付宝公钥', '', 'text', 'payment', '支付宝平台公钥', 1, 1, 0),
(920000000000000024, 'mall.pay.alipay.notify-url', '支付宝回调地址', '', 'text', 'payment', '支付宝异步通知 notify_url', 1, 1, 0),
(920000000000000007, 'marketing.coupon.visible', '优惠券入口可见', 'true', 'boolean', 'marketing', '控制前台优惠券入口展示状态', 1, 1, 0),
(920000000000000008, 'after.sale.window.days', '售后申请时效天数', '7', 'number', 'after_sale', '订单完成后允许发起售后的天数', 1, 1, 0)
ON DUPLICATE KEY UPDATE
  config_name = VALUES(config_name),
  config_value = VALUES(config_value),
  value_type = VALUES(value_type),
  group_code = VALUES(group_code),
  description = VALUES(description),
  editable = VALUES(editable),
  status = VALUES(status),
  deleted = VALUES(deleted);

INSERT INTO oms_delivery_company (id, code, name, contact_phone, sort, status, deleted) VALUES
(930000000000000001, 'SF', '顺丰速运', '95338', 10, 1, 0),
(930000000000000002, 'JD', '京东物流', '950616', 20, 1, 0),
(930000000000000003, 'ZTO', '中通快递', '95311', 30, 1, 0),
(930000000000000004, 'YTO', '圆通速递', '95554', 40, 1, 0),
(930000000000000005, 'YD', '韵达快递', '95546', 50, 1, 0),
(930000000000000006, 'STO', '申通快递', '95543', 60, 1, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  contact_phone = VALUES(contact_phone),
  sort = VALUES(sort),
  status = VALUES(status),
  deleted = VALUES(deleted);

SELECT CONCAT('✅ 测试数据注入完成：',
  (SELECT COUNT(*) FROM pms_brand WHERE deleted=0), ' 品牌 | ',
  (SELECT COUNT(*) FROM pms_category WHERE deleted=0), ' 分类 | ',
  (SELECT COUNT(*) FROM pms_spu WHERE deleted=0), ' SPU | ',
  (SELECT COUNT(*) FROM pms_sku WHERE deleted=0), ' SKU | ',
  (SELECT COUNT(*) FROM sms_coupon WHERE deleted=0), ' 优惠券 | ',
  (SELECT COUNT(*) FROM ums_user_favorite WHERE deleted=0), ' 收藏 | ',
  (SELECT COUNT(*) FROM pms_product_comment WHERE deleted=0), ' 评价 | ',
  (SELECT COUNT(*) FROM sys_config WHERE deleted=0), ' 配置 | ',
  (SELECT COUNT(*) FROM oms_delivery_company WHERE deleted=0), ' 物流公司'
) AS result;
