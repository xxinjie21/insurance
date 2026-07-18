# 改造进度记录

## 模块状态

| 模块 | 状态 | 开始时间 | 完成时间 |
|------|------|---------|---------|
| 模块一：基础数据完善 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块二：报销计算引擎重构 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
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

---

### 模块二：报销计算引擎重构 ✅ 2026-07-18

**改动摘要**：报销计算从固定三档比例改为规则引擎驱动，引入起付线/封顶线/乙类先自付/医院等级差异/参保类型差异/年度累计。

**新报销公式**：
```
1. 总费用 = sum(所有费用)
2. 乙类先自付 = sum(乙类费用 × category_b_self_ratio)
3. 可报销基数 = 总费用 - 乙类先自付 - 自费类费用
4. 剩余起付线 = max(0, 规则的起付线 - 年度已累计起付线)
5. 实际可报销 = max(0, 可报销基数 - 剩余起付线)
6. 统筹支付 = min(实际可报销 × 报销比例, 年度封顶线剩余)
7. 自付总额 = 总费用 - 统筹支付
8. 个账支付 = min(自付总额, 个人账户余额)
9. 现金支付 = 自付总额 - 个账支付
```

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| DDL | `insurance/docs/int.sql` | 新增 `reimburse_rule` 表 + 24条种子数据（职工/居民×6等级×门诊/住院）；新增 `year_accumulate` 表(uk: user_id+year) |
| PO | `domain/po/ReimburseRule.java` | **新增**：insuranceType/hospitalLevel/visitType/deductible/reimburseRatio/annualCap/categoryBSelfRatio |
| PO | `domain/po/YearAccumulate.java` | **新增**：userId/year/deductibleUsed/poolingTotal |
| Mapper | `mapper/ReimburseRuleMapper.java` | **新增** |
| Mapper | `mapper/YearAccumulateMapper.java` | **新增** |
| Service | `service/IReimburseRuleService.java` | **新增**接口：findRule(insuranceType, hospitalLevel, visitType) |
| Service | `service/impl/ReimburseRuleServiceImpl.java` | **新增**实现：精确匹配 → 同参保类型回退最近等级 |
| Service | `service/impl/SettleServiceImpl.java` | 注入IReimburseRuleService+YearAccumulateMapper；executeCalculateWithTransaction() 重写为规则引擎计算；新增 getOrCreateYearAccumulate()；移除旧的 getReimburseRate() |

**技术点落实**：
- 规则表驱动：报销参数由数据库规则表控制，不硬编码；无匹配规则时自动回退到同保险类型+同就诊类型的最近等级
- 年度累计：year_accumulate 表(user_id+year唯一索引)追踪起付线和统筹支付，每次结算实时更新
- BeanUtils.copyProperties：SettleVO 新增的 poolingPay/accountPay/cashPay 自动从 PO 复制
- 三层幂等/分布式锁/编程式事务均保持原有体系不变
- Settle.reimburse 字段语义变更为"统筹支付金额(=poolingPay)"，batch 汇总时统计逻辑自然正确

**遗漏风险**：
- 前端未暴露 reimburse_rule 管理界面，规则变更需直接操作数据库（后续模块可按需加）
- UserAccountServiceImpl.pay() 仍从自建账户扣款，模块四/五会改造为使用 user.personal_account_balance
