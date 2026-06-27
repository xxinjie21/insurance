/** 日期时间范围（与 el-date-picker value-format 一致） */
export type DateTimeRange = [string, string] | null

/** 组装分页 + 可选日期筛选参数 */
export function buildPageQuery(
  pageNum: number,
  pageSize: number,
  timeRange?: DateTimeRange,
  extra?: Record<string, string | number>
): Record<string, string | number> {
  const params: Record<string, string | number> = {
    pageNum,
    pageSize,
    ...extra,
  }
  if (timeRange && timeRange.length === 2) {
    params.startTime = timeRange[0]
    params.endTime = timeRange[1]
  }
  return params
}
