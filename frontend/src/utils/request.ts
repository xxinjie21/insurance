import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 响应接口定义
export interface ApiResponse<T = any> {
  success: boolean
  errorMsg?: string
  data: T
  total?: number
  code?: number
  msg?: string
}

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 不需要 token 的白名单接口（精确匹配，避免 /user/login/xxx 误匹配）
const WHITE_LIST = ['/user/login', '/user/sign', '/hospital/sign']

function isWhiteListed(url: string): boolean {
  return WHITE_LIST.some(path => url === path || url.startsWith(path + '?'))
}

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const url = config.url || ''
    if (isWhiteListed(url)) {
      return config
    }

    const token = localStorage.getItem('token')

    if (token) {
      // 注意：后端 UserInfoInterceptor 从 "token" header 读取
      // 如需迁移到标准 Authorization header，需同步修改后端拦截器
      config.headers['token'] = token
    } else {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
      return Promise.reject(new Error('请先登录'))
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 后端 Result: { success, errorMsg, data, total, code }
    if (typeof res.success === 'boolean') {
      if (!res.success) {
        const message = res.errorMsg || '请求失败'
        // 登录过期，跳转到登录页
        if (res.code === 401) {
          const userStore = useUserStore()
          userStore.logout()
          window.location.href = '/login'
          return Promise.reject(new Error(message))
        }
        ElMessage.error(message)
        return Promise.reject(new Error(message))
      }
      return response
    }

    // 兼容旧 { code, msg, data } 结构
    if (typeof res.code === 'number' && res.code !== 200) {
      const message = res.msg || '请求失败'
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        window.location.href = '/login'
        return Promise.reject(new Error(message))
      }

      if (res.code === 403) {
        ElMessage.error('无权限访问')
        return Promise.reject(new Error(message))
      }

      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }

    return response
  },
  (error) => {
    if (error.message === '请先登录') {
      return Promise.reject(error)
    }

    if (error.response) {
      const status = error.response.status
      const responseData = error.response.data
      // 兼容后端拦截器返回的 JSON Result 格式
      const message =
        typeof responseData === 'string'
          ? responseData
          : responseData?.errorMsg || responseData?.msg || '请求失败'

      if (status === 401) {
        const userStore = useUserStore()
        userStore.logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      if (status === 403) {
        ElMessage.error(message || '无权限访问')
        return Promise.reject(error)
      }

      ElMessage.error(message)
      return Promise.reject(error)
    }

    if (error.message.includes('Network Error')) {
      ElMessage.error('网络连接失败，请检查网络')
    } else if (error.message.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('服务器错误，请稍后重试')
    }

    return Promise.reject(error)
  }
)

// 导出请求方法
export default service

export const http = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.get(url, config).then((res: AxiosResponse<ApiResponse<T>>) => res.data)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.post(url, data, config).then((res: AxiosResponse<ApiResponse<T>>) => res.data)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.put(url, data, config).then((res: AxiosResponse<ApiResponse<T>>) => res.data)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.delete(url, config).then((res: AxiosResponse<ApiResponse<T>>) => res.data)
  },
}

export * from '@/api'
