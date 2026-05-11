import request from '@/utils/request'
import type { ApiId, ApiResult } from './product'

export interface CartItem {
  skuId: ApiId
  spuId?: ApiId
  spuName?: string
  skuName?: string
  image?: string
  price?: number
  stock?: number
  publishStatus?: number
  invalid?: boolean
  specData?: Record<string, string>
  quantity: number
  selected: number
  totalAmount?: number
}

export interface CartInfo {
  items: CartItem[]
  totalCount: number
  selectedCount: number
  selectedAmount: number
}

export interface AddCartPayload {
  skuId: ApiId
  quantity: number
}

export interface UpdateCartPayload {
  skuId: ApiId
  quantity?: number
  selected?: number
}

export function getCart() {
  return request.get<ApiResult<CartInfo>>('/api/cart')
}

export function addCart(data: AddCartPayload) {
  return request.post<ApiResult<void>>('/api/cart', data)
}

export function updateCart(data: UpdateCartPayload) {
  return request.put<ApiResult<void>>('/api/cart', data)
}

export function removeCartItem(skuId: ApiId) {
  return request.delete<ApiResult<void>>(`/api/cart/${skuId}`)
}

export function clearCart() {
  return request.delete<ApiResult<void>>('/api/cart')
}

export function selectAllCart(selected: boolean) {
  return request.put<ApiResult<void>>('/api/cart/select-all', null, {
    params: { selected },
  })
}
