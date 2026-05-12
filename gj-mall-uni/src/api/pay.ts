import { request } from '@/utils/request'
import type { ApiId } from './product'

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
  return request<PayResult>({ url: '/api/pay', method: 'POST', data })
}
