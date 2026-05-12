import { request } from '@/utils/request'
import type { ApiId } from './product'

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
  return request<CartInfo>({ url: '/api/cart', method: 'GET' })
}

export function addCart(data: AddCartPayload) {
  return request<void>({ url: '/api/cart', method: 'POST', data })
}

export function updateCart(data: UpdateCartPayload) {
  return request<void>({ url: '/api/cart', method: 'PUT', data })
}

export function removeCartItem(skuId: ApiId) {
  return request<void>({ url: `/api/cart/${skuId}`, method: 'DELETE' })
}

export function clearCart() {
  return request<void>({ url: '/api/cart', method: 'DELETE' })
}

export function selectAllCart(selected: boolean) {
  return request<void>({
    url: `/api/cart/select-all?selected=${selected ? 'true' : 'false'}`,
    method: 'PUT',
  })
}
