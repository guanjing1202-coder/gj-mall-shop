import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from './product'

export interface HistoryItem {
  spuId: ApiId
  spuName?: string
  subTitle?: string
  mainImage?: string
  price?: number
  saleCount?: number
  publishStatus?: number
  brandId?: ApiId
  brandName?: string
  categoryId?: ApiId
  categoryName?: string
  browseTime?: string
}

export interface HistoryQuery {
  current?: number
  size?: number
}

export function getHistoryPage(params: HistoryQuery) {
  return request.get<ApiResult<PageResult<HistoryItem>>>('/api/user/history/page', { params })
}

export function recordHistory(spuId: ApiId) {
  return request.post<ApiResult<void>>(`/api/user/history/${spuId}`)
}

export function removeHistory(spuId: ApiId) {
  return request.delete<ApiResult<void>>(`/api/user/history/${spuId}`)
}

export function clearHistory() {
  return request.delete<ApiResult<void>>('/api/user/history')
}
