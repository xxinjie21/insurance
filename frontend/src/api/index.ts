// API 基础路径（已废弃，直接使用完整 URL）
export const API_BASE_URL = ''

// 用户相关 API
export const USER_API = {
  LOGIN: '/user/login',
  REGISTER: '/user/sign',
  LOGOUT: '/user/loginout',
  SEARCH: '/user/search',
}

// 医院相关 API
export const HOSPITAL_API = {
  SIGN: '/hospital/sign',
  LIST: '/hospital/list',
  PATIENT_LIST: '/hospital/patient/list',
  APPROVE: (hospitalId: string | number) => `/hospital/approve/${hospitalId}`,
  REJECT: (hospitalId: string | number) => `/hospital/reject/${hospitalId}`,
  ENABLE: (hospitalId: string | number) => `/hospital/enable/${hospitalId}`,
  DISABLE: (hospitalId: string | number) => `/hospital/disable/${hospitalId}`,
  SELECT: (hospitalId: string | number) => `/hospital/select/${hospitalId}`,
  UNSELECT: '/hospital/unselect',
  SELECTED: '/hospital/selected',
}

// 就诊相关 API
export const VISIT_API = {
  ADD: '/visit/add',
  MY_LIST: '/visit/my/list',
  HOSPITAL_LIST: '/visit/hospital/list',
  DELETE: (visitId: string | number) => `/visit/${visitId}`,
  DETAIL: (visitId: string | number) => `/visit/${visitId}`,
}

// 费用相关 API
export const FEE_API = {
  BATCH_ADD: '/fee/batch/add',
  LIST_BY_VISIT: '/fee/listByVisitId',
  DELETE: (feeId: string | number) => `/fee/${feeId}`,
  MY_LIST: '/fee/my/list',
}

// 首页仪表盘 API
export const DASHBOARD_API = {
  STATS: '/dashboard/stats',
}

// 结算相关 API
export const SETTLE_API = {
  CALCULATE: (visitId: string | number) => `/settle/calculate/${visitId}`,
  DETAIL: (visitId: string | number) => `/settle/detail/${visitId}`,
  MY_LIST: '/settle/my/list',  // 患者查询自己的结算单
  HOSPITAL_LIST: '/settle/hospital/list',  // 医院查询本院结算单
  AVAILABLE_FOR_BATCH: '/settle/available-for-batch',  // 查询可添加到批次的结算单
}

// 批次相关 API
export const BATCH_API = {
  CREATE: '/batch/create',  // hospitalId 从 Token 获取
  ADD_SETTLE: (batchId: string | number, settleId: string | number) => `/batch/add-settle/${batchId}/${settleId}`,
  DETAIL: (batchId: string | number) => `/batch/detail/${batchId}`,
  HOSPITAL_LIST: '/batch/hospital/list',  // 医院查询本院批次
  MEDICAL_LIST: '/batch/medical/list',  // 医保局查询所有批次
  DECLARE: (batchId: string | number) => `/batch/declare/${batchId}`,  // 申报批次
  WITHDRAW: (batchId: string | number) => `/batch/withdraw/${batchId}`,  // 撤回申报
  DELETE: (batchId: string | number) => `/batch/${batchId}`,  // 删除未申报批次
  PENDING_LIST: '/batch/pending-list',  // 查询待申报批次列表（用于结算单加入批次）
}

// 申报明细相关 API
export const BATCH_ITEM_API = {
  BATCH_PAGE: (batchId: string | number) => `/batch-item/batch/${batchId}/page`,
  BY_SETTLE: (settleId: string | number) => `/batch-item/by-settle/${settleId}`,
  MEDICAL_LIST: '/batch-item/medical/list',
  HOSPITAL_LIST: '/batch-item/hospital/list',
}

// 拨付相关 API
export const PAY_API = {
  PAY_BATCH: (batchId: string | number) => `/pay/pay-batch/${batchId}`,
  BY_BATCH: (batchId: string | number) => `/pay/by-batch/${batchId}`,
  REJECT_BATCH: (batchId: string | number) => `/pay/reject-batch/${batchId}`,
}

// 账户相关 API
export const ACCOUNT_API = {
  GET: '/account/get',
  RECHARGE: '/account/recharge',
  PAY: '/account/pay',
  RECHARGE_LIST: '/account/recharge/list',
  CONSUMPTION_LIST: '/account/consumption/list',
}

// 目录相关 API
export const CATALOG_API = {
  DRUG_LIST: '/catalog/drug/list',
  TREATMENT_LIST: '/catalog/treatment/list',
  CONSUMABLE_LIST: '/catalog/consumable/list',
}

// 挂号相关 API
export const REGISTRATION_API = {
  ADD: '/registration/add',
  MY_LIST: '/registration/my/list',
  HOSPITAL_LIST: '/registration/hospital/list',
}

// 住院相关 API
export const INPATIENT_API = {
  ADMIT: '/inpatient/admit',
  DEPOSIT: '/inpatient/deposit',
  DISCHARGE: (inpatientId: string | number) => `/inpatient/discharge/${inpatientId}`,
  HOSPITAL_LIST: '/inpatient/hospital/list',
  MY_LIST: '/inpatient/my/list',
}

// 审核相关 API
export const AUDIT_API = {
  SETTLE: (settleId: string | number) => `/audit/settle/${settleId}`,
}

// 医生相关 API
export const DOCTOR_API = {
  LIST: '/doctor/list',
}

// 处方相关 API
export const PRESCRIPTION_API = {
  PRESCRIBE: '/prescription/prescribe',
  APPROVE: (id: string | number) => `/prescription/approve/${id}`,
  REJECT: (id: string | number) => `/prescription/reject/${id}`,
  LIST: (visitId: string | number) => `/prescription/list/${visitId}`,
}

// 异地就医相关 API
export const REMOTE_FILING_API = {
  FILE: '/remote-filing/file',
  CANCEL: (id: string | number) => `/remote-filing/cancel/${id}`,
  MY: '/remote-filing/my',
}

// 退款相关 API
export const REFUND_API = {
  APPLY: '/refund/apply',
  APPROVE: (id: string | number) => `/refund/approve/${id}`,
  REJECT: (id: string | number) => `/refund/reject/${id}`,
  LIST: (settleId: string | number) => `/refund/list/${settleId}`,
}

// 报表相关 API
export const REPORT_API = {
  FUND: '/report/fund',
  FEE_COMPOSITION: '/report/fee-composition',
  VISIT_STATS: '/report/visit-stats',
}

// 年度管理 API
export const YEAR_END_API = {
  ROLLOVER: '/year-end/rollover',
  RECONCILE: '/year-end/reconcile',
}
