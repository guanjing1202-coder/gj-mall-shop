import request from '@/utils/request'

export type ApiId = string | number

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string | number
}

export interface PageResult<T> {
  total: string | number
  pageNum: string | number
  pageSize: string | number
  list: T[]
}

export interface OperationLogQuery {
  keyword?: string
  adminId?: ApiId
  requestMethod?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface OperationLogRecord {
  id: ApiId
  adminId?: ApiId
  username?: string
  module?: string
  operation?: string
  requestMethod: string
  requestUri: string
  requestParams?: string
  requestSummary?: string
  ip?: string
  status: number
  statusDesc?: string
  errorMessage?: string
  costTime?: number
  createTime?: string
}

export function getOperationLogPage(params: OperationLogQuery) {
  return request.get<ApiResult<PageResult<OperationLogRecord>>>('/api/admin/operation-log/page', { params })
}
