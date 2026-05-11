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
