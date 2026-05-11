import request from '@/utils/request'
import type { ApiId, ApiResult } from './product'

export type PayChannel = 'mock' | 'wechat' | 'alipay'

export interface PayPayload {
  orderId: ApiId
  channel: PayChannel
}

export interface PayResult {
  channel: PayChannel
  payNo: string
  thirdPayNo?: string
  paid: boolean
  amount?: number
  payInfo?: string
}

export function createPay(data: PayPayload) {
  return request.post<ApiResult<PayResult>>('/api/pay', data)
}
