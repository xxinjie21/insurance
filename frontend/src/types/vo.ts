export interface HospitalVO {
  id: string
  name: string
  address?: string
  phone?: string
  status?: number
  /** 医院等级：1-三甲 2-三乙 3-二甲 4-二乙 5-一级 6-社区 */
  level?: number
  /** 定点协议有效期 */
  agreementExpire?: string
  createTime?: string
}

export interface UserVO {
  id: string
  name: string
  idCard?: string
  role: number
  hospitalId?: string
  createTime?: string
}

export interface VisitVO {
  id: string
  userId: string
  hospitalId: string
  type: number
  /** 就诊科室 */
  dept?: string
  /** 接诊医生 */
  doctorName?: string
  diagnosis: string
  status?: number
  hospitalName?: string
  userName?: string
  patientIdCard?: string
  idCard?: string
  createTime?: string
  updateTime?: string
}

export interface FeeVO {
  id: string
  visitId: string
  name: string
  type: number
  /** 医保项目编码 */
  insuranceCode?: string
  /** 药品规格 */
  specification?: string
  /** 用法用量 */
  usageMethod?: string
  price: number
  num: number
  total: number
  hospitalName?: string
  createTime?: string
}

export interface SettleVO {
  id: string
  visitId: string
  hospitalId: string
  total: number
  reimburse: number
  selfPay: number
  /** 统筹支付金额 */
  poolingPay?: number
  /** 个人账户支付金额 */
  accountPay?: number
  /** 个人现金支付金额 */
  cashPay?: number
  status: number
  hospitalName?: string
  createTime?: string
  patientName?: string
  patientIdCard?: string
  idCard?: string
  visitType?: number
  diagnosis?: string
}

export interface BatchItemVO {
  id: string
  batchId: string
  settleId: string
  audit: number
  patientName?: string
  patientIdCard?: string
  idCard?: string
  settleTotal?: number
  settleReimburse?: number
  settleSelfPay?: number
  settleStatus?: number
  batchNo?: string
  batchStatus?: number
  batchCreateTime?: string
  createTime?: string
}

export interface BatchVO {
  id: string
  hospitalId: string
  hospitalName?: string
  batchNo: string
  settleCnt: number
  totalAmt: number
  status: number
  createTime?: string
  batchItems?: BatchItemVO[]
  rejectReason?: string
}

export interface PayVO {
  id: string
  batchId: string
  hospitalId: string
  amount: number
  status: number
  payTime?: string
  createTime?: string
  rejectReason?: string
}

export interface RechargeRecordVO {
  id: string
  userId: string
  orderNo: string
  amount: number
  type: number
  status: number
  payTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ConsumptionRecordVO {
  id: string
  userId: string
  visitId?: string
  settleId?: string
  orderNo: string
  amount: number
  type: number
  status: number
  balanceBefore: number
  balanceAfter: number
  remark?: string
  createTime?: string
}

export interface UserLoginVO {
  userId: string
  name: string
  role: number
  hospitalId?: string
  token: string
  hospitalName?: string
}

export interface UserRegisterVO {
  id: string
  name: string
  idCard?: string
  role: number
  hospitalId?: string
  createTime?: string
}

export interface UserAccountVO {
  id: string
  userId: string
  userName: string
  balance: number
  totalRecharge: number
  totalConsumption: number
  status: number
  createTime?: string
  updateTime?: string
}

// 药品目录
export interface DrugCatalogVO {
  id: string
  code: string
  name: string
  specification?: string
  manufacturer?: string
  category: number
  selfPayRatio?: number
  remark?: string
}

// 诊疗目录
export interface TreatmentCatalogVO {
  id: string
  code: string
  name: string
  projectType?: string
  category: number
  unitPriceCap?: number
  remark?: string
}

// 耗材目录
export interface ConsumableCatalogVO {
  id: string
  code: string
  name: string
  specification?: string
  category: number
  limitAmount?: number
  remark?: string
}

// 费用录入表单（含目录选择）
export interface FeeAddForm {
  visitId: string
  name?: string
  type?: number
  price: number
  num: number
  insuranceCode?: string
  specification?: string
  usageMethod?: string
  /** 目录类型：drug/treatment/consumable */
  catalogType?: string
  /** 目录项ID */
  catalogId?: string
}
