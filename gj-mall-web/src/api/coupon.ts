import request from '@/utils/request'
import type { ApiId, ApiResult } from './product'

export interface MyCoupon {
  id: ApiId
  couponId: ApiId
  name: string
  type?: number
  typeDesc?: string
  discountAmount?: number
  discountRate?: number
  minAmount?: number
  startTime?: string
  endTime?: string
  status?: number
  statusDesc?: string
  createTime?: string
}

export interface CouponCenterItem {
  id: ApiId
  name: string
  type?: number
  typeDesc?: string
  discountAmount?: number
  discountRate?: number
  minAmount?: number
  totalCount?: number
  receivedCount?: number
  usedCount?: number
  remainCount?: number
  startTime?: string
  endTime?: string
  status?: number
}

export function getCouponCenter() {
  return request.get<ApiResult<CouponCenterItem[]>>('/api/coupon/center')
}

export function getMyCoupons(status?: number) {
  return request.get<ApiResult<MyCoupon[]>>('/api/coupon/my', {
    params: { status },
  })
}

export function getAvailableCoupons(orderAmount?: number) {
  return request.get<ApiResult<MyCoupon[]>>('/api/coupon/available', {
    params: { orderAmount },
  })
}

export function receiveCoupon(couponId: ApiId) {
  return request.post<ApiResult<void>>(`/api/coupon/${couponId}/receive`)
}
