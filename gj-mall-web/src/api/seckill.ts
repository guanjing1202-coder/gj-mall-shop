import request from '@/utils/request'
import type { ApiId, ApiResult } from './product'

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
  return request.get<ApiResult<SeckillActivity[]>>('/api/seckill/active')
}

export function getSeckillDetail(seckillId: ApiId) {
  return request.get<ApiResult<SeckillActivity>>(`/api/seckill/${seckillId}`)
}

export function createSeckillOrder(seckillSkuId: ApiId, addressId: ApiId) {
  return request.post<ApiResult<string>>(`/api/seckill/${seckillSkuId}/order`, undefined, {
    params: { addressId },
  })
}
