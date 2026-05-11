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

export interface CouponQuery {
  keyword?: string
  type?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface CouponRecord {
  id: ApiId
  name: string
  type: number
  typeDesc?: string
  discountAmount?: number
  discountRate?: number
  minAmount?: number
  totalCount: number
  receivedCount: number
  usedCount: number
  remainCount: number
  startTime?: string
  endTime?: string
  status: number
  statusDesc?: string
  validityStatus?: string
  createTime?: string
  updateTime?: string
}

export interface CouponPayload {
  id?: ApiId
  name: string
  type: number
  discountAmount?: number
  discountRate?: number
  minAmount?: number
  totalCount: number
  startTime: string
  endTime: string
  status?: number
}

export interface CouponUserQuery {
  userKeyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface CouponUserRecord {
  id: ApiId
  couponId: ApiId
  userId: ApiId
  username?: string
  nickname?: string
  phone?: string
  status: number
  statusDesc?: string
  usedAt?: string
  orderId?: ApiId
  createTime?: string
}

export function getCouponPage(params: CouponQuery) {
  return request.get<ApiResult<PageResult<CouponRecord>>>('/api/admin/marketing/coupon/page', { params })
}

export function getCouponDetail(id: ApiId) {
  return request.get<ApiResult<CouponRecord>>(`/api/admin/marketing/coupon/${id}`)
}

export function createCoupon(data: CouponPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/marketing/coupon', data)
}

export function updateCoupon(data: CouponPayload) {
  return request.put<ApiResult<void>>('/api/admin/marketing/coupon', data)
}

export function updateCouponStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/marketing/coupon/${id}/status`, null, {
    params: { status },
  })
}

export function deleteCoupon(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/marketing/coupon/${id}`)
}

export function issueCoupon(id: ApiId, userIds: ApiId[]) {
  return request.post<ApiResult<number>>(`/api/admin/marketing/coupon/${id}/issue`, { userIds })
}

export function getCouponUsers(id: ApiId, params: CouponUserQuery) {
  return request.get<ApiResult<PageResult<CouponUserRecord>>>(
    `/api/admin/marketing/coupon/${id}/users`,
    { params },
  )
}
