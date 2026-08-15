-- ============================================================
-- GC Coffee 咖啡店小程序 数据库初始化脚本 (MySQL 8)
-- 执行方式：mysql -uroot -p < sql/init.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS gc_coffee DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gc_coffee;

-- ---------- 用户 ----------
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL,
    username VARCHAR(50) DEFAULT NULL COMMENT '店长账号用户名',
    password VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt 加密',
    nickname VARCHAR(50) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    phone VARCHAR(20) DEFAULT '',
    role VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '储值余额',
    points INT NOT NULL DEFAULT 0 COMMENT '积分',
    total_spend DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计消费(等级依据)',
    default_address VARCHAR(120) DEFAULT '' COMMENT '默认配送门牌号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_openid (openid),
    KEY idx_role (role)
) ENGINE=InnoDB COMMENT='用户/会员';

-- ---------- 余额流水 ----------
CREATE TABLE IF NOT EXISTS t_balance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    change_amount DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL DEFAULT 0,
    biz_type VARCHAR(30) NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND/PACKAGE/ADJUST',
    biz_no VARCHAR(40) DEFAULT '',
    remark VARCHAR(200) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id, created_at),
    KEY idx_biz (biz_type, created_at)
) ENGINE=InnoDB COMMENT='余额流水';

-- ---------- 积分流水 ----------
CREATE TABLE IF NOT EXISTS t_points_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    change_points INT NOT NULL,
    points_after INT NOT NULL DEFAULT 0,
    biz_type VARCHAR(30) NOT NULL COMMENT 'EARN/REDEEM/ADJUST',
    biz_no VARCHAR(40) DEFAULT '',
    remark VARCHAR(200) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='积分流水';

-- ---------- 咖啡液产品 ----------
CREATE TABLE IF NOT EXISTS t_coffee_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    spec VARCHAR(50) DEFAULT '',
    desc_text VARCHAR(500) DEFAULT '',
    image VARCHAR(255) DEFAULT '',
    price DECIMAL(10,2) NOT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='咖啡液产品';

-- ---------- 咖啡液月度套餐 ----------
CREATE TABLE IF NOT EXISTS t_coffee_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    bottle_count INT NOT NULL COMMENT '每月瓶数',
    price DECIMAL(10,2) NOT NULL,
    extra_desc VARCHAR(200) DEFAULT '',
    active TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='咖啡液月度套餐';

-- ---------- 用户月度套餐(额度账户) ----------
CREATE TABLE IF NOT EXISTS t_user_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    package_name VARCHAR(80) NOT NULL,
    month CHAR(7) NOT NULL COMMENT '2025-08',
    total_quota INT NOT NULL,
    used_quota INT NOT NULL DEFAULT 0,
    amount DECIMAL(10,2) NOT NULL,
    pay_type VARCHAR(20) NOT NULL DEFAULT 'WX_MOCK' COMMENT 'BALANCE/WX_MOCK',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, month),
    KEY idx_month (month)
) ENGINE=InnoDB COMMENT='用户月度套餐额度';

-- ---------- 咖啡液预订单 ----------
CREATE TABLE IF NOT EXISTS t_coffee_reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(80) NOT NULL,
    quantity INT NOT NULL,
    package_used INT NOT NULL DEFAULT 0 COMMENT '套餐额度抵扣瓶数',
    pay_quantity INT NOT NULL DEFAULT 0 COMMENT '需支付瓶数',
    unit_price DECIMAL(10,2) NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实付金额',
    pay_type VARCHAR(20) DEFAULT 'WX_MOCK' COMMENT 'BALANCE/WX_MOCK',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/DELIVERING/DELIVERED/CANCELLED',
    delivery_date DATE NOT NULL,
    time_slot VARCHAR(30) NOT NULL,
    address VARCHAR(120) NOT NULL COMMENT '楼栋门牌号',
    contact_name VARCHAR(30) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    remark VARCHAR(200) DEFAULT '',
    staff_id BIGINT DEFAULT NULL COMMENT '认领派送员(t_user.id)',
    cancel_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id, delivery_date),
    KEY idx_delivery (delivery_date, status),
    KEY idx_staff (staff_id, status)
) ENGINE=InnoDB COMMENT='咖啡液预订单(次日派送)';

