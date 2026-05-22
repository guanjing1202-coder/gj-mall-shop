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

export interface PaymentQuery {
  keyword?: string
  userId?: ApiId
  channel?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface PaymentRecord {
  id: ApiId
  orderId: ApiId
  orderNo: string
  userId: ApiId
  username?: string
  nickname?: string
  phone?: string
  payNo: string
  thirdPayNo?: string
  channel: number
  channelDesc?: string
  amount: number
  status: number
  statusDesc?: string
  payTime?: string
  callbackData?: string
  orderStatus?: number
  orderStatusDesc?: string
  orderPayAmount?: number
  orderPayTime?: string
  refundRecord?: RefundRecord
  createTime?: string
  updateTime?: string
}

export interface RefundRecord {
  id: ApiId
  refundNo: string
  paymentId: ApiId
  payNo: string
  thirdPayNo?: string
  afterSaleId?: ApiId
  afterSaleNo?: string
  orderId: ApiId
  orderNo: string
  userId: ApiId
  channel: number
  channelDesc?: string
  amount: number
  status: number
  statusDesc?: string
  reason?: string
  operatorType?: string
  callbackData?: string
  successTime?: string
  createTime?: string
  updateTime?: string
}

export interface PaymentChannelSummary {
  channel: number
  channelDesc?: string
  count: number
  amount: number
}

export interface PaymentSummary {
  totalCount: number
  totalAmount: number
  pendingCount: number
  pendingAmount: number
  paidCount: number
  paidAmount: number
  failedCount: number
  failedAmount: number
  refundedCount: number
  refundedAmount: number
  todayPaidAmount: number
  todayRefundedAmount: number
  successRate: number
  refundRate: number
  channels: PaymentChannelSummary[]
}

export interface PaymentAccessChannel {
  channel: number
  name: string
  desc: string
  enabled: boolean
  status: string
}

export interface PaymentAccess {
  mode: string
  callbackRequireSignature: boolean
  callbackPath: string
  devSignatureAlgorithm: string
  devSignatureHeader?: string
  devSignaturePayload?: string
  devSignature?: string
  devCallbackExample?: string
  channels: PaymentAccessChannel[]
}

export interface PaymentCallbackQuery {
  keyword?: string
  channel?: number
  signatureStatus?: number
  processStatus?: number
  pageNum?: number
  pageSize?: number
}

export interface PaymentCallbackRecord {
  id: ApiId
  callbackNo: string
  channel: number
  channelDesc?: string
  channelName?: string
  payNo?: string
  thirdPayNo?: string
  notifyId?: string
  eventType?: string
  amount?: number
  signatureStatus?: number
  signatureStatusDesc?: string
  processStatus?: number
  processStatusDesc?: string
  retryCount?: number
  errorMessage?: string
  rawData?: string
  requestHeaders?: string
  createTime?: string
  updateTime?: string
}

export interface PaymentRefundPayload {
  amount?: number
  reason?: string
}

export function getPaymentSummary() {
  return request.get<ApiResult<PaymentSummary>>('/api/admin/payment/summary')
}

export function getPaymentAccess() {
  return request.get<ApiResult<PaymentAccess>>('/api/admin/payment/access')
}

export function getPaymentCallbackPage(params: PaymentCallbackQuery) {
  return request.get<ApiResult<PageResult<PaymentCallbackRecord>>>('/api/admin/payment/callback/page', { params })
}

export function replayCallback(callbackId: ApiId) {
  return request.put<ApiResult<void>>(`/api/admin/payment/callback/${callbackId}/replay`)
}

export function getPaymentPage(params: PaymentQuery) {
  return request.get<ApiResult<PageResult<PaymentRecord>>>('/api/admin/payment/page', { params })
}

export function getPaymentDetail(id: ApiId) {
  return request.get<ApiResult<PaymentRecord>>(`/api/admin/payment/${id}`)
}

export function markPaymentPaid(id: ApiId, thirdPayNo?: string) {
  return request.put<ApiResult<void>>(`/api/admin/payment/${id}/paid`, null, {
    params: { thirdPayNo },
  })
}

export function markPaymentFailed(id: ApiId, reason?: string) {
  return request.put<ApiResult<void>>(`/api/admin/payment/${id}/failed`, null, {
    params: { reason },
  })
}

export function refundPayment(id: ApiId, data: PaymentRefundPayload) {
  return request.post<ApiResult<void>>(`/api/admin/payment/${id}/refund`, data)
}

export function retryRefund(refundId: ApiId) {
  return request.put<ApiResult<void>>(`/api/admin/payment/refund/${refundId}/retry`)
}

export function markRefundFailed(refundId: ApiId, reason?: string) {
  return request.put<ApiResult<void>>(`/api/admin/payment/refund/${refundId}/failed`, null, {
    params: { reason },
  })
}
