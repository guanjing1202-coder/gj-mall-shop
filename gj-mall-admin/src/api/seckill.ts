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

export interface SeckillQuery {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SeckillSkuRecord {
  id: ApiId
  seckillId: ApiId
  spuId: ApiId
  spuName?: string
  skuId: ApiId
  skuName?: string
  skuImage?: string
  specData?: Record<string, string>
  originalPrice?: number
  productStock?: number
  lockedStock?: number
  seckillPrice: number
  seckillStock: number
  seckillLimit: number
  soldCount?: number
  remainStock?: number
  createTime?: string
  updateTime?: string
}

export interface SeckillRecord {
  id: ApiId
  name: string
  startTime?: string
  endTime?: string
  status: number
  statusDesc?: string
  timeStatus?: string
  skuCount?: number
  totalStock?: number
  soldCount?: number
  remainStock?: number
  createTime?: string
  updateTime?: string
  skus?: SeckillSkuRecord[]
}

export interface SeckillPayload {
  id?: ApiId
  name: string
  startTime: string
  endTime: string
  status?: number
}

export interface SeckillSkuPayload {
  id?: ApiId
  seckillId: ApiId
  spuId: ApiId
  skuId: ApiId
  seckillPrice: number
  seckillStock: number
  seckillLimit: number
}

export function getSeckillPage(params: SeckillQuery) {
  return request.get<ApiResult<PageResult<SeckillRecord>>>('/api/admin/marketing/seckill/page', { params })
}

export function getSeckillDetail(id: ApiId) {
  return request.get<ApiResult<SeckillRecord>>(`/api/admin/marketing/seckill/${id}`)
}

export function createSeckill(data: SeckillPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/marketing/seckill', data)
}

export function updateSeckill(data: SeckillPayload) {
  return request.put<ApiResult<void>>('/api/admin/marketing/seckill', data)
}

export function updateSeckillStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/marketing/seckill/${id}/status`, null, {
    params: { status },
  })
}

export function deleteSeckill(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/marketing/seckill/${id}`)
}

export function saveSeckillSku(data: SeckillSkuPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/marketing/seckill/sku', data)
}

export function deleteSeckillSku(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/marketing/seckill/sku/${id}`)
}

export function warmUpSeckill(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/admin/marketing/seckill/${id}/warm-up`)
}