-- ---------- 饮品分类 ----------
CREATE TABLE IF NOT EXISTS t_drink_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='饮品分类';

-- ---------- 饮品商品 ----------
CREATE TABLE IF NOT EXISTS t_drink_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    desc_text VARCHAR(300) DEFAULT '',
    image VARCHAR(255) DEFAULT '',
    price DECIMAL(10,2) NOT NULL,
    tags VARCHAR(80) DEFAULT '' COMMENT '热/冰/大杯 逗号分隔',
    stock INT NOT NULL DEFAULT -1 COMMENT '-1不限',
    active TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_category (category_id)
) ENGINE=InnoDB COMMENT='饮品商品';

-- ---------- 饮品订单 ----------
CREATE TABLE IF NOT EXISTS t_drink_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PAID' COMMENT 'PAID/MAKING/READY/FINISHED/CANCELLED',
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    pay_amount DECIMAL(10,2) NOT NULL,
    pay_type VARCHAR(20) NOT NULL COMMENT 'BALANCE/WX_MOCK',
    coupon_id BIGINT DEFAULT NULL,
    pickup_code VARCHAR(10) DEFAULT '',
    remark VARCHAR(200) DEFAULT '',
    paid_at DATETIME DEFAULT NULL,
    finished_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id, created_at),
    KEY idx_status (status, created_at)
) ENGINE=InnoDB COMMENT='饮品订单';

-- ---------- 饮品订单明细 ----------
CREATE TABLE IF NOT EXISTS t_drink_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(80) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    KEY idx_order (order_id)
) ENGINE=InnoDB COMMENT='饮品订单明细';

-- ---------- 优惠券模板 ----------
CREATE TABLE IF NOT EXISTS t_coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'FULL_REDUCTION满减/DISCOUNT折扣/CASH现金券',
    value DECIMAL(10,2) NOT NULL,
    min_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛',
    valid_days INT NOT NULL DEFAULT 7,
    total_count INT NOT NULL DEFAULT 0 COMMENT '0不限',
    issued_count INT NOT NULL DEFAULT 0,
    per_user_limit INT NOT NULL DEFAULT 1,
    redeem_points INT NOT NULL DEFAULT 0 COMMENT '积分兑换所需, 0不可兑换',
    active TINYINT NOT NULL DEFAULT 1,
    start_at DATETIME DEFAULT NULL,
    end_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='优惠券模板';

-- ---------- 用户优惠券 ----------
CREATE TABLE IF NOT EXISTS t_user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(20) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    min_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
    source VARCHAR(20) NOT NULL DEFAULT 'CLAIM' COMMENT 'CLAIM/REDEEM/GIFT',
    expire_at DATETIME NOT NULL,
    used_at DATETIME DEFAULT NULL,
    used_order_no VARCHAR(40) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id, status)
) ENGINE=InnoDB COMMENT='用户优惠券';

-- ---------- 月度/季度活动 ----------
CREATE TABLE IF NOT EXISTS t_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY',
    subtitle VARCHAR(200) DEFAULT '',
    content TEXT,
    image VARCHAR(255) DEFAULT '',
    start_at DATE NOT NULL,
    end_at DATE NOT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='月度/季度活动';

