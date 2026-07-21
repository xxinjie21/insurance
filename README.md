# 医保核销系统

<div align="center">

![JDK](https://img.shields.io/badge/JDK-17-blue.svg?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg?style=flat-square)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.3.1-orange.svg?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-7.0-red.svg?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-green.svg?style=flat-square)
![Redisson](https://img.shields.io/badge/Redisson-3.17.7-critical.svg?style=flat-square)
![Vue 3](https://img.shields.io/badge/Vue-3-42b883.svg?style=flat-square&logo=vue.js)
![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg?style=flat-square&logo=typescript)

**面向医疗机构的医保费用结算中台，覆盖门诊就诊、医保结算、批次申报、基金拨付、住院管理全流程数字化**

[核心特性](#-核心特性) • [技术栈](#-技术栈) • [快速开始](#-快速开始) • [API 接口](#-api-接口) • [数据库设计](#-数据库设计) • [项目结构](#-项目结构)

</div>

---

## 项目介绍

本项目是一个 **医保核销系统**，面向医疗机构的医保费用结算中台，覆盖门诊就诊结算、医保批���申报、财政基金拨付全流程数字化。包含 Spring Boot 后端 + Vue 3 前端，共 **20 个 Controller、55 个 API 接口、27 张数据库表**。

### 系统角色

| 角色 | 职责 |
|------|------|
| 患者（PATIENT） | 挂号就诊、账户充值、支付自付部分、查询费用 |
| 医院（HOSPITAL） | 录入费用、医保结算、创建批次、申报拨付 |
| 医保局（MEDICAL） | 审核批次、基金拨付、拒绝退回、报表统计 |
| 管理员（ADMIN） | 管理医院、代理操作（超级权限） |

### 核心业务流程

```
患者挂号 → 医院录入费用 → 医保结算（甲类100%/乙类80%/自费0%）
    → 患者支付自付部分 → 医院创建批次 → 批次申报
    → 医保局审核 → 基金拨付 → 批次完成
```

---

## 核心特性

### 1. 三层递进式幂等校验

```
Redis 预校验 → Redisson 分布式锁(单据级) → 数据库唯一索引
```

- `SettleServiceImpl`：结算接口三层防护，高并发下同一单据不会重复结算
- `lock:settle:{visitId}` 单据级锁粒度，30 秒租约
- Redis 幂等 Key `idempotent:settle:visit:{visitId}` TTL 1 小时

### 2. 编程式事务保证缓存一致性

- `@Transactional` 回滚时无法撤销已写入的 Redis 缓存
- 改用 `TransactionTemplate`，仅事务提交成功后才写入���等缓存

### 3. 操作日志 AOP

- `@OperationLog("操作描述")` 注解标记方法
- `OperationLogAspect` 切面自动记录：用户、方法、参数、结果、耗时
- 持久化到 `operation_log` 表 + 控制台输出

### 4. 声明式权限校验

- `@Permission({HOSPITAL, ADMIN})` 注解 + `UserInfoInterceptor` 拦截器
- Controller/Method 级声明式角色校验
- ADMIN 角色始终通过（超级权限）
- 无需在业务代码中硬编码角色判断

### 5. MQ Outbox 可靠消息

- 事务性 Outbox 模式：业务事务内写入 `mq_outbox` 表
- 事务提交后投递 RabbitMQ，Confirm 回调更新状态
- `MqRetryJob` 每 5 分钟重试失败消息
- `MqIdempotentHelper` Redisson 幂等消费去重

### 6. 缓存穿透防护

- 就诊记录 Cache-Aside + 空值短 TTL（30 分钟）
- `cache:visit:{visitId}` 防止不存在数据反复击穿数据库

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.0 |
| 分布式锁 | Redisson | 3.17.7 |
| 消息队列 | RabbitMQ | 3.x |
| 认证 | JWT (jjwt 0.9.1) + Redis Token | - |
| 密码 | Spring Security Crypto (BCrypt) | 5.7.11 |
| 工具 | HuTool 5.7.17、Fastjson 1.2.83 | - |
| AOP | AspectJ 1.9.20 | - |
| 前端 | Vue 3.4 + TypeScript 5.3 + Element Plus 2.5 | - |
| 构建 | Vite 5.0 | - |

### 核心依赖

- **Redisson**：分布式锁 + 幂等校验 + MQ 幂等消费
- **TransactionTemplate**：编程式事务保证缓存一致性
- **JWT + Redis**：Token 认证 + 角色/医院 ID 缓存
- **RabbitMQ**：Outbox 可靠消息 + 死信队列
- **HuTool**：简化工具类开发

---

## 快速开始

### 1. 环境准备

```bash
# JDK 17+
java -version

# MySQL 8.0+, Redis 7.0+, RabbitMQ 3.x
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

编辑 `insurance/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/medical_insurance?useUnicode=true&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
```

### 5. 启动后端

```bash
cd insurance
mvn spring-boot:run
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 7. 访问

```
前端：http://localhost:5173
后端：http://localhost:8080
```

---

## 数据库设计

### 27 张表（5 大模块）

#### 核心业务表

| 表��� | 说明 | 唯一约束 |
|------|------|----------|
| `hospital` | 医院 | `uk_name` |
| `user` | 用户（四角色） | `uk_id_card` |
| `visit` | 就诊记录 | - |
| `fee` | 费用明细（甲/乙/自费） | - |
| `settle` | 结算单 | `uk_visit_id` |
| `batch` | 申报批次 | `uk_batch_no` |
| `batch_item` | 批次明细 | - |
| `pay` | 拨付记录 | `uk_batch_id` |

#### 账户与财务表

| 表名 | 说明 | 唯一约束 |
|------|------|----------|
| `user_account` | 患者账户 | `uk_user_id` |
| `recharge_record` | 充值记录 | `uk_order_no` |
| `consumption_record` | 消费记录 | `uk_order_no` |
| `refund` | 退款记录 | - |
| `year_accumulate` | 年度累计 | `uk_user_year` |

#### 目录与规则表

| 表名 | 说明 | 唯一约束 |
|------|------|----------|
| `reimburse_rule` | 报销规则（24条） | - |
| `drug_catalog` | 药品目录 | `uk_code` |
| `treatment_catalog` | 诊疗目录 | `uk_code` |
| `consumable_catalog` | 耗材目录 | `uk_code` |
| `audit_rule` | 审核规则 | - |

#### 诊疗流程表

| 表名 | 说明 | 唯一约束 |
|------|------|----------|
| `registration` | 门诊挂号 | - |
| `inpatient` | 住院记录 | `uk_inpatient_no` |
| `inpatient_deposit` | 住院押金 | - |
| `doctor` | 医生 | `uk_insurance_code` |
| `prescription` | 处方 | - |
| `remote_medical_filing` | 异地就医备案 | - |
| `chronic_disease_cert` | 慢病认证 | - |

#### 系统表

| 表名 | 说明 | 唯一约束 |
|------|------|----------|
| `operation_log` | 操作日志（AOP） | - |
| `mq_outbox` | MQ 事务性 Outbox | `uk_message_id` |

---

## API 接口（55 个）

### 用户模块（4 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/user/login` | 公开 | 用户登录 |
| POST | `/user/sign` | 公开 | 用户注册 |
| POST | `/user/loginout` | 登录 | 用户登出 |
| GET | `/user/search` | 登录 | 搜索患者 |

### 医院模块（10 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/hospital/sign` | 公开 | 医院注册 |
| GET | `/hospital/list` | 登录 | 医院列表 |
| GET | `/hospital/patient/list` | 医院/管理员 | 本院患者 |
| POST | `/hospital/approve/{id}` | 医保局/管理员 | 审批通过 |
| POST | `/hospital/reject/{id}` | 医保局/管理员 | 拒绝注册 |
| POST | `/hospital/enable/{id}` | 医保局/管理员 | 启用医院 |
| POST | `/hospital/disable/{id}` | 医保局/管理员 | 禁用医院 |
| POST | `/hospital/select/{id}` | 管理员 | 选择医院 |
| POST | `/hospital/unselect` | 管理员 | 取消选择 |
| GET | `/hospital/selected` | 管理员 | 已选医院 |

### 就诊模块（5 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/visit/add` | 医院/管理员 | 添加就诊记录 |
| GET | `/visit/my/list` | 患者/管理员 | 个人就诊记录 |
| GET | `/visit/hospital/list` | 医院/管理员 | 本院就诊记录 |
| GET | `/visit/{visitId}` | 医院/管理员 | 就诊详情 |
| DELETE | `/visit/{visitId}` | 医院/管理员 | 删除就诊记录 |

### 费用模块（4 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/fee/batch/add` | 医院/管理员 | 批量添加费用 |
| GET | `/fee/listByVisitId` | 登录 | 按就诊查费用 |
| DELETE | `/fee/{feeId}` | 医院/管理员 | 删除费用明细 |
| GET | `/fee/my/list` | 患者/管理员 | 个人费用明细 |

### 结算模块（5 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/settle/calculate/{visitId}` | 医院/管理员 | **医保结算计算** |
| GET | `/settle/detail/{visitId}` | 医院/管理员 | 结算单详情 |
| GET | `/settle/my/list` | 患者/管理员 | 个人结算列表 |
| GET | `/settle/hospital/list` | 医院/管理员 | 本院结算列表 |
| GET | `/settle/available-for-batch` | 医院/管理员 | 可申报结算单 |

### 批次模块（9 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/batch/create` | 医院/管理员 | 创建批次 |
| POST | `/batch/add-settle/{batchId}/{settleId}` | 医院/管理员 | 添加结算单到批次 |
| GET | `/batch/detail/{batchId}` | 登录 | 批次详情 |
| GET | `/batch/hospital/list` | 医院/管理员 | 本院批次 |
| GET | `/batch/medical/list` | 医保局/管理员 | 所有批次 |
| POST | `/batch/declare/{batchId}` | 医院/管理员 | **申报批次** |
| POST | `/batch/withdraw/{batchId}` | 医院/管理员 | 撤回申报 |
| GET | `/batch/pending-list` | 医院/管理员 | 待申报批次 |
| DELETE | `/batch/{batchId}` | 医院/管理员 | 删除批次 |

### 拨付模块（3 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| POST | `/pay/pay-batch/{batchId}` | 医保局/管理员 | **拨付批次款项** |
| GET | `/pay/by-batch/{batchId}` | 医保局/管理员 | 查询拨付信息 |
| POST | `/pay/reject-batch/{batchId}` | 医保局/管理员 | 拒绝拨付 |

### 账户模块（5 个）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| GET | `/account/get` | 患者/管理员 | 查询账户信息 |
| POST | `/account/recharge` | 管理员 | 账户充值 |
| POST | `/account/pay` | 患者/管理员 | 支付结算单 |
| GET | `/account/recharge/list` | 患者/管理员 | 充值记录 |
| GET | `/account/consumption/list` | 患者/管理员 | 消费记录 |

### 其他模块（10 个）

| 方法 | URL | 权��� | 说明 |
|------|-----|------|------|
| GET | `/dashboard/stats` | 登录 | 统计数据看板 |
| GET | `/catalog/drug/list` | 医院/管理员 | 药品目录 |
| GET | `/catalog/treatment/list` | 医院/管理员 | 诊疗目录 |
| GET | `/catalog/consumable/list` | 医院/管理员 | 耗材目录 |
| POST | `/registration/add` | 医院/管理员 | 门诊挂号 |
| GET | `/registration/my/list` | 患者/管理员 | 个人挂号记录 |
| GET | `/registration/hospital/list` | 医院/管理员 | 本院挂号记录 |
| POST | `/inpatient/admit` | 医院/管理员 | 入院登记 |
| POST | `/inpatient/deposit` | 医院/管理员 | 缴纳住院押金 |
| POST | `/inpatient/discharge/{id}` | 医院/管理员 | 出院结算 |
| GET | `/audit/settle/{settleId}` | 医保局/管理员 | 审核结算单 |
| POST | `/prescription/prescribe` | 医院/管理员 | 医生开方 |
| POST | `/prescription/approve/{id}` | 医院/管理员 | 审核通过处方 |
| POST | `/prescription/reject/{id}` | 医院/管理员 | 驳回处方 |
| GET | `/prescription/list/{visitId}` | 登录 | 处方列表 |
| POST | `/remote-filing/file` | 患者/管理员 | 异地就医备案 |
| POST | `/remote-filing/cancel/{id}` | 患者/管理员 | 取消备案 |
| GET | `/remote-filing/my` | 患者/管理员 | 查询备案 |
| POST | `/refund/apply` | 医院/管理员 | 申请退款 |
| POST | `/refund/approve/{id}` | 医保局/管理员 | 审批退款 |
| POST | `/refund/reject/{id}` | 医保局/管理员 | 拒绝退款 |
| GET | `/refund/list/{settleId}` | 登录 | 查询退款 |
| GET | `/report/fund` | 医保局/管理员 | 基金收支报表 |
| GET | `/report/fee-composition` | 医保局/管理员 | 费用构成分析 |
| GET | `/report/visit-stats` | 医保局/管理员 | 就诊统计报表 |
| POST | `/year-end/rollover` | 医保局/管理员 | 年度结转 |
| GET | `/year-end/reconcile` | 医保局/管理员 | 年度对账报表 |

---

## 项目结构

```
insurance/
├── doc/                               # 文档
│   ├── api-test-cases.md              # API 测试案例
│   ├── interview-guide.md             # 面试复习指南
│   └── technical-architecture.md      # 技术架构文档
├── frontend/                          # Vue 3 前端（24 个视图组件）
│   ├── src/
│   │   ├── api/index.ts              # 16 组 API 常量
│   │   ├── router/index.ts           # 28 条路由（角色守卫）
│   │   ├── stores/user.ts            # Pinia 状态管理
│   │   ├── types/                    # 10 枚举 + 21 接口
│   │   ├── views/                    # 24 Vue 组件
│   │   │   ├── patient/              # 6 个患者视图
│   │   │   ├── hospital/             # 12 个医院视图
│   │   │   └── medical/              # 4 个医保局视图
│   │   └── utils/                    # Axios 封装、格式化、分页
│   └── package.json                  # Vue 3.4 + Element Plus 2.5 + Pinia 2.1
├── insurance/                         # Spring Boot 后端
│   ├── docs/int.sql                  # 27 张表建表 + 种子数据
│   └── src/main/java/com/xxj/insurance/
│       ├── InsuranceApplication.java  # @EnableScheduling
│       ├── common/
│       │   ├── annotation/            # @Permission, @OperationLog
│       │   ├── aspect/               # OperationLogAspect（AOP 日志）
│       │   ├── config/               # Redisson, MVC, RabbitMQ, MyBatis, Jackson, Async
│       │   ├── constants/            # Redis Key, 账户常量, 报销常量
│       │   ├── enums/                # Role（PATIENT/HOSPITAL/MEDICAL/ADMIN）
│       │   ├── exception/            # GlobalExceptionHandler + BusinessException
│       │   ├── interceptors/         # UserInfoInterceptor（Token + @Permission）
│       │   ├── mq/                   # Outbox Sender + Consumers + RetryJob + Idempotent
│       │   └── utils/                # JwtUtil, UserHolder
│       ├── controller/               # 20 个 Controller
│       ├── service/impl/             # 21 个 Service 实现
│       ├── mapper/                   # 27 个 Mapper
│       └── domain/
│           ├── dto/                  # 8 个 DTO
│           ├── vo/                   # 12 个 VO
│           └── po/                   # 27 个 PO（实体）
└── README.md
```

---

## Redis 缓存设计

| Key 模式 | 用途 | TTL |
|---------|------|-----|
| `login:token:{token}` → userId | 登录 Token | 24h |
| `login:role:{userId}` → roleCode | 用户角色 | 24h |
| `login:hospitalId:{userId}` → hospitalId | 医院 ID | 24h |
| `admin:selectedHospital:{userId}` → hospitalId | 管理员选中医院 | 24h |
| `cache:visit:{visitId}` → visitData | 就诊记录缓存 | 30min |
| `idempotent:settle:visit:{visitId}` | 结算幂等 Key | 1h |
| `idempotent:mq:{messageId}` | MQ 消费幂等 | 24h |
| `lock:settle:{visitId}` | Redisson 分布式锁 | 30s |
| `lock:batch:{batchId}` | 批次分布式锁 | 30s |
| `lock:pay:{batchId}` | 拨付分布式锁 | 30s |

---

## MQ 队列设计

| 交换机 | 队列 | Routing Key | 说明 |
|--------|------|-------------|------|
| `insurance.settle.direct` | `queue.outpatient.settle` | `settle.outpatient` | 门诊结算归档 |
| `insurance.settle.direct` | `queue.remote.settle` | `settle.remote` | 异地结算归档 |
| `insurance.settle.direct` | `queue.batch.reconcile` | `batch.reconcile` | 批次对账 |
| `insurance.settle.direct` | `queue.audit.notify` | `audit.notify` | 审核通知 |

- 事务性 Outbox 模式，`mq_outbox` 表持久化
- 手动 ACK + 幂等消费（Redisson 去重）
- 死信队列（DLX）处理失败消息
- `MqRetryJob` 每 5 分钟重试未成功消息

---

## 技术特点

| 特点 | 说明 |
|------|------|
| **三层幂等校验** | Redis 预校验 → Redisson 单据锁 → DB 唯一索引 |
| **编程式事务** | TransactionTemplate 保证事务提交后才写缓存 |
| **声明式权限** | `@Permission` 注解 + 拦截器实现 4 角色隔离 |
| **操作日志 AOP** | `@OperationLog` 注解自动记录操作审计 |
| **MQ Outbox** | 事务性 Outbox + 幂等消费 + 定时重试 |
| **缓存穿透防护** | 空值短 TTL（30min）+ Cache-Aside |
| **N+1 查询消除** | ID 聚合 → 批量加载 → Map 映射 |
| **Vue 3 前端** | TypeScript + Element Plus + Pinia + 24 个视图 |

---

## 常见问题

### Q: 项目启动失败？

检查 MySQL、Redis、RabbitMQ 是否启动，检查 `application.yml` 配置。

### Q: 结算接口返回重复提交？

检查 Redisson 是否启动，��查分布式锁 Key 设计，检查 `operation_log` 表确认操作记录。

### Q: 前端页面空白？

确保后端已启动（CORS 已配置），检查 `frontend/vite.config.ts` 中的 API 代理配置。

---

## 许可证

仅供学习参考

---

<div align="center">

**如果本项目对你有帮助，请给个 Star 支持！**

</div>
