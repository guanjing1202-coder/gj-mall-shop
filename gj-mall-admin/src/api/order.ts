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

export interface OrderQueryParams {
  pageNum?: number
  pageSize?: number
  status?: number
  orderNo?: string
  userId?: ApiId
  deliveryNo?: string
}

export interface Receiver {
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
  spuId?: ApiId
  skuId?: ApiId
  skuName: string
  skuImage?: string
  specData?: Record<string, string>
  price: number
  quantity: number
  totalAmount: number
}

export interface OrderRecord {
  id: ApiId
  orderNo: string
  userId: ApiId
  totalAmount: number
  payAmount: number
  freightAmount?: number
  couponAmount?: number
  couponUserId?: ApiId
  status: number
  statusDesc?: string
  payType?: number
  payTime?: string
  deliveryTime?: string
  receiveTime?: string
  deliveryCompany?: string
  deliveryNo?: string
  deliveryRemark?: string
  receiver?: Receiver
  remark?: string
  createTime?: string
  items?: OrderItem[]
}

export interface OrderFulfillmentSummary {
  totalOrderCount?: number
  pendingPayCount?: number
  pendingDeliveryCount?: number
  pendingReceiveCount?: number
  completedCount?: number
  canceledCount?: number
  refundingCount?: number
  refundedCount?: number
  todayOrderCount?: number
  pendingDeliveryAmount?: number
  totalPayAmount?: number
}

export interface DeliverOrderPayload {
  deliveryCompany: string
  deliveryNo: string
  deliveryRemark?: string
}

export function getOrderPage(params: OrderQueryParams) {
  return request.get<ApiResult<PageResult<OrderRecord>>>('/api/admin/order/page', { params })
}

export function getOrderFulfillmentSummary() {
  return request.get<ApiResult<OrderFulfillmentSummary>>('/api/admin/order/fulfillment/summary')
}

export function deliverOrder(id: ApiId, data: DeliverOrderPayload) {
  return request.post<ApiResult<OrderRecord>>(`/api/admin/order/${id}/deliver`, data)
}

export function cancelOrder(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/admin/order/${id}/cancel`)
}
