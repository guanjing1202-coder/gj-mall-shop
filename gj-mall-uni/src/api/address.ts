import { request } from '@/utils/request'
import type { ApiId } from './product'

export interface AddressItem {
  id: ApiId
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  postCode?: string
  isDefault?: number
}

export interface AddressPayload {
  id?: ApiId
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  postCode?: string
  isDefault?: number
}

export function getAddressList() {
  return request<AddressItem[]>({ url: '/api/user/address', method: 'GET' })
}

export function addAddress(data: AddressPayload) {
  return request<ApiId>({ url: '/api/user/address', method: 'POST', data })
}

export function updateAddress(data: AddressPayload) {
  return request<void>({ url: '/api/user/address', method: 'PUT', data })
}

export function deleteAddress(id: ApiId) {
  return request<void>({ url: `/api/user/address/${id}`, method: 'DELETE' })
}

export function setDefaultAddress(id: ApiId) {
  return request<void>({ url: `/api/user/address/${id}/default`, method: 'PUT' })
}
