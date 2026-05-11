// ================================================================
// GJ Mall MongoDB 商品详情数据
// 执行方式: mongosh gj_mall_doc 03-mongo-details.js
// 或直接在 mongosh 中: load("D:/caludeWorkspace/gj-mall-shop/docs/db/03-mongo-details.js")
// ================================================================

db = db.getSiblingDB('gj_mall_doc');

// 清理旧数据（开发环境）
db.product_detail.deleteMany({});

const now = new Date();

db.product_detail.insertMany([

// ──────────────────────────────────────────────────────────────────
// 1. 小米14 Pro
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_mi14pro",
  spuId: NumberLong("2052032017358196737"),
  title: "小米14 Pro",
  description: "第二代骁龙8旗舰，徕卡光学镜头，120W旋转闪充，4880mAh超大电池",
  features: ["第二代骁龙8处理器，性能狂飙","徕卡光学镜头，专业影像体验","120W旋转闪充，23分钟满电","4880mAh大电池，续航无忧","IP68级防水防尘"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"小米14 Pro"},
      {label:"操作系统", value:"MIUI 15 (基于Android 14)"},
      {label:"机身颜色", value:"黑色 / 白色"},
      {label:"机身重量", value:"223g"},
      {label:"机身材质", value:"素皮/陶瓷后盖 + 铝合金边框"},
      {label:"SIM卡", value:"双卡双待 Nano SIM"}
    ]},
    { group: "屏幕", items: [
      {label:"屏幕尺寸", value:"6.73英寸"},
      {label:"屏幕材质", value:"LTPO OLED"},
      {label:"分辨率", value:"3200×1440 (522 PPI)"},
      {label:"刷新率", value:"1~120Hz自适应"},
      {label:"亮度", value:"最高3000nit"}
    ]},
    { group: "处理器", items: [
      {label:"芯片", value:"高通第二代骁龙8"},
      {label:"制程", value:"4nm"},
      {label:"CPU", value:"1×3.3GHz A+4×3.0GHz A+3×2.3GHz"},
      {label:"GPU", value:"Adreno 740"},
      {label:"运行内存", value:"12GB / 16GB LPDDR5X"},
      {label:"存储", value:"256GB / 512GB / 1TB UFS 4.0"}
    ]},
    { group: "影像", items: [
      {label:"主摄", value:"50MP 光圈f/1.42 徕卡正宗镜头"},
      {label:"超广角", value:"50MP 光圈f/2.2 超高清超广角"},
      {label:"长焦", value:"50MP 光圈f/2.0 75mm人像长焦"},
      {label:"前摄", value:"32MP 光圈f/2.0"},
      {label:"视频", value:"8K@24fps / 4K@60fps Dolby Vision"}
    ]},
    { group: "电池与充电", items: [
      {label:"电池容量", value:"4880mAh"},
      {label:"有线充电", value:"120W旋转闪充"},
      {label:"无线充电", value:"50W小米无线充电"},
      {label:"反向充电", value:"10W无线反充"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(135deg,#ff6900,#fc0);padding:48px 32px;border-radius:16px;text-align:center;margin-bottom:32px">
    <h1 style="color:#fff;font-size:36px;margin:0 0 12px;font-weight:700">小米14 Pro</h1>
    <p style="color:rgba(255,255,255,0.9);font-size:18px;margin:0">第二代骁龙8 · 徕卡光学 · 120W闪充</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:32px">
    <div style="background:#f5f5f5;border-radius:12px;padding:24px;text-align:center">
      <div style="font-size:32px;margin-bottom:8px">⚡</div>
      <div style="font-weight:700;font-size:16px">120W旋转闪充</div>
      <div style="color:#666;font-size:13px;margin-top:4px">23分钟满电</div>
    </div>
    <div style="background:#f5f5f5;border-radius:12px;padding:24px;text-align:center">
      <div style="font-size:32px;margin-bottom:8px">📷</div>
      <div style="font-weight:700;font-size:16px">徕卡光学镜头</div>
      <div style="color:#666;font-size:13px;margin-top:4px">专业级影像体验</div>
    </div>
    <div style="background:#f5f5f5;border-radius:12px;padding:24px;text-align:center">
      <div style="font-size:32px;margin-bottom:8px">🔥</div>
      <div style="font-weight:700;font-size:16px">骁龙8 Gen2</div>
      <div style="color:#666;font-size:13px;margin-top:4px">旗舰性能芯片</div>
    </div>
  </div>
  <div style="background:#fff7f0;border:1px solid #ffdcc2;border-radius:12px;padding:24px;margin-bottom:24px">
    <h3 style="color:#ff6900;margin:0 0 16px;font-size:18px">✨ 核心亮点</h3>
    <ul style="margin:0;padding-left:20px;line-height:2">
      <li>第二代骁龙8处理器，骁龙AI 10亿参数大模型</li>
      <li>徕卡50MP三摄系统，支持徕卡真实/徕卡鲜艳风格</li>
      <li>6.73英寸2K+ LTPO OLED，峰值亮度3000nit</li>
      <li>4880mAh大电池，IP68级防水防尘</li>
      <li>LPDDR5X + UFS 4.0，读写速度业界领先</li>
    </ul>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 2. iPhone 16 Pro
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_iphone16pro",
  spuId: NumberLong("310000000000000001"),
  title: "iPhone 16 Pro",
  description: "A18 Pro芯片，4K120fps超视频录制，钛金属边框，相机控制按钮",
  features: ["A18 Pro，最先进的手机芯片","相机控制按钮，全新操控方式","4K 120fps视频，超流畅超清晰","最大2TB存储","ProMotion自适应刷新率","Action按钮可自定义"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"iPhone 16 Pro"},
      {label:"操作系统", value:"iOS 18"},
      {label:"机身颜色", value:"黑色钛金属 / 原色钛金属 / 白色钛金属 / 沙漠色钛金属"},
      {label:"机身材质", value:"4级钛金属边框 + 哑光磨砂玻璃背板"},
      {label:"防水等级", value:"IP68（水深6米，时长30分钟）"},
      {label:"SIM卡", value:"eSIM（中国大陆限双eSIM）"}
    ]},
    { group: "屏幕", items: [
      {label:"屏幕尺寸", value:"6.3英寸"},
      {label:"屏幕材质", value:"Super Retina XDR OLED（ProMotion）"},
      {label:"分辨率", value:"2868×1320 (460 PPI)"},
      {label:"刷新率", value:"1~120Hz ProMotion自适应"},
      {label:"峰值亮度", value:"室外2000nit HDR，峰值4000nit"},
      {label:"Always-On", value:"支持"}
    ]},
    { group: "处理器", items: [
      {label:"芯片", value:"A18 Pro（3nm第二代）"},
      {label:"CPU核心", value:"6核（2性能+4能效）"},
      {label:"GPU核心", value:"6核"},
      {label:"神经网络引擎", value:"16核"},
      {label:"内存", value:"8GB"},
      {label:"存储", value:"128GB / 256GB / 512GB / 1TB"}
    ]},
    { group: "影像", items: [
      {label:"主摄", value:"48MP，f/1.78，第二代传感器位移式OIS"},
      {label:"超广角", value:"48MP，f/2.2，自动对焦"},
      {label:"长焦", value:"12MP，f/2.8，5倍光学变焦（120mm等效）"},
      {label:"前摄", value:"12MP TrueDepth，f/1.9"},
      {label:"视频", value:"4K@120fps Dolby Vision HDR"},
      {label:"特色功能", value:"相机控制按钮，Apple Intelligence摄影风格"}
    ]},
    { group: "电池与充电", items: [
      {label:"电池", value:"3582mAh不可拆卸锂离子电池"},
      {label:"有线充电", value:"USB-C，27W MagSafe"},
      {label:"无线充电", value:"MagSafe 25W / Qi2 15W"},
      {label:"续航", value:"官方最长33小时视频播放"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(160deg,#1c1c1e 0%,#2c2c2e 100%);padding:56px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:#f5a623;font-size:13px;letter-spacing:3px;text-transform:uppercase;margin:0 0 12px">Apple</p>
    <h1 style="color:#fff;font-size:42px;margin:0 0 12px;font-weight:700;letter-spacing:-1px">iPhone 16 Pro</h1>
    <p style="color:rgba(255,255,255,0.6);font-size:17px;margin:0">Hello, Apple Intelligence.</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:20px;margin-bottom:32px">
    <div style="background:#f5f5f7;border-radius:16px;padding:28px">
      <div style="font-size:36px;margin-bottom:12px">🎬</div>
      <h3 style="font-size:18px;margin:0 0 8px">4K 120fps</h3>
      <p style="color:#6e6e73;font-size:14px;margin:0">首款支持4K 120fps Dolby Vision HDR的iPhone，电影级画质触手可及</p>
    </div>
    <div style="background:#f5f5f7;border-radius:16px;padding:28px">
      <div style="font-size:36px;margin-bottom:12px">📸</div>
      <h3 style="font-size:18px;margin:0 0 8px">相机控制按钮</h3>
      <p style="color:#6e6e73;font-size:14px;margin:0">全新实体按钮，一触即拍，支持变焦、曝光调节，像随身携带了专业相机</p>
    </div>
    <div style="background:#f5f5f7;border-radius:16px;padding:28px">
      <div style="font-size:36px;margin-bottom:12px">🧠</div>
      <h3 style="font-size:18px;margin:0 0 8px">A18 Pro 芯片</h3>
      <p style="color:#6e6e73;font-size:14px;margin:0">3nm第二代工艺，6核GPU，支持Apple Intelligence硬件级AI加速</p>
    </div>
    <div style="background:#f5f5f7;border-radius:16px;padding:28px">
      <div style="font-size:36px;margin-bottom:12px">🔩</div>
      <h3 style="font-size:18px;margin:0 0 8px">钛金属边框</h3>
      <p style="color:#6e6e73;font-size:14px;margin:0">4级钛合金，比钢轻，比铝硬，哑光磨砂玻璃背板，握持更舒适</p>
    </div>
  </div>
  <div style="border:1px solid #e5e5ea;border-radius:16px;overflow:hidden;margin-bottom:24px">
    <div style="background:#0071e3;color:#fff;padding:16px 24px;font-weight:600;font-size:16px">📋 技术规格摘要</div>
    <table style="width:100%;border-collapse:collapse">
      <tr style="border-bottom:1px solid #f2f2f7"><td style="padding:14px 24px;color:#6e6e73;width:40%">屏幕</td><td style="padding:14px 24px">6.3" Super Retina XDR OLED，2868×1320，120Hz</td></tr>
      <tr style="border-bottom:1px solid #f2f2f7;background:#fafafa"><td style="padding:14px 24px;color:#6e6e73">芯片</td><td style="padding:14px 24px">A18 Pro，3nm，6核CPU，6核GPU</td></tr>
      <tr style="border-bottom:1px solid #f2f2f7"><td style="padding:14px 24px;color:#6e6e73">摄像头</td><td style="padding:14px 24px">48MP主摄 + 48MP超广角 + 12MP 5x长焦</td></tr>
      <tr style="border-bottom:1px solid #f2f2f7;background:#fafafa"><td style="padding:14px 24px;color:#6e6e73">存储</td><td style="padding:14px 24px">128GB / 256GB / 512GB / 1TB</td></tr>
      <tr style="border-bottom:1px solid #f2f2f7"><td style="padding:14px 24px;color:#6e6e73">电池</td><td style="padding:14px 24px">3582mAh，MagSafe 25W无线充电</td></tr>
      <tr><td style="padding:14px 24px;color:#6e6e73;background:#fafafa">防护</td><td style="padding:14px 24px;background:#fafafa">IP68，水下6m/30min</td></tr>
    </table>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 3. 华为 Mate 70 Pro
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_mate70pro",
  spuId: NumberLong("310000000000000002"),
  title: "华为 Mate 70 Pro",
  description: "麒麟9010芯片，卫星通信，超级变焦，华为自研鸿蒙系统",
  features: ["麒麟9010，性能大幅跃升","天通卫星通话，信号全覆盖","超级变焦影像系统，最高100倍","鸿蒙NEXT系统，流畅不卡顿","IP68+防水防尘","SuperCharge 100W超级快充"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"华为 Mate 70 Pro"},
      {label:"操作系统", value:"HarmonyOS NEXT"},
      {label:"机身颜色", value:"砚黑 / 宣白 / 龙晶紫"},
      {label:"机身重量", value:"229g"},
      {label:"防水等级", value:"IP68+（1.5m水下60分钟）"}
    ]},
    { group: "屏幕", items: [
      {label:"屏幕尺寸", value:"6.8英寸"},
      {label:"屏幕材质", value:"柔性OLED，支持AOD"},
      {label:"分辨率", value:"2800×1260"},
      {label:"刷新率", value:"1~120Hz"}
    ]},
    { group: "处理器", items: [
      {label:"芯片", value:"麒麟9010"},
      {label:"制程", value:"先进制程"},
      {label:"内存", value:"12GB / 16GB"},
      {label:"存储", value:"256GB / 512GB"}
    ]},
    { group: "影像", items: [
      {label:"主摄", value:"50MP，可变光圈f/1.4-f/4"},
      {label:"超广角", value:"40MP，f/2.2"},
      {label:"长焦", value:"12MP，5x光学，100x超级变焦"},
      {label:"卫星功能", value:"天通卫星通话、北斗卫星消息"}
    ]},
    { group: "电池与充电", items: [
      {label:"电池容量", value:"5200mAh 硅碳负极电池"},
      {label:"有线充电", value:"100W SuperCharge"},
      {label:"无线充电", value:"80W无线超级快充"},
      {label:"反向充电", value:"20W无线反充"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(135deg,#c0392b,#e74c3c);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:rgba(255,255,255,0.8);font-size:13px;letter-spacing:3px;margin:0 0 12px">HUAWEI</p>
    <h1 style="color:#fff;font-size:40px;margin:0 0 12px;font-weight:700">Mate 70 Pro</h1>
    <p style="color:rgba(255,255,255,0.85);font-size:17px;margin:0">超越天际 · 麒麟归来</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:32px">
    <div style="background:#fef9f0;border:1px solid #fde8c2;border-radius:12px;padding:20px;text-align:center">
      <div style="font-size:30px;margin-bottom:8px">🛰️</div>
      <div style="font-weight:600;font-size:14px">天通卫星通话</div>
      <div style="color:#888;font-size:12px;margin-top:4px">无信号也能通话</div>
    </div>
    <div style="background:#fef9f0;border:1px solid #fde8c2;border-radius:12px;padding:20px;text-align:center">
      <div style="font-size:30px;margin-bottom:8px">🔬</div>
      <div style="font-weight:600;font-size:14px">100倍超级变焦</div>
      <div style="color:#888;font-size:12px;margin-top:4px">远处看清看细节</div>
    </div>
    <div style="background:#fef9f0;border:1px solid #fde8c2;border-radius:12px;padding:20px;text-align:center">
      <div style="font-size:30px;margin-bottom:8px">⚡</div>
      <div style="font-weight:600;font-size:14px">100W超充</div>
      <div style="color:#888;font-size:12px;margin-top:4px">快速补充电量</div>
    </div>
  </div>
  <div style="background:#f8f8f8;border-radius:12px;padding:24px;margin-bottom:24px">
    <h3 style="margin:0 0 16px;color:#c0392b">🌟 鸿蒙NEXT全新体验</h3>
    <p style="color:#444;line-height:1.8;margin:0">华为 Mate 70 Pro 搭载全新鸿蒙NEXT操作系统，实现设备全场景无缝协同。全新盘古大模型AI助手，智慧加持每一个应用场景，让您的手机真正读懂您的需求。</p>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 4. 三星 Galaxy S25 Ultra
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_s25ultra",
  spuId: NumberLong("310000000000000003"),
  title: "三星 Galaxy S25 Ultra",
  description: "骁龙8 Elite，Galaxy AI，2亿像素，S Pen，钛合金边框",
  features: ["骁龙8 Elite性能炸裂","Galaxy AI全面深度集成","2亿像素主摄+50倍变焦","全新S Pen手写体验","钛合金机身更轻更坚固","大模型端侧推理"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"三星 Galaxy S25 Ultra"},
      {label:"操作系统", value:"Android 15 / One UI 7"},
      {label:"机身颜色", value:"幻影黑 / 皎月白 / 钛星灰 / 幻冰蓝"},
      {label:"机身重量", value:"218g（史上最轻Ultra）"},
      {label:"防水等级", value:"IP68"},
      {label:"S Pen", value:"内置 S Pen，支持低时延书写"}
    ]},
    { group: "屏幕", items: [
      {label:"屏幕尺寸", value:"6.9英寸"},
      {label:"屏幕材质", value:"Dynamic AMOLED 2X"},
      {label:"分辨率", value:"3088×1440 (QHD+, 505 PPI)"},
      {label:"刷新率", value:"1~120Hz 自适应"},
      {label:"峰值亮度", value:"2600nit"}
    ]},
    { group: "处理器", items: [
      {label:"芯片", value:"高通骁龙8 Elite for Galaxy"},
      {label:"内存", value:"12GB LPDDR5X"},
      {label:"存储", value:"256GB / 512GB / 1TB UFS 4.0"}
    ]},
    { group: "影像", items: [
      {label:"主摄", value:"200MP，f/1.7，OIS防抖"},
      {label:"超广角", value:"50MP，f/1.9，120°视野"},
      {label:"长焦1", value:"10MP，f/2.4，3倍光学"},
      {label:"长焦2", value:"50MP，f/3.4，5倍光学，50倍空间变焦"},
      {label:"视频", value:"8K@30fps，4K@120fps"}
    ]},
    { group: "电池与充电", items: [
      {label:"电池容量", value:"5000mAh"},
      {label:"有线充电", value:"45W Super Fast Charging"},
      {label:"无线充电", value:"15W Wireless PowerShare"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(135deg,#1a237e,#3949ab);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:rgba(255,255,255,0.7);font-size:13px;letter-spacing:3px;margin:0 0 12px">SAMSUNG</p>
    <h1 style="color:#fff;font-size:40px;margin:0 0 12px;font-weight:700">Galaxy S25 Ultra</h1>
    <p style="color:rgba(255,255,255,0.85);font-size:17px;margin:0">Galaxy AI · 无限创想</p>
  </div>
  <div style="background:#e8eaf6;border-radius:12px;padding:24px;margin-bottom:24px;border-left:4px solid #3949ab">
    <h3 style="color:#1a237e;margin:0 0 12px">✍️ S Pen 随心书写</h3>
    <p style="color:#444;line-height:1.8;margin:0">内置S Pen，2.8ms超低延迟，笔尖压感9级，随时记录灵感。配合AI文字识别，手写即可转文字，会议纪要一键整理。</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:20px;margin-bottom:32px">
    <div style="background:#f5f5f7;border-radius:12px;padding:24px">
      <div style="font-size:32px;margin-bottom:8px">📡</div>
      <h4 style="margin:0 0 8px">Galaxy AI 全面加持</h4>
      <p style="color:#666;font-size:13px;margin:0">AI通话翻译、AI摘要生成、AI图片消除、AI生成图像，日常效率倍增</p>
    </div>
    <div style="background:#f5f5f7;border-radius:12px;padding:24px">
      <div style="font-size:32px;margin-bottom:8px">🔭</div>
      <h4 style="margin:0 0 8px">2亿像素四摄系统</h4>
      <p style="color:#666;font-size:13px;margin:0">200MP主摄，50倍空间变焦，支持8K超高清视频录制</p>
    </div>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 5. MacBook Pro 16 M4 Pro
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_mbp16m4",
  spuId: NumberLong("310000000000000005"),
  title: "MacBook Pro 16英寸 M4 Pro",
  description: "M4 Pro芯片，16英寸Liquid Retina XDR显示屏，24GB统一内存，专业创作首选",
  features: ["M4 Pro：12核CPU，20核GPU","Liquid Retina XDR，10亿色彩","24GB/48GB统一内存，流畅处理超大文件","Thunderbolt 4高速接口","24小时超长续航","Nano-texture玻璃可选"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"MacBook Pro 16英寸（M4 Pro）"},
      {label:"操作系统", value:"macOS Sequoia"},
      {label:"机身颜色", value:"银色 / 深空黑色"},
      {label:"机身重量", value:"2.14kg"},
      {label:"机身尺寸", value:"35.57 × 24.59 × 1.68cm"}
    ]},
    { group: "屏幕", items: [
      {label:"屏幕尺寸", value:"16.2英寸"},
      {label:"屏幕类型", value:"Liquid Retina XDR显示屏"},
      {label:"分辨率", value:"3456×2234 (254 PPI)"},
      {label:"亮度", value:"XDR 1600nit，SDR 1000nit"},
      {label:"色域", value:"P3广色域，10亿色彩"},
      {label:"刷新率", value:"ProMotion 自适应 24~120Hz"}
    ]},
    { group: "处理器与内存", items: [
      {label:"芯片", value:"Apple M4 Pro"},
      {label:"CPU", value:"12核（8性能核+4能效核）"},
      {label:"GPU", value:"20核"},
      {label:"神经网络引擎", value:"16核"},
      {label:"统一内存", value:"24GB / 48GB"},
      {label:"内存带宽", value:"273GB/s"},
      {label:"存储", value:"512GB / 1TB / 2TB / 4TB SSD"}
    ]},
    { group: "接口", items: [
      {label:"Thunderbolt 4", value:"3个（USB-C，最高40Gb/s）"},
      {label:"HDMI", value:"1个（支持8K@60Hz）"},
      {label:"SD卡槽", value:"SDXC UHS-II"},
      {label:"MagSafe 3", value:"专用充电接口"},
      {label:"耳机孔", value:"3.5mm（支持高阻抗耳机）"}
    ]},
    { group: "电池与充电", items: [
      {label:"电池容量", value:"100Wh"},
      {label:"续航", value:"最长24小时（Apple TV app视频播放）"},
      {label:"充电", value:"140W USB-C电源适配器"},
      {label:"MagSafe充电", value:"支持"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(160deg,#1c1c1e,#38383a);padding:56px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:#a8a8b3;font-size:13px;letter-spacing:3px;margin:0 0 12px">APPLE</p>
    <h1 style="color:#fff;font-size:38px;margin:0 0 12px;font-weight:700">MacBook Pro</h1>
    <p style="color:#a8a8b3;font-size:17px;margin:0">16英寸 · M4 Pro · 专业创作利器</p>
  </div>
  <div style="background:#f0f9ff;border:1px solid #bae6fd;border-radius:16px;padding:24px;margin-bottom:24px">
    <h3 style="color:#0284c7;margin:0 0 16px">🚀 M4 Pro — 重新定义Pro级性能</h3>
    <p style="color:#334155;line-height:1.8;margin:0">M4 Pro芯片采用3nm工艺，12核CPU相比M3 Pro提升性能高达20%。20核GPU确保流畅驾驭Final Cut Pro、Xcode、Blender等专业应用。273GB/s内存带宽彻底消除卡顿瓶颈。</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:32px">
    <div style="text-align:center;padding:20px;background:#f5f5f7;border-radius:12px">
      <div style="font-size:28px;margin-bottom:8px">⏱️</div>
      <div style="font-weight:700;font-size:20px;color:#0071e3">24h</div>
      <div style="color:#6e6e73;font-size:13px">超长续航</div>
    </div>
    <div style="text-align:center;padding:20px;background:#f5f5f7;border-radius:12px">
      <div style="font-size:28px;margin-bottom:8px">🖥️</div>
      <div style="font-weight:700;font-size:20px;color:#0071e3">XDR</div>
      <div style="color:#6e6e73;font-size:13px">1600nit峰值亮度</div>
    </div>
    <div style="text-align:center;padding:20px;background:#f5f5f7;border-radius:12px">
      <div style="font-size:28px;margin-bottom:8px">💾</div>
      <div style="font-weight:700;font-size:20px;color:#0071e3">24GB</div>
      <div style="color:#6e6e73;font-size:13px">统一内存起</div>
    </div>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 6. 联想小新 Pro 16
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_lenovoxpro16",
  spuId: NumberLong("310000000000000006"),
  title: "联想小新 Pro 16 2025",
  description: "酷睿Ultra 9处理器，2.5K 165Hz高刷屏，32GB大内存，轻薄高性能全能本",
  features: ["酷睿Ultra 9 275H，AI PC时代","2.5K分辨率165Hz高刷","32GB大内存，专业创作无压力","轻薄机身1.79kg","4560mAh超大电池","支持2242/2280双SSD位"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"联想小新 Pro 16 2025"},
      {label:"操作系统", value:"Windows 11 Home"},
      {label:"机身颜色", value:"绅士灰 / 远山蓝"},
      {label:"机身重量", value:"1.79kg"},
      {label:"机身厚度", value:"16.9mm"}
    ]},
    { group: "屏幕", items: [
      {label:"屏幕尺寸", value:"16英寸"},
      {label:"分辨率", value:"2560×1600 (2.5K)"},
      {label:"刷新率", value:"165Hz高刷新率"},
      {label:"色域", value:"100% sRGB，DCI-P3色域"},
      {label:"亮度", value:"400nit"},
      {label:"认证", value:"护眼低蓝光认证，TÜV莱茵"}
    ]},
    { group: "处理器与内存", items: [
      {label:"处理器", value:"英特尔® 酷睿™ Ultra 9 275H / Ultra 7 265H"},
      {label:"核心数", value:"16核（6P+10E+2LPE）"},
      {label:"内存", value:"16GB / 32GB LPDDR5x-7467"},
      {label:"固态硬盘", value:"512GB / 1TB PCIe 4.0 NVMe SSD"},
      {label:"集成显卡", value:"Intel Arc 140T 核显"}
    ]},
    { group: "接口与连接", items: [
      {label:"USB-A", value:"2×USB 3.2 Gen1"},
      {label:"USB-C", value:"1×Thunderbolt 4，1×USB 3.2"},
      {label:"HDMI", value:"1×HDMI 2.1"},
      {label:"读卡器", value:"SD 4.0"},
      {label:"WiFi", value:"WiFi 6E + 蓝牙 5.3"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(135deg,#e74c3c,#e91e63);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:rgba(255,255,255,0.8);font-size:13px;letter-spacing:3px;margin:0 0 12px">LENOVO</p>
    <h1 style="color:#fff;font-size:38px;margin:0 0 12px;font-weight:700">小新 Pro 16</h1>
    <p style="color:rgba(255,255,255,0.85);font-size:17px;margin:0">AI PC时代 · 全能轻薄旗舰</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:20px;margin-bottom:32px">
    <div style="background:#fff5f5;border:1px solid #fecdd3;border-radius:12px;padding:24px">
      <h4 style="color:#e74c3c;margin:0 0 12px">⚡ 酷睿Ultra 9 275H</h4>
      <p style="color:#555;font-size:14px;line-height:1.8;margin:0">16核处理器，AI加速引擎，NPU每秒算力达48TOPS，本地运行AI大模型零延迟</p>
    </div>
    <div style="background:#fff5f5;border:1px solid #fecdd3;border-radius:12px;padding:24px">
      <h4 style="color:#e74c3c;margin:0 0 12px">🖥️ 2.5K 165Hz高刷屏</h4>
      <p style="color:#555;font-size:14px;line-height:1.8;margin:0">2560×1600分辨率，165Hz高刷新率，1ms响应时间，游戏+创作双场景无妥协</p>
    </div>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 7. AirPods Pro 3
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_airpodspro3",
  spuId: NumberLong("310000000000000007"),
  title: "AirPods Pro 3",
  description: "H3芯片，业界顶级主动降噪，空间音频，33小时总续航",
  features: ["H3芯片，降噪能力提升50%","个性化空间音频，环绕立体声","自适应音频，自动感知环境","通话级降噪，外界噪音归零","33小时总续航（含充电盒）","IP54防水防汗"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"AirPods Pro 3"},
      {label:"芯片", value:"Apple H3"},
      {label:"颜色", value:"白色"},
      {label:"连接", value:"蓝牙 5.3"},
      {label:"防水等级", value:"耳机IP54 / 充电盒IP54"}
    ]},
    { group: "音频", items: [
      {label:"驱动单元", value:"定制高偏移苹果驱动单元"},
      {label:"主动降噪", value:"支持，降噪水平提升50%"},
      {label:"通透模式", value:"支持，自适应通透"},
      {label:"空间音频", value:"个性化空间音频 + 动态头部追踪"},
      {label:"音频格式", value:"AAC / SBC"}
    ]},
    { group: "续航", items: [
      {label:"耳机续航", value:"最长8小时（开降噪6小时）"},
      {label:"充电盒续航", value:"额外27小时"},
      {label:"总续航", value:"最长33小时"},
      {label:"快充", value:"充电5分钟=1小时聆听"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto;color:#1a1a1a">
  <div style="background:linear-gradient(160deg,#6366f1,#8b5cf6);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <h1 style="color:#fff;font-size:40px;margin:0 0 12px;font-weight:700">AirPods Pro 3</h1>
    <p style="color:rgba(255,255,255,0.85);font-size:17px;margin:0">H3芯片 · 降噪巅峰之作</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:32px">
    <div style="background:#f5f5f7;border-radius:12px;padding:20px;text-align:center">
      <div style="font-size:28px;margin-bottom:8px">🔇</div>
      <div style="font-weight:700">主动降噪</div>
      <div style="color:#6e6e73;font-size:12px;margin-top:4px">提升50%降噪效果</div>
    </div>
    <div style="background:#f5f5f7;border-radius:12px;padding:20px;text-align:center">
      <div style="font-size:28px;margin-bottom:8px">🎵</div>
      <div style="font-weight:700">空间音频</div>
      <div style="color:#6e6e73;font-size:12px;margin-top:4px">个性化立体声场</div>
    </div>
    <div style="background:#f5f5f7;border-radius:12px;padding:20px;text-align:center">
      <div style="font-size:28px;margin-bottom:8px">🔋</div>
      <div style="font-weight:700">33小时续航</div>
      <div style="color:#6e6e73;font-size:12px;margin-top:4px">含充电盒总续航</div>
    </div>
  </div>
</div>`,
  createTime: now
},

// ──────────────────────────────────────────────────────────────────
// 8-15 精简版（其他产品）
// ──────────────────────────────────────────────────────────────────
{
  _id: "detail_sonyxm6",
  spuId: NumberLong("310000000000000008"),
  title: "索尼 WH-1000XM6",
  description: "索尼行业领先降噪耳机第六代，LDAC高解析，QN3HD降噪处理器",
  features: ["QN3HD降噪芯片，降噪旗舰","LDAC 990kbps高解析无损","30小时超长续航","折叠设计，便携出行","多点连接2台设备","快速充电，充电3分钟听3小时"],
  specs: [
    { group: "基本参数", items: [
      {label:"芯片", value:"QN3HD降噪处理器"},
      {label:"连接", value:"蓝牙 5.3"},
      {label:"编码", value:"LDAC / AAC / SBC"},
      {label:"重量", value:"250g"}
    ]},
    { group: "音频", items: [
      {label:"单元尺寸", value:"40mm 动圈"},
      {label:"频响", value:"4Hz-40,000Hz（LDAC）"},
      {label:"降噪", value:"行业领先自适应降噪"},
      {label:"空间音频", value:"360 Reality Audio"}
    ]},
    { group: "续航", items: [
      {label:"续航", value:"30小时（开降噪）"},
      {label:"快充", value:"充电3分钟=3小时"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:#e2b714;font-size:13px;letter-spacing:3px;margin:0 0 12px">SONY</p>
    <h1 style="color:#fff;font-size:40px;margin:0 0 12px;font-weight:700">WH-1000XM6</h1>
    <p style="color:rgba(255,255,255,0.75);font-size:17px;margin:0">行业标杆 · 降噪新巅峰</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:20px">
    <div style="background:#f5f5f7;border-radius:12px;padding:24px"><h4 style="margin:0 0 8px">🎧 LDAC高解析</h4><p style="color:#666;font-size:14px;margin:0">990kbps码率传输，无损Hi-Res音质，真正做到听见每一个细节</p></div>
    <div style="background:#f5f5f7;border-radius:12px;padding:24px"><h4 style="margin:0 0 8px">🔇 QN3HD降噪芯片</h4><p style="color:#666;font-size:14px;margin:0">每秒8百万次自适应降噪计算，乘地铁飞机再嘈杂也静享好音乐</p></div>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_applewatch10",
  spuId: NumberLong("310000000000000009"),
  title: "Apple Watch Series 10",
  description: "Apple Watch史上最薄机身，全新更大OLED屏幕，睡眠呼吸暂停检测",
  features: ["史上最薄Watch设计","更大更亮全新OLED屏幕","睡眠呼吸暂停检测（FDA认证）","深度睡眠追踪","快速充电，80%仅需30分钟","watchOS 11全新功能"],
  specs: [
    { group: "基本参数", items: [
      {label:"系列", value:"Apple Watch Series 10"},
      {label:"材质", value:"铝金属 / 不锈钢 / 钛金属"},
      {label:"防水等级", value:"WR50防水（游泳可用）"},
      {label:"连接", value:"GPS / GPS+蜂窝"}
    ]},
    { group: "健康功能", items: [
      {label:"心率监测", value:"光电心率传感器，持续监测"},
      {label:"血氧检测", value:"支持"},
      {label:"心电图", value:"ECG心电图App"},
      {label:"睡眠检测", value:"睡眠阶段 + 睡眠呼吸暂停检测"},
      {label:"跌倒检测", value:"支持，自动呼救"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#0d1b2a,#1b4332);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <h1 style="color:#fff;font-size:40px;margin:0 0 12px;font-weight:700">Apple Watch Series 10</h1>
    <p style="color:rgba(255,255,255,0.8);font-size:17px;margin:0">史上最薄 · 健康守护每一刻</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px">
    <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;padding:20px;text-align:center"><div style="font-size:28px">💤</div><div style="font-weight:600;margin-top:8px">睡眠呼吸暂停</div><div style="color:#16a34a;font-size:12px">FDA认证健康检测</div></div>
    <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;padding:20px;text-align:center"><div style="font-size:28px">❤️</div><div style="font-weight:600;margin-top:8px">ECG心电图</div><div style="color:#16a34a;font-size:12px">随时监测心脏健康</div></div>
    <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;padding:20px;text-align:center"><div style="font-size:28px">⚡</div><div style="font-weight:600;margin-top:8px">快速充电</div><div style="color:#16a34a;font-size:12px">30分钟充至80%</div></div>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_dysonairwrap",
  spuId: NumberLong("310000000000000010"),
  title: "Dyson Airwrap 多功能造型器",
  description: "Dyson独创超音速气流，卷发直发吹发三合一，不依赖极热，护发不伤发",
  features: ["Dyson超音速马达，每分钟10万转","Coanda气流卷发，无需极热","多种卷筒头吸附自动卷发","防飞散气流，锁定造型","卷发/直发/吹发三合一","适用干发/湿发"],
  specs: [
    { group: "基本参数", items: [
      {label:"马达", value:"Dyson数码马达V9，每分钟10万转"},
      {label:"功率", value:"1300W"},
      {label:"温度设置", value:"60°C / 80°C / 100°C / 120°C"},
      {label:"附件", value:"6个卷筒/梳子/柔顺刷"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#7c3aed,#db2777);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:rgba(255,255,255,0.8);font-size:13px;letter-spacing:3px;margin:0 0 12px">DYSON</p>
    <h1 style="color:#fff;font-size:36px;margin:0 0 12px;font-weight:700">Airwrap 造型器</h1>
    <p style="color:rgba(255,255,255,0.85);font-size:17px;margin:0">不依赖极热 · 卷发直发吹发三合一</p>
  </div>
  <div style="background:#fdf4ff;border:1px solid #e9d5ff;border-radius:12px;padding:24px;margin-bottom:24px">
    <h3 style="color:#7c3aed;margin:0 0 12px">🌀 Coanda气流技术</h3>
    <p style="color:#4b5563;line-height:1.8;margin:0">Dyson独创Coanda吸附效应，利用强大气流形成旋涡，自动将发丝卷绕在筒身上。全程无需极热，最高温度仅120°C，大幅减少热损伤，让发丝更健康有光泽。</p>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_airjordan1",
  spuId: NumberLong("310000000000000011"),
  title: "Nike Air Jordan 1 Retro High OG",
  description: "1985年经典复刻，芝加哥配色，全粒面真皮鞋面，Nike Air气垫缓震",
  features: ["1985年原款复刻，经典不过时","全粒面真皮鞋面，耐用有质感","Nike Air-Sole气垫，轻量缓震","高帮设计，脚踝支撑保护","橡胶外底，室内外均适用","限量发售，值得收藏"],
  specs: [
    { group: "基本参数", items: [
      {label:"品牌", value:"Nike"},
      {label:"型号", value:"Air Jordan 1 Retro High OG"},
      {label:"配色", value:"黑红芝加哥 / 黑白熊猫"},
      {label:"适用", value:"篮球/休闲/收藏"}
    ]},
    { group: "材质", items: [
      {label:"鞋面", value:"全粒面头层牛皮"},
      {label:"鞋底", value:"橡胶大底 + Nike Air气垫"},
      {label:"内里", value:"布料内衬，舒适透气"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#be123c,#1c1917);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:rgba(255,255,255,0.7);font-size:13px;letter-spacing:4px;margin:0 0 12px">NIKE</p>
    <h1 style="color:#fff;font-size:36px;margin:0 0 12px;font-weight:800">Air Jordan 1 Retro</h1>
    <p style="color:rgba(255,255,255,0.8);font-size:16px;margin:0">1985 · Chicago · 永恒经典复刻</p>
  </div>
  <div style="background:#fff5f5;border-left:4px solid #be123c;border-radius:0 12px 12px 0;padding:24px;margin-bottom:24px">
    <h3 style="color:#be123c;margin:0 0 12px">👟 篮球传奇的起点</h3>
    <p style="color:#444;line-height:1.8;margin:0">1985年，迈克尔·乔丹穿着Air Jordan 1登上NBA赛场，开创了一个时代。芝加哥配色（Chicago）以其大红与白黑的碰撞成为球鞋史上最具标志性的配色之一，每次复刻都引发全球球迷疯抢。</p>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_ultraboost24",
  spuId: NumberLong("310000000000000012"),
  title: "Adidas Ultraboost 24",
  description: "革命性BOOST中底，Linear Energy Push板，Primeknit编织鞋面，马拉松跑鞋",
  features: ["BOOST中底，能量回弹75%+","LEP回弹板，推进效率提升","Primeknit+编织鞋面，透气贴合","Continental橡胶大底，防滑耐磨","适合长跑训练及日常穿搭","部分系列采用海洋再生塑料"],
  specs: [
    { group: "基本参数", items: [
      {label:"品牌", value:"Adidas"},
      {label:"系列", value:"Ultraboost 24"},
      {label:"适合人群", value:"男款"},
      {label:"适用场景", value:"跑步/日常"}
    ]},
    { group: "材质与技术", items: [
      {label:"鞋面", value:"Primeknit+ 针织鞋面"},
      {label:"中底", value:"100% BOOST材料"},
      {label:"推进板", value:"Linear Energy Push (LEP)"},
      {label:"大底", value:"Continental™橡胶"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#000,#1a1a1a);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:#fff;font-size:20px;font-weight:900;letter-spacing:6px;margin:0 0 12px">ADIDAS</p>
    <h1 style="color:#fff;font-size:36px;margin:0 0 12px;font-weight:700">Ultraboost 24</h1>
    <p style="color:#a3a3a3;font-size:16px;margin:0">BOOST能量 · 每步都是突破</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:20px">
    <div style="background:#f5f5f5;border-radius:12px;padding:24px"><h4 style="margin:0 0 8px">🏃 BOOST中底</h4><p style="color:#666;font-size:14px;margin:0">热塑性聚氨酯粒子融合成型，每一步能量回弹超75%，跑得越远越轻盈</p></div>
    <div style="background:#f5f5f5;border-radius:12px;padding:24px"><h4 style="margin:0 0 8px">🧵 Primeknit鞋面</h4><p style="color:#666;font-size:14px;margin:0">一体成型针织工艺，360°包裹脚型，透气排汗，跑步全程舒适无约束</p></div>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_skiifacial",
  spuId: NumberLong("310000000000000013"),
  title: "SK-II 神仙水精华露",
  description: "含90%+PITERA™酵母精华，改善肌肤纹理，提亮透明感，30年全球热销",
  features: ["PITERA™含量超90%，核心专利成分","改善5大肌肤问题：斑点/皱纹/毛孔/暗沉/粗糙","30年临床验证，全球女性信赖","质地清爽不油腻，适合各肤质","日本原产，品质保证"],
  specs: [
    { group: "产品信息", items: [
      {label:"品牌", value:"SK-II"},
      {label:"系列", value:"Facial Treatment Essence（神仙水）"},
      {label:"规格", value:"160ml / 230ml / 330ml"},
      {label:"产地", value:"日本"},
      {label:"保质期", value:"3年"}
    ]},
    { group: "核心成分", items: [
      {label:"PITERA™", value:"90%+酵母精华，SK-II专利核心成分"},
      {label:"烟酰胺", value:"提亮肤色，缩小毛孔"},
      {label:"透明质酸", value:"深层锁水保湿"},
      {label:"精氨酸", value:"修护肌肤屏障"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#fdf2f8,#fce7f3);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px;border:1px solid #f9a8d4">
    <p style="color:#be185d;font-size:13px;letter-spacing:4px;margin:0 0 12px">SK-II</p>
    <h1 style="color:#1a1a1a;font-size:38px;margin:0 0 12px;font-weight:700">神仙水精华露</h1>
    <p style="color:#be185d;font-size:17px;margin:0">Facial Treatment Essence · 30年传奇</p>
  </div>
  <div style="background:#fef9ff;border:1px solid #e9d5ff;border-radius:12px;padding:24px;margin-bottom:24px">
    <h3 style="color:#7c3aed;margin:0 0 16px">✨ PITERA™ — 天然酵母精华</h3>
    <p style="color:#4b5563;line-height:1.8;margin:0">SK-II 1980年代在日本清酒酿造厂发现，老杜氏们的手皮肤异常光滑年轻，经研究发现是酿酒过程中产生的酵母精华——PITERA™。这一发现改变了护肤界的历史，也成就了神仙水的传奇地位。</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:12px;text-align:center">
    <div style="background:#fdf4ff;border-radius:10px;padding:16px;"><div style="font-size:22px">🌟</div><div style="font-size:12px;margin-top:6px;color:#7c3aed">斑点</div></div>
    <div style="background:#fdf4ff;border-radius:10px;padding:16px;"><div style="font-size:22px">✨</div><div style="font-size:12px;margin-top:6px;color:#7c3aed">皱纹</div></div>
    <div style="background:#fdf4ff;border-radius:10px;padding:16px;"><div style="font-size:22px">💧</div><div style="font-size:12px;margin-top:6px;color:#7c3aed">毛孔</div></div>
    <div style="background:#fdf4ff;border-radius:10px;padding:16px;"><div style="font-size:22px">☀️</div><div style="font-size:12px;margin-top:6px;color:#7c3aed">暗沉</div></div>
    <div style="background:#fdf4ff;border-radius:10px;padding:16px;"><div style="font-size:22px">🌸</div><div style="font-size:12px;margin-top:6px;color:#7c3aed">粗糙</div></div>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_lancomebb",
  spuId: NumberLong("310000000000000014"),
  title: "兰蔻小黑瓶精华液",
  description: "兰蔻夜间修护标志性产品，Pro-Xylane™+透明质酸+益生菌发酵精华，淡纹嫩肤",
  features: ["Pro-Xylane™专利成分，兰蔻核心","益生菌发酵精华，焕活肌肤","透明质酸深层保湿","夜间密集修护，次日肌肤焕新","法国原装进口，品质保证","适合熟龄肌/干性肌/混合肌"],
  specs: [
    { group: "产品信息", items: [
      {label:"品牌", value:"兰蔻 Lancôme"},
      {label:"系列", value:"小黑瓶精华液 Advanced Génifique"},
      {label:"规格", value:"30ml / 50ml / 100ml"},
      {label:"产地", value:"法国"},
      {label:"适合肌肤", value:"各种肤质"}
    ]},
    { group: "核心成分", items: [
      {label:"Pro-Xylane™", value:"兰蔻专利成分，刺激胶原蛋白合成"},
      {label:"益生菌发酵液", value:"增强肌肤屏障，提升自愈力"},
      {label:"透明质酸", value:"多分子量，多层次保湿"},
      {label:"维他命CG", value:"提亮均匀肤色"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:#d4af37;font-size:13px;letter-spacing:4px;margin:0 0 12px">LANCÔME</p>
    <h1 style="color:#fff;font-size:36px;margin:0 0 12px;font-weight:700">小黑瓶精华液</h1>
    <p style="color:rgba(212,175,55,0.9);font-size:17px;margin:0">Advanced Génifique · 夜间修护传奇</p>
  </div>
  <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:12px;padding:24px;margin-bottom:24px">
    <h3 style="color:#92400e;margin:0 0 12px">🌙 夜间密集修护 7天焕新</h3>
    <p style="color:#78350f;line-height:1.8;margin:0">连续使用7晚，77%的女性感受肌肤更有弹性光泽。小黑瓶的秘密在于其独特的益生菌发酵精华配方——帮助重建肌肤微生物平衡，从根本上强化肌肤抵御力，让肌肤在夜间充分自我修复。</p>
  </div>
</div>`,
  createTime: now
},

{
  _id: "detail_squirrelnut",
  spuId: NumberLong("310000000000000015"),
  title: "三只松鼠 每日坚果大礼包",
  description: "8种混合坚果，独立小袋包装，原味零添加，营养健康好零食",
  features: ["8种坚果混合，营养均衡全面","独立小包装，方便携带保新鲜","精选优质原料，严格品控","无多余添加，原味自然","每日一袋，轻松满足坚果摄入","热门礼品首选"],
  specs: [
    { group: "产品信息", items: [
      {label:"品牌", value:"三只松鼠"},
      {label:"口味", value:"混合坚果原味"},
      {label:"配料", value:"碧根果仁、腰果、夏威夷果、巴旦木、蔓越莓干、枸杞、南瓜子仁、葡萄干"},
      {label:"规格", value:"750g（30袋）/ 1250g（50袋）/ 2500g（100袋）"},
      {label:"保质期", value:"180天"},
      {label:"储存", value:"常温，避光防潮密封"}
    ]},
    { group: "营养成分（每袋25g）", items: [
      {label:"能量", value:"约580kJ"},
      {label:"蛋白质", value:"约4g"},
      {label:"脂肪", value:"约10g（不饱和脂肪酸为主）"},
      {label:"碳水化合物", value:"约8g"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#d97706,#f59e0b);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <div style="font-size:48px;margin-bottom:12px">🐿️</div>
    <h1 style="color:#fff;font-size:36px;margin:0 0 12px;font-weight:700">三只松鼠</h1>
    <p style="color:rgba(255,255,255,0.9);font-size:17px;margin:0">每日坚果大礼包 · 8种营养混合</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:24px;text-align:center">
    <div style="background:#fffbeb;border-radius:10px;padding:16px"><div style="font-size:24px">🥜</div><div style="font-size:12px;color:#92400e;margin-top:6px">碧根果</div></div>
    <div style="background:#fffbeb;border-radius:10px;padding:16px"><div style="font-size:24px">🌰</div><div style="font-size:12px;color:#92400e;margin-top:6px">腰果</div></div>
    <div style="background:#fffbeb;border-radius:10px;padding:16px"><div style="font-size:24px">🥥</div><div style="font-size:12px;color:#92400e;margin-top:6px">夏威夷果</div></div>
    <div style="background:#fffbeb;border-radius:10px;padding:16px"><div style="font-size:24px">🫘</div><div style="font-size:12px;color:#92400e;margin-top:6px">巴旦木</div></div>
  </div>
  <div style="background:#fef9c3;border:1px solid #fde047;border-radius:12px;padding:20px;text-align:center">
    <p style="color:#713f12;font-size:16px;margin:0;font-weight:600">🎁 送礼首选，精美礼盒包装，节日氛围满分</p>
  </div>
</div>`,
  createTime: now
},

// OPPO Find X8 Pro（补充）
{
  _id: "detail_findx8pro",
  spuId: NumberLong("310000000000000004"),
  title: "OPPO Find X8 Pro",
  description: "天玑9400处理器，哈苏专业影像，5600mAh超大电池，100W超级闪充",
  features: ["天玑9400，台积电3nm领先工艺","哈苏联合调校影像系统","5600mAh超大硅碳负极电池","100W超级闪充，42分钟满电","50W无线闪充","IP69防水防尘旗舰级"],
  specs: [
    { group: "基本参数", items: [
      {label:"商品名称", value:"OPPO Find X8 Pro"},
      {label:"操作系统", value:"ColorOS 15（基于Android 15）"},
      {label:"机身颜色", value:"星云紫 / 流光白 / 深空黑"},
      {label:"防水等级", value:"IP69（史上最高级别旗舰手机）"}
    ]},
    { group: "处理器", items: [
      {label:"芯片", value:"MediaTek 天玑9400"},
      {label:"制程", value:"台积电3nm第二代"},
      {label:"CPU", value:"1×Cortex-X925@3.63GHz + 3×X4 + 4×A720"},
      {label:"内存", value:"12GB / 16GB"},
      {label:"存储", value:"256GB / 512GB / 1TB"}
    ]},
    { group: "影像", items: [
      {label:"主摄", value:"50MP LYT-900，哈苏自然色彩优化"},
      {label:"超广角", value:"50MP LYT-600，广角微距两用"},
      {label:"长焦", value:"50MP 3倍光学 / 6倍潜望长焦"},
      {label:"哈苏专业模式", value:"RAW格式，大师级色彩预设"}
    ]},
    { group: "电池", items: [
      {label:"容量", value:"5600mAh 硅碳负极电池"},
      {label:"有线闪充", value:"100W超级闪充"},
      {label:"无线闪充", value:"50W AIRVOOC"},
      {label:"反向充电", value:"10W无线反充"}
    ]}
  ],
  detailHtml: `<div style="font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;max-width:780px;margin:0 auto">
  <div style="background:linear-gradient(135deg,#0ea5e9,#6366f1);padding:48px 32px;border-radius:20px;text-align:center;margin-bottom:32px">
    <p style="color:rgba(255,255,255,0.8);font-size:13px;letter-spacing:3px;margin:0 0 12px">OPPO</p>
    <h1 style="color:#fff;font-size:38px;margin:0 0 12px;font-weight:700">Find X8 Pro</h1>
    <p style="color:rgba(255,255,255,0.85);font-size:17px;margin:0">哈苏影像 · 天玑旗舰 · 超强续航</p>
  </div>
  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px">
    <div style="background:#f0f9ff;border-radius:12px;padding:20px;text-align:center"><div style="font-size:28px">📷</div><div style="font-weight:600;margin-top:8px">哈苏影像</div><div style="color:#0284c7;font-size:12px">联合调校专业色彩</div></div>
    <div style="background:#f0f9ff;border-radius:12px;padding:20px;text-align:center"><div style="font-size:28px">🔋</div><div style="font-weight:600;margin-top:8px">5600mAh</div><div style="color:#0284c7;font-size:12px">超大硅碳电池</div></div>
    <div style="background:#f0f9ff;border-radius:12px;padding:20px;text-align:center"><div style="font-size:28px">⚡</div><div style="font-weight:600;margin-top:8px">100W闪充</div><div style="color:#0284c7;font-size:12px">42分钟充满</div></div>
  </div>
</div>`,
  createTime: now
}

]);

print("✅ MongoDB商品详情插入完成，共" + db.product_detail.countDocuments() + "条记录");
