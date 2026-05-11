import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from '@/api/product'

export interface InventoryQuery {
  keyword?: string
  spuId?: ApiId
  brandId?: ApiId
  categoryId?: ApiId
  publishStatus?: number
  stockStatus?: 'empty' | 'low' | 'normal' | 'locked'
  pageNum?: number
  pageSize?: number
}

export interface InventoryRecord {
  skuId: ApiId
  spuId: ApiId
  spuName?: string
  subTitle?: string
  skuCode?: string
  skuName?: string
  mainImage?: string
  skuImage?: string
  brandId?: ApiId
  brandName?: string
  categoryId?: ApiId
  categoryName?: string
  price?: number
  stock?: number
  lockedStock?: number
  warnStock?: number
  totalStock?: number
  alertGap?: number
  saleCount?: number
  publishStatus?: number
  stockStatus?: string
  stockStatusDesc?: string
  specData?: Record<string, string>
  updateTime?: string
}

export interface InventorySummary {
  totalSkuCount?: number
  emptySkuCount?: number
  lowSkuCount?: number
  lockedSkuCount?: number
  totalAvailableStock?: number
  totalLockedStock?: number
  totalStock?: number
  stockAmount?: number
}

export interface InventoryAdjustPayload {
  stockDelta: number
  remark?: string
}

export interface InventoryBatchAdjustPayload {
  items: Array<{
    skuId: ApiId
    stockDelta: number
    remark?: string
  }>
  remark?: string
}

export interface InventoryWarnStockPayload {
  warnStock: number
}

export interface InventoryLogQuery {
  skuId?: ApiId
  spuId?: ApiId
  pageNum?: number
  pageSize?: number
}

export interface InventoryLogRecord {
  id: ApiId
  spuId: ApiId
  spuName?: string
  skuId: ApiId
  skuName?: string
  skuCode?: string
  changeType?: number
  changeTypeDesc?: string
  changeQuantity?: number
  stockBefore?: number
  stockAfter?: number
  lockedStockBefore?: number
  lockedStockAfter?: number
  remark?: string
  createTime?: string
}

export function getInventoryPage(params: InventoryQuery) {
  return request.get<ApiResult<PageResult<InventoryRecord>>>('/api/admin/product/inventory/page', { params })
}

export function getInventorySummary(params: InventoryQuery) {
  return request.get<ApiResult<InventorySummary>>('/api/admin/product/inventory/summary', { params })
}

export function adjustInventoryStock(skuId: ApiId, data: InventoryAdjustPayload) {
  return request.put<ApiResult<InventoryRecord>>(`/api/admin/product/inventory/${skuId}/adjust`, data)
}

export function batchAdjustInventoryStock(data: InventoryBatchAdjustPayload) {
  return request.put<ApiResult<InventoryRecord[]>>('/api/admin/product/inventory/batch-adjust', data)
}

export function updateInventoryWarnStock(skuId: ApiId, data: InventoryWarnStockPayload) {
  return request.put<ApiResult<InventoryRecord>>(`/api/admin/product/inventory/${skuId}/warn-stock`, data)
}

export function getInventoryLogPage(params: InventoryLogQuery) {
  return request.get<ApiResult<PageResult<InventoryLogRecord>>>('/api/admin/product/inventory/log/page', { params })
}
