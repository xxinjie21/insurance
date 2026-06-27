export const formatMoney = (value?: number | string | null) => {
  const amount = Number(value ?? 0)
  if (Number.isNaN(amount)) return '0.00'
  return amount.toFixed(2)
}

export const formatDateTime = (value?: string | null) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

/** 展示身份证号，兼容 patientIdCard / idCard 字段 */
export const formatIdCard = (row?: { patientIdCard?: string | null; idCard?: string | null } | string | null) => {
  if (row == null) return '-'
  if (typeof row === 'string') {
    return row.trim() || '-'
  }
  const card = row.patientIdCard || row.idCard
  return card?.trim() ? card : '-'
}

export const getVisitStatusName = (status?: number) => {
  if (status === 1) return '已结算'
  if (status === 0) return '未结算'
  return '未知'
}

export const getVisitStatusType = (status?: number): '' | 'info' | 'success' => {
  return status === 1 ? 'success' : 'info'
}

export const getSettleStatusType = (status?: number): '' | 'info' | 'success' | 'warning' => {
  const types: Record<number, '' | 'info' | 'success' | 'warning'> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'success',
  }
  return status !== undefined ? types[status] || '' : ''
}

export const getFeeTypeTag = (type?: number): '' | 'success' | 'warning' | 'info' => {
  const types: Record<number, '' | 'success' | 'warning' | 'info'> = {
    1: 'success',
    2: 'warning',
    3: 'info',
  }
  return type !== undefined ? types[type] || '' : ''
}

export const getVisitTypeName = (type?: number) => {
  const names: Record<number, string> = {
    1: '门诊',
    2: '住院',
  }
  return type ? names[type] || '未知' : '未知'
}

export const getFeeTypeName = (type?: number) => {
  const names: Record<number, string> = {
    1: '甲类',
    2: '乙类',
    3: '自费',
  }
  return type ? names[type] || '未知' : '未知'
}

export const getSettleStatusName = (status?: number) => {
  const names: Record<number, string> = {
    0: '待申报',
    1: '已申报',
    2: '已自付',
    3: '已拨付',
  }
  return status !== undefined ? names[status] || '未知' : '未知'
}

export const getBatchStatusName = (status?: number) => {
  const names: Record<number, string> = {
    0: '待申报',
    1: '已申报',
    2: '已完成',
    3: '拨付拒绝',
  }
  return status !== undefined ? names[status] || '未知' : '未知'
}

export const getBatchStatusType = (status?: number): '' | 'info' | 'success' | 'warning' | 'danger' => {
  const types: Record<number, '' | 'info' | 'success' | 'warning' | 'danger'> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
  }
  return status !== undefined ? types[status] || '' : ''
}

export const getPayStatusName = (status?: number) => {
  const names: Record<number, string> = {
    0: '未拨付',
    1: '已拨付',
    2: '拒绝拨付',
  }
  return status !== undefined ? names[status] || '未知' : '未知'
}

export const getPayStatusType = (status?: number): '' | 'info' | 'success' | 'danger' => {
  const types: Record<number, '' | 'info' | 'success' | 'danger'> = {
    0: 'info',
    1: 'success',
    2: 'danger',
  }
  return status !== undefined ? types[status] || '' : ''
}
