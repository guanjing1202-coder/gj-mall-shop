# GJ Mall Shop

GJ Mall Shop 是一个前后端分离的多端电商商城项目，包含 PC 商城、移动端 H5/小程序、管理后台和 Spring Boot 后端服务。项目以单体多模块后端为核心，配套 MySQL、Redis、MongoDB、RabbitMQ 完成商品、用户、购物车、订单、支付、营销、后台管理等业务能力。

## 项目结构

```text
gj-mall-shop/
├─ gj-mall-server/      # 后端：Spring Boot 2.7 + JDK 8 + Maven 多模块
├─ gj-mall-web/         # PC 商城：Vue 3 + Vite + TypeScript + Element Plus
├─ gj-mall-admin/       # 管理后台：Vue 3 + Vite + TypeScript + Ant Design Vue
├─ gj-mall-uni/         # 移动端：uni-app Vue 3，支持 H5 和微信小程序构建
├─ docs/
│  └─ db/               # 数据库建表、测试数据、Mongo 商品详情数据
└─ README.md
```

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、Java 8、Maven、MyBatis-Plus、JWT、Knife4j、Redisson |
| PC 商城 | Vue 3、Vite 5、TypeScript、Pinia、Vue Router、Element Plus |
| 管理后台 | Vue 3、Vite 5、TypeScript、Pinia、Vue Router、Ant Design Vue |
| 移动端 | uni-app Vue 3、TypeScript、Pinia |
| 数据与中间件 | MySQL 8、Redis、MongoDB、RabbitMQ |

## 当前功能概览

项目主链路已经覆盖一个商城从浏览、下单到履约、售后和后台运营的核心流程：

| 模块 | 能力 |
| --- | --- |
| 商品与搜索 | 商品分类、品牌、SKU、库存、商品详情、搜索联想、移动端搜索适配 |
| 购物车与结算 | 购物车管理、优惠券试算、运费展示、发票信息、PC/H5 下单 |
| 订单履约 | 订单列表与详情、支付后履约、发货、物流提示、确认收货、订单状态流转 |
| 支付退款 | Mock 支付、微信 Native Pay、支付宝 Page Pay、支付回调记录、回调签名样例、失败回调重放、支付状态同步、退款资格判断、全额退款确认、退款成功消息通知 |
| 售后 | 用户申请售后、后台创建售后、审核、退货物流、确认收货、退款记录、售后资格校验 |
| 营销 | 优惠券、秒杀活动、活动库存、后台营销管理 |
| 消息中心 | 订单、物流、支付、售后消息，未读统计，全部已读，清空已读 |
| 后台运营 | 商品、库存、订单、物流、售后、支付、报表、上传存储、系统配置、角色权限、操作日志 |
| 多端适配 | PC 商城、管理后台、Uni H5，PC 商城已做移动浏览器响应式优化 |

## 本地环境

当前本机项目依赖安装在 D 盘，仓库内脚本会优先使用以下默认位置：

```text
D:\dev
├─ redis
├─ mongodb
├─ erlang
└─ rabbitmq
```

已安装组件：

| 组件 | 版本 | 地址 / 端口 | 账号 |
| --- | --- | --- | --- |
| MySQL | 8.0.44 | `127.0.0.1:3306` | `root / 123456` |
| Redis | 5.0.14.1 | `127.0.0.1:6379` | 无密码 |
| MongoDB | 7.0.14 | `127.0.0.1:27017` | 无密码 |
| RabbitMQ | 3.13.7 | `127.0.0.1:5672` | `guest / guest` |
| RabbitMQ Management | 3.13.7 | `http://127.0.0.1:15672` | `guest / guest` |

项目内还包含本地便携 JDK 和 Maven：

```text
.codex-tools/
├─ jdk8/
└─ apache-maven-3.9.9/
```

## 快速启动

如果你在当前机器上运行，优先使用仓库内脚本：

```powershell
# 启动 MySQL、Redis、MongoDB、RabbitMQ、后端、PC 商城、管理后台、Uni H5
PowerShell -ExecutionPolicy Bypass -File scripts\start-all.ps1

# 查看全部服务状态
PowerShell -ExecutionPolicy Bypass -File scripts\status-all.ps1

# 停止后端和三个前端 dev server
PowerShell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
```

`start-deps.ps1` 会尝试启动 Redis、MongoDB、RabbitMQ。MySQL 因本机安装方式不固定，脚本只检查 `127.0.0.1:3306` 是否可用；如果未监听，需要先手动启动本机 MySQL 服务。

启动成功后访问：

| 服务 | 地址 |
| --- | --- |
| 后端健康检查 | `http://127.0.0.1:8080/api/v1/ping` |
| 接口文档 | `http://127.0.0.1:8080/doc.html` |
| PC 商城 | `http://127.0.0.1:5173` |
| 管理后台 | `http://127.0.0.1:5174` |
| Uni H5 | `http://127.0.0.1:5175` |
| RabbitMQ 管理台 | `http://127.0.0.1:15672` |

