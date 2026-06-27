# 医保核销系统

一个基于 Spring Boot 的完整医保费用结算与核销管理系统，支持患者就诊、费用结算、批次申报、基金拨付等全流程业务。

## 📋 项目信息

- **项目名称**: 医保核销系统 (Medical Insurance Reimbursement System)
- **技术栈**: Spring Boot 2.7.18 + MyBatis-Plus + MySQL + Redis + Redisson
- **JDK 版本**: 17
- **版本**: 1.0.0

## 🎯 核心功能

### 1. 就诊管理
- 患者挂号就诊
- 医院录入费用明细
- 就诊状态管理

### 2. 医保结算
- 自动计算报销金额（甲类 100%、乙类 80%、自费 0%）
- 生成结算记录
- 防止重复结算（分布式锁 + 幂等控制）

### 3. 批次申报
- 医院创建申报批次
- 批量添加结算单
- 申报状态流转

### 4. 基金拨付
- 医保局审核批次
- 基金拨付到医院
- 拨付记录管理

### 5. 患者账户
- 账户充值（微信/支付宝/银行卡/现金）
- 余额支付自付部分
- 充值/消费记录查询

## 🏗️ 系统架构

### 技术架构
```
┌─────────────────────────────────────────┐
│          Controller 层                   │
│   (用户交互、参数校验、权限控制)            │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│          Service 层                      │
│   (业务逻辑、分布式锁、事务管理)            │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│          Mapper 层                       │
│   (数据访问、MyBatis-Plus)                │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│     MySQL (数据持久化) + Redis (缓存)     │
└─────────────────────────────────────────┘
```

### 系统角色
| 角色 | 标识 | 权限说明 |
|------|------|----------|
| 患者 | PATIENT(1) | 就诊、充值、查询、支付 |
| 医院 | HOSPITAL(2) | 录入费用、结算、申报批次 |
| 医保局 | MEDICAL(3) | 审核批次、基金拨付 |

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.6+

### 安装步骤

#### 1. 克隆项目
```bash
git clone https://github.com/xxinjie21/insurance.git
cd insurance
```

#### 2. 初始化数据库
```bash
# 执行 SQL 脚本
mysql -u root -p < docs/int.sql
```

#### 3. 配置应用
编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/medical_insurance?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
```

#### 4. 启动项目
```bash
# Maven 方式
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/insurance-1.0.0.jar
```

#### 5. 访问系统
```
默认端口：8080
API 文档：http://localhost:8080/api-docs (如有 Swagger)
```

## 📚 核心业务表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `hospital` | 医院表 | id, name |
| `user` | 用户表 | id, password, name, id_card, hospital_id, role |
| `visit` | 就诊表 | id, user_id, hospital_id, type, diagnosis, status |
| `fee` | 费用明细表 | id, visit_id, name, price, num, total, type |
| `settle` | 结算表 | id, visit_id, total, reimburse, self_pay, status |
| `batch` | 申报批次表 | id, hospital_id, batch_no, settle_cnt, total_amt, status |
| `batch_item` | 申报明细表 | id, batch_id, settle_id, audit |
| `pay` | 基金拨付表 | id, batch_id, hospital_id, amount, status |
| `user_account` | 患者账户表 | id, user_id, balance, total_recharge, total_consumption |
| `recharge_record` | 充值记录表 | id, user_id, order_no, amount, type |
| `consumption_record` | 消费记录表 | id, user_id, visit_id, amount, balance_before, balance_after |

## 🔑 核心技术特性

### 1. 并发重复提交防重
- Redis 预校验 → Redisson 分布式锁 → 数据库唯一索引，三层递进式幂等校验
- 应用场景：就诊结算、批次申报、基金拨付

### 2. 事务与缓存一致性
- 编程式事务（`TransactionTemplate`）替代声明式事务（`@Transactional`）
- 事务提交成功后才写入 Redis 幂等缓存，回滚不产生脏数据

### 3. 业务粒度分布式锁
- 基于 Redisson 实现单据级细粒度锁（`lock:settle:{visitId}`）
- 仅串行化同一单据的并发请求，消除全局锁竞争阻塞

### 4. 细粒度权限管控
- `@Permission` 注解 + SpringMVC 拦截器，Controller/Method 级声明式权限校验
- 支持患者/医院/医保局/管理员四种角色，无需硬编码角色判断

### 5. 缓存穿透防护
- Cache-Aside 模式 + 空值短 TTL 缓存，防止不存在的数据击穿数据库

### 6. N+1 查询消除
- 结算单详情多级关联（结算单→就诊→用户），ID 聚合 → 批量加载 → Map 映射，SQL 从 O(N) 降至常数级

## 📖 API 接口概览

### 用户模块
- `POST /user/sign` - 用户注册
- `POST /user/login` - 用户登录
- `POST /user/loginout` - 用户登出

### 医院模块
- `POST /hospital/sign` - 医院注册
- `GET /hospital/list` - 获取医院列表
- `GET /hospital/patient/list` - 查询本院患者

### 就诊模块
- `POST /visit/add` - 新增就诊
- `GET /visit/my/list` - 查询个人就诊
- `GET /visit/hospital/list` - 医院查询就诊
- `GET /visit/{visitId}` - 查询就诊详情

### 费用模块
- `POST /fee/batch/add` - 批量添加费用
- `GET /fee/listByVisitId` - 查询费用明细

### 结算模块
- `POST /settle/calculate/{visitId}` - 就诊结算
- `GET /settle/detail/{visitId}` - 查询结算详情

### 批次模块
- `POST /batch/create/{hospitalId}` - 创建批次
- `POST /batch/add-settle` - 添加结算单到批次
- `GET /batch/detail/{batchId}` - 查询批次详情

### 拨付模块
- `POST /pay/pay-batch/{batchId}` - 拨付批次款项
- `GET /pay/by-batch/{batchId}` - 查询拨付信息

### 账户模块
- `GET /account/get` - 获取账户信息
- `POST /account/recharge` - 账户充值
- `POST /account/pay` - 账户支付
- `GET /account/recharge/list` - 充值记录列表
- `GET /account/consumption/list` - 消费记录列表

## 📊 业务流程示例

### 就诊结算完整流程

```
1. 患者注册 → 2. 医院创建就诊 → 3. 添加费用明细
     ↓
