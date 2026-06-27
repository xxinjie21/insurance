import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Role } from '@/types'

export interface UserInfo {
  userId: string
  name: string
  role: Role
  hospitalId?: string
  hospitalName?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(readUserInfo())
  // 管理员选中的医院 ID（管理员操作医院功能时使用）
  const selectedHospitalId = ref<string | null>(
    localStorage.getItem('selectedHospitalId') || null
  )
  // 管理员选中的医院名称
  const selectedHospitalName = ref<string>(localStorage.getItem('selectedHospitalName') || '')

  // 设置 token 和用户信息（原子操作，避免不一致）
  const setLoginData = (newToken: string, info: UserInfo) => {
    token.value = newToken
    userInfo.value = info
    localStorage.setItem('token', newToken)
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  // 管理员选择医院
  const setSelectedHospital = (hospitalId: string, hospitalName: string) => {
    selectedHospitalId.value = hospitalId
    selectedHospitalName.value = hospitalName
    localStorage.setItem('selectedHospitalId', String(hospitalId))
    localStorage.setItem('selectedHospitalName', hospitalName)
  }

  // 管理员取消选择医院
  const clearSelectedHospital = () => {
    selectedHospitalId.value = null
    selectedHospitalName.value = ''
    localStorage.removeItem('selectedHospitalId')
    localStorage.removeItem('selectedHospitalName')
  }

  const getToken = () => token.value
  const getUserInfo = () => userInfo.value
  const getRole = () => userInfo.value?.role

  const roleValue = computed(() => Number(userInfo.value?.role))

  const isPatient = computed(() => roleValue.value === 1)
  const isHospital = computed(() => roleValue.value === 2)
  const isMedical = computed(() => roleValue.value === 3)
  const isAdmin = computed(() => roleValue.value === 4)

  const logout = () => {
    token.value = ''
    userInfo.value = null
    selectedHospitalId.value = null
    selectedHospitalName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('selectedHospitalId')
    localStorage.removeItem('selectedHospitalName')
  }

  return {
    token,
    userInfo,
    selectedHospitalId,
    selectedHospitalName,
    setLoginData,
    setToken,
    setUserInfo,
    setSelectedHospital,
    clearSelectedHospital,
    getToken,
    getUserInfo,
    getRole,
    roleValue,
    isPatient,
    isHospital,
    isMedical,
    isAdmin,
    logout,
  }
})

function readUserInfo(): UserInfo | null {
  const raw = localStorage.getItem('userInfo')
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw)
    // 基本结构校验
    if (typeof parsed === 'object' && parsed !== null && (typeof parsed.userId === 'string' || typeof parsed.userId === 'number')) {
      return parsed as UserInfo
    }
    localStorage.removeItem('userInfo')
    return null
  } catch {
    localStorage.removeItem('userInfo')
    return null
  }
}
