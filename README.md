# gj-mall-shop

多端电商商城：**PC Web + H5 + 微信小程序 + 管理后台**，前后端分离。

## 一、项目结构

```
gj-mall-shop/
├── gj-mall-server/     # 后端：Spring Boot 2.7 + JDK 8 + Maven 多模块单体
├── gj-mall-web/        # PC 商城：Vue 3 + Vite + TS + Element Plus      端口 5173
├── gj-mall-uni/        # H5 + 微信小程序：uni-app Vue 3 + TS              端口 5175 (H5)
├── gj-mall-admin/      # 管理后台：Vue 3 + Vite + TS + Ant Design Vue    端口 5174
├── docs/               # 接口文档、ER 图、SQL
└── README.md
```

## 二、本地中间件（已安装）

| 组件 | 版本 | 端口 | 安装目录 | 说明 |
|------|------|------|----------|------|
| MySQL | 8.0.44 | 3306 | `C:\Program Files\MySQL\MySQL Server 8.0` | 用户 `root` / 密码 `123456`；库 `gj_mall` |
| Redis | 5.0.14 | 6379 | `D:\dev\redis` | Windows 服务名 `redis` |
| MongoDB | 7.0.14 | 27017 | `D:\dev\mongodb` | 服务名 `MongoDB`；库 `gj_mall_doc` |
| Erlang OTP | 26.2.5 | - | `D:\dev\erlang` | RabbitMQ 依赖 |
| RabbitMQ | 3.13.7 | 5672 / 15672 | `D:\dev\rabbitmq` | 服务名 `RabbitMQ`；管理台 http://localhost:15672 (guest/guest) |

`RABBITMQ_BASE` 已设为 `D:\dev\rabbitmq\data`，`ERLANG_HOME` 已设为 `D:\dev\erlang`（机器级环境变量）。

### 服务启停

```powershell
# 启动 / 停止单个服务（管理员 PowerShell）
Start-Service redis,MongoDB,RabbitMQ,MySQL80
Stop-Service  RabbitMQ,MongoDB,redis  # MySQL80 通常常驻

# 状态总览
Get-Service redis,MongoDB,RabbitMQ,MySQL80
```

## 三、后端（gj-mall-server）

### 模块划分（11 个子模块）

```
gj-mall-server/
├── gj-mall-common         # 通用：Result/异常/常量/枚举
├── gj-mall-framework      # 框架封装：JWT/Redis/MQ/Mybatis/Knife4j/CORS
├── gj-mall-user           # 用户中心
├── gj-mall-product        # 商品中心
├── gj-mall-cart           # 购物车
├── gj-mall-order          # 订单中心
├── gj-mall-pay            # 支付（策略模式：Mock / Wechat / Alipay）
├── gj-mall-marketing      # 优惠券 / 秒杀 / 满减
├── gj-mall-search         # 搜索（暂用 MySQL，后期接 ES）
├── gj-mall-admin-api      # 后台 API 聚合
└── gj-mall-app            # Main 启动模块
```

### 启动

```bash
cd gj-mall-server
mvn -DskipTests clean install
cd gj-mall-app
mvn spring-boot:run
# 或
java -jar target/gj-mall-app.jar
```

启动成功后：
- 健康检查：http://localhost:8080/api/v1/ping
- 接口文档：http://localhost:8080/doc.html
- Actuator：http://localhost:8080/actuator/health

### 配置文件

- `gj-mall-app/src/main/resources/application.yml` — 通用
- `gj-mall-app/src/main/resources/application-dev.yml` — 本机连接串（已对接 4 个中间件）

## 四、前端

### 4.1 PC Web 商城 (gj-mall-web)

```bash
cd gj-mall-web
npm install     # 已装
npm run dev     # 起 http://localhost:5173
npm run build
```

- 框架：Vue 3 + Vite 5 + TS + Pinia + Vue Router 4 + Element Plus
- 自动按需引入 Element Plus
- 已配置 `/api` 代理到 `http://127.0.0.1:8080`

### 4.2 管理后台 (gj-mall-admin)

```bash
cd gj-mall-admin
npm install     # 已装
npm run dev     # 起 http://localhost:5174
```

- 框架：Vue 3 + Vite + TS + Ant Design Vue
- 已带：登录页、布局、仪表盘、商品/订单/用户管理空壳页
- 默认登录 `admin / 123456`（mock 通过，后端 RBAC 接口待实现）

### 4.3 移动端 H5 + 微信小程序 (gj-mall-uni)

```bash
cd gj-mall-uni
npm install                     # 已装
npm run dev:h5                  # H5 调试，起 http://localhost:5175
npm run dev:mp-weixin           # 编译产物在 dist/dev/mp-weixin/，用微信开发者工具打开
npm run build:h5
npm run build:mp-weixin
```

- 框架：uni-app Vue 3 + TS + Pinia
- 已建 4 个 tab 页：首页/分类/购物车/我的 + 商品详情页
- 微信小程序 AppID 占位：`wxXXXXXXXXXXXXXXXX`（后续在 `src/manifest.json` 替换为真实值）

## 五、关键设计

### 鉴权

- 用户端 / 后台端各自独立的 JWT 签发与拦截器
- access token 7200s（2h），refresh token 7d
- 客户端通过请求头 `X-Client-Type: pc | h5 | mp-weixin | admin` 标识来源

### 支付（策略模式）

```
PayService.pay(order, channel)
  ├─ MockPayStrategy        # 当前默认（mall.pay.mode=mock）
  ├─ WechatPayStrategy      # 后续接入 wechatpay-java
  └─ AlipayStrategy         # 后续接入 alipay-sdk-java
```

切换为真实支付：`application-dev.yml` 改 `mall.pay.mode: real` 并配置 AppID/MchID/证书。

### 库存防超卖

- 下单时 Redis Lua 脚本预扣（原子）
- 订单超时未支付 → RabbitMQ 延迟队列释放库存

## 六、下一步开发计划（按优先级）

1. **数据库 schema**：创建用户/商品/订单/营销/系统等表，写入 `docs/db/01-schema.sql`
2. **用户模块**：注册/登录（账密 + 手机号 + 微信 code 换 openId）、JWT 签发、地址管理
3. **商品模块**：分类树、商品 CRUD、SPU/SKU、详情走 MongoDB
4. **购物车 + 下单 + Mock 支付** 闭环
5. **后台 RBAC** 与商品/订单管理实现
6. **营销模块**：优惠券、秒杀
7. 前端三端对接真实接口

## 七、故障排查

- **后端启动失败连不上 MySQL**：确认 `application-dev.yml` 密码与 MySQL 一致；用 `mysql -uroot -p` 手测
- **RabbitMQ 服务起不来（错误 1067）**：确认 `RABBITMQ_BASE` 环境变量已设到 `D:\dev\rabbitmq\data`，且该目录 SYSTEM 用户有写权限
- **前端 dev 启动报端口冲突**：改对应 `vite.config.ts` 里的 `server.port`

---

🚀 由 Claude 协助搭建。
