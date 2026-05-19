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

export interface AfterSaleQuery {
  keyword?: string
  userId?: ApiId
  type?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface AfterSaleItem {
  id: ApiId
  orderId: ApiId
  orderNo: string
  spuId: ApiId
  skuId: ApiId
  skuName: string
  skuImage?: string
  specData?: string | Record<string, string>
  price: number
  quantity: number
  totalAmount: number
  createTime?: string
}

export interface AfterSaleRecord {
  id: ApiId
  afterSaleNo: string
  orderId: ApiId
  orderNo: string
  userId: ApiId
  username?: string
  nickname?: string
  phone?: string
  type: number
  typeDesc?: string
  amount: number
  reason?: string
  description?: string
  images?: string[]
  orderStatusSnapshot?: number
  orderStatusSnapshotDesc?: string
  status: number
  statusDesc?: string
  auditRemark?: string
  rejectReason?: string
  returnCompany?: string
  returnNo?: string
  refundPaymentId?: ApiId
  auditTime?: string
  receiveTime?: string
  refundTime?: string
  createTime?: string
  updateTime?: string
  orderPayAmount?: number
  currentOrderStatus?: number
  currentOrderStatusDesc?: string
  items?: AfterSaleItem[]
}

export interface AfterSaleTypeSummary {
  type: number
  typeDesc?: string
  count: number
  amount: number
}

export interface AfterSaleSummary {
  totalCount: number
  totalAmount: number
  pendingCount: number
  pendingAmount: number
  waitReturnCount: number
  waitReturnAmount: number
  waitRefundCount: number
  waitRefundAmount: number
  rejectedCount: number
  rejectedAmount: number
  completedCount: number
  completedAmount: number
  canceledCount: number
  canceledAmount: number
  processingCount: number
  processingAmount: number
  todayNewCount: number
  todayNewAmount: number
  todayRefundedCount: number
  todayRefundedAmount: number
  completionRate: number
  rejectionRate: number
  types: AfterSaleTypeSummary[]
}

export interface AfterSaleCreatePayload {
  orderId: ApiId
  type: number
  amount?: number
  reason?: string
  description?: string
  images?: string[]
}

export interface AfterSaleActionPayload {
  auditRemark?: string
  rejectReason?: string
  returnCompany?: string
  returnNo?: string
}

export function getAfterSaleSummary() {
  return request.get<ApiResult<AfterSaleSummary>>('/api/admin/after-sale/summary')
}

export function getAfterSalePage(params: AfterSaleQuery) {
  return request.get<ApiResult<PageResult<AfterSaleRecord>>>('/api/admin/after-sale/page', { params })
}

export function getAfterSaleDetail(id: ApiId) {
  return request.get<ApiResult<AfterSaleRecord>>(`/api/admin/after-sale/${id}`)
}

export function createAfterSale(data: AfterSaleCreatePayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/after-sale', data)
}

export function approveAfterSale(id: ApiId, data: AfterSaleActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/after-sale/${id}/approve`, data)
}

export function rejectAfterSale(id: ApiId, data: AfterSaleActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/after-sale/${id}/reject`, data)
}

export function receiveAfterSale(id: ApiId, data: AfterSaleActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/after-sale/${id}/receive`, data)
}

export function refundAfterSale(id: ApiId, data: AfterSaleActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/after-sale/${id}/refund`, data)
}

export function cancelAfterSale(id: ApiId, data: AfterSaleActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/after-sale/${id}/cancel`, data)
}
