# 医保核销系统 - Vue 3 前端

基于 Vue 3 + TypeScript + Element Plus 的现代化前端项目。

## 🚀 技术栈

- **框架**: Vue 3.4
- **语言**: TypeScript 5.3
- **构建工具**: Vite 5.0
- **UI 组件库**: Element Plus 2.5
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.2
- **HTTP 客户端**: Axios 1.6
- **CSS 预处理器**: Sass

## 📦 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口定义
│   │   └── index.ts
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── user.ts
│   ├── styles/           # 全局样式
│   │   ├── index.scss
│   │   └── variables.scss
│   ├── types/            # TypeScript 类型定义
│   │   └── index.ts
│   ├── utils/            # 工具函数
│   │   └── request.ts
│   ├── views/            # 页面组件
│   │   ├── Login.vue
│   │   ├── Register.vue
│   │   ├── Home.vue
│   │   ├── patient/      # 患者相关页面
│   │   ├── hospital/     # 医院相关页面
│   │   └── medical/      # 医保局相关页面
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 🎯 功能模块

### 已实现的核心页面

1. **认证模块**
   - ✅ 登录页面 (`/login`)
   - ✅ 注册页面 (`/register`)

2. **公共模块**
   - ✅ 主布局 (`MainLayout.vue`)
   - ✅ 首页 (`/home`)

3. **患者模块** (待实现)
   - 我的就诊列表
   - 账户管理
   - 账户充值
   - 充值记录
   - 消费记录

4. **医院模块** (待实现)
   - 就诊管理
   - 新增就诊
   - 费用管理
   - 医保结算
   - 申报批次
   - 批次详情

5. **医保局模块** (待实现)
   - 申报批次列表
   - 批次详情
   - 基金拨付

## 🛠️ 开发指南

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装依赖

```bash
cd frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

## 🔧 配置说明

### 代理配置

开发环境下，`vite.config.ts` 已配置代理：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

确保后端服务运行在 `http://localhost:8080`

### 环境变量

可以创建 `.env` 文件配置环境变量：

```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=医保核销系统
```

## 📝 编码规范

### 组件命名

- 页面组件：PascalCase，如 `Login.vue`, `VisitList.vue`
- 普通组件：PascalCase，如 `UserTable.vue`, `SearchForm.vue`

### 文件组织

- 每个页面组件放在 `views/` 目录下
- 按功能模块划分子目录（`patient/`, `hospital/`, `medical/`）
- 可复用组件放在 `components/` 目录

### API 调用

统一在 `api/index.ts` 中定义 API 路径，使用封装的 `http` 工具：

```typescript
import { http, USER_API } from '@/utils/request'

// GET 请求
const response = await http.get<UserVO>('/user/info')

// POST 请求
await http.post(USER_API.LOGIN, loginData)
```

### 状态管理

使用 Pinia 管理全局状态：

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
userStore.setToken(token)
userStore.setUserInfo(userInfo)
```

## 🎨 UI 设计规范

### 主题色

- 主色：`#667eea` (渐变紫蓝)
- 成功：`#52c41a` (绿色)
- 警告：`#faad14` (橙色)
- 危险：`#f5222d` (红色)
- 信息：`#1890ff` (蓝色)

### 字体

- 主字体：`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto`
- 中文字体：`'Helvetica Neue', Arial, 'Noto Sans', sans-serif`

### 间距

使用统一的间距系统：
- xs: 4px
- sm: 8px
- md: 16px
- lg: 24px
- xl: 32px

## 🔐 权限控制

### 路由守卫

已实现基于角色的路由守卫：

```typescript
meta: {
  title: '我的就诊',
  requiresAuth: true,
  roles: [1] // 1-患者，2-医院，3-医保局
}
```

### 动态菜单

根据用户角色动态生成侧边栏菜单。

## 📱 响应式设计

- 桌面优先设计
- 侧边栏在 < 1200px 时自动折叠
- 适配移动端（待完善）

## 🐛 已知问题

1. 部分页面组件待实现
2. 移动端适配待完善
3. 缺少错误边界处理
4. 需要添加单元测试

## 📋 待办事项

- [ ] 完善患者模块所有页面
- [ ] 完善医院模块所有页面
- [ ] 完善医保局模块所有页面
- [ ] 添加公共组件（表格、表单、搜索框等）
- [ ] 添加加载状态和空状态
- [ ] 添加错误边界
- [ ] 添加单元测试
- [ ] 添加 E2E 测试
- [ ] 性能优化（路由懒加载、组件懒加载）
- [ ] PWA 支持
- [ ] 移动端适配

## 📄 License

MIT

---

**开发团队**: 医保核销系统开发组  
**创建时间**: 2026-04-27  
**最后更新**: 2026-04-27
