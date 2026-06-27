// 用户角色枚举
export enum Role {
  PATIENT = 1, // 患者
  HOSPITAL = 2, // 医院
  MEDICAL = 3, // 医保局
  ADMIN = 4, // 管理员
}

// 就诊类型
export enum VisitType {
  OUTPATIENT = 1, // 门诊
  INPATIENT = 2, // 住院
}

// 就诊状态
export enum VisitStatus {
  UNSETTLED = 0, // 未结算
  SETTLED = 1, // 已结算
}

// 费用类型
export enum FeeType {
  CATEGORY_A = 1, // 甲类
  CATEGORY_B = 2, // 乙类
  SELF_PAY = 3, // 自费
}

// 批次状态
export enum BatchStatus {
  PENDING = 0, // 待申报
  DECLARED = 1, // 已申报
  COMPLETED = 2, // 已完成
}

// 拨付状态
export enum PayStatus {
  UNPAID = 0, // 未拨付
  PAID = 1, // 已拨付
}

// 充值类型
export enum RechargeType {
  WECHAT = 1, // 微信
  ALIPAY = 2, // 支付宝
  BANK_CARD = 3, // 银行卡
  CASH = 4, // 现金
}

// 账户状态
export enum AccountStatus {
  FROZEN = 0, // 冻结
  NORMAL = 1, // 正常
}

export interface PageResult<T> {
  records: T[]
  total: number
  size?: number
  current?: number
  pages?: number
}

export * from './vo'
