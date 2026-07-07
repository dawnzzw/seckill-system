-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seckill;

-- ============================================================
-- 1. 商品表
-- ============================================================
CREATE TABLE IF NOT EXISTS `product` (
                                         `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
                                         `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `description` VARCHAR(500) DEFAULT '' COMMENT '商品描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================================
-- 2. 订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `order` (
                                       `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
                                       `order_no` VARCHAR(20) NOT NULL UNIQUE COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `total_price` DECIMAL(10,2) NOT NULL COMMENT '订单总价',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待支付，1已支付，2已取消，3超时取消',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    INDEX idx_order_no (`order_no`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 3. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                                      `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 4. 商户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant` (
                                          `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商户ID',
                                          `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '商户用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `shop_name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';

-- ============================================================
-- 5. 初始数据
-- ============================================================

-- 插入默认商户（密码：admin123）
INSERT INTO merchant (username, password, shop_name) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '官方旗舰店');

-- 插入商品数据
INSERT INTO product (id, name, price, stock, description) VALUES
                                                              (1, 'iPhone 15 Pro Max', 8999.00, 100, '苹果旗舰手机，A17 Pro芯片，钛金属边框'),
                                                              (2, 'MacBook Pro 14英寸', 16999.00, 50, '苹果笔记本电脑，M3 Pro芯片，18小时续航'),
                                                              (3, 'AirPods Pro 2', 1899.00, 200, '苹果降噪耳机，H2芯片，主动降噪');