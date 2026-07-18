# 改造进度记录

## 模块状态

| 模块 | 状态 | 开始时间 | 完成时间 |
|------|------|---------|---------|
| 模块一：基础数据完善 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块二：报销计算引擎重构 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块三：医保目录体系 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块四：结算单支付拆分 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块五：支付流程改造 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块六：门诊/住院流程拓展 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块七：审核规则引擎 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
| 模块八：年度管理 | ✅ 已完成 | 2026-07-18 | 2026-07-18 |
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

---

### 模块三：医保目录体系 ✅ 2026-07-18

**改动摘要**：引入药品/诊疗/耗材三大标准目录，费用录入从自由文本→标准目录选择+自动回填。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| DDL | `insurance/docs/int.sql` | 新增 `drug_catalog` 表(12条种子数据) + `treatment_catalog` 表(16条) + `consumable_catalog` 表(6条) |
| PO | `domain/po/DrugCatalog.java` | **新增**：code/name/specification/manufacturer/category/selfPayRatio/remark |
| PO | `domain/po/TreatmentCatalog.java` | **新增**：code/name/projectType/category/unitPriceCap/remark |
| PO | `domain/po/ConsumableCatalog.java` | **新增**：code/name/specification/category/limitAmount/remark |
| Mapper | `mapper/DrugCatalogMapper.java` | **新增** |
| Mapper | `mapper/TreatmentCatalogMapper.java` | **新增** |
| Mapper | `mapper/ConsumableCatalogMapper.java` | **新增** |
| Service | `service/ICatalogService.java` | **新增**：drugList/treatmentList/consumableList(分页+模糊搜索) + getById |
| Service | `service/impl/CatalogServiceImpl.java` | **新增**：按name/code模糊搜索，默认20条/页 |
| Controller | `controller/CatalogController.java` | **新增** 3个GET接口：/catalog/{drug\|treatment\|consumable}/list?keyword= |
| DTO | `domain/dto/FeeAddDTO.java` | +catalogType(String) +catalogId(Long) 可选字段 |
| Service | `service/impl/FeeServiceImpl.java` | 注入ICatalogService；executeBatchAddWithTransaction 新增 fillFromCatalog()：选目录时根据catalogType查询→自动回填name/type/insuranceCode/specification |
| 前端 | `frontend/src/types/vo.ts` | 新增 DrugCatalogVO/TreatmentCatalogVO/ConsumableCatalogVO/FeeAddForm 类型 |

**技术点落实**：
- 目录查询：ICatalogService 按编码/名称/规格三字段模糊搜索，支持分页
- 目录回填：FeeAddDTO.catalogId + catalogType("drug"\"treatment"\"consumable") → FeeServiceImpl.fillFromCatalog() 自动查目录回填 name/type/insuranceCode/specification；查询失败不阻塞，fallback 使用手动录入值
- 兼容模式：不传 catalogId 时保持原有自由文本录入流程，catalogId 为可选字段
- BeanUtils.copyProperties：FeeAddDTO→Fee PO 自动携带回填后的字段
- @Permission 注解：目录查询接口限医院/医保局/管理员角色访问

**遗漏风险**：
- Excel/CSV批量导入接口暂未实现（目录种子数据已覆盖常用项目，按需再加）
- 目录管理CRUD（增删改）未暴露前端接口，种子数据变更需直接操作数据库

---

### 模块四：结算单支付拆分 ✅ 2026-07-18

**改动摘要**：结算单详情增加逐项费用拆分（对标真实医保结算单格式），支付扣款改为从 user.personal_account_balance 扣除。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| VO | `domain/vo/FeeDetailVO.java` | **新增**：id/name/insuranceCode/specification/num/price/total/type/reimburse/selfPay |
| VO | `domain/vo/SettleVO.java` | +feeDetails(List\<FeeDetailVO\>)：查询详情时填充费用明细列表 |
| Service | `SettleServiceImpl.java` | getSettleDetail 返回时调用 buildFeeDetails()；新增 buildFeeDetails() 方法：费用按类型拆分，甲/乙类按金额比例分配统筹支付，自费类统筹=0 |
| Service | `UserAccountServiceImpl.java` | executePayWithTransaction 重写：扣款来源从 user_account 改为 user.personal_account_balance；同步更新 user_account 保持兼容 |
| 前端 | `frontend/src/types/vo.ts` | 新增 FeeDetailVO 接口；SettleVO +feeDetails |

