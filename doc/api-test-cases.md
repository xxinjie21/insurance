# 医保核销系统 API 接口测试案例

## 目录

- [1. 用户模块](#1-用户模块)
- [2. 医院模块](#2-医院模块)
- [3. 就诊模块](#3-就诊模块)
- [4. 费用模块](#4-费用模块)
- [5. 结算模块](#5-结算模块)
- [6. 批次模块](#6-批次模块)
- [7. 拨付模块](#7-拨付模块)
- [8. 账户模块](#8-账户模块)

---

## 通用说明

### 基础地址
```
http://localhost:8080
```

### 请求头
```
Content-Type: application/json
token: {登录后获取的token}  // 除登录、注册外都需要
```

### 响应格式
```json
{
  "success": true,
  "data": {},
  "errorMsg": null,
  "total": null,
  "code": null
}
```

### 角色编码
| 角色 | code |
|------|------|
| 患者 | 1 |
| 医院 | 2 |
| 医保局 | 3 |
| 管理员 | 4 |

---

## 1. 用户模块

### 1.1 用户注册

**接口**: `POST /user/sign`

**请求体**:
```json
{
  "name": "张三",
  "password": "123456",
  "idCard": "110101199001011234",
  "role": 1
}
```

**测试案例**:

| 案例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 正常注册-患者 | name="张三", password="123456", idCard="110101199001011234", role=1 | success=true, 返回用户信息 |
| 正常注册-医院 | name="北京医院", password="123456", idCard="91110000123456789X", role=2 | success=true, 返回用户信息 |
| 正常注册-医保局 | name="医保局用户", password="123456", idCard="110101199001011235", role=3 | success=true, 返回用户信息 |
| 身份证已存在 | 使用已注册的身份证 | success=false, errorMsg="身份证号已注册" |
| 参数为空 | name=null | success=false, 参数校验失败 |
| 角色无效 | role=5 | success=false, errorMsg="无效的角色" |

---

### 1.2 用户登录

**接口**: `POST /user/login`

**请求体**:
```json
{
  "idCard": "110101199001011234",
  "password": "123456"
}
```

**测试案例**:

| 案例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 正常登录 | 正确的身份证和密码 | success=true, 返回token和用户信息 |
| 密码错误 | 错误的密码 | success=false, errorMsg="密码错误" |
| 用户不存在 | 未注册的身份证 | success=false, errorMsg="用户不存在" |
| 参数为空 | idCard=null | 参数校验失败 |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 123456789,
      "name": "张三",
      "idCard": "110101199001011234",
      "role": 1
    }
  }
}
```

---

### 1.3 用户登出

**接口**: `POST /user/loginout`

**请求头**:
```
token: {用户token}
```

**测试案例**:

| 案例 | 请求头 | 预期结果 |
|------|--------|----------|
| 正常登出 | 有效token | success=true |
| 无token | token=null | success=false, errorMsg="请先登录" |
| token已过期 | 过期的token | success=false, errorMsg="登录已过期" |

---

## 2. 医院模块

### 2.1 医院注册

**接口**: `POST /hospital/sign`

**请求体**:
```json
{
  "name": "北京协和医院",
  "password": "hospital123"
}
```

**测试案例**:

| 案例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 正常注册 | name="北京协和医院", password="hospital123" | success=true, 返回医院信息 |
| 医院名为空 | name=null | 参数校验失败 |
| 医院名重复 | 使用已存在的医院名 | success=false, errorMsg="医院名称已存在" |

---

### 2.2 获取医院列表

**接口**: `GET /hospital/list`

**权限**: 管理员、医保局

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 管理员查询 | success=true, 返回所有医院列表 |
| 医保局查询 | success=true, 返回所有医院列表 |
| 医院角色查询 | success=false, errorMsg="无权限访问" |
| 患者角色查询 | success=false, errorMsg="无权限访问" |

---

### 2.3 管理员选择医院

**接口**: `POST /hospital/select/{hospitalId}`

**权限**: 管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 管理员选择医院 | success=true, 后续操作以该医院身份进行 |
| 非管理员选择 | success=false, errorMsg="无权限访问" |
| 医院ID不存在 | success=false, errorMsg="医院不存在" |

---

## 3. 就诊模块

### 3.1 新增就诊

**接口**: `POST /visit/add`

**权限**: 医院、管理员

**请求体**:
```json
{
  "userId": 123456789,
  "type": 1,
  "diagnosis": "感冒"
}
```

**测试案例**:

| 案例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 正常新增 | userId=有效患者ID, type=1, diagnosis="感冒" | success=true, 返回就诊记录 |
| 患者不存在 | userId=不存在的ID | success=false, errorMsg="患者不存在" |
| 非医院角色 | 患者角色访问 | success=false, errorMsg="无权限访问" |
| 参数为空 | userId=null | 参数校验失败 |

---

### 3.2 患者查询个人就诊

**接口**: `GET /visit/my/list`

**权限**: 患者、管理员

**请求参数**:
```
pageNum=1&pageSize=10
```

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 患者查询自己 | success=true, 返回该患者的就诊列表 |
| 管理员查询 | success=true, 需先选择医院 |
| 医院角色查询 | success=false, errorMsg="无权限访问" |

---

### 3.3 医院查询就诊列表

**接口**: `GET /visit/hospital/list`

**权限**: 医院、管理员

**请求参数**:
```
pageNum=1&pageSize=10&patientName=张三
```

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 医院查询本院 | success=true, 返回本院就诊列表 |
| 按患者姓名筛选 | success=true, 返回匹配的就诊列表 |
| 患者角色查询 | success=false, errorMsg="无权限访问" |

---

## 4. 费用模块

### 4.1 批量添加费用

**接口**: `POST /fee/batch/add`

**权限**: 医院、管理员

**请求体**:
```json
{
  "visitId": 123456789,
  "fees": [
    {
      "name": "阿莫西林胶囊",
      "price": 15.50,
      "num": 2,
      "type": 1
    },
    {
      "name": "血常规检查",
      "price": 25.00,
      "num": 1,
      "type": 1
    }
  ]
}
```

**费用类型**:
- 1: 甲类（100%报销）
- 2: 乙类（80%报销）
- 3: 自费（0%报销）

**测试案例**:

| 案例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 正常添加 | 有效visitId, 多条费用 | success=true |
| 就诊记录不存在 | visitId=不存在的ID | success=false, errorMsg="就诊记录不存在" |
| 就诊已结算 | 已结算的就诊记录 | success=false, errorMsg="就诊已结算，无法添加费用" |
| 费用列表为空 | fees=[] | 参数校验失败 |

---

### 4.2 查询费用明细

**接口**: `GET /fee/listByVisitId?visitId=123`

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常查询 | success=true, 返回费用明细列表 |
| 就诊记录不存在 | success=false, errorMsg="就诊记录不存在" |

---

## 5. 结算模块

### 5.1 医保结算计算

**接口**: `POST /settle/calculate/{visitId}`

**权限**: 医院、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常结算 | success=true, 返回结算单（包含报销金额、自付金额） |
| 就诊记录不存在 | success=false, errorMsg="就诊记录不存在" |
| 已结算过 | success=false, errorMsg="该就诊已结算" |
| 无费用明细 | 就诊无费用记录 | success=false, errorMsg="无费用明细，无法结算" |
| 并发结算 | 同时发起多个结算请求 | 只有一个成功，其他返回"正在处理中" |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": 987654321,
    "visitId": 123456789,
    "total": 56.00,
    "reimburse": 56.00,
    "selfPay": 0.00,
    "status": 0
  }
}
```

---

### 5.2 查询结算详情

**接口**: `GET /settle/detail/{visitId}`

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常查询 | success=true, 返回结算详情+费用明细 |
| 结算单不存在 | success=false, errorMsg="结算单不存在" |
| 无权限查询他人 | 患者查询他人结算 | success=false, errorMsg="无权限查询" |

---

### 5.3 患者查询结算列表

**接口**: `GET /settle/my/list`

**权限**: 患者、管理员

**请求参数**:
```
pageNum=1&pageSize=10
```

---

### 5.4 医院查询结算列表

**接口**: `GET /settle/hospital/list`

**权限**: 医院、管理员

**请求参数**:
```
pageNum=1&pageSize=10&patientName=张三
```

---

## 6. 批次模块

### 6.1 创建批次

**接口**: `POST /batch/create/{hospitalId}`

**权限**: 医院、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常创建 | success=true, 返回批次信息（批次号、状态） |
| 医院只能创建本院批次 | hospitalId!=当前医院ID | success=false, errorMsg="只能为当前登录医院创建批次" |
| 并发创建 | 同时创建多个批次 | 成功创建多个，批次号不同 |

---

### 6.2 添加结算单到批次

**接口**: `POST /batch/add-settle/{batchId}/{settleId}`

**权限**: 医院、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常添加 | success=true |
| 批次不存在 | success=false, errorMsg="批次不存在" |
| 结算单不存在 | success=false, errorMsg="结算单不存在" |
| 结算单已添加到其他批次 | success=false, errorMsg="该结算单已添加到其他批次" |
| 批次状态不允许 | 批次已申报 | success=false, errorMsg="批次状态不允许添加结算单" |
| 重复添加 | 同一结算单添加两次 | success=false, errorMsg="该结算单已添加到批次" |

---

### 6.3 批次申报

**接口**: `POST /batch/declare/{batchId}`

**权限**: 医院、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常申报 | success=true, 批次状态变为"已申报" |
| 批次为空 | 批次无结算单 | success=false, errorMsg="批次中没有结算单，无法申报" |
| 状态不正确 | 批次非"待申报"状态 | success=false, errorMsg="只有待申报状态的批次才能申报" |

---

### 6.4 批次撤回

**接口**: `POST /batch/withdraw/{batchId}`

**权限**: 医院、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常撤回 | success=true, 批次状态变为"待申报" |
| 已拨付 | 批次已拨付 | success=false, errorMsg="该批次已拨付，无法撤回" |
| 状态不正确 | 批次非"已申报"状态 | success=false, errorMsg="只有已申报且医保局未处理的批次才能撤回" |

---

### 6.5 医保局查询批次列表

**接口**: `GET /batch/medical/list`

**权限**: 医保局、管理员

**请求参数**:
```
pageNum=1&pageSize=10
```

---

### 6.6 医院查询批次列表

**接口**: `GET /batch/hospital/list`

**权限**: 医院、管理员

---

## 7. 拨付模块

### 7.1 批次拨付

**接口**: `POST /pay/pay-batch/{batchId}`

**权限**: 医保局、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常拨付 | success=true, 批次状态变为"已完成" |
| 批次状态不正确 | 非已申报状态 | success=false, errorMsg="仅已申报状态的批次可拨付" |
| 重复拨付 | 已拨付的批次 | success=false, errorMsg="该批次已拨付" |
| 批次金额为0 | 批次无结算单或金额为0 | success=false, errorMsg="批次金额必须大于0" |

---

### 7.2 拒绝拨付

**接口**: `POST /pay/reject-batch/{batchId}`

**权限**: 医保局、管理员

**请求体**:
```json
{
  "reason": "材料不齐全，请补充"
}
```

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常拒绝 | success=true, 批次状态变为"拨付拒绝" |
| 理由为空 | reason="" | success=false, errorMsg="拒绝理由不能为空" |
| 已拨付 | 批次已拨付 | success=false, errorMsg="该批次已拨付，无法拒绝" |

---

## 8. 账户模块

### 8.1 获取账户信息

**接口**: `GET /account/get`

**权限**: 患者、管理员

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常查询 | success=true, 返回余额、总充值、总消费 |
| 患者无账户 | 新患者 | 自动创建账户，余额为0 |

---

### 8.2 账户充值

**接口**: `POST /account/recharge`

**权限**: 患者、管理员

**请求体**:
```json
{
  "amount": 100.00,
  "type": 1
}
```

**充值类型**:
- 1: 微信
- 2: 支付宝
- 3: 银行卡
- 4: 现金

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常充值 | success=true, 余额增加 |
| 金额为0或负数 | amount<=0 | success=false, errorMsg="充值金额必须大于0" |
| 并发充值 | 同时多次充值 | 余额正确累加，无数据丢失 |

---

### 8.3 账户支付

**接口**: `POST /account/pay`

**权限**: 患者、管理员

**请求体**:
```json
{
  "visitId": 123456789
}
```

**测试案例**:

| 案例 | 预期结果 |
|------|----------|
| 正常支付 | success=true, 扣除自付金额，结算状态变为"已支付" |
| 余额不足 | 余额<自付金额 | success=false, errorMsg="余额不足，请先充值" |
| 已支付过 | 重复支付 | success=false, errorMsg="该就诊已支付" |
| 未结算 | 就诊未结算 | success=false, errorMsg="请先结算" |

---

### 8.4 充值记录列表

**接口**: `GET /account/recharge/list`

**权限**: 患者、管理员

---

### 8.5 消费记录列表

**接口**: `GET /account/consumption/list`

**权限**: 患者、管理员

---

## 9. 异常场景测试

### 9.1 登录过期

**场景**: Token过期或无效

**预期响应**:
```json
{
  "success": false,
  "code": 401,
  "errorMsg": "登录已过期，请重新登录"
}
```

**前端行为**: 自动跳转到登录页

---

### 9.2 权限不足

**场景**: 角色不匹配

**预期响应**:
```json
{
  "success": false,
  "errorMsg": "无权限访问"
}
```

---

### 9.3 并发操作

**场景**: 同一用户同时发起多个相同操作

**预期**: 只有一个成功，其他返回"操作正在进行中"

**三级防护**:
1. Redis 预校验拦截：幂等标记已存在则直接拒绝
2. Redisson 锁竞争失败：tryLock 获取不到锁返回提示
3. 数据库唯一索引兜底：极端情况插入冲突

**涉及接口**:
- 结算计算（`lock:settle:{visitId}` + 幂等标记）
- 批次申报（`lock:batch:declare:{batchId}` + 幂等标记）
- 拨付操作（`lock:pay:batch:{batchId}` + 幂等标记）
- 充值操作（`lock:account:{userId}`）

---

### 9.4 参数校验失败

**场景**: 必填参数为空或格式错误

**预期响应**:
```json
{
  "success": false,
  "errorMsg": "参数校验失败: xxx不能为空"
}
```

---

## 10. 完整业务流程测试

### 10.1 患者就诊完整流程

```
1. 患者注册 → POST /user/sign
2. 患者登录 → POST /user/login
3. 医院创建就诊 → POST /visit/add
4. 医院添加费用 → POST /fee/batch/add
5. 医院结算 → POST /settle/calculate/{visitId}
6. 患者充值 → POST /account/recharge
7. 患者支付 → POST /account/pay
```

### 10.2 批次申报拨付流程

```
1. 医院创建批次 → POST /batch/create/{hospitalId}
2. 医院添加结算单 → POST /batch/add-settle/{batchId}/{settleId}
3. 医院申报批次 → POST /batch/declare/{batchId}
4. 医保局拨付 → POST /pay/pay-batch/{batchId}
```

### 10.3 批次拒绝重新申报流程

```
1. 医院创建批次并申报
2. 医保局拒绝 → POST /pay/reject-batch/{batchId}
3. 医院查看拒绝原因 → GET /batch/detail/{batchId}
4. 医院修改后重新申报
```

---

## 11. 性能测试要点

### 11.1 并发测试

| 接口 | 并发数 | 锁Key | 预期结果 |
|------|--------|-------|----------|
| 结算计算 | 10 | `lock:settle:{visitId}` | 只有一个成功，其余返回"正在处理中" |
| 充值 | 10 | `lock:account:{userId}` | 余额正确累加，无数据丢失 |
| 批次创建 | 10 | `lock:batch:create:{hospitalId}` | 全部成功，批次号各有不同 |

### 11.2 压力测试

| 接口 | QPS | 响应时间 |
|------|-----|----------|
| 登录 | 100 | <500ms |
| 列表查询 | 200 | <200ms |
| 详情查询 | 300 | <100ms |

---

## 12. 测试工具推荐

- **Postman**: 接口测试
- **JMeter**: 压力测试
- **Apifox**: 接口文档+测试
