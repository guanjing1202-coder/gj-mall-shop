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
  sensitive?: boolean
  masked?: boolean
  createTime?: string
  updateTime?: string
}

export interface ConfigGroupSummary {
  groupCode: string
  groupName?: string
  totalCount: number
  requiredCount: number
  readyCount: number
  enabledCount: number
  filledCount: number
  missingCount: number
  uninitializedCount?: number
  sensitiveCount: number
  completenessPercent: number
  missingKeys?: string[]
  uninitializedKeys?: string[]
}

export interface ConfigInitResult {
  groupCode: string
  createdCount: number
  restoredCount?: number
  existingCount: number
  createdKeys?: string[]
  restoredKeys?: string[]
  existingKeys?: string[]
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

export interface UploadStorageFile {
  url: string
  relativePath: string
  size: number
  lastModified?: string
  cleanupCandidate?: boolean
}

export interface UploadStorageSummary {
  rootPath: string
  retainDays: number
  totalFiles: number
  totalSize: number
  referencedFiles: number
  orphanFiles: number
  orphanSize: number
  cleanupCandidates: number
  cleanupCandidateSize: number
  referenceCount: number
  recentOrphans?: UploadStorageFile[]
}

export interface UploadCleanupResult {
  retainDays: number
  dryRun: boolean
  scannedFiles: number
  deletedFiles: number
  deletedSize: number
  candidateFiles: number
  candidateSize: number
  files?: UploadStorageFile[]
}

export function getSystemConfigPage(params: SystemConfigQuery) {
  return request.get<ApiResult<PageResult<SystemConfigRecord>>>('/api/admin/sys/config/page', { params })
}

export function getSystemConfigSummary(groupCode: string) {
  return request.get<ApiResult<ConfigGroupSummary>>('/api/admin/sys/config/summary', {
    params: { groupCode },
  })
}

export function initializePaymentConfigs() {
  return request.post<ApiResult<ConfigInitResult>>('/api/admin/sys/config/payment/initialize')
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

export function getUploadStorageSummary(retainDays = 7) {
  return request.get<ApiResult<UploadStorageSummary>>('/api/admin/upload-storage/summary', {
    params: { retainDays },
  })
}

export function cleanupUploadStorage(retainDays = 7, dryRun = true) {
  return request.post<ApiResult<UploadCleanupResult>>('/api/admin/upload-storage/cleanup', null, {
    params: { retainDays, dryRun },
  })
}
