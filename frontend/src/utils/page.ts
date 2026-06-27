import type { ApiResponse } from '@/utils/request'
import type { PageResult } from '@/types'

export const readPage = <T>(response: ApiResponse<PageResult<T> | T[]>) => {
  const data = response.data

  if (Array.isArray(data)) {
    return {
      records: data,
      total: response.total || data.length,
    }
  }

  return {
    records: data?.records || [],
    total: data?.total || response.total || 0,
  }
}
