-- ==================== 积分兑换商城模块 ====================
-- 执行此脚本前请确保已执行主数据库初始化脚本

USE cityquest_db;

-- 创建商品分类表
CREATE TABLE IF NOT EXISTS product_category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description TEXT COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    status TINYINT DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);

-- 创建商品信息表
CREATE TABLE IF NOT EXISTS product_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    category_id INT COMMENT '分类ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    image VARCHAR(255) COMMENT '商品主图',
    points_price INT NOT NULL COMMENT '所需积分',
    stock INT DEFAULT 0 COMMENT '库存数量',
    limit_per_user INT DEFAULT 0 COMMENT '每人限购数量（0表示不限购）',
    status TINYINT DEFAULT 1 COMMENT '状态：0=下架，1=上架',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (category_id) REFERENCES product_category(id) ON DELETE SET NULL,
    FOREIGN KEY (create_by) REFERENCES user_info(id) ON DELETE SET NULL
);

-- 创建兑换订单表
CREATE TABLE IF NOT EXISTS exchange_order (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    total_points INT NOT NULL COMMENT '总积分',
    status TINYINT DEFAULT 0 COMMENT '订单状态：0=待发货，1=已发货，2=已完成，3=已取消',
    receiver_name VARCHAR(100) COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) COMMENT '收货人电话',
    receiver_address TEXT COMMENT '收货地址',
    logistics_info TEXT COMMENT '物流信息',
    remark TEXT COMMENT '备注',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
);

-- 创建订单明细表（支持一个订单多个商品）
CREATE TABLE IF NOT EXISTS exchange_order_item (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id INT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称（快照）',
    product_image VARCHAR(255) COMMENT '商品图片（快照）',
    points_price INT NOT NULL COMMENT '单价积分（快照）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    subtotal_points INT NOT NULL COMMENT '小计积分',
    create_time DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES exchange_order(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product_info(id) ON DELETE RESTRICT
);

-- 创建商城模块索引
CREATE INDEX idx_product_category_status ON product_category(status, sort_order);
CREATE INDEX idx_product_category ON product_info(category_id);
CREATE INDEX idx_product_status ON product_info(status);
CREATE INDEX idx_product_create_time ON product_info(create_time);
CREATE INDEX idx_order_user ON exchange_order(user_id);
CREATE INDEX idx_order_status ON exchange_order(status);
CREATE INDEX idx_order_create_time ON exchange_order(create_time);
CREATE INDEX idx_order_no ON exchange_order(order_no);
CREATE INDEX idx_order_item_order ON exchange_order_item(order_id);
CREATE INDEX idx_order_item_product ON exchange_order_item(product_id);

-- 插入默认分类数据
INSERT INTO product_category (name, description, sort_order, status, create_time, update_time)
VALUES 
    ('实物商品', '需要邮寄的实体商品', 1, 1, NOW(), NOW()),
    ('虚拟商品', '电子优惠券、会员权益等', 2, 1, NOW(), NOW()),
    ('服务类商品', '线下服务、体验券等', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

