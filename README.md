# GC Coffee 咖啡店微信小程序（全栈）

面向小区咖啡店的微信小程序：**咖啡液次日派送预订 + 饮品下单** 双主入口，极简奶咖色 UI，管理后台内嵌小程序（店长角色），后端 Java 17 + Spring Boot 3 + MySQL 8。

```
GC_Coffee/
├── backend/                 # Spring Boot 3.4 后端
│   ├── src/main/java/com/gccoffee/
│   │   ├── common/          # 统一响应/分页/异常
│   │   ├── config/          # 拦截器、MyBatis-Plus、Jackson、数据初始化
│   │   ├── security/        # JWT 认证（用户 token + 店长 token 双通道）
│   │   ├── util/            # 订单号、微信登录（模拟/真实可切换）
│   │   └── module/
│   │       ├── user/        # 用户、储值余额、积分、等级
│   │       ├── coffee/      # 咖啡液产品、月度套餐、预订(次日派送)
│   │       ├── drink/       # 饮品分类/商品/订单(取餐码)
│   │       ├── coupon/      # 优惠券模板、领取/积分兑换/核销
│   │       ├── activity/    # 月度/季度活动、海报
│   │       └── admin/       # 台账统计、商品/营销/会员/派送/设置管理
│   ├── sql/init.sql         # 建库建表 + 演示数据
│   └── docker-compose.yml   # MySQL 8 一键起库
└── miniprogram/             # 微信原生小程序
    ├── custom-tab-bar/      # 悬浮胶囊式自定义 TabBar
    ├── utils/               # request 封装、格式化工具
    └── pages/
        ├── home/            # 首页：两大主入口 + 海报轮播 + 快捷入口
        ├── menu/            # 饮品点单（分类 + 购物车）
        ├── checkout/        # 结算（优惠券/余额/模拟支付）
        ├── orders/          # 订单列表  order-detail/ 订单详情(取餐码)
        ├── reserve/         # 咖啡液预订（次日派送 + 额度抵扣）
        ├── reserve-records/ # 预订记录
        ├── monthly/         # 月度咖啡液详情：花费/剩余/明细
        ├── member/          # 会员中心（储值/积分/等级）
        ├── coupons/         # 优惠券（领券中心 + 积分兑换）
        ├── activity/        # 月度/季度活动 + 海报墙
        ├── recharge/        # 模拟充值
        ├── profile/         # 个人中心（含店长入口）
        └── admin/           # 管理端：看板/台账/派送/订单/商品/优惠券/活动海报/会员/设置
```

## 一、快速开始

### 1. 启动数据库

方式 A（推荐，Docker）：

```bash
cd backend
docker compose up -d        # MySQL 8 自动执行 sql/init.sql 初始化
```

方式 B（本机 MySQL）：执行 `backend/sql/init.sql`，并把 `application.yml` 的账号密码改成你的。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或打包运行：mvn -DskipTests package && java -jar target/gc-coffee-backend-1.0.0.jar
```

启动后自动初始化：
- 店长账号：`admin / admin123`（管理后台登录）
- 演示用户：微信登录时使用 code `mock_demo`（余额200元/500积分）
- 默认设置：派送时段 07:00-21:00 每两小时一段、小区名「阳光花园小区」等

验证：`curl http://127.0.0.1:8080/api/public/health` 返回 `up`。

### 3. 运行小程序

1. 微信开发者工具 → 导入项目 → 选择 `miniprogram/` 目录（AppID 先用测试号或 `touristappid`）。
2. 详情 → 本地设置 → 勾选 **「不校验合法域名」**（本地开发用 http）。
3. `miniprogram/app.js` 中 `baseUrl` 默认 `http://127.0.0.1:8080`；真机预览改为电脑局域网 IP。
4. 本机未配置微信 AppID 时后端自动走**模拟登录**（稳定 mock openid），无需真实微信账号即可体验全流程；后端配置了真实 appid/secret 后，把 `app.js` 的 `useRealWxLogin` 改为 `true` 即切换真实登录。
5. 管理后台入口：我的 → 「店长管理后台」→ 账号 `admin` / 密码 `admin123`。

## 二、核心业务规则（已实现）

> 本项目已通过端到端冒烟验证：真实 MySQL 上跑通 登录/充值/月卡购买/额度抵扣+混合支付预订/取消退款/月度统计/领券核销下单/积分/看板/台账CSV/派送状态机/订单状态机/会员调整/设置/越权拦截 共 38 项检查（`backend` 编译打包通过，`miniprogram` 26 个页面配置与语法校验通过）。

| 能力 | 规则 |
| --- | --- |
| 咖啡液预订 | 当日预订、**次日**派送；配送日期默认明天起 7 天内可选，时段由后台设置 |
| 月度套餐 | 月卡（如 30瓶/月）预付费购买，每人每月一张；预订时**额度优先抵扣**，超出部分按次购买（余额/微信） |
| 混合支付 | 一笔预订可「额度抵 N 瓶 + 支付 M 瓶」，金额按 支付瓶数×单价 计算 |
| 月度详情页 | 本月花费（套餐+按次）=、剩余瓶数、每单明细、额度进度条；额度当月有效、次月清零 |
| 派送状态机 | 待确认 → 已确认 → 派送中 → 已送达；送达前一天用户可自行取消（退款+还额度） |
| 饮品订单 | 即点即做，取餐码=订单号后4位；状态：已支付 → 制作中 → 待取餐 → 已完成 |
| 储值+积分 | 充值得余额；消费 1 元=1 积分（可配）；积分可兑换优惠券 |
| 优惠券 | 满减/折扣/现金券三类；后台设总量、限领、有效期、积分兑换价；下单核销、取消退回 |
| 会员等级 | 按累计消费自动：普通→银卡(300)→金卡(1000)→黑金(3000)，阈值可在后台改 |
| 台账 | 饮品/咖啡液/充值/套餐四类流水，按日期区间汇总收入与笔数，支持复制 CSV 到 Excel |
| 海报 | 后台发布，首页轮播；可关联活动跳转详情 |
| 月度/季度活动 | 后台发布，活动页按类型展示，进行中+即将开始 |

