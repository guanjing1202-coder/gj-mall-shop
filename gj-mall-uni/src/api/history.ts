import { request } from '@/utils/request'
import type { ApiId, PageResult } from './product'

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
  return request<PageResult<HistoryItem>>({ url: '/api/user/history/page', method: 'GET', data: params })
}

export function recordHistory(spuId: ApiId) {
  return request<void>({ url: `/api/user/history/${spuId}`, method: 'POST' })
}

export function removeHistory(spuId: ApiId) {
  return request<void>({ url: `/api/user/history/${spuId}`, method: 'DELETE' })
}

export function clearHistory() {
  return request<void>({ url: '/api/user/history', method: 'DELETE' })
}