4. 就诊结算（计算报销金额） → 5. 患者充值（如余额不足）
     ↓
6. 账户支付自付部分 → 7. 医院创建申报批次
     ↓
8. 添加结算单到批次 → 9. 医保局审核拨付
```

### 金额计算示例

```
费用明细:
- 阿莫西林（甲类）: 31.00 × 100% = 31.00
- 血常规（甲类）: 25.00 × 100% = 25.00

总费用：56.00
报销金额：56.00
自付金额：0.00
```

## 📁 项目结构

```
insurance/
├── doc/                             # 项目文档
│   ├── api-test-cases.md           # API 测试案例
│   ├── interview-guide.md          # 面试复习指南
│   └── technical-architecture.md   # 技术架构文档
├── insurance/
│   ├── docs/int.sql                # 数据库初始化脚本
│   └── src/main/java/com/xxj/insurance/
│       ├── common/                  # 公共模块
│       │   ├── annotation/         # 自定义注解（@Permission）
│       │   ├── config/             # 配置类（Redisson、MVC、MyBatis）
│       │   ├── constants/          # 常量定义
│       │   ├── domain/             # 通用响应体（Result、PageDTO）
│       │   ├── enums/              # 枚举（Role）
│       │   ├── exception/          # 全局异常处理
│       │   ├── interceptors/       # 拦截器（Token + 权限校验）
│       │   └── utils/              # 工具类（JWT、UserHolder）
│       ├── controller/             # Controller 层
│       ├── domain/
│       │   ├── dto/                # 数据传输对象
│       │   ├── po/                 # 持久化对象（MyBatis-Plus Entity）
│       │   └── vo/                 # 视图对象
│       ├── mapper/                 # Mapper 接口 + XML
│       └── service/
│           ├── impl/               # Service 实现
│           └── I*Service.java      # Service 接口
├── frontend/                       # Vue3 前端
└── README.md
```

## 🧪 测试

### 测试案例文档
详细测试案例请查看：[API 测试案例](docs/api-test-cases.md)

### 快速测试
```bash
# 运行单元测试
mvn test

# 运行特定测试
mvn test -Dtest=UserServiceTest
```

## 📝 开发规范

### 代码风格
- 使用 Lombok 简化代码
- 统一使用 `Result` 包装返回结果
- Service 层统一使用 `@RequiredArgsConstructor` 构造器注入
- 事务使用编程式 `TransactionTemplate`，精确控制事务边界与缓存写入时机

### 命名规范
- 表名、字段名：下划线命名（如 `user_account`）
- 类名：大驼峰命名（如 `UserAccount`）
- 方法名：小驼峰命名（如 `getUserInfo`）
- 常量：全大写 + 下划线（如 `CACHE_VISIT_TTL`）

## 🔧 常见问题

### 1. 启动报错
- 检查 MySQL 是否启动
- 检查 Redis 是否启动
- 检查数据库连接配置

### 2. 登录失败
- 确认用户已注册
- 检查密码是否正确（BCrypt 加密）
- 查看 Redis 是否正常运行

### 3. 权限不足
- 确认 Token 是否正确
- 检查用户角色是否匹配
- 查看拦截器日志

## 📄 许可证

本项目仅供学习参考使用。

## 👥 开发团队

- 开发：xxj
- 版本：1.0.0