**技术点落实**：
- 费用拆分算法：甲/乙类总额比例 × 统筹支付总额 → 各项报销金额；自费项统筹=0
- 个人账户扣款：deductAmt = min(payAmount, personalAccountBalance)；不足部分为现金自付
- 支付幂等保持：仍使用消费记录表的 user+visit+type+status 四字段判重
- 三层幂等/分布式锁/编程式事务体系不变
- BeanUtils.copyProperties：SettleVO.poolingPay/accountPay/cashPay 自动从 PO 复制

**遗漏风险**：
- 列表查询不返回 feeDetails（仅详情接口返回），避免接口过重
- user_account 表仍作为兼容层同步更新，模块五会彻底改造

---

### 模块五：支付流程改造 ✅ 2026-07-18

**改动摘要**："手动充值→支付"模式改为医保个人账户直接支付；患者注册时职工医保自动初始化个账余额；充值改为管理员专属操作。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| Controller | `UserAccountController.java` | recharge 接口加 @Permission({Role.ADMIN})，患者端不再暴露充值入口 |
| Service | `UserAccountServiceImpl.java` | executeRechargeWithTransaction 重写：直接操作 user.personal_account_balance，不再依赖 user_account 做主扣款；user_account 仅作兼容同步 |
| DTO | `UserRegisterDTO.java` | +insuranceType(Integer) +insuranceCity(String) 可选字段 |
| Service | `UserServiceImpl.java` | sign() 增加参保信息处理：职工(insuranceType=1)自动初始化 personalAccountBalance=5000.00；居民=0.00 |
| Constants | `AccountConstants.java` | 新增 PERSONAL_ACCOUNT_INIT_BALANCE(5000.00) + FLOW_TYPE_ACCOUNT_IN/VISIT_PAY/REFUND 流水类型 |
| 前端 | `frontend/src/types/vo.ts` | UserRegisterVO +insuranceType +insuranceCity |

**技术点落实**：
- 充值语义变更：从"用户自己充钱"改为"管理员模拟个人账户划入"，操作的是 user.personal_account_balance
- 注册初始化：职工医保参保人注册时自动获得初始个人账户余额（模拟账户按月划入）
- 兼容层保持：user_account 表仍同步更新，旧的充值/消费记录表保留为"流水记录"
- 个人账户余额在主表(user)中，结算和支付直接读写，不再经过中间账户表
- UserServiceImpl 对 insuranceType 为 null 的注册（非患者角色）不做额外处理，向后兼容

**遗漏风险**：
- 旧的用户如果 user_account 有余额但 user.personal_account_balance 为0，需要数据迁移脚本（按需）
- 前端注册表单需增加参保类型选择（UI改动不在后端范围内）

---

### 模块六：门诊/住院流程拓展 ✅ 2026-07-18

**改动摘要**：新增挂号环节（分诊→付费）、住院入院→押金→出院结算完整链路、fee_date支持每日清单、出院自动调用结算引擎。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| DDL | `int.sql` | 新增 `registration` 表(挂号)；新增 `inpatient` 表(住院)；新增 `inpatient_deposit` 表(押金)；fee ALTER ADD fee_date |
| PO | `Registration.java` | **新增** |
| PO | `Inpatient.java` | **新增** |
| PO | `InpatientDeposit.java` | **新增** |
| PO | `Fee.java` | +feeDate(LocalDate) |
| Mapper | 3个Mapper | **新增** |
| Service | `IRegistrationService.java` + Impl | **新增**：挂号(身份证查患者→校验医院→挂号费15/30) / myList / hospitalList |
| Service | `IInpatientService.java` + Impl | **新增**：admit(校验住院类型→生成住院号) / deposit(押金流水) / discharge(汇总费用→调settleService.calculate()→更新状态) / 列表查询 |
| Controller | `RegistrationController.java` | **新增**：POST /registration/add / GET /registration/my/list / GET /registration/hospital/list |
| Controller | `InpatientController.java` | **新增**：POST /inpatient/admit / POST /inpatient/deposit / POST /inpatient/discharge/{id} / GET /inpatient/{hospital\|my}/list |
| VO | `FeeVO.java` | +feeDate |
| 前端 | `vo.ts` | 新增 RegistrationVO / InpatientVO；FeeVO +feeDate |