-- ---------- 海报 ----------
CREATE TABLE IF NOT EXISTS t_poster (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    image VARCHAR(255) NOT NULL,
    link_type VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/ACTIVITY',
    link_id BIGINT DEFAULT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='海报';

-- ---------- 系统设置 ----------
CREATE TABLE IF NOT EXISTS t_setting (
    key_name VARCHAR(50) PRIMARY KEY,
    value_text TEXT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统设置';

-- ============================================================
-- 演示数据
-- ============================================================

INSERT INTO t_drink_category (name, sort) VALUES
('经典咖啡', 1),
('拿铁系列', 2),
('手冲冷萃', 3),
('特调果饮', 4);

INSERT INTO t_drink_product (category_id, name, desc_text, price, tags, stock, active, sort) VALUES
(1, '美式咖啡', '醇厚黑咖，提神醒脑', 16.00, '热/冰', -1, 1, 1),
(1, '浓缩咖啡', '双份浓缩，浓郁到底', 15.00, '热', -1, 1, 2),
(2, '生椰拿铁', '椰香与咖啡的丝滑碰撞，门店爆款', 22.00, '冰,推荐', -1, 1, 1),
(2, '燕麦拿铁', '燕麦奶香，植物基更轻盈', 24.00, '热/冰', -1, 1, 2),
(2, '焦糖拿铁', '焦糖酱拉花，甜而不腻', 23.00, '热/冰', -1, 1, 3),
(3, '冷萃咖啡', '12小时低温萃取，顺滑微酸', 25.00, '冰', -1, 1, 1),
(3, '手冲耶加雪菲', '花果香明显，风味明亮', 28.00, '热', -1, 1, 2),
(4, '柠檬气泡美式', '气泡感十足，清爽解暑', 21.00, '冰,新品', -1, 1, 1),
(4, '蜜桃乌龙茶', '白桃果肉+乌龙茶底', 19.00, '冰,新品', -1, 1, 2);

INSERT INTO t_coffee_product (name, spec, desc_text, price, active, sort) VALUES
('冷萃咖啡液', '500ml/瓶', '0糖0脂，冷藏可存7天，兑水兑奶即饮', 15.00, 1, 1),
('意式浓缩液', '500ml/瓶', '门店同款豆，风味醇厚，适合做拿铁', 18.00, 1, 2),
('SOE 精品咖啡液', '250ml/袋(10袋装)', '单一产区豆，花果香突出', 68.00, 1, 3);

INSERT INTO t_coffee_package (name, bottle_count, price, extra_desc, active, sort) VALUES
('轻享月卡', 15, 198.00, '每月15瓶，合13.2元/瓶', 1, 1),
('畅饮月卡', 30, 360.00, '每月30瓶，合12元/瓶', 1, 2);

INSERT INTO t_coupon_template (name, type, value, min_amount, valid_days, total_count, per_user_limit, redeem_points, active) VALUES
('新人立减5元', 'CASH', 5.00, 0, 30, 0, 1, 0, 1),
('满30减6', 'FULL_REDUCTION', 6.00, 30.00, 7, 0, 3, 0, 1),
('全场9折券', 'DISCOUNT', 0.90, 0, 7, 0, 1, 100, 1),
('满50减10', 'FULL_REDUCTION', 10.00, 50.00, 14, 0, 2, 200, 1);

INSERT INTO t_activity (title, type, subtitle, content, start_at, end_at, active, sort) VALUES
('本月畅饮季：买咖啡液送杯套', 'MONTHLY', '本月预订咖啡液满10瓶送限定杯套', '活动期间，凡预订咖啡液单次满10瓶，即赠限定款杯套一个，颜色随机，送完即止。\n\n· 活动时间：本月1日-月底\n· 适用商品：全部咖啡液产品\n· 领取方式：随次日派送一同送达', DATE_FORMAT(NOW(), '%Y-%m-01'), LAST_DAY(NOW()), 1, 1),
('季度储值返利 充300送30', 'QUARTERLY', '本季度储值最高返 12%', 'Q3 储值返利活动：\n\n· 充值 200 元送 15 元\n· 充值 300 元送 30 元\n· 充值 500 元送 60 元\n\n充值金额即时到账余额，可叠加会员积分使用。', DATE_SUB(NOW(), INTERVAL 1 MONTH), DATE_ADD(NOW(), INTERVAL 2 MONTH), 1, 2);

INSERT INTO t_poster (title, image, link_type, link_id, active, sort) VALUES
('冷萃咖啡液 · 次日送达', '', 'ACTIVITY', 1, 1, 1),
('季度储值返利最高12%', '', 'ACTIVITY', 2, 1, 2),
('生椰拿铁 门店爆款', '', 'NONE', NULL, 1, 3);
