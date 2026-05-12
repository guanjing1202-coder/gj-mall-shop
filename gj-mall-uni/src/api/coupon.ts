import { request } from '@/utils/request'
import type { ApiId } from './product'

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
  startTime?: string
  endTime?: string
}

export interface MyCoupon extends CouponCenterItem {
  couponId?: ApiId
  status?: number
  statusDesc?: string
  createTime?: string
}

export function getCouponCenter() {
  return request<CouponCenterItem[]>({ url: '/api/coupon/center', method: 'GET' })
}

export function getMyCoupons() {
  return request<MyCoupon[]>({ url: '/api/coupon/my', method: 'GET' })
}

export function getAvailableCoupons(orderAmount?: number) {
  const query = typeof orderAmount === 'number' ? `?orderAmount=${encodeURIComponent(String(orderAmount))}` : ''
  return request<MyCoupon[]>({ url: `/api/coupon/available${query}`, method: 'GET' })
}

export function receiveCoupon(couponId: ApiId) {
  return request<ApiId>({ url: `/api/coupon/${couponId}/receive`, method: 'POST' })
}
