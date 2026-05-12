import { request } from '@/utils/request'
import type { ApiId } from './product'

export interface SeckillActivity {
  id: ApiId
  name: string
  startTime?: string
  endTime?: string
  status?: number
  statusDesc?: string
  skus?: SeckillSku[]
}

export interface SeckillSku {
  id: ApiId
  seckillId: ApiId
  spuId: ApiId
  skuId: ApiId
  spuName?: string
  skuName?: string
  image?: string
  specData?: Record<string, string>
  originalPrice?: number
  seckillPrice?: number
  remainStock?: number
  seckillLimit?: number
  soldCount?: number
}

export function getActiveSeckills() {
  return request<SeckillActivity[]>({ url: '/api/seckill/active', method: 'GET' })
}

export function getSeckillDetail(seckillId: ApiId) {
  return request<SeckillActivity>({ url: `/api/seckill/${seckillId}`, method: 'GET' })
}

export function createSeckillOrder(seckillSkuId: ApiId, addressId: ApiId) {
  return request<string>({
    url: `/api/seckill/${seckillSkuId}/order?addressId=${encodeURIComponent(String(addressId))}`,
    method: 'POST',
  })
}
