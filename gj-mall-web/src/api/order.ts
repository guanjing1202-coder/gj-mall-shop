import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from './product'

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

export interface FreightQuotePayload {
  addressId: ApiId
  orderAmount: number
}

export interface FreightQuote {
  orderAmount?: number
  freightAmount?: number
  baseFreight?: number
  chargedBaseFreight?: number
  remoteExtra?: number
  chargedRemoteExtra?: number
  freeThreshold?: number
  freeThresholdReached?: boolean
  freeShipping?: boolean
  remoteArea?: boolean
  province?: string
  nextFreeAmount?: number
  summary?: string
  hint?: string
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

export interface OrderLogisticsTrace {
  title: string
  description?: string
  time?: string
  active?: boolean
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

export interface CreateOrderCommentPayload {
  orderItemId: ApiId
  score: number
  content: string
  images?: string[]
}

export interface AfterSaleItem extends OrderItem {}

export interface AfterSaleEligibility {
  available?: boolean
  unavailableReason?: string
  requestedType?: number
  refundOnlyAllowed?: boolean
  returnRefundAllowed?: boolean
  windowDays?: number
  startTime?: string
  deadline?: string
}

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
  refundRecord?: AfterSaleRefundRecord
  timeline?: AfterSaleTimelineItem[]
}

export interface AfterSaleTimelineItem {
  title?: string
  description?: string
  time?: string
  active?: boolean
  tone?: string
}

export interface AfterSaleRefundRecord {
  id?: ApiId
  refundNo?: string
  paymentId?: ApiId
  payNo?: string
  thirdPayNo?: string
  afterSaleId?: ApiId
  afterSaleNo?: string
  orderId?: ApiId
  orderNo?: string
  channel?: number
  channelDesc?: string
  amount?: number
  status?: number
  statusDesc?: string
  reason?: string
  operatorType?: string
  successTime?: string
  createTime?: string
  updateTime?: string
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
  invoice?: InvoiceInfo
  remark?: string
  createTime?: string
  updateTime?: string
  items?: OrderItem[]
}

export function createOrder(data: CreateOrderPayload) {
  return request.post<ApiResult<string>>('/api/order', data)
}

export function quoteFreight(data: FreightQuotePayload) {
  return request.post<ApiResult<FreightQuote>>('/api/order/freight/quote', data)
}

export function getOrderPage(params: OrderQuery) {
  return request.get<ApiResult<PageResult<OrderDetail>>>('/api/order/page', { params })
}

export function getOrderDetail(id: ApiId) {
  return request.get<ApiResult<OrderDetail>>(`/api/order/${id}`)
}

export function getOrderLogistics(id: ApiId) {
  return request.get<ApiResult<OrderLogistics>>(`/api/order/${id}/logistics`)
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

export function getAfterSaleEligibility(orderId: ApiId, type?: number) {
  return request.get<ApiResult<AfterSaleEligibility>>(`/api/order/${orderId}/after-sale/eligibility`, {
    params: type ? { type } : undefined,
  })
}

export function createAfterSale(orderId: ApiId, data: CreateAfterSalePayload) {
  return request.post<ApiResult<AfterSale>>(`/api/order/${orderId}/after-sale`, data)
}

export function getAfterSalePage(params: OrderQuery) {
  return request.get<ApiResult<PageResult<AfterSale>>>('/api/after-sale/page', { params })
}

export function getAfterSaleDetail(id: ApiId) {
  return request.get<ApiResult<AfterSale>>(`/api/after-sale/${id}`)
}

export function cancelAfterSale(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/after-sale/${id}/cancel`)
}

export function submitAfterSaleReturn(id: ApiId, data: SubmitAfterSaleReturnPayload) {
  return request.post<ApiResult<void>>(`/api/after-sale/${id}/return`, data)
}
