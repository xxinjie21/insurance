# 医保核销系统技术架构与代码思想

## 目录

- [1. 系统概述](#1-系统概述)
- [2. 技术栈详解](#2-技术栈详解)
- [3. 系统架构设计](#3-系统架构设计)
- [4. 核心设计思想](#4-核心设计思想)
- [5. 模块设计与实现](#5-模块设计与实现)
- [6. 数据库设计](#6-数据库设计)
- [7. 安全设计](#7-安全设计)
- [8. 性能优化](#8-性能优化)
- [9. 代码规范与最佳实践](#9-代码规范与最佳实践)

---

## 1. 系统概述

### 1.1 项目背景

医保核销系统是一个完整的医疗保险费用结算与管理平台，实现了从患者挂号、费用录入、规则引擎驱动医保结算、批次申报审核、基金拨付、异地就医、大病救助、退费到报表审计的全流程数字化管理。系统已按15个模块完成贴近现实医保业务的改造。

### 1.2 核心业务流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           医保核销业务流程                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐             │
│  │ 患者挂号 │───▶│ 录入费用 │───▶│ 医保结算 │───▶│ 患者支付 │             │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘             │
│       │              │              │              │                   │
│       ▼              ▼              ▼              ▼                   │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐             │
│  │创建就诊 │    │费用明细 │    │计算报销 │    │自付部分 │             │
│  │记录    │    │甲/乙/自费│    │金额    │    │账户扣款 │             │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘             │
│                                                     │                   │
│                                                     ▼                   │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐             │
│  │基金拨付 │◀───│医保审核 │◀───│批次申报 │◀───│加入批次 │             │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘             │
│       │              │              │              │                   │
│       ▼              ▼              ▼              ▼                   │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐             │
│  │拨付到医院│    │审核通过 │    │创建批次 │    │结算单入 │             │
│  │批次完成 │    │或拒绝  │    │添加明细 │    │批次    │             │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.3 系统角色

| 角色 | 标识 | 职责 | 主要功能 |
|------|------|------|----------|
| 患者 | PATIENT(1) | 就诊、支付 | 挂号、查询费用、支付自付部分、异地备案 |
| 医院 | HOSPITAL(2) | 诊疗、申报 | 挂号管理、医生开方、录入费用、医保结算、创建批次、申报、住院管理 |
| 医保局 | MEDICAL(3) | 审核、拨付 | 审核批次、智能审核结算单、基金拨付、拒绝申请、统计报表 |
| 管理员 | ADMIN(4) | 系统管理 | 管理医院、代理操作、年度结转 |

---

## 2. 技术栈详解

### 2.1 后端技术栈

| 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|----------|
| Spring Boot | 2.7.18 | 基础框架 | 快速开发、自动配置、生态完善 |
| MyBatis-Plus | 3.5.3.1 | ORM框架 | 简化CRUD、支持Lambda查询、分页插件 |
| MySQL | 8.0 | 关系数据库 | 事务支持、数据一致性、成熟稳定 |
| Redis | 7.0 | 缓存/分布式锁 | 高性能、支持多种数据结构 |
| Redisson | 3.17.7 | 分布式锁客户端 | 封装Redis分布式锁、API友好 |
| JWT | 0.9.1 | Token认证 | 无状态、跨服务共享 |
| Hutool | 5.7.17 | 工具库 | 日期、字符串、加密等工具 |
| Lombok | 1.18.44 | 代码简化 | 自动生成getter/setter/构造方法 |
| BCrypt | 5.7.11 | 密码加密 | 安全性高、自带盐值 |
| RabbitMQ | 3.x (spring-boot-starter-amqp) | 异步消息 | 主链路异步化、Confirm+ACK、DLX兜底 |

### 2.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | - | 前端框架 |
| TypeScript | - | 类型安全 |
| Element Plus | - | UI组件库 |
| Vite | - | 构建工具 |
| Pinia | - | 状态管理 |
| Axios | - | HTTP客户端 |

### 2.3 依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                        Spring Boot                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    Spring Web                        │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │   │
│  │  │ Controller  │  │  Service    │  │   Mapper    │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ MyBatis-Plus │  │   Redisson   │  │ Spring Data  │      │
│  │   (ORM)      │  │(分布式锁)    │  │   Redis      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│          │                │                │                │
│          ▼                ▼                ▼                │
│  ┌──────────────┐  ┌──────────────────────────────┐        │
│  │    MySQL     │  │           Redis              │        │
│  │  (持久化)    │  │   (缓存/锁/Session)          │        │
│  └──────────────┘  └──────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 系统架构设计

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      表现层 (Controller)                     │
│  - 接收请求、参数校验                                        │
│  - 调用Service、封装响应                                     │
│  - 权限注解控制                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      业务层 (Service)                        │
│  - 核心业务逻辑                                              │
│  - 分布式锁控制                                              │
│  - 事务管理                                                  │
│  - 幂等性控制                                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据层 (Mapper)                         │
│  - 数据访问                                                  │
│  - MyBatis-Plus CRUD                                        │
│  - 自定义SQL                                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      存储层 (Storage)                        │
│  ┌────────────────────┐    ┌────────────────────┐          │
│  │       MySQL        │    │       Redis        │          │
│  │   (持久化存储)      │    │  (缓存/锁/Token)   │          │
│  └────────────────────┘    └────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 包结构设计

```
com.xxj.insurance
├── common                          # 公共模块
│   ├── annotation                 # 自定义注解
│   │   └── Permission.java       # 权限注解
│   ├── config                     # 配置类
│   │   ├── MvcConfig.java        # MVC配置（拦截器、跨域）
│   │   ├── RedissonConfig.java   # Redisson配置
│   │   ├── RabbitMQConfig.java   # RabbitMQ交换机/队列/DLX声明
│   │   └── PasswordConfig.java   # 密码加密配置
│   ├── constants                  # 常量定义
│   │   ├── RedisConstants.java   # Redis Key前缀
│   │   ├── ReimburseConstants.java # 报销业务常量
│   │   └── AccountConstants.java # 账户常量（个账初始余额/流水类型）
│   ├── domain                     # 通用领域对象
│   │   ├── Result.java           # 统一响应
│   │   └── PageDTO.java          # 分页DTO
│   ├── enums                      # 枚举
│   │   └── Role.java             # 角色枚举
│   ├── exception                  # 异常处理
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── interceptors               # 拦截器
│   │   └── UserInfoInterceptor.java
│   ├── mq                         # RabbitMQ消息组件
│   │   ├── MqMessageSender.java  # 消息发送器（outbox+Confirm）
│   │   ├── MqIdempotentHelper.java # 消费幂等（Redisson）
│   │   ├── MqConsumers.java      # 4个业务队列消费者
│   │   └── MqRetryJob.java       # 兜底补偿定时任务
│   └── utils                      # 工具类
│       ├── UserHolder.java       # 用户上下文
│       └── JwtUtil.java          # JWT工具
├── controller                      # 控制器层
├── domain                          # 领域模型
│   ├── dto                        # 数据传输对象
│   ├── po                         # 持久化对象
│   └── vo                         # 视图对象
├── mapper                          # 数据访问层
└── service                         # 业务层
    ├── impl                       # 实现类
    └── IXxxService.java           # 接口
```

### 3.3 请求处理流程

```
HTTP Request
     │
     ▼
┌─────────────────┐
│   Dispatcher    │
│    Servlet      │
└─────────────────┘
     │
     ▼
┌─────────────────┐     ┌─────────────────┐
│     Filter      │────▶│   CORS Filter   │
└─────────────────┘     └─────────────────┘
     │
     ▼
┌─────────────────┐     ┌─────────────────┐
│   Interceptor   │────▶│UserInfoInterceptor│
│                 │     │ - Token校验      │
│                 │     │ - 权限校验       │
│                 │     │ - 用户信息存入   │
└─────────────────┘     │   ThreadLocal    │
     │                  └─────────────────┘
     ▼
┌─────────────────┐
│   Controller    │
│ - 参数校验      │
│ - 调用Service   │
└─────────────────┘
     │
     ▼
┌─────────────────┐
│    Service      │
│ - 业务逻辑      │
│ - 分布式锁      │
│ - 事务控制      │
└─────────────────┘
     │
     ▼
┌─────────────────┐
│    Mapper       │
│ - 数据访问      │
└─────────────────┘
     │
     ▼
HTTP Response
```

---

## 4. 核心设计思想

### 4.1 统一响应设计

所有接口返回统一的 `Result` 对象，便于前端统一处理：

```java
@Data
public class Result {
    private Boolean success;      // 是否成功
    private String errorMsg;      // 错误信息
    private Object data;          // 业务数据
    private Long total;           // 分页总数
    private Integer code;         // 状态码（401未登录等）
}
```

**设计思想**：
- 前端只需判断 `success` 字段即可知道请求是否成功
- `code` 字段用于特殊状态码（如401登录过期）
- `total` 字段用于分页查询

### 4.2 权限控制设计

#### 4.2.1 自定义权限注解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    Role[] value();
}
```

#### 4.2.2 权限校验流程

```java
// 在拦截器中校验
Permission permission = hm.getMethodAnnotation(Permission.class);
if (permission == null) {
    permission = hm.getBeanType().getAnnotation(Permission.class);
}

// 管理员特权
if (userRole == Role.ADMIN) {
    return true;
}

// 校验角色是否匹配
for (Role requiredRole : permission.value()) {
    if (requiredRole == userRole) {
        return true;
    }
}
```

**设计思想**：
- 注解可放在类或方法上，方法优先
- 管理员拥有所有权限
- 支持多角色授权

### 4.3 用户上下文设计

使用 `ThreadLocal` 存储当前请求的用户信息：

```java
public class UserHolder {
    private static final ThreadLocal<Long> TL = new ThreadLocal<>();
    private static final ThreadLocal<Long> HOSPITAL_TL = new ThreadLocal<>();

    public static void save(Long userId, Long hospitalId) {
        TL.set(userId);
        HOSPITAL_TL.set(hospitalId);
    }

    public static Long getUserId() {
        return TL.get();
    }

    public static Long requireHospitalId() {
        Long hospitalId = HOSPITAL_TL.get();
        if (hospitalId == null) {
            throw new BusinessException("请先选择医院");
        }
        return hospitalId;
    }

    public static void remove() {
        TL.remove();
        HOSPITAL_TL.remove();
    }
}
```

**设计思想**：
- 线程隔离，避免并发问题
- 请求结束后必须清理，防止内存泄漏
- 提供 `requireHospitalId()` 方法，强制要求医院ID

### 4.4 分布式锁设计

#### 4.4.1 锁的使用模式

```java
String lockKey = "lock:settle:" + visitId;
RLock lock = redissonClient.getLock(lockKey);

try {
    // 尝试获取锁，等待10秒，持有30秒
    if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        return Result.fail("操作正在进行中，请勿重复提交");
    }

    // 执行业务逻辑
    Result result = doBusiness();

    return result;
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return Result.fail("操作被中断");
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

#### 4.4.2 锁的粒度设计

| 业务 | 锁Key | 粒度 |
|------|-------|------|
| 结算 | `lock:settle:{visitId}` | 就诊级别 |
| 批次创建 | `lock:batch:create:{hospitalId}` | 医院级别 |
| 添加结算单 | `lock:batch:add:{batchId}` | 批次级别 |
| 拨付 | `lock:pay:batch:{batchId}` | 批次级别 |
| 充值 | `lock:account:recharge:{userId}` | 用户级别 |

**设计思想**：
- 锁粒度要细，避免不必要的阻塞
- 使用 `tryLock` 而非 `lock`，避免死锁
- 设置合理的 `leaseTime`，避免看门狗无限续期

### 4.5 幂等性设计

#### 4.5.1 三层幂等校验

```
┌─────────────────────────────────────────────────────────────┐
│                  第一层：Redis 幂等预检查                     │
│  - 快速拦截重复请求                                          │
│  - idempotentBucket.isExists() → 直接返回"已处理"            │
│  - 实现位置：各 Service 方法开头                             │
└─────────────────────────────────────────────────────────────┘
                              │ 未命中，继续
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  第二层：Redisson 分布式锁                    │
│  - 防止并发请求同时执行                                      │
│  - lock.tryLock(10, 30, SECONDS) → 获取失败返回"处理中"     │
│  - 实现位置：RedissonConfig.java 提供 RedissonClient        │
│              各 Service 注入 RLock                           │
└─────────────────────────────────────────────────────────────┘
                              │ 获取锁成功，进入事务
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                第三层：数据库唯一索引校验                      │
│  - 最终兜底，防止 Redis/锁 同时失效的极端情况                │
│  - uk_visit_id 唯一约束 → 重复插入抛异常 → 事务回滚         │
│  - 定义位置：docs/int.sql (DDL)                             │
│              各 Mapper 继承 MyBatis-Plus BaseMapper          │
└─────────────────────────────────────────────────────────────┘
```

**三层幂等在项目中的使用文件**：

| 业务 | Service 文件 | 锁 Key | 幂等 Key | DB 唯一索引 |
|------|-------------|--------|----------|-------------|
| 就诊结算 | `SettleServiceImpl.java` | `lock:settle:{visitId}` | `idempotent:settle:visit:{visitId}` | `settle.uk_visit_id` |
| 批次申报 | `BatchServiceImpl.java` | `lock:batch:declare:{batchId}` | `idempotent:batch:declare:{batchId}` | `batch.uk_batch_no` |
| 批次撤回 | `BatchServiceImpl.java` | `lock:batch:withdraw:{batchId}` | `idempotent:batch:withdraw:{batchId}` | — |
| 添加结算单到批次 | `BatchServiceImpl.java` | `lock:batch:add:{batchId}` | `idempotent:batch:item:{settleId}` | `batch_item` 业务校验 |
| 批次拨付 | `PayServiceImpl.java` | `lock:pay:batch:{batchId}` | `idempotent:pay:batch:{batchId}` | `pay.uk_batch_id` |
| 拒绝拨付 | `PayServiceImpl.java` | `lock:pay:batch:{batchId}` | `idempotent:pay:reject:{batchId}` | — |

> 完整源码见 `insurance/src/main/java/com/xxj/insurance/service/impl/` 目录下对应文件。

#### 4.5.2 幂等标记写入时机

```java
// 错误：事务内写入幂等标记
@Transactional
public Result calculate() {
    // 业务逻辑
    save(settle);
    // 幂等标记（错误！事务可能回滚）
    idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
}

// 正确：事务提交后写入幂等标记
public Result calculate() {
    Result result = transactionTemplate.execute(status -> {
        // 业务逻辑
        save(settle);
        return Result.ok(settle);
    });

    // 事务提交成功后才写入幂等标记
    if (result != null && result.getSuccess()) {
        idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
    }
    return result;
}
```

**设计思想**：
- 幂等标记必须在事务提交成功后写入
- 防止事务回滚但幂等标记已写入的情况

### 4.6 事务管理设计

使用编程式事务 `TransactionTemplate` 而非声明式事务 `@Transactional`：

```java
public Result calculate(Long visitId) {
    // 获取锁
    lock.tryLock();

    try {
        // 编程式事务
        Result result = transactionTemplate.execute(status -> {
            // 业务逻辑
            return doBusiness();
        });

        // 事务提交后操作
        if (result != null && result.getSuccess()) {
            // 写入幂等标记
            // 清理缓存
        }

        return result;
    } finally {
        lock.unlock();
    }
}
```

**设计思想**：
- 锁和事务的边界要清晰
- 避免锁在事务内导致锁持有时间过长
- 事务提交后的操作（如写幂等标记）在锁内、事务外执行

---

## 5. 模块设计与实现

### 5.1 用户模块

#### 5.1.1 登录流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  用户登录   │────▶│  校验密码   │────▶│  生成Token  │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                        ┌─────────────────────────────────┐
                        │          Redis存储              │
                        │  login:token:{token} → userId   │
                        │  login:role:{userId} → role     │
                        │  login:hospitalId:{userId} → id │
                        └─────────────────────────────────┘
```

#### 5.1.2 Token校验流程

```java
// 拦截器中校验
String token = request.getHeader("token");
if (StrUtil.isBlank(token)) {
    return Result.fail(401, "请先登录");
}

String userId = redisTemplate.opsForValue().get("login:token:" + token);
if (userId == null) {
    return Result.fail(401, "登录已过期");
}

// 获取角色
String roleStr = redisTemplate.opsForValue().get("login:role:" + userId);
```

### 5.2 结算模块

#### 5.2.1 结算计算流程（规则引擎驱动）

```
┌─────────────────────────────────────────────────────────────┐
│                   结算计算流程（规则引擎）                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 幂等校验（三层）                                         │
│     ├─ Redis预检查                                          │
│     ├─ 获取分布式锁                                         │
│     └─ 数据库校验                                           │
│                                                             │
│  2. 规则引擎加载                                             │
│     ├─ 查询用户参保类型（职工/居民）                          │
│     ├─ 查询医院等级（三甲/二甲/社区）                         │
│     └─ 匹配 reimburse_rule 表规则                           │
│                                                             │
│  3. 四层报销计算                                             │
│     ├─ 第一层 基本统筹：起付线→报销比例→封顶线               │
│     ├─ 第二层 大病保险：封顶线用完自动进入，60%报销           │
│     ├─ 第三层 医疗救助：困难群体额外70%报销                   │
│     └─ 第四层 患者自付：个账优先→现金支付                    │
│                                                             │
│  4. 异地校验                                                 │
│     └─ 参保地≠就医地 → 查 remote_medical_filing 备案         │
│                                                             │
│  5. 年度累计更新                                             │
│     └─ year_accumulate 表更新起付线累计 + 统筹支付累计        │
│                                                             │
│  6. MQ异步                                                   │
│     └─ 结算完成 → 发 MQ 归档/稽核任务                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**代码位置**：[`SettleServiceImpl.java:168-332`](../insurance/src/main/java/com/xxj/insurance/service/impl/SettleServiceImpl.java#L168)

#### 5.2.2 报销规则表 (reimburse_rule)

24条规则覆盖职工/居民 × 6个医院等级 × 门诊/住院：

| 参保类型 | 医院等级 | 就诊 | 起付线 | 报销比例 | 封顶线 |
|---------|---------|------|--------|---------|--------|
| 职工 | 三甲 | 门诊 | 300 | 55% | 2000 |
| 职工 | 三甲 | 住院 | 800 | 85% | 300,000 |
| 职工 | 社区 | 门诊 | 0 | 75% | 2000 |
| 居民 | 三甲 | 住院 | 600 | 75% | 150,000 |

### 5.3 新增模块概览

| 模块 | 核心文件 | 功能 |
|------|---------|------|
| 医保目录 | `CatalogServiceImpl.java` | 药品/诊疗/耗材三大目录模糊搜索，费用录入选目录自动回填 |
| 挂号管理 | `RegistrationServiceImpl.java` | 门诊挂号（普通15元/专家30元），医事服务费独立计算 |
| 住院管理 | `InpatientServiceImpl.java` | 入院登记→押金缴纳→出院结算（调settleService） |
| 审核引擎 | `AuditServiceImpl.java` | 诊断-药品匹配/重复用药/年龄限制，拨付前逐单审核 |
| 年度管理 | `YearEndServiceImpl.java` | 起付线封顶线归零、个账1.5%计息、对账报表 |
| 异地就医 | `RemoteFilingServiceImpl.java` | 备案→结算时校验→无备案拒绝 |
| 多层次保障 | `SettleServiceImpl.java`（扩展） | 大病保险60%/医疗救助70% |
| 退费体系 | `RefundServiceImpl.java` | 申请→审批→原路退回（统筹/个账/现金三方） |
| 医生处方 | `PrescriptionServiceImpl.java` | 医生开方→药师审核→处方关联费用 |
| 统计报表 | `ReportServiceImpl.java` | 基金收支/费用构成/就诊统计 |
| MQ异步 | `MqMessageSender.java` + `MqConsumers.java` | outbox+Confirm+手动ACK+DLX+兜底Job |

### 5.3 批次模块

#### 5.3.1 批次状态流转

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   待申报    │────▶│   已申报    │────▶│   已完成    │
│  (PENDING)  │     │ (DECLARED)  │     │ (COMPLETED) │
└─────────────┘     └─────────────┘     └─────────────┘
       ▲                   │
       │                   ▼
       │            ┌─────────────┐
       └────────────│  拨付拒绝   │
                    │(PAY_REJECTED)│
                    └─────────────┘
```

#### 5.3.2 批次号生成规则

```java
String batchNo = DateUtil.format(new Date(), "yyyyMMddHHmmss")  // 时间戳
        + String.format("%04d", hospitalId % 10000)            // 医院ID后4位
        + String.format("%06d", SECURE_RANDOM.nextInt(1000000)); // 6位随机数
```

### 5.4 账户模块

#### 5.4.1 充值流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  获取锁    │────▶│  校验金额   │────▶│  更新余额   │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                        ┌─────────────────────────────────┐
                        │         创建充值记录            │
                        │  - 订单号（雪花算法）           │
                        │  - 充值金额                     │
                        │  - 充值类型                     │
                        └─────────────────────────────────┘
```

#### 5.4.2 支付流程

```java
public Result pay(Long visitId) {
    // 1. 校验结算单
    Settle settle = settleService.getByVisitId(visitId);
    if (settle == null) {
        return Result.fail("请先结算");
    }
    if (settle.getStatus() == SETTLE_STATUS_PAID) {
        return Result.fail("该就诊已支付");
    }

    // 2. 校验余额
    UserAccount account = accountService.getByUserId(userId);
    if (account.getBalance().compareTo(settle.getSelfPay()) < 0) {
        return Result.fail("余额不足，请先充值");
    }

    // 3. 扣款（分布式锁 + 事务）
    // 4. 更新结算状态
    // 5. 创建消费记录
}
```

---

## 6. 数据库设计

### 6.1 核心表结构

#### 6.1.1 用户表 (user) — 已扩展

```sql
CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT '用户ID',
  `password` varchar(64) NOT NULL COMMENT '密码（BCrypt加密）',
  `name` varchar(32) NOT NULL COMMENT '姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `id_card` varchar(18) NOT NULL COMMENT '身份证号',
  `hospital_id` bigint DEFAULT NULL COMMENT '所属医院ID',
  `role` tinyint NOT NULL COMMENT '1患者 2医院 3医保局 4管理员',
  `insurance_type` tinyint DEFAULT NULL COMMENT '参保类型：1-职工 2-居民',
  `insurance_no` varchar(32) DEFAULT NULL COMMENT '医保编号',
  `insurance_city` varchar(32) DEFAULT NULL COMMENT '参保地',
  `personal_account_balance` decimal(12,2) DEFAULT 0.00 COMMENT '个人账户余额',
  `medical_assistance` tinyint DEFAULT 0 COMMENT '医疗救助标记',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_id_card` (`id_card`)
);
```

#### 6.1.2 结算表 (settle) — 已扩展

```sql
CREATE TABLE `settle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算ID',
  `visit_id` bigint NOT NULL COMMENT '就诊ID',
  `hospital_id` bigint NOT NULL COMMENT '医院ID',
  `total` decimal(10,2) NOT NULL COMMENT '总金额',
  `reimburse` decimal(10,2) NOT NULL COMMENT '报销金额(统筹)',
  `self_pay` decimal(10,2) NOT NULL COMMENT '自付金额',
  `pooling_pay` decimal(10,2) DEFAULT 0.00 COMMENT '统筹支付',
  `account_pay` decimal(10,2) DEFAULT 0.00 COMMENT '个账支付',
  `cash_pay` decimal(10,2) DEFAULT 0.00 COMMENT '现金支付',
  `catastrophic_pay` decimal(10,2) DEFAULT 0.00 COMMENT '大病支付',
  `assistance_pay` decimal(10,2) DEFAULT 0.00 COMMENT '救助支付',
  `status` tinyint NOT NULL COMMENT '0待申报 1已申报 2已自付 3已拨付',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visit_id` (`visit_id`)
);
```

#### 6.1.3 新增核心表（共27张）

| 表名 | 用途 |
|------|------|
| `reimburse_rule` | 报销规则（24条/职工居民×6等级×门诊住院） |
| `year_accumulate` | 年度起付线/统筹累计（按用户+年份唯一） |
| `drug_catalog` / `treatment_catalog` / `consumable_catalog` | 三大医保目录 |
| `registration` | 门诊挂号 |
| `inpatient` / `inpatient_deposit` | 住院管理 + 押金 |
| `audit_rule` | 审核规则（诊断匹配/重复用药/年龄性别限制） |
| `doctor` / `prescription` | 医生 + 处方 |
| `remote_medical_filing` | 异地就医备案 |
| `chronic_disease_cert` | 慢特病认定 |
| `refund` | 退款（统筹/个账/现金三方） |
| `operation_log` | 操作审计日志 |
| `mq_outbox` | RabbitMQ事务消息表 |
```

### 6.2 索引设计原则

1. **主键索引**：使用雪花算法生成的分布式ID
2. **唯一索引**：业务唯一字段（如身份证、批次号）
3. **普通索引**：高频查询字段（如hospital_id、visit_id）
4. **联合索引**：多条件查询场景

---

## 7. 安全设计

### 7.1 认证安全

1. **密码加密**：使用BCrypt，自带盐值，防止彩虹表攻击
2. **Token机制**：JWT + Redis双校验，支持主动失效
3. **登录过期**：Token过期自动跳转登录页

### 7.2 授权安全

1. **角色权限**：基于 `@Permission` 注解的方法级权限控制
2. **数据隔离**：
   - 患者只能查看自己的数据
   - 医院只能操作本院数据
   - 管理员可操作所有数据

### 7.3 接口安全

1. **参数校验**：使用 `@Valid` 注解校验参数
2. **SQL注入防护**：使用MyBatis-Plus的Lambda查询
3. **XSS防护**：前端对用户输入进行转义

---

## 8. 性能优化

### 8.1 缓存策略

#### 8.1.1 缓存使用场景

| 数据类型 | 缓存Key | TTL | 说明 |
|----------|---------|-----|------|
| 就诊信息 | `cache:visit:{id}` | 30分钟 | 高频查询，Cache-Aside |
| 用户信息 | `cache:user:{id}` | 30分钟 | 高频查询，Cache-Aside |
| 医院信息 | `cache:hospital:{id}` | 30分钟 | 高频查询，Cache-Aside |
| 空值占位 | `cache:visit:{id}` | 1分钟 | DB无记录时缓存空串，防穿透 |
| Token信息 | `login:token:{token}` | 24小时 | 登录状态 |

#### 8.1.2 缓存更新策略（Cache-Aside + 穿透防护）

```java
// Cache-Aside 模式 + 空值短TTL防穿透
public Visit getVisitWithCache(Long visitId) {
    String cacheKey = "cache:visit:" + visitId;

    // 1. 先查缓存
    String cacheJson = redisTemplate.opsForValue().get(cacheKey);
    if (StrUtil.isNotBlank(cacheJson)) {
        return JSON.parseObject(cacheJson, Visit.class);
    }

    // 2. 缓存未命中，查数据库
    Visit visit = visitService.getById(visitId);

    // 3. 回写缓存
    if (visit != null) {
        // 正常数据：30分钟TTL
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(visit), 30, TimeUnit.MINUTES);
    } else {
        // 空值短TTL：1分钟，防止缓存穿透。恶意查询不存在的ID不会反复击穿DB
        redisTemplate.opsForValue().set(cacheKey, "", 1, TimeUnit.MINUTES);
    }

    return visit;
}
```

**设计思想**：
- Cache-Aside（旁路缓存）：读请求负责填充缓存，缓存仅在被请求时才加载
- 空值短 TTL：DB 无记录时缓存空串（1分钟），下一次相同请求命中缓存不再查 DB
- 无主动失效：依赖 TTL 过期自然淘汰，数据变更不频繁的场景下，30 分钟窗口内短暂不一致可接受

### 8.2 N+1 查询优化 — ID 聚合 → 批量加载 → Map 映射

结算单详情接口涉及**结算单 → 就诊 → 患者 → 医院**多级关联，将逐条 `getById()` 重构为：

```java
// 在 SettleServiceImpl.enrichSettleVOList() 中

// Step 1 — ID 聚合：收集所有 visitId / userId / hospitalId
Set<Long> visitIds = records.stream()
    .map(Settle::getVisitId)
    .filter(Objects::nonNull)
    .collect(Collectors.toSet());

// Step 2 — 批量加载：缓存命中直接入 Map，未命中 ID 聚合后用 listByIds 一条 IN 查询
Map<Long, Visit> visitMap = new HashMap<>();
List<Long> missedVisitIds = new ArrayList<>();
for (Long id : visitIds) {
    String json = redisTemplate.opsForValue().get("cache:visit:" + id);
    if (StrUtil.isNotBlank(json)) {
        visitMap.put(id, JSON.parseObject(json, Visit.class));  // 缓存命中
    } else {
        missedVisitIds.add(id);  // 收集未命中
    }
}
if (!missedVisitIds.isEmpty()) {
    // 一次 WHERE id IN (...) 批量查询，替代 N 次 getById
    List<Visit> visitList = visitService.listByIds(missedVisitIds);
    for (Visit visit : visitList) {
        visitMap.put(visit.getId(), visit);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(visit), 30, MINUTES);
    }
}

// Step 3 — Map 映射：组装 VO 时按 ID 从 Map 取值，O(1)
for (Settle settle : records) {
    Visit visit = visitMap.get(settle.getVisitId());  // O(1) Map lookup
    User user = userMap.get(visit.getUserId());
    Hospital hospital = hospitalMap.get(settle.getHospitalId());
    // ... 组装 VO
}
```

**效果**：单次请求 SQL 从 O(N)（N 条结算 × 3 级关联 = 3N+1 次查询）降至常数级（3 条 IN 查询 + N 次缓存命中）。Visit、User、Hospital 三个维度统一采用此模式。

### 8.3 分页查询优化

使用MyBatis-Plus的分页插件：

```java
Page<Settle> page = this.page(
    new Page<>(pageNum, pageSize),
    wrapper
);
```

---

## 9. 代码规范与最佳实践

### 9.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `SettleServiceImpl` |
| 方法名 | 小驼峰 | `calculateReimburse` |
| 常量 | 全大写+下划线 | `LOCK_LEASE_TIME` |
| 表名 | 下划线 | `user_account` |
| 字段名 | 下划线 | `hospital_id` |

### 9.2 异常处理

```java
// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统异常，请联系管理员");
    }
}
```

### 9.3 日志规范

```java
// 关键操作记录日志
log.info("结算成功，就诊ID:{}, 结算ID:{}", visitId, settle.getId());
log.error("结算操作被中断，就诊ID:{}", visitId, e);
log.warn("缓存解析失败 key:{}", cacheKey, e);
```

### 9.4 代码注释

```java
/**
 * 医保结算计算
 *
 * 业务逻辑：
 * 1. 校验就诊记录是否存在且状态为"待结算"
 * 2. 遍历所有费用明细，计算报销金额
 * 3. 创建结算单，状态为"待申报"
 *
 * 幂等性保证：
 * - Redis 预检查：快速拦截重复请求
 * - 分布式锁：防止并发结算
 * - 数据库校验：最终兜底
 *
 * @param visitId 就诊记录 ID
 * @return 结算结果
 */
```

---

## 10. RabbitMQ 异步消息架构

### 10.1 MQ 拓扑

```
Exchange: insurance.settle.direct (durable direct)
  ├── queue.outpatient.settle  ← routingKey: settle.outpatient   → DLQ: queue.outpatient.settle.dlq
  ├── queue.remote.settle      ← routingKey: settle.remote       → DLQ: queue.remote.settle.dlq
  ├── queue.batch.reconcile    ← routingKey: batch.reconcile     → DLQ: queue.batch.reconcile.dlq
  └── queue.audit.notify       ← routingKey: audit.notify

DLX: insurance.dlx.direct (durable direct)
```

### 10.2 消息流转

```
业务操作(事务内)
    │
    ├─ 写业务数据
    ├─ 写 mq_outbox (status=0)
    │
    ▼ (事务提交后)
MqMessageSender.sendAfterCommit()
    │
    ├─ rabbitTemplate.convertAndSend()
    ├─ Confirm回调 → onSuccess: outbox.status=1 / onFailure: outbox.status=2
    │
    ▼
消费者 MqConsumers.onXxx()
    │
    ├─ 幂等: idempotentHelper.tryConsume(messageId) → Redis idempotent:mq:{id}
    ├─ 成功: channel.basicAck()
    └─ 失败: channel.basicNack() → DLQ
           │
           └─ MqRetryJob @Scheduled(5min) 补推 outbox status IN (0,2)
```

### 10.3 代码文件

| 文件 | 说明 |
|------|------|
| [`RabbitMQConfig.java`](../insurance/src/main/java/com/xxj/insurance/common/config/RabbitMQConfig.java) | 交换机/队列/DLX声明 |
| [`MqMessageSender.java`](../insurance/src/main/java/com/xxj/insurance/common/mq/MqMessageSender.java) | 消息发送（outbox+Confirm） |
| [`MqIdempotentHelper.java`](../insurance/src/main/java/com/xxj/insurance/common/mq/MqIdempotentHelper.java) | Redisson幂等去重 |
| [`MqConsumers.java`](../insurance/src/main/java/com/xxj/insurance/common/mq/MqConsumers.java) | 手动ACK消费 |
| [`MqRetryJob.java`](../insurance/src/main/java/com/xxj/insurance/common/mq/MqRetryJob.java) | @Scheduled兜底补偿 |

---

## 11. 总结

本项目经过15个模块改造，从基础数据模型到报销规则引擎、医保目录、门诊住院、智能审核、异地就医、多层次保障、退费、报表审计、RabbitMQ异步消息——全面贴近现实医保核销业务。

核心设计思想：

1. **统一响应**：便于前端统一处理成功/失败/异常
2. **注解式权限**：@Permission + SpringMVC 拦截器，Controller/Method 级声明式权限校验
3. **分布式锁**：Redisson 实现单据级细粒度锁，仅串行化同一单据的并发请求
4. **三层幂等**：Redis 预校验 → Redisson 锁 → DB 唯一索引，递进式防止重复结算
5. **事务与缓存一致性**：编程式事务 + 提交后写缓存，解决回滚导致 Redis 脏数据问题
6. **缓存穿透防护**：Cache-Aside 模式 + 空值短 TTL 缓存，防止不存在的数据击穿 DB
7. **N+1 查询消除**：ID 聚合 → 批量加载 → Map 映射，多级关联查询从 O(N) 降至常数级
8. **规则引擎驱动**：报销参数由 reimburse_rule 表配置，替代硬编码固定比例
9. **MQ 异步化**：outbox+Confirm+手动ACK+DLX+兜底Job，主链路不阻塞
10. **全链路联动**：DDL → PO → VO → DTO → 前端TS → MQ消息体，六层同步约束

这些设计思想可以应用到其他企业级项目中。
