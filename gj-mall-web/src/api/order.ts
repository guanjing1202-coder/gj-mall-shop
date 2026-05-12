import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from './product'

export interface CreateOrderPayload {
  addressId: ApiId
  remark?: string
  couponId?: ApiId
}

export interface OrderQuery {
  pageNum?: number
  pageSize?: number
  status?: number
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

export interface CreateOrderCommentPayload {
  orderItemId: ApiId
  score: number
  content: string
  images?: string[]
}

export interface AfterSaleItem extends OrderItem {}

export interface AfterSale {
  id: ApiId
  afterSaleNo: string
  orderId: ApiId
  orderNo: string
  userId?: ApiId
  type: number
  typeDesc?: string
  amount?: number
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
  auditTime?: string
  receiveTime?: string
  refundTime?: string
  createTime?: string
  updateTime?: string
  items?: AfterSaleItem[]
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
  remark?: string
  createTime?: string
  updateTime?: string
  items?: OrderItem[]
}

export function createOrder(data: CreateOrderPayload) {
  return request.post<ApiResult<string>>('/api/order', data)
}

export function getOrderPage(params: OrderQuery) {
  return request.get<ApiResult<PageResult<OrderDetail>>>('/api/order/page', { params })
}

export function getOrderDetail(id: ApiId) {
  return request.get<ApiResult<OrderDetail>>(`/api/order/${id}`)
}

export function getOrderByNo(orderNo: string) {
  return request.get<ApiResult<OrderDetail>>(`/api/order/no/${orderNo}`)
}

export function cancelOrder(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/order/${id}/cancel`)
}

export function receiveOrder(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/order/${id}/receive`)
}

export function getOrderComments(orderId: ApiId) {
  return request.get<ApiResult<OrderComment[]>>(`/api/order/${orderId}/comment`)
}

export function createOrderComment(orderId: ApiId, data: CreateOrderCommentPayload) {
  return request.post<ApiResult<void>>(`/api/order/${orderId}/comment`, data)
}

export function getOrderAfterSales(orderId: ApiId) {
  return request.get<ApiResult<AfterSale[]>>(`/api/order/${orderId}/after-sale`)
}

export function createAfterSale(orderId: ApiId, data: CreateAfterSalePayload) {
  return request.post<ApiResult<AfterSale>>(`/api/order/${orderId}/after-sale`, data)
}

export function getAfterSalePage(params: OrderQuery) {
  return request.get<ApiResult<PageResult<AfterSale>>>('/api/after-sale/page', { params })
}

export function cancelAfterSale(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/after-sale/${id}/cancel`)
}

export function submitAfterSaleReturn(id: ApiId, data: SubmitAfterSaleReturnPayload) {
  return request.post<ApiResult<void>>(`/api/after-sale/${id}/return`, data)
}
