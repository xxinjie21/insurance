# 改造进度记录

## 模块状态

| 模块 | 状态 | 开始时间 | 完成时间 |
|------|------|---------|---------|
| 模块一：基础数据完善 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块二：报销计算引擎重构 | ⬜ 待开始 | - | - |
| 模块三：医保目录体系 | ⬜ 待开始 | - | - |
| 模块四：结算单支付拆分 | ⬜ 待开始 | - | - |
| 模块五：支付流程改造 | ⬜ 待开始 | - | - |
| 模块六：门诊/住院流程拓展 | ⬜ 待开始 | - | - |
| 模块七：审核规则引擎 | ⬜ 待开始 | - | - |
| 模块八：年度管理 | ⬜ 待开始 | - | - |
| 模块九：异地就医 | ⬜ 待开始 | - | - |
| 模块十：多层次保障 | ⬜ 待开始 | - | - |
| 模块十一：退费体系 | ⬜ 待开始 | - | - |
| 模块十二：医生角色与处方 | ⬜ 待开始 | - | - |
| 模块十三：报表与导出 | ⬜ 待开始 | - | - |
| 模块十四：数据安全与审计 | ⬜ 待开始 | - | - |
| 模块十五：RabbitMQ 异步消息基础设施 | ⬜ 待开始 | - | - |

状态：⬜ 待开始 | 🔄 进行中 | ✅ 已完成 | ❌ 已跳过

---

## 改造记录

### 模块一：基础数据完善 ✅ 2026-07-18

**改动摘要**：5张核心表各新增业务字段，补齐现实医保业务所需的基础数据维度。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| DDL | `insurance/docs/int.sql` | hospital +level +agreement_expire；user +insurance_type +insurance_no +insurance_city +personal_account_balance；visit +dept +doctor_name；settle +pooling_pay +account_pay +cash_pay；fee +insurance_code +specification +usage_method；同步更新所有测试数据 |
| PO | `domain/po/Hospital.java` | +level(Integer) +agreementExpire(LocalDate) |
| PO | `domain/po/User.java` | +insuranceType +insuranceNo +insuranceCity +personalAccountBalance |
| PO | `domain/po/Visit.java` | +dept +doctorName |
| PO | `domain/po/Settle.java` | +poolingPay +accountPay +cashPay |
| PO | `domain/po/Fee.java` | +insuranceCode +specification +usageMethod |
| VO | `domain/vo/SettleVO.java` | +poolingPay +accountPay +cashPay |
| VO | `domain/vo/VisitVO.java` | +dept +doctorName |
| VO | `domain/vo/FeeVO.java` | +insuranceCode +specification +usageMethod |
| DTO | `domain/dto/VisitAddDTO.java` | +dept +doctorName |
| DTO | `domain/dto/FeeAddDTO.java` | +insuranceCode +specification +usageMethod |
| 前端 | `frontend/src/types/vo.ts` | HospitalVO +level +agreementExpire；VisitVO +dept +doctorName；FeeVO +insuranceCode +specification +usageMethod；SettleVO +poolingPay +accountPay +cashPay |
| 前端 | `frontend/src/types/index.ts` | 新增InsuranceType枚举、HospitalLevel枚举 |

**技术点落实**：
- BeanUtils.copyProperties 自动复制 DTO→PO 同名字段，FeeServiceImpl/VisitServiceImpl 无需额外修改
- 所有新增字段均为可选(DEFAULT NULL / `?` 可选)，向后兼容存量数据
- 全链路联动：DDL → PO → VO → DTO → 前端TS类型，6层同步

**遗漏风险**：无。新增字段均为可选，不影响现有API响应结构。