管理后台默认账号：

```text
用户名：admin
密码：123456
```

## 数据库初始化

如果拉取了新代码，建议重新导入最新测试数据：

```powershell
$mysql = '<你的 mysql.exe 路径>'
$mongosh = '<你的 mongosh.cmd 路径>'

& $mysql -uroot -p123456 -e "DROP DATABASE IF EXISTS gj_mall; CREATE DATABASE gj_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
cmd /c "`"$mysql`" -uroot -p123456 --default-character-set=utf8mb4 gj_mall < docs\db\01-schema.sql"
cmd /c "`"$mysql`" -uroot -p123456 --default-character-set=utf8mb4 gj_mall < docs\db\02-test-data.sql"
& $mongosh gj_mall_doc docs\db\03-mongo-details.js --quiet
```

数据源配置位于：

```text
gj-mall-server/gj-mall-app/src/main/resources/application-dev.yml
```

默认连接信息：

```text
MySQL:   jdbc:mysql://127.0.0.1:3306/gj_mall
Redis:   127.0.0.1:6379
MongoDB: mongodb://127.0.0.1:27017/gj_mall_doc
RabbitMQ: 127.0.0.1:5672
```

## 后端开发

后端是 Maven 多模块项目，入口模块为 `gj-mall-app`。

```text
gj-mall-server/
├─ gj-mall-common       # 通用返回、异常、枚举、常量
├─ gj-mall-framework    # JWT、拦截器、配置、跨域、MyBatis、Knife4j
├─ gj-mall-user         # 用户、登录、地址、收藏、浏览历史、消息
├─ gj-mall-product      # 商品、分类、品牌、SKU、库存、评价、搜索
├─ gj-mall-cart         # 购物车
├─ gj-mall-order        # 订单、售后、物流、评价、超时消息
├─ gj-mall-pay          # 支付策略、支付记录、回调
├─ gj-mall-marketing    # 优惠券、秒杀
├─ gj-mall-search       # 搜索体验：热搜词、搜索建议、同义词兜底
├─ gj-mall-admin-api    # 后台管理 API
└─ gj-mall-app          # Spring Boot 启动模块
```

构建后端：

```powershell
cd D:\caludeWorkspace\gj-mall-shop\gj-mall-server

$root = 'D:\caludeWorkspace\gj-mall-shop'
$jdk = Get-ChildItem "$root\.codex-tools\jdk8" -Directory | Select-Object -First 1
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$root\.codex-tools\apache-maven-3.9.9\bin;$env:Path"

mvn -DskipTests package
```

手动启动后端：

```powershell
cd D:\caludeWorkspace\gj-mall-shop\gj-mall-server\gj-mall-app
java -jar target\gj-mall-app.jar
```

## 前端开发

三个前端目录都已安装 `node_modules`。如需重新安装依赖，进入对应目录执行 `npm ci`。

### PC 商城

```powershell
cd D:\caludeWorkspace\gj-mall-shop\gj-mall-web
npm run dev
npm run build
```

默认端口：`5173`

### 管理后台

```powershell
cd D:\caludeWorkspace\gj-mall-shop\gj-mall-admin
npm run dev
npm run build
```

默认端口：`5174`

### Uni H5 / 微信小程序

```powershell
cd D:\caludeWorkspace\gj-mall-shop\gj-mall-uni
npm run dev:h5
npm run build:h5
npm run dev:mp-weixin
npm run build:mp-weixin
```

H5 默认端口：`5175`

微信小程序产物在 `dist/dev/mp-weixin` 或 `dist/build/mp-weixin`，需要使用微信开发者工具打开。

## 常用命令

```powershell
# 拉取最新代码
git pull --ff-only

# 查看中间件状态
PowerShell -ExecutionPolicy Bypass -File scripts\status-deps.ps1

# 只启动中间件
PowerShell -ExecutionPolicy Bypass -File scripts\start-deps.ps1

# 只停止中间件
PowerShell -ExecutionPolicy Bypass -File scripts\stop-deps.ps1

# 查看全部项目服务状态
PowerShell -ExecutionPolicy Bypass -File scripts\status-all.ps1
```

## 鉴权说明

项目使用 JWT 鉴权，前端请求会携带：

```text
Authorization: Bearer <accessToken>
X-Client-Type: pc | h5 | mp-weixin | admin
```

后台和用户端使用不同来源标识，后端会根据 `X-Client-Type` 区分请求来源。登录后会返回 `accessToken` 和 `refreshToken`，前端在 token 过期时可调用刷新接口重新获取。

## 支付与订单

当前支付模块采用策略模式：

```text
PayService
├─ MockPayStrategy
├─ WechatPayStrategy
└─ AlipayStrategy
```

