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
  createTime?: string
  updateTime?: string
}

export interface PaymentRefundPayload {
  amount?: number
  reason?: string
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
