# 医保核销系统面试复习指南

## 目录

- [1. 项目介绍话术](#1-项目介绍话术)
- [2. 技术问题](#2-技术问题)
- [3. 业务问题](#3-业务问题)
- [4. 架构设计问题](#4-架构设计问题)
- [5. 场景设计问题](#5-场景设计问题)
- [6. 代码细节问题](#6-代码细节问题)
- [7. 扩展问题](#7-扩展问题)

---

## 1. 项目介绍话术

### 1.1 一分钟项目介绍

> 这是一个面向医疗机构的医保费用结算中台，覆盖门诊就诊结算、医保批次申报、财政基金拨付全流程数字化，支撑医院日常医保报销业务。
>
> 后端技术栈：Spring Boot + MyBatis-Plus + MySQL + Redis + Redisson + JWT。
>
> 系统支持四种角色：患者、医院、医保局、管理员。通过 @Permission 注解 + SpringMVC 拦截器实现 Controller/Method 级声明式权限校验。
>
> 我在项目中主导后端核心模块开发，重点解决了六个技术问题：并发重复提交防重、事务与缓存一致性、业务粒度分布式锁、细粒度权限管控、缓存穿透防护、N+1查询消除。

### 1.2 详细项目介绍

> **项目背景**：
> 传统医保报销流程繁琐，需要患者多次往返医院和医保局，纸质材料传递效率低。本项目旨在实现医保报销全流程数字化，提高办事效率。
>
> **技术选型**：
> - 后端框架：Spring Boot 2.7，快速开发、生态完善
> - ORM框架：MyBatis-Plus，简化CRUD操作，支持Lambda查询
> - 数据库：MySQL 8.0，保证数据一致性
> - 缓存：Redis，用于Token存储、分布式锁、业务缓存
> - 分布式锁：Redisson，封装Redis分布式锁，API友好
> - 认证：JWT + Redis，实现无状态认证
>
> **核心功能**：
> 1. 就诊管理：患者挂号、医院录入费用
> 2. 医保结算：自动计算报销金额（甲类100%、乙类80%、自费0%）
> 3. 批次申报：医院批量申报结算单
> 4. 基金拨付：医保局审核拨付
> 5. 患者账户：充值、支付自付部分
>
> **技术亮点**：
> 1. 并发重复提交防重：Redis 预校验 → Redisson 分布式锁 → 数据库唯一索引，递进式三层幂等校验
> 2. 事务与缓存一致性：编程式事务替代声明式事务，事务提交成功后才写 Redis 幂等缓存，避免回滚脏数据
> 3. 业务粒度分布式锁：基于 Redisson 实现单据级细粒度锁（lock:settle:{visitId}），消除全局锁竞争
> 4. 细粒度权限管控：@Permission 注解 + SpringMVC 拦截器，Controller/Method 级声明式权限校验
> 5. 缓存穿透防护：Cache-Aside 模式 + 空值短 TTL 缓存，防止高频查询击穿数据库
> 6. N+1 查询消除：结算单详情接口 ID 聚合 → 批量加载 → Map 映射，SQL 从 O(N) 降至常数级

---

## 2. 技术问题

### 2.1 Spring Boot 相关

#### Q: 为什么选择 Spring Boot 2.7 版本？

**回答要点**：
1. **稳定性**：2.7 是长期支持版本，bug修复完善
2. **兼容性**：与 MyBatis-Plus 3.5、Redisson 3.17 兼容性好
3. **Java 17 支持**：支持最新的 LTS 版本 JDK

#### Q: Spring Boot 自动配置原理？

**回答要点**：
1. `@EnableAutoConfiguration` 注解开启自动配置
2. 通过 `spring.factories` 文件加载自动配置类
3. 根据 `@Conditional` 条件判断是否生效
4. 例如：`RedisAutoConfiguration` 在有 Redis 依赖时自动配置

#### Q: 你是如何理解 Spring 的 IOC 和 AOP 的？

**回答要点**：
1. **IOC（控制反转）**：
   - 对象的创建权交给 Spring 容器
   - 通过依赖注入（DI）实现解耦
   - 本项目使用构造器注入（`@RequiredArgsConstructor`）

2. **AOP（面向切面编程）**：
   - 将横切关注点（如事务、日志）分离
   - 本项目中用于全局异常处理、权限校验

### 2.2 MyBatis-Plus 相关

#### Q: 为什么选择 MyBatis-Plus 而不是 MyBatis？

**回答要点**：
1. **简化 CRUD**：内置通用 Mapper，无需手写基础 SQL
2. **Lambda 查询**：类型安全的查询构造器
3. **分页插件**：内置分页功能
4. **代码生成**：支持代码生成器

```java
// Lambda 查询示例
LambdaQueryWrapper<Settle> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Settle::getHospitalId, hospitalId)
       .orderByDesc(Settle::getCreateTime);
```

#### Q: MyBatis-Plus 如何实现分页？

**回答要点**：
1. 配置 `MybatisPlusInterceptor` 分页插件
2. 使用 `Page` 对象进行分页查询
3. 插件会自动拦截 SQL，添加 LIMIT 语句

#### Q: 什么是 N+1 问题？如何解决？

**回答要点**：
1. **问题**：查询 N 条主记录后，循环查询关联记录，导致 N+1 次 SQL
2. **解决**：使用批量查询 + Map 映射

```java
// 错误：N+1 查询
for (BatchItem item : items) {
    Settle settle = settleService.getById(item.getSettleId());
}

// 正确：批量查询
Set<Long> settleIds = items.stream().map(BatchItem::getSettleId).collect(Collectors.toSet());
List<Settle> settleList = settleService.listByIds(settleIds);
Map<Long, Settle> settleMap = settleList.stream()
    .collect(Collectors.toMap(Settle::getId, Function.identity()));
```

### 2.3 Redis 相关

#### Q: Redis 在项目中有哪些应用场景？

**回答要点**：
1. **Token 存储**：`login:token:{token}` → userId
2. **分布式锁**：使用 Redisson 实现
3. **幂等性控制**：`idempotent:settle:{visitId}`
4. **业务缓存**：就诊信息、用户信息缓存

#### Q: 为什么用 Redis 存储 Token 而不是只用 JWT？

**回答要点**：
1. **主动失效**：可以手动删除 Redis 中的 Token，实现强制下线
2. **续期方便**：可以延长 Token 有效期
3. **安全审计**：可以统计在线用户

#### Q: Redis 缓存穿透、击穿、雪崩了解吗？

**回答要点**：

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 缓存穿透 | 查询不存在的数据 | 布隆过滤器、缓存空值 |
| 缓存击穿 | 热点 Key 过期 | 加互斥锁、永不过期 |
| 缓存雪崩 | 大量 Key 同时过期 | 随机过期时间、多级缓存 |

**本项目解决方案**：
- 缓存穿透：Cache-Aside 模式 + 空值短 TTL 缓存。DB 查不到就诊记录时，缓存空串（1分钟TTL），防止恶意/异常请求直接击穿 DB。
- 缓存击穿：热点数据（就诊信息、用户信息）使用分布式锁互斥加载，仅一个线程回源 DB。
- 缓存雪崩：不同业务缓存 TTL 差异化设置（30min / 1min / 24h），自然分散过期时间。

#### Q: Redisson 分布式锁原理？

**回答要点**：
1. **加锁**：`SET lock:key value NX PX 30000`
2. **看门狗**：自动续期，防止业务未执行完锁过期
3. **可重入**：使用 Hash 结构存储锁计数
4. **释放锁**：Lua 脚本保证原子性

```java
RLock lock = redissonClient.getLock("lock:settle:" + visitId);
try {
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 执行业务
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

### 2.4 分布式锁相关

#### Q: 为什么不用 `synchronized` 而用分布式锁？

**回答要点**：
1. **适用场景不同**：
   - `synchronized`：单机环境，JVM 内锁
   - 分布式锁：集群环境，跨 JVM 锁
2. **本项目场景**：可能部署多实例，需要分布式锁保证并发安全

#### Q: 分布式锁的锁粒度如何设计？

**回答要点**：
1. **原则**：锁粒度要细，避免不必要的阻塞
2. **本项目设计**：
   - 结算锁：`lock:settle:{visitId}` - 就诊级别
   - 批次锁：`lock:batch:add:{batchId}` - 批次级别
   - 充值锁：`lock:account:recharge:{userId}` - 用户级别

#### Q: 分布式锁获取失败怎么处理？

**回答要点**：
1. 使用 `tryLock` 而非 `lock`，设置等待时间
2. 获取失败返回友好提示："操作正在进行中，请勿重复提交"
3. 避免无限等待导致线程阻塞

### 2.5 事务相关

#### Q: 为什么用编程式事务而不是 `@Transactional`？

**回答要点**：
1. **事务与缓存一致性**：`@Transactional` 回滚时无法撤销已写入 Redis 的幂等缓存，导致脏数据。编程式事务将缓存写入放在 `execute()` 成功后，回滚时不写缓存。
2. **锁与事务配合**：锁在事务外获取，避免锁持有时间过长
3. **精确控制边界**：可以精确控制事务边界，事务提交后执行清理缓存等操作

```java
// 编程式事务 — 核心模板
Result result = transactionTemplate.execute(status -> {
    // 事务内：业务逻辑
    return doBusiness();
});

// 事务提交成功后才写缓存 — 关键：回滚时不执行
if (result != null && result.getSuccess()) {
    idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
}
// 如果事务回滚，execute() 返回 null，不写缓存 — 消除脏数据
```

#### Q: 事务传播行为了解吗？

**回答要点**：
1. **REQUIRED**（默认）：有事务就加入，没有就新建
2. **REQUIRES_NEW**：总是新建事务
3. **NESTED**：嵌套事务，可独立回滚

#### Q: 事务失效的场景有哪些？

**回答要点**：
1. 方法不是 public
2. 同类方法调用（未走代理）
3. 异常被 catch 捕获未抛出
4. 抛出非 RuntimeException
5. 数据库引擎不支持事务

---

## 3. 业务问题

### 3.1 结算业务

#### Q: 医保结算的计算规则是什么？

**回答要点**：
1. **甲类药品**：100% 报销
2. **乙类药品**：80% 报销
3. **自费药品**：0% 报销

```java
private BigDecimal getReimburseRate(Integer type) {
    switch (type) {
        case 1: return new BigDecimal("1.00");    // 甲类
        case 2: return new BigDecimal("0.80");    // 乙类
        default: return BigDecimal.ZERO;          // 自费
    }
}
```

#### Q: 结算金额计算为什么用 BigDecimal？

**回答要点**：
1. **精度问题**：浮点数有精度丢失问题
2. **金融场景**：金额计算必须精确
3. **注意点**：
   - 使用 `setScale(2, RoundingMode.HALF_UP)` 设置精度
   - 先累加再四舍五入，避免累积误差

#### Q: 如何保证结算不重复？

**回答要点**：三层幂等校验

1. **Redis 预检查**：快速拦截
2. **分布式锁**：防止并发
3. **数据库校验**：最终兜底

```
请求 → Redis检查 → 获取锁 → DB检查 → 执行业务 → 写幂等标记
```

### 3.2 批次业务

#### Q: 批次状态是如何流转的？

**回答要点**：

```
待申报 → 已申报 → 已完成
           ↓
       拨付拒绝 → 重新申报
```

| 状态 | 说明 | 可执行操作 |
|------|------|------------|
| 待申报 | 新建批次 | 添加结算单、申报 |
| 已申报 | 已提交医保局 | 撤回、拨付、拒绝 |
| 已完成 | 拨付成功 | 无 |
| 拨付拒绝 | 被医保局拒绝 | 修改后重新申报 |

#### Q: 批次号是如何生成的？

**回答要点**：
```
批次号 = 时间戳(14位) + 医院ID后4位 + 随机数(6位)
示例：20260518143025_0001_123456
```

1. **时间戳**：精确到秒，便于排序
2. **医院ID**：区分不同医院
3. **随机数**：防止重复

### 3.3 账户业务

#### Q: 充值如何保证并发安全？

**回答要点**：
1. **分布式锁**：用户级别锁
2. **事务控制**：更新余额和创建记录在同一事务
3. **乐观锁**：可使用版本号控制

```java
String lockKey = "lock:account:recharge:" + userId;
RLock lock = redissonClient.getLock(lockKey);
try {
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 更新余额
        account.setBalance(account.getBalance().add(amount));
        accountService.updateById(account);
        // 创建充值记录
        rechargeRecordService.save(record);
    }
} finally {
    lock.unlock();
}
```

#### Q: 支付流程是怎样的？

**回答要点**：
1. 校验结算单状态
2. 校验账户余额
3. 扣款（分布式锁）
4. 更新结算状态
5. 创建消费记录

---

## 4. 架构设计问题

### 4.1 权限设计

#### Q: 权限是如何设计的？

**回答要点**：
1. **自定义注解**：`@Permission({Role.HOSPITAL, Role.ADMIN})`
2. **拦截器校验**：在 `UserInfoInterceptor` 中校验
3. **管理员特权**：管理员拥有所有权限

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    Role[] value();
}
```

#### Q: 为什么不用 Spring Security？

**回答要点**：
1. **项目规模**：中小型项目，权限模型简单
2. **学习成本**：Spring Security 配置复杂
3. **灵活性**：自定义注解更灵活，易于理解

### 4.2 用户上下文设计

#### Q: 如何在请求中传递用户信息？

**回答要点**：
1. 使用 `ThreadLocal` 存储
2. 拦截器中写入，业务代码中读取
3. 请求结束后清理，防止内存泄漏

```java
public class UserHolder {
    private static final ThreadLocal<Long> TL = new ThreadLocal<>();

    public static void save(Long userId) {
        TL.set(userId);
    }

    public static Long getUserId() {
        return TL.get();
    }

    public static void remove() {
        TL.remove();
    }
}
```

#### Q: ThreadLocal 在什么情况下会有问题？

**回答要点**：
1. **线程池场景**：线程复用导致数据错乱
2. **异步调用**：子线程无法获取父线程数据
3. **解决方案**：
   - 使用 `InheritableThreadLocal`
   - 使用 `TransmittableThreadLocal`（阿里开源）

### 4.3 统一响应设计

#### Q: 为什么设计统一的响应格式？

**回答要点**：
1. **前端友好**：统一判断 `success` 字段
2. **错误处理**：统一错误信息格式
3. **扩展性**：可添加 `code` 等扩展字段

```java
@Data
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;
    private Integer code;
}
```

---

## 5. 场景设计问题

### 5.1 高并发场景

#### Q: 如果结算接口 QPS 很高，如何优化？

**回答要点**：
1. **缓存优化**：热点数据缓存
2. **异步处理**：非核心逻辑异步执行
3. **限流**：接口级别限流
4. **队列削峰**：使用消息队列

#### Q: 如何防止接口被恶意刷？

**回答要点**：
1. **限流**：IP 限流、用户限流
2. **验证码**：关键接口加验证码
3. **签名校验**：请求签名验证
4. **黑名单**：异常 IP 加入黑名单

### 5.2 数据一致性场景

#### Q: 分布式事务如何处理？

**回答要点**：
1. **本项目方案**：单库事务 + 最终一致性
2. **其他方案**：
   - Seata 分布式事务框架
   - TCC 模式
   - 本地消息表
   - 事务消息

#### Q: 缓存和数据库一致性如何保证？

**回答要点**：
1. **Cache-Aside 模式**：先更新数据库，再删除缓存
2. **延迟双删**：删除缓存 → 更新数据库 → 延迟删除缓存
3. **本项目方案**：事务提交后删除缓存

```java
Result result = transactionTemplate.execute(status -> {
    // 更新数据库
    updateData();
    return Result.ok();
});

// 事务提交后删除缓存
if (result != null && result.getSuccess()) {
    redisTemplate.delete(cacheKey);
}
```

### 5.3 安全场景

#### Q: 如何防止重复提交？

**回答要点**：
1. **前端防抖**：按钮禁用
2. **后端幂等**：Token 机制、唯一索引、分布式锁
3. **本项目方案**：Redis 幂等标记 + 分布式锁

#### Q: 密码如何安全存储？

**回答要点**：
1. **BCrypt 加密**：自带盐值，防止彩虹表攻击
2. **不使用 MD5**：MD5 可被彩虹表破解
3. **加盐**：每个用户独立盐值

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// 加密
String encoded = passwordEncoder.encode(rawPassword);
// 校验
boolean matches = passwordEncoder.matches(rawPassword, encoded);
```

---

## 6. 代码细节问题

### 6.1 代码规范

#### Q: 项目中使用了哪些代码规范？

**回答要点**：
1. **命名规范**：类名大驼峰、方法名小驼峰、常量全大写
2. **注释规范**：类注释、方法注释、关键逻辑注释
3. **异常处理**：全局异常处理器
4. **日志规范**：关键操作记录日志

#### Q: 为什么使用 Lombok？

**回答要点**：
1. **减少样板代码**：自动生成 getter/setter/构造方法
2. **代码简洁**：提高可读性
3. **常用注解**：
   - `@Data`：getter/setter/toString/equals/hashCode
   - `@RequiredArgsConstructor`：final 字段构造方法
   - `@Slf4j`：日志对象

### 6.2 设计模式

#### Q: 项目中用到了哪些设计模式？

**回答要点**：
1. **策略模式**：费用类型计算报销比例
2. **模板方法**：业务流程模板
3. **单例模式**：Spring Bean 默认单例
4. **代理模式**：AOP 实现

#### Q: 如何处理循环依赖？

**回答要点**：
1. **原因**：A 依赖 B，B 依赖 A
2. **解决方案**：
   - 使用 `@Lazy` 延迟加载
   - 使用 setter 注入代替构造器注入
   - 重构代码，解除循环依赖

```java
// 本项目解决方案：setter 注入 + @Lazy
private IPayService payService;

@Lazy
public void setPayService(IPayService payService) {
    this.payService = payService;
}
```

---

## 7. 扩展问题

### 7.1 项目扩展

#### Q: 如果要支持多医院集团，如何设计？

**回答要点**：
1. **数据隔离**：按医院 ID 隔离数据
2. **权限升级**：增加集团管理员角色
3. **分库分表**：按医院 ID 分库

#### Q: 如何实现数据报表功能？

**回答要点**：
1. **数据仓库**：将数据同步到数仓
2. **OLAP 查询**：使用 ClickHouse 等分析型数据库
3. **定时任务**：定时统计汇总

### 7.2 性能优化

#### Q: 如何进行性能调优？

**回答要点**：
1. **数据库优化**：索引、SQL 优化、分库分表
2. **缓存优化**：多级缓存、缓存预热
3. **代码优化**：减少循环查询、批量操作
4. **架构优化**：异步处理、消息队列

#### Q: 如何定位性能瓶颈？

**回答要点**：
1. **监控工具**：Prometheus + Grafana
2. **链路追踪**：SkyWalking、Zipkin
3. **日志分析**：ELK
4. **JVM 分析**：Arthas、JProfiler

### 7.3 项目难点

#### Q: 项目中遇到的最大难点是什么？

**回答要点**（示例）：

> **难点一：并发重复提交防重**
>
> 同一个就诊记录可能被多次结算，导致重复扣款。我设计了三层递进式幂等校验：
> 1. Redis 预检查：快速拦截重复请求
> 2. Redisson 分布式锁：串行化同一就诊的并发请求
> 3. 数据库唯一索引：最终兜底
>
> **难点二：事务与缓存一致性**
>
> 幂等标记写入时机很关键。使用 `@Transactional` 时，事务回滚但幂等标记已写入 Redis，导致后续正常请求被误拦截。
>
> 解决方案：改用编程式事务 `TransactionTemplate.execute()`，仅在事务提交成功后写入 Redis 幂等缓存。回滚时 `execute()` 返回 null，不写缓存，消除脏数据。

#### Q: 如何保证代码质量？

**回答要点**：
1. **代码审查**：团队 Code Review
2. **单元测试**：核心逻辑编写单元测试
3. **静态分析**：SonarQube 代码扫描
4. **持续集成**：CI/CD 流水线

---

## 8. 面试技巧

### 8.1 回答问题的框架

1. **先总后分**：先说结论，再展开细节
2. **结合项目**：用项目中的实际案例说明
3. **展示深度**：不仅说怎么做，还要说为什么
4. **承认不足**：不懂的问题诚实回答，展示学习能力

### 8.2 常见陷阱问题

#### Q: 你的项目有什么不足？

**回答要点**：
1. **诚实但有改进思路**
2. 示例：
   - 没有完善的监控告警 → 可以引入 Prometheus
   - 缺少单元测试 → 可以补充核心模块测试
   - 没有做灰度发布 → 可以引入灰度发布机制

#### Q: 如果重新设计这个项目，你会怎么做？

**回答要点**：
1. **微服务拆分**：按业务域拆分服务
2. **引入消息队列**：异步处理、解耦
3. **完善监控**：全链路监控、告警
4. **容器化部署**：Docker + K8s

### 8.3 自我介绍模板

> 面试官您好，我叫 xxx，有 x 年 Java 开发经验。
>
> 我最近参与的项目是一个医保核销系统，这是一个基于 Spring Boot 的医疗保险费用结算平台。
>
> 项目采用前后端分离架构，后端使用 Spring Boot + MyBatis-Plus + Redis + Redisson。
>
> 我在项目中主要负责核心业务模块的开发，包括医保结算、批次申报、基金拨付等。在开发过程中，我重点解决了并发安全、幂等性控制等技术难点。
>
> 通过这个项目，我对分布式锁、事务管理、缓存策略有了更深入的理解。

---

## 9. 快速复习清单

### 9.1 必背知识点

- [ ] 项目背景和业务流程
- [ ] 技术栈选型理由
- [ ] 三层幂等校验方案（Redis预校验 → Redisson锁 → DB唯一索引）
- [ ] 事务与缓存一致性（编程式事务 + 提交后写缓存）
- [ ] 分布式锁粒度设计（单据级 vs 全局级）
- [ ] 缓存穿透防护（Cache-Aside + 空值短TTL）
- [ ] N+1 查询消除（ID聚合 → 批量加载 → Map映射）
- [ ] 权限控制方案（@Permission + 拦截器）
- [ ] ThreadLocal 使用和注意事项
- [ ] BigDecimal 金额计算

### 9.2 代码片段记忆

```java
// 1. 分布式锁模板
RLock lock = redissonClient.getLock(lockKey);
try {
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}

// 2. 编程式事务模板
Result result = transactionTemplate.execute(status -> {
    // 业务逻辑
    return Result.ok();
});

// 3. Lambda 查询模板
LambdaQueryWrapper<Settle> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Settle::getHospitalId, hospitalId)
       .orderByDesc(Settle::getCreateTime);

// 4. 批量查询 + Map 映射
Set<Long> ids = list.stream().map(Entity::getId).collect(Collectors.toSet());
Map<Long, Entity> map = service.listByIds(ids).stream()
    .collect(Collectors.toMap(Entity::getId, Function.identity()));
```

### 9.3 面试高频问题 Top 10

1. 项目介绍（必问）
2. 三层幂等校验方案（递进式：预检查 → 锁 → DB约束）
3. 分布式锁原理和锁粒度设计
4. 事务与缓存一致性（编程式事务 + 提交后写缓存）
5. 缓存穿透防护（Cache-Aside + 空值短TTL）
6. 并发安全和幂等性保证
7. N+1 查询问题和消除方案
8. 权限设计方案（@Permission + 拦截器）
9. Redis 缓存使用场景
10. 项目难点和解决方案
