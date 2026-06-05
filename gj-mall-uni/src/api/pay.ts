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

export interface PayStatus {
  payNo: string
  thirdPayNo?: string
  channel?: number
  channelName?: PayChannel | string
  channelDesc?: string
  paid?: boolean
  status?: number
  statusDesc?: string
  amount?: number
  payTime?: string
  orderId?: ApiId
  orderNo?: string
  orderStatus?: number
  orderStatusDesc?: string
}

export interface PayChannelStatus {
  channel?: number
  name: PayChannel
  desc?: string
  enabled?: boolean
  status?: string
}

export function createPay(data: PayPayload) {
  return request<PayResult>({ url: '/api/pay', method: 'POST', data })
}

export function getPayChannels() {
  return request<PayChannelStatus[]>({ url: '/api/pay/channels', method: 'GET' })
}

export function getPayStatus(payNo: string) {
  return request<PayStatus>({ url: `/api/pay/${encodeURIComponent(payNo)}/status`, method: 'GET' })
}
