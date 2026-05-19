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
  invoiceInfo?: InvoiceInfo
  items?: CreateOrderItemPayload[]
}

export interface InvoiceInfo {
  type?: number
  title?: string
  taxNo?: string
  email?: string
  content?: string
}

export interface CreateOrderItemPayload {
  skuId: ApiId
  quantity: number
}

export interface OrderDetail {
  id: ApiId
  orderNo: string
  userId?: ApiId
  totalAmount?: number
  payAmount?: number
  freightAmount?: number
  couponAmount?: number
  status: number
  statusDesc?: string
  payType?: number
  payTime?: string
  deliveryTime?: string
  receiveTime?: string
  deliveryCompany?: string
  deliveryNo?: string
  deliveryRemark?: string
  receiver?: ReceiverInfo
  invoice?: InvoiceInfo
  remark?: string
  createTime?: string
  items?: OrderItem[]
}

export interface ReceiverInfo {
  receiver?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  detail?: string
  postCode?: string
}

export interface OrderItem {
  id: ApiId
  spuId: ApiId
  skuId: ApiId
  skuName?: string
  skuImage?: string
  specData?: Record<string, string>
  price?: number
  quantity: number
  totalAmount?: number
}

export interface OrderComment {
  id: ApiId
  orderId: ApiId
  orderNo: string
  orderItemId: ApiId
  spuId: ApiId
  skuId: ApiId
  score: number
  content: string
  images?: string[]
  status?: number
  statusDesc?: string
  replyContent?: string
  replyTime?: string
  createTime?: string
}

export interface OrderLogistics {
  orderId: ApiId
  orderNo: string
  status?: number
  statusDesc?: string
  deliveryCompany?: string
  deliveryNo?: string
  deliveryRemark?: string
  currentAction?: string
  nextHint?: string
  traces?: OrderLogisticsTrace[]
}

export interface OrderLogisticsTrace {
  title: string
  description?: string
  time?: string
  active?: boolean
}

export interface CreateOrderCommentPayload {
  orderItemId: ApiId
  score: number
  content: string
  images?: string[]
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
  description?: string
  images?: string[]
  status: number
  statusDesc?: string
  auditRemark?: string
  rejectReason?: string
  returnCompany?: string
  returnNo?: string
  createTime?: string
  updateTime?: string
  items?: OrderItem[]
}

export interface CreateAfterSalePayload {
  type: number
  reason: string
  description?: string
  images?: string[]
}

export interface SubmitAfterSaleReturnPayload {
  returnCompany: string
  returnNo: string
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

export function getOrderDetail(id: ApiId) {
  return request<OrderDetail>({ url: `/api/order/${id}`, method: 'GET' })
}

export function getOrderLogistics(id: ApiId) {
  return request<OrderLogistics>({ url: `/api/order/${id}/logistics`, method: 'GET' })
}

export function cancelOrder(id: ApiId) {
  return request<void>({ url: `/api/order/${id}/cancel`, method: 'POST' })
}

export function receiveOrder(id: ApiId) {
  return request<void>({ url: `/api/order/${id}/receive`, method: 'POST' })
}

export function getOrderComments(orderId: ApiId) {
  return request<OrderComment[]>({ url: `/api/order/${orderId}/comment`, method: 'GET' })
}

export function createOrderComment(orderId: ApiId, data: CreateOrderCommentPayload) {
  return request<void>({ url: `/api/order/${orderId}/comment`, method: 'POST', data })
}

export function getAfterSalePage(params: OrderQuery) {
  return request<PageResult<AfterSale>>({ url: '/api/after-sale/page', method: 'GET', data: params })
}

export function getOrderAfterSales(orderId: ApiId) {
  return request<AfterSale[]>({ url: `/api/order/${orderId}/after-sale`, method: 'GET' })
}

export function createAfterSale(orderId: ApiId, data: CreateAfterSalePayload) {
  return request<AfterSale>({ url: `/api/order/${orderId}/after-sale`, method: 'POST', data })
}

export function cancelAfterSale(id: ApiId) {
  return request<void>({ url: `/api/after-sale/${id}/cancel`, method: 'POST' })
}

export function submitAfterSaleReturn(id: ApiId, data: SubmitAfterSaleReturnPayload) {
  return request<void>({ url: `/api/after-sale/${id}/return`, method: 'POST', data })
}