## 三、主要 API（前缀 /api，除 auth/public 外均需 Bearer token）

| 模块 | 接口 |
| --- | --- |
| 登录 | `POST /auth/login`（wx code，mock 模式下 code 即 openid）、`POST /auth/admin-login` |
| 公开 | `GET /public/home`（门店/公告/海报）、`GET /public/settings` |
| 用户 | `GET/PUT /user/me`、`POST /user/recharge`、`GET /user/balance-records`、`GET /user/points-records` |
| 咖啡液 | `GET /coffee/products`、`GET /coffee/packages`、`GET /coffee/package/current`、`POST /coffee/package/buy`、`POST /coffee/reserve`、`GET /coffee/reservations`、`POST /coffee/reservations/{id}/cancel`、`GET /coffee/monthly`、`GET /coffee/delivery-slots` |
| 饮品 | `GET /drink/menu`、`POST /drink/order`、`GET /drink/orders`、`GET /drink/orders/{id}`、`POST /drink/orders/{id}/cancel` |
| 优惠券 | `GET /coupon/templates`、`POST /coupon/{id}/claim`、`POST /coupon/{id}/redeem`、`GET /coupon/mine`、`GET /coupon/usable` |
| 活动 | `GET /activity/list?type=`、`GET /activity/posters` |
| 管理-看板 | `GET /admin/stats/overview|trend|top` |
| 管理-台账 | `GET /admin/stats/ledger`、`GET /admin/stats/ledger/csv` |
| 管理-目录 | `/admin/catalog/drink-categories|drink-products|coffee-products|coffee-packages`（增删改查） |
| 管理-营销 | `/admin/marketing/coupons|activities|posters`（增删改查） |
| 管理-会员 | `GET /admin/members`、`POST /admin/members/{id}/balance|points|status` |
| 管理-运营 | `GET /admin/ops/reservations|orders`、`PUT .../status|cancel`、`GET/PUT /admin/ops/settings` |

## 四、上线前还缺什么（按优先级）

1. **微信小程序 AppID**：注册小程序账号，替换 `project.config.json` 的 `touristappid`，并在后端 `application.yml` 配置 `app.wx.appid/secret` 开启真实登录。
2. **服务器 + 备案域名 + HTTPS**：小程序正式版只允许 https 请求合法域名；需一台云服务器（2C4G 起）+ 已备案域名 + 证书（Nginx 反代到 8080），并在小程序后台「开发管理→服务器域名」配置 request 合法域名。
3. **微信支付商户号**：申请微信支付商户号（mchid + APIv3 密钥），在支付服务中把「模拟支付」替换为统一下单/回调验签/退款接口；当前所有支付走 `WX_MOCK` 直接标记成功。
4. **生产数据库**：云 MySQL 8 或自建，修改 `application.yml` 数据源与 **JWT 密钥**（`app.jwt.secret` 用 32 字节以上随机 BASE64）。
5. **图片资源**：商品图/海报当前是渐变占位 + emoji，需接入对象存储（腾讯云 COS/阿里 OSS）或图床，后台直接填 URL 即可生效。
6. **订阅消息**：派送提醒、出餐提醒需要在小程序后台申请订阅消息模板并在代码里接入 `wx.requestSubscribeMessage`（当前未接）。
7. **店长账号安全**：上线前必须修改默认密码（admin/admin123）；可自行在数据库或代码中更换。
8. **门牌号选择增强（可选）**：当前手填门牌号；如需地图选点需申请腾讯位置服务 key。
9. **合规资质**：自制咖啡液属食品经营，需营业执照 + 食品经营许可证；小程序类目选择「食品」相关类目。
10. **数据备份**：建议每日备份 MySQL + 开启云盘快照。
11. **真机联调**：把 `baseUrl` 改为局域网 IP 在真机预览一轮，再切正式域名。
12. **会员等级权益深化（可选）**：当前等级仅展示与门槛计算，等级专属折扣/专属券可在优惠券发放时叠加实现。

## 六、附注

- 项目根目录的 `tools/` 为本机开发辅助（Maven 发行版、本地仓库、测试 MySQL 数据目录），可随时删除，不影响项目本身。
- Maven 编译已内置 Lombok 显式注解处理器路径，兼容 JDK 21/23 下 javac 不自动发现处理器的问题。
- MyBatis-Plus 3.5.9 起分页插件在独立模块 `mybatis-plus-jsqlparser`，已在 pom 中声明。

## 七、常见问题

- **Q：开发者工具提示请求失败？** 后端没启动或 `baseUrl` 不对；工具需勾选「不校验合法域名」。
- **Q：每次登录都是新用户？** 模拟登录下 openid 按设备缓存，同一工具内稳定；切换「演示账号」入口在 我的→底部。
- **Q：余额支付提示不足？** 先到 会员→充值（模拟支付即时到账）。
- **Q：台账 CSV 怎么用？** 管理端台账页点「复制CSV」，粘贴到记事本保存为 `.csv`，Excel 直接打开。
- **Q：月度套餐额度当月没用完？** 次月自动清零（业务设定如此，可在代码中调整）。
