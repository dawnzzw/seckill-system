# Seckill System · 高并发秒杀系统

基于 Spring Boot 的高可用秒杀系统，支持商品预热、分布式锁扣库存、Kafka 异步落库、三级限流防刷及 JWT 认证。

## 开发与测试工具

| 工具 | 用途 |
|------|------|
| IntelliJ IDEA 2025.2 | 项目开发 |
| JDK 21 | Java 运行环境 |
| Navicat | 数据库管理 |
| Apifox | 接口测试 |
| JMeter | 压力测试 |

## 技术栈

| 模块 | 技术选型 | 用途 |
|------|---------|------|
| 核心框架 | Spring Boot 3.4.4 | 应用基础框架 |
| ORM | MyBatis 3.0.4 | 数据持久化 |
| 数据库 | MySQL 8.0 | 业务数据存储 |
| 缓存 | Redis 7.0 | 商品/库存缓存、分布式锁、限流计数 |
| 消息队列 | Kafka 3.4.0 | 订单异步落库、流量削峰 |
| 分布式锁 | Redisson 3.38.1 | 库存扣减原子性保证 |
| 认证授权 | JWT 0.11.5 | 身份认证、角色区分 |
| 限流 | AOP + Redis | 三级限流（IP/用户/商品） |
| 反向代理 | Nginx 1.20 | 静态资源托管、API 反向代理 |


## 功能模块

### 用户端

- 用户注册与登录（JWT 认证）
- 商品列表查询（Redis 缓存）
- 秒杀下单（分布式锁扣库存）
- 模拟支付
- 订单列表查询（分页 + 状态筛选）
- 订单结果轮询

### 商家端

- 商户注册与登录
- 单商品预热与批量商品预热
- 秒杀活动控制（结束、批量结束）
- 紧急熔断（一键关闭所有秒杀活动）
- 支付数据统计看板
- 全量订单管理（状态筛选 + 用户搜索）


## 核心设计

### 1. 多级缓存架构

商品信息与库存数据预热至 Redis，下单链路全程读取缓存，降低 MySQL 压力，QPS 提升约 10 倍。

### 2. 分布式锁防超卖

基于 Redisson 实现分布式锁，粒度细化至商品维度，确保同一商品同一时刻仅单线程执行库存扣减，彻底解决超卖问题。

### 3. 异步订单落库

下单后仅扣减 Redis 库存并发送 Kafka 消息，立即返回；消费者异步消费并写入 MySQL，有效削峰填谷，数据库连接池压力降低约 80%。

### 4. 三级限流防刷

基于 AOP 和 Redis 实现 IP 级、用户级、商品级限流，支持注解式配置，灵活应用于任意接口。

### 5. 超时自动恢复

定时任务扫描超时未支付订单（30 分钟），自动取消并回滚 Redis 库存，避免库存长期锁定。

### 6. JWT 双角色认证

支持用户（USER）与商户（MERCHANT）双角色注册登录，Token 携带角色信息，接口级权限隔离。


## 项目结构
```
seckill/
├── src/main/java/com/example/seckill/
│ ├── annotation/ # 自定义注解（RateLimiter）
│ ├── aspect/ # AOP 切面（RateLimitAspect）
│ ├── config/ # 配置类（JWT、Redis、Redisson、CORS）
│ ├── constants/ # 常量定义（RedisConstants）
│ ├── controller/ # 控制器
│ ├── entity/ # 实体类
│ ├── exception/ # 全局异常处理 + Result 封装
│ ├── mapper/ # MyBatis Mapper 接口
│ ├── service/ # 业务服务层
│ ├── util/ # 工具类
│ └── SeckillApplication.java
├── src/main/resources/
│ ├── mapper/ # MyBatis XML 映射文件
│ └── application.properties
├── frontend/ # 前端静态页面
│ ├── index.html # 用户端
│ └── admin.html # 商家端
└── pom.xml
```


## 快速启动

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 7.0+
- Kafka 3.4.0

### 启动步骤

1. 启动 Redis

```bash
redis-server
```
2. 启动 Kafka（含 ZooKeeper）

```bash
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.
```
3. 初始化数据库

   执行src/main/resourdes/db/schema.sql 创建表结构并插入初始数据。

4. 启动应用

   在IDEA中运行 SeckillApplication.java

5. 访问前端
- 用户端：http://localhost/index.html
- 商家端：http://localhost/admin.html

## API 概览
| 分类 | 接口说明 |
|------|---------|
| 认证 | 注册、登录、Token 验证 |
| 订单 | 下单、支付、取消、列表查询、轮询结果 |
| 管理 | 预热、秒杀控制、熔断、状态查询、统计看板 |

## 作者
邹子溦

## 许可证
MIT License