import { request } from '@/utils/request'
import type { ApiId, PageResult } from './product'

export interface OrderQuery {
  pageNum?: number
  pageSize?: number
  status?: number
}

export interface CreateOrderPayload {
  addressId: ApiId
  remark?: string
  couponId?: ApiId
}

export interface OrderDetail {
  id: ApiId
  orderNo: string
  totalAmount?: number
  payAmount?: number
  status: number
  statusDesc?: string
  createTime?: string
}

export interface AfterSale {
  id: ApiId
  afterSaleNo: string
  orderId: ApiId
  orderNo: string
  type: number
  typeDesc?: string
  amount?: number
  reason?: string
  status: number
  statusDesc?: string
  createTime?: string
}

export function getOrderPage(params: OrderQuery) {
  return request<PageResult<OrderDetail>>({ url: '/api/order/page', method: 'GET', data: params })
}

export function createOrder(data: CreateOrderPayload) {
  return request<string>({ url: '/api/order', method: 'POST', data })
}

export function getOrderByNo(orderNo: string) {
  return request<OrderDetail>({ url: `/api/order/no/${orderNo}`, method: 'GET' })
}

export function getAfterSalePage(params: OrderQuery) {
  return request<PageResult<AfterSale>>({ url: '/api/after-sale/page', method: 'GET', data: params })
}
