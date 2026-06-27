# 快速启动指南

## 前后端联调步骤

### 1. 启动后端服务

```bash
cd d:\javaproject\insurance
mvn spring-boot:run
```

后端服务将运行在：http://localhost:8080

### 2. 启动前端服务

```bash
cd d:\javaproject\insurance\frontend
npm install
npm run dev
```

前端服务将运行在：http://localhost:3000

### 3. 测试登录

访问 http://localhost:3000/login

使用测试账号登录：
- **患者**: userId=1, password=123456, role=1
- **医院**: userId=2, password=123456, role=2
- **医保局**: userId=3, password=123456, role=3

## 开发环境配置

### 前端代理配置

前端已配置代理，所有 `/api` 请求将转发到后端：
- 前端请求：`/api/user/login`
- 后端接收：`http://localhost:8080/user/login`

### 跨域处理

开发环境通过 Vite 代理解决跨域问题，无需额外配置。

生产环境需要配置 CORS 或在同域部署。

## 数据库初始化

确保已执行 SQL 脚本：
```bash
mysql -u root -p < docs/int.sql
```

## 常见问题

### 1. 前端启动失败

**问题**: `npm install` 失败

**解决**:
```bash
# 清理缓存
npm cache clean --force

# 删除 node_modules
rm -rf node_modules
rm package-lock.json

# 重新安装
npm install
```

### 2. 后端连接失败

**问题**: 前端请求后端 404 或网络错误

**解决**:
1. 确保后端服务已启动
2. 检查后端端口是否为 8080
3. 检查 `vite.config.ts` 中的代理配置

### 3. 登录失败

**问题**: 提示"用户不存在"或"密码错误"

**解决**:
1. 确保数据库已初始化
2. 检查用户表是否有数据
3. 密码是 MD5 加密存储的

### 4. Token 失效

**问题**: 登录后刷新页面需要重新登录

**解决**: 这是正常行为，Token 存储在 localStorage 中，刷新页面会自动读取

## 项目结构说明

```
insurance/
├── src/                    # 后端代码
│   ├── controller/        # Controller 层
│   ├── service/           # Service 层
│   ├── mapper/            # DAO 层
│   └── domain/            # 数据模型
├── frontend/              # 前端代码
│   ├── src/
│   │   ├── api/          # API 接口
│   │   ├── views/        # 页面组件
│   │   ├── layouts/      # 布局组件
│   │   ├── router/       # 路由配置
│   │   └── stores/       # 状态管理
│   └── package.json
└── docs/                  # 文档
    ├── int.sql           # 数据库脚本
    └── README.md         # 项目文档
```

## 下一步开发

### 待实现的页面

1. **医院模块**
   - [ ] 就诊管理列表
   - [ ] 新增就诊
   - [ ] 费用管理
   - [ ] 医保结算
   - [ ] 申报批次
   - [ ] 批次详情

2. **医保局模块**
   - [ ] 批次列表
   - [ ] 批次详情
   - [ ] 基金拨付

### 功能优化

- [ ] 添加表单验证
- [ ] 添加加载状态
- [ ] 添加空状态
- [ ] 添加错误处理
- [ ] 响应式适配移动端
- [ ] 添加权限按钮控制

## 技术栈

### 后端
- Spring Boot 2.7.18
- MyBatis-Plus
- MySQL 8.0+
- Redis 7.0+
- Redisson

### 前端
- Vue 3.4
- TypeScript 5.3
- Element Plus 2.5
- Vite 5.0
- Pinia 2.1
- Vue Router 4.2

## 开发规范

### Git 提交规范

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建/工具链相关
```

### 代码规范

- 使用 ESLint 检查代码
- 使用 Prettier 格式化代码
- 遵循 Vue 3 组合式 API 风格
- 使用 TypeScript 严格模式

---

**创建时间**: 2026-04-27  
**维护人员**: 医保核销系统开发团队