**技术点落实**：
- 挂号费固定：普通门诊15元 专家门诊30元，按甲类参与后续报销计算
- 入院校验：visit.type必须为2(住院)，同一visit不可重复入院(uk_visit_id)
- 出院结算：事务内调用 settleService.calculate() 复用完整报销引擎(规则/起付线/封顶线/年度累计)
- 押金管理：inpatient_deposit 表记录每笔押金流水，inpatient.deposit_total 实时汇总
- 住院号：IP+yyyyMMdd+4位随机数，唯一索引防重
- 分布式锁/幂等体系由 settleService.calculate() 内部保障

**遗漏风险**：
- 挂号后创建visit的流程由前端串联（先挂号→再创建就诊），后端未做自动关联
- 出院退押金（deposit_total > total_fee时退差额）未实现计算逻辑
- 住院费用未自动回填inpatient.total_fee字段

---

### 模块七：审核规则引擎 ✅ 2026-07-18

**改动摘要**：新增规则驱动的智能审核引擎，支持诊断-药品匹配/重复用药/年龄限制/性别限制四类规则；拨付前自动审核并逐单调减金额。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| DDL | `int.sql` | 新增 `audit_rule` 表(8条种子数据)；`batch_item` ALTER ADD adjust_amount |
| PO | `AuditRule.java` | **新增**：ruleType/description/paramKey/paramValue/severity/enabled |
| PO | `BatchItem.java` | +adjustAmount(BigDecimal) |
| Mapper | `AuditRuleMapper.java` | **新增** |
| Service | `IAuditService.java` + Impl | **新增**：auditSettle(settle) 加载启用规则→逐规则检查→返回问题列表(含建议调减金额) |
| Service | `PayServiceImpl.java` | 注入IAuditService；executePayBatchWithTransaction 新增审核步骤：逐结算单审核→severity=2的累计suggestDeductAmount→更新BatchItem.audit=1+adjustAmount |
| VO | `BatchItemVO.java` | +adjustAmount |
| Service | `BatchServiceImpl.java` | getBatchDetail 同步填充 adjustAmount |
| Controller | `AuditController.java` | **新增**：GET /audit/settle/{settleId} 医保局审核单笔结算单 |
| 前端 | `vo.ts` | BatchItemVO +adjustAmount |

**规则类型实现**：
- DIAG_DRUG_MATCH：诊断含paramKey时，费用必须在允许列表内，否则预警+建议调减50%
- DUPLICATE_DRUG：paramKey和paramValue代表的两类药同时出现→预警
- AGE_RESTRICT：费用含受限药品时预警
- SEX_RESTRICT：费用含受限制药品时预警

**技术点落实**：
- 规则驱动：审核逻辑全由 audit_rule 表配置，新增规则只需插数据不改代码
- 逐单审核：每个BatchItem独立调用auditSettle，调减金额记录在adjustAmount，audit=1标记扣款
- 拨付前审核：审核步骤在Pay记录创建之前执行，调减结果写入BatchItem后跟随批次一起拨付
- 审核结果含feeId/feeName/suggestDeductAmount，前端可展示逐项审核详情
- @Permission 注解：审核接口限医保局/管理员角色

**遗漏风险**：
- review_rule 管理CRUD未暴露接口，规则通过SQL维护
- 审核结果未写入单独的 audit_result 表（仅记录在BatchItem上）

---

### 模块八：年度管理 ✅ 2026-07-18

**改动摘要**：年度结转（起付线/封顶线归零 + 个账计息1.5%）、年度对账报表（按医院聚合统筹支付）。

**改动文件清单**：

| 层级 | 文件 | 改动 |
|------|------|------|
| Service | `IYearEndService.java` + Impl | **新增**：rollover()事务内清year_accumulate+个账计息；reconcileReport()按年/医院聚合已拨付结算 |
| Controller | `YearEndController.java` | **新增**：POST /year-end/rollover + GET /year-end/reconcile?year= |

**技术点落实**：
- 年度结转：事务内更新 year_accumulate（deductibleUsed=0, poolingTotal=0, year+1）；职工医保用户 personal_account_balance += balance × 1.5%
- 对账报表：按年份筛选已拨付结算，按医院聚合统筹支付总额 + 批次统计
- @Permission：限医保局/管理员

**遗漏风险**：年度结转需手动触发（未做定时Job），生产环境需改成 `@Scheduled(cron="0 0 0 1 1 ?")`
