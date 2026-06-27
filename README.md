# 医保核销系统

面向医疗机构的医保费用结算中台，覆盖门诊就诊结算、医保批次申报、财政基金拨付全流程数字化。

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 2.7 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.0 |
| 分布式锁 | Redisson 3.17 |
| 认证 | JWT + Redis Token |
| 工具 | HuTool、Lombok |
| 前端 | Vue 3 + TypeScript + Element Plus |

## 系统角色

| 角色 | 职责 |
|------|------|
| 患者 | 挂号就诊、账户充值、支付自付部分、查询费用 |
| 医院 | 录入费用、医保结算、创建批次、申报拨付 |
| 医保局 | 审核批次、基金拨付、拒绝退回 |
| 管理员 | 管理医院、代理操作 |

## 核心业务

```
患者挂号 → 医院录入费用 → 医保结算（甲类100%/乙类80%/自费0%）
    → 患者支付自付部分 → 医院创建批次 → 批次申报
    → 医保局审核 → 基金拨付 → 批次完成
```

## 工程亮点

**并发重复提交防重**
高并发下同一单据可能被重复结算，设计 Redis 预校验 → Redisson 分布式锁 → 数据库唯一索引三层递进式幂等校验链。

**事务与缓存一致性**
声明式 `@Transactional` 回滚时无法撤销已写入的 Redis 幂等缓存。改用 `TransactionTemplate` 编程式事务，仅事务提交成功后写入缓存，消除回滚脏数据。

**业务粒度分布式锁**
全局锁导致无关单据并发阻塞。基于 Redisson 实现单据级锁粒度（`lock:settle:{visitId}`），仅串行化同一单据的并发请求。

**细粒度权限管控**
多角色接口访问隔离。`@Permission` 注解 + SpringMVC 拦截器实现 Controller/Method 级声明式权限校验，无需在业务代码中硬编码角色判断。

**缓存穿透防护**
就诊记录高频查询，Cache-Aside 模式 + 空值短 TTL 缓存（1 分钟），防止不存在的数据反复击穿数据库。

**N+1 查询消除**
结算单详情涉及 结算单 → 就诊 → 用户 三级关联。ID 聚合 → 批量加载 → Map 映射，SQL 从 O(N) 降至常数级。

## 快速开始

**环境要求**：JDK 17+、MySQL 8.0+、Redis 7.0+、Maven 3.6+

```bash
# 1. 克隆
git clone https://github.com/xxinjie21/insurance.git
cd insurance

# 2. 初始化数据库（创建 medical_insurance 库后执行）
mysql -u root -p medical_insurance < insurance/docs/int.sql

# 3. 修改配置 insurance/src/main/resources/application.yml（数据库连接、Redis连接）

# 4. 启动
cd insurance
mvn spring-boot:run

# 5. 访问 http://localhost:8080
```

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

## 核心数据表

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
| `consumption_record` | 消费记录 | `uk_order_no` |

## API 概览

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
| 拨付 | `/pay/reject-batch/{batchId}` | POST | 医保局 |
| 账户 | `/account/recharge` | POST | 患者 |
| 账户 | `/account/pay` | POST | 患者 |

## 许可证

仅供学习参考。
