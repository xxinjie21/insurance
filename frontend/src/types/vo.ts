export interface HospitalVO {
  id: string
  name: string
  address?: string
  phone?: string
  status?: number
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
