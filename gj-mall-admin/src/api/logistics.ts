import request from '@/utils/request'
import type { ApiId, OrderRecord, DeliverOrderPayload, PageResult, ApiResult } from './order'

export interface LogisticsOrderQuery {
  pageNum?: number
  pageSize?: number
  status?: number
  orderNo?: string
  userId?: ApiId
  deliveryNo?: string
  deliveryCompany?: string
}

export interface LogisticsSummary {
  pendingDeliveryCount?: number
  pendingReceiveCount?: number
  shippedTodayCount?: number
  receivedTodayCount?: number
  activeCompanyCount?: number
  pendingDeliveryAmount?: number
  pendingReceiveAmount?: number
  shippedTodayAmount?: number
  receivedTodayAmount?: number
  overdueDeliveryCount?: number
  overdueReceiveCount?: number
  overdueDeliveryAmount?: number
  overdueReceiveAmount?: number
}

export interface DeliveryCompanyQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export interface DeliveryCompanyRecord {
  id: ApiId
  code: string
  name: string
  contactPhone?: string
  sort?: number
  status: number
  statusDesc?: string
  createTime?: string
  updateTime?: string
}

export interface DeliveryCompanyPayload {
  id?: ApiId
  code: string
  name: string
  contactPhone?: string
  sort?: number
  status?: number
}

export function getLogisticsSummary() {
  return request.get<ApiResult<LogisticsSummary>>('/api/admin/logistics/summary')
}

export function getLogisticsOrders(params: LogisticsOrderQuery) {
  return request.get<ApiResult<PageResult<OrderRecord>>>('/api/admin/logistics/orders', { params })
}

export function deliverLogisticsOrder(id: ApiId, data: DeliverOrderPayload) {
  return request.post<ApiResult<OrderRecord>>(`/api/admin/logistics/orders/${id}/deliver`, data)
}

export function getDeliveryCompanyOptions() {
  return request.get<ApiResult<DeliveryCompanyRecord[]>>('/api/admin/logistics/companies/options')
}

export function getDeliveryCompanyPage(params: DeliveryCompanyQuery) {
  return request.get<ApiResult<PageResult<DeliveryCompanyRecord>>>('/api/admin/logistics/companies/page', { params })
}

export function createDeliveryCompany(data: DeliveryCompanyPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/logistics/companies', data)
}

export function updateDeliveryCompany(data: DeliveryCompanyPayload) {
  return request.put<ApiResult<void>>('/api/admin/logistics/companies', data)
}

export function updateDeliveryCompanyStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/logistics/companies/${id}/status`, null, {
    params: { status },
  })
}

export function deleteDeliveryCompany(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/logistics/companies/${id}`)
}
