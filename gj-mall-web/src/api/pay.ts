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
  return request.post<ApiResult<PayResult>>('/api/pay', data)
}

export function getPayChannels() {
  return request.get<ApiResult<PayChannelStatus[]>>('/api/pay/channels')
}

export function getPayStatus(payNo: string) {
  return request.get<ApiResult<PayStatus>>(`/api/pay/${encodeURIComponent(payNo)}/status`)
}
