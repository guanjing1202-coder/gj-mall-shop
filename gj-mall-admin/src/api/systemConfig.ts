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

export interface SystemConfigQuery {
  keyword?: string
  groupCode?: string
  valueType?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SystemConfigRecord {
  id: ApiId
  configKey: string
  configName: string
  configValue?: string
  valueType: string
  valueTypeDesc?: string
  groupCode: string
  groupName?: string
  description?: string
  editable: number
  status: number
  statusDesc?: string
  createTime?: string
  updateTime?: string
}

export interface SystemConfigPayload {
  id?: ApiId
  configKey: string
  configName: string
  configValue?: string
  valueType?: string
  groupCode?: string
  description?: string
  editable?: number
  status?: number
}

export function getSystemConfigPage(params: SystemConfigQuery) {
  return request.get<ApiResult<PageResult<SystemConfigRecord>>>('/api/admin/sys/config/page', { params })
}

export function getSystemConfigDetail(id: ApiId) {
  return request.get<ApiResult<SystemConfigRecord>>(`/api/admin/sys/config/${id}`)
}

export function createSystemConfig(data: SystemConfigPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/sys/config', data)
}

export function updateSystemConfig(data: SystemConfigPayload) {
  return request.put<ApiResult<void>>('/api/admin/sys/config', data)
}

export function updateSystemConfigStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/sys/config/${id}/status`, null, {
    params: { status },
  })
}

export function deleteSystemConfig(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/sys/config/${id}`)
}
