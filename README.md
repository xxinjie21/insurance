# 医保核销系统

<div align="center">

![JDK](https://img.shields.io/badge/JDK-17-blue.svg?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen.svg?style=flat-square)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-orange.svg?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-7.0-red.svg?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square)
![Vue 3](https://img.shields.io/badge/Vue-3-42b883.svg?style=flat-square&logo=vue.js)

**面向医疗机构的医保费用结算中台，覆盖门诊就诊结算、医保批次申报、财政基金拨付全流程数字化**

[核心特性](#-核心特性) • [技术栈](#-技术栈) • [快速开始](#-快速开始) • [项目结构](#-项目结构) • [面试考点](#-面试考点)

</div>

---

## 项目介绍

本项目是一个 **医保核销系统**，面向医疗机构的医保费用结算中台，覆盖门诊就诊结算、医保批次申报、财政基金拨付全流程数字化。

### 系统角色

| 角色 | 职责 |
|------|------|
| 患者 | 挂号就诊、账户充值、支付自付部分、查询费用 |
| 医院 | 录入费用、医保结算、创建批次、申报拨付 |
| 医保局 | 审核批次、基金拨付、拒绝退回 |
| 管理员 | 管理医院、代理操作 |

### 核心业务流程

```
患者挂号 → 医院录入费用 → 医保结算（甲类100%/乙类80%/自费0%）
    → 患者支付自付部分 → 医院创建批次 → 批次申报
    → 医保局审核 → 基金拨付 → 批次完成
```

---

## 核心特性

### 1. 并发重复提交防重

```
Redis 预校验 → Redisson 分布式锁 → 数据库唯一索引（三层递进式幂等校验链）
```

高并发下同一单据可能被重复结算，设计三层递进式幂等校验链。

### 2. 事务与缓存一致性

声明式 `@Transactional` 回滚时无法撤销已写入的 Redis 幂等缓存。改用 `TransactionTemplate` 编程式事务，仅事务提交成功后写入缓存，消除回滚脏数据。

### 3. 业务粒度分布式锁

全局锁导致无关单据并发阻塞。基于 Redisson 实现单据级锁粒度（`lock:settle:{visitId}`），仅串行化同一单据的并发请求。

### 4. 细粒度权限管控

`@Permission` 注解 + SpringMVC 拦截器实现 Controller/Method 级声明式权限校验，无需在业务代码中硬编码角色判断。

### 5. 缓存穿透防护

就诊记录高频查询，Cache-Aside 模式 + 空值短 TTL 缓存（1 分钟），防止不存在的数据反复击穿数据库。

### 6. N+1 查询消除

结算单详情涉及 结算单 → 就诊 → 用户 三级关联。ID 聚合 → 批量加载 → Map 映射，SQL 从 O(N) 降至常数级。

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 2.7 |
| ORM | MyBatis-Plus | 3.5 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.0 |
| 分布式锁 | Redisson | 3.17 |
| 认证 | JWT + Redis Token | - |
| 工具 | HuTool、Lombok | - |
| 前端 | Vue 3 + TypeScript + Element Plus | - |

### 核心依赖

- **Redisson**：分布式锁 + 单据级锁粒度
- **JWT**：Token 认证 + Redis 存储
- **TransactionTemplate**：编程式事务保证缓存一致性
- **HuTool**：简化工具类开发

---

## 快速开始

### 1. 环境准备

```bash
# JDK 17+
java -v

# MySQL 8.0+
# Redis 7.0+
# Maven 3.6+
```

### 2. 克隆项目

```bash
git clone https://github.com/xxinjie21/insurance.git
cd insurance
```

### 3. 初始化数据库

```bash
mysql -u root -p medical_insurance < insurance/docs/int.sql
```

### 4. 修改配置

编辑 `insurance/src/main/resources/application.yml`（数据库连接、Redis连接）。

### 5. 启动项目

```bash
cd insurance
mvn spring-boot:run
```

### 6. 访问

```
http://localhost:8080
```

---

## 功能详解

### 核心数据表

| 表 | 说明 | 唯一约束 |
|----|------|----------|
| `hospital` | 医院 | `uk_name` |
| `user` | 用户（四角色） | `uk_id_card` |
| `visit` | 就诊记录 | — |
| `fee` | 费用明细（甲/乙/自费） | — |
| `settle` | 结算单 | `uk_visit_id` |
| `batch` | 申报批次 | `uk_batch_no` |
| `batch_item` | 批次明细 | — |
| `pay` | 拨付记录 | `uk_batch_id` |
| `user_account` | 患者账户 | `uk_user_id` |
| `recharge_record` | 充值记录 | `uk_order_no` |

### API 概览

| 模块 | 接口 | 方法 | 权限 |
|------|------|------|------|
| 用户 | `/user/sign` | POST | 公开 |
| 用户 | `/user/login` | POST | 公开 |
| 就诊 | `/visit/add` | POST | 医院 |
| 就诊 | `/visit/my/list` | GET | 患者 |
| 费用 | `/fee/batch/add` | POST | 医院 |
| 结算 | `/settle/calculate/{visitId}` | POST | 医院 |
| 结算 | `/settle/detail/{visitId}` | GET | 医院/患者 |
| 批次 | `/batch/create/{hospitalId}` | POST | 医院 |
| 批次 | `/batch/declare/{batchId}` | POST | 医院 |
| 批次 | `/batch/medical/list` | GET | 医保局 |
| 拨付 | `/pay/pay-batch/{batchId}` | POST | 医保局 |
| 账户 | `/account/recharge` | POST | 患者 |
| 账户 | `/account/pay` | POST | 患者 |

---

## 项目结构

```
insurance/
├── doc/
│   ├── api-test-cases.md           # API 测试案例
│   ├── interview-guide.md          # 面试复习指南
│   └── technical-architecture.md   # 技术架构文档
├── frontend/                       # Vue 3 前端
├── insurance/
│   ├── docs/int.sql                # 建表 + 种子数据
│   └── src/main/java/com/xxj/insurance/
│       ├── common/
│       │   ├── annotation/          # @Permission、@OperationLog
│       │   ├── aspect/              # 操作日志 AOP
│       │   ├── config/              # Redisson、MVC、MyBatis 配置
│       │   ├── constants/           # Redis Key、业务常量
│       │   ├── domain/              # Result、PageDTO
│       │   ├── enums/               # Role 枚举
│       │   ├── exception/           # 全局异常处理
│       │   ├── interceptors/        # Token + 权限拦截器
│       │   └── utils/               # JwtUtil、UserHolder
│       ├── controller/              # REST 控制器
│       ├── domain/{dto,po,vo}/      # DTO / Entity / VO
│       ├── mapper/                  # MyBatis-Plus Mapper
│       └── service/impl/            # 业务逻辑 + 分布式锁 + 事务
└── README.md
```

---

## 技术特点

| 特点 | 说明 |
|------|------|
| **三层幂等校验** | Redis 预校验 → Redisson 分布式锁 → 数据库唯一索引 |
| **编程式事务** | TransactionTemplate 保证事务提交后才写缓存 |
| **单据级锁粒度** | Redisson 实现 `lock:settle:{visitId}` 细粒度锁 |
| **声明式权限** | `@Permission` 注解 + 拦截器实现角色隔离 |
| **空值缓存防穿透** | Cache-Aside + 空值短 TTL（1 分钟） |
| **N+1 查询消除** | ID 聚合 → 批量加载 → Map 映射 |

---

## 面试考点

### 1. 事务与缓存一致性

**Q1: `@Transactional` 回滚时 Redis 缓存怎么办？**

**参考答案**：
> 1. **问题**：声明式事务回滚时，已写入的 Redis 缓存无法撤销
> 2. **解决**：改用 `TransactionTemplate` 编程式事务
> 3. **原则**：仅事务提交成功后才写入缓存
> 4. **效果**：消除回滚脏数据，保证一致性

### 2. 分布式锁

**Q2: 为什么不用全局锁？**

**参考答案**：
> 1. **问题**：全局锁导致无关单据并发阻塞
> 2. **解决**：基于 Redisson 实现单据级锁粒度
> 3. **Key 设计**：`lock:settle:{visitId}`
> 4. **效果**：仅串行化同一单据的并发请求

### 3. 幂等性

**Q3: 如何防止重复结算？**

**参考答案**：
> 1. **Redis 预校验**：先查 Redis 判断是否已结算
> 2. **分布式锁**：Redisson 锁住单据 ID
> 3. **唯一索引**：数据库层面兜底防重
> 4. **三层递进**：任一层拦截即可保证幂等

---

## 常见问题

### Q: 项目启动失败？

检查 MySQL、Redis 是否启动，检查 `application.yml` 配置。

### Q: 结算接口返回重复提交？

检查 Redisson 是否启动，检查分布式锁 Key 设计。

### Q: 批次申报失败？

检查批次状态是否正确，检查数据库唯一约束。

---

## 许可证

仅供学习参考

---

<div align="center">

**如果本项目对你有帮助，请给个 Star 支持！**

</div>