开发环境默认适合使用 Mock 支付。后台支付管理支持查看支付汇总、回调验签样例、最近渠道回调、失败回调重放、按成功回调同步支付状态、退款资格展示、全额退款确认和退款失败重试。后台直接全额退款成功后会写入用户消息中心，用户可在 PC/H5 消息中心看到“退款已完成”通知。

如需启用真实支付，先将 `mall.pay.mode` 配置为 `real`。支付配置优先读取后台系统配置表 `sys_config`，没有配置时再读取 Spring 配置或环境变量。真实渠道需要补齐以下配置：

```text
mall.pay.callback.require-signature=true
mall.pay.callback.secret=<自定义回调密钥>

mall.pay.wechat.app-id=<微信应用 AppID>
mall.pay.wechat.mch-id=<微信商户号>
mall.pay.wechat.api-v3-key=<微信 APIv3 密钥>
mall.pay.wechat.merchant-serial-no=<微信商户证书序列号>
mall.pay.wechat.private-key-path=<服务进程可读取的商户私钥文件路径>
mall.pay.wechat.notify-url=https://<域名>/api/pay/callback/wechat

mall.pay.alipay.app-id=<支付宝应用 AppID>
mall.pay.alipay.private-key=<应用私钥>
mall.pay.alipay.alipay-public-key=<支付宝公钥>
mall.pay.alipay.notify-url=https://<域名>/api/pay/callback/alipay
```

真实支付回调必须使用公网可访问的 HTTPS 地址。微信回调路径固定为 `/api/pay/callback/wechat`，支付宝回调路径固定为 `/api/pay/callback/alipay`。本地单测只能验证 SDK 调用封装、配置校验、签名/回调解析和订单入账逻辑；真实扣款、渠道验签、回调送达仍需要有效商户号、证书、私钥和 HTTPS 回调域名做联调。

订单模块包含普通下单、秒杀下单、支付结果、售后申请、订单评价、物流查询等能力。库存和订单超时释放依赖 Redis 与 RabbitMQ。

## 常见问题

### 端口被占用

先查看当前状态：

```powershell
PowerShell -ExecutionPolicy Bypass -File scripts\status-all.ps1
```

如果已有旧服务运行，可以先停止再启动：

```powershell
PowerShell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
PowerShell -ExecutionPolicy Bypass -File scripts\start-all.ps1
```

### 后端连接不上数据库

确认中间件已启动：

```powershell
PowerShell -ExecutionPolicy Bypass -File scripts\status-deps.ps1
```

再确认 `application-dev.yml` 中 MySQL、Redis、MongoDB、RabbitMQ 的连接信息是否和本机一致。

### 前端页面打开但接口失败

确认后端健康检查能访问：

```text
http://127.0.0.1:8080/api/v1/ping
```

三个前端项目的 Vite 代理会把 `/api` 转发到 `http://127.0.0.1:8080`，所以后端没启动时页面能打开，但数据接口会失败。

### 拉新代码后功能异常

推荐顺序：

```powershell
git pull --ff-only
PowerShell -ExecutionPolicy Bypass -File scripts\stop-all.ps1

# 重新导入 docs/db 下的最新 SQL 和 Mongo 数据
# 然后重新构建后端、前端

PowerShell -ExecutionPolicy Bypass -File scripts\start-all.ps1
```

## 当前验证结果

当前工作区已用以下命令做过本地验证：

```powershell
# 一键执行主要验证
PowerShell -ExecutionPolicy Bypass -File scripts\verify-all.ps1
```

也可以按模块拆开执行：

```powershell
# 服务状态
PowerShell -ExecutionPolicy Bypass -File scripts\status-all.ps1

# 后端目标单测
cd gj-mall-server
mvn -pl gj-mall-pay,gj-mall-order,gj-mall-admin-api -am -Dtest=PayServiceImplTest,AfterSaleServiceImplTest,AdminPaymentCallbackVOTest -DfailIfNoTests=false test

# 后端打包
mvn -DskipTests package

# 前端工具测试
cd ..\gj-mall-web
npm test
cd ..\gj-mall-admin
npm test
cd ..\gj-mall-uni
npm test
npm run build:h5
```

支付与售后主流程已通过本地浏览器联调，覆盖 PC/H5 支付结果页、PC/H5 订单详情售后入口、管理后台支付全额退款确认弹窗。联调截图和一次性验证脚本位于 `.codex-run/`，该目录仅作为本机运行产物，不纳入版本管理。

当前环境状态：

```text
Backend              127.0.0.1:8080  OK
Web                  127.0.0.1:5173  OK
Admin                127.0.0.1:5174  OK
UniH5                127.0.0.1:5175  OK
MySQL                127.0.0.1:3306  OK
Redis                127.0.0.1:6379  OK
MongoDB              127.0.0.1:27017 OK
RabbitMQ             127.0.0.1:5672  OK
RabbitMQManagement   127.0.0.1:15672 OK
```
