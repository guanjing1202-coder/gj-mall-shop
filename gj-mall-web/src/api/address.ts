import request from '@/utils/request'
import type { ApiId, ApiResult } from './product'

export interface AddressItem {
  id: ApiId
  userId?: ApiId
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  postCode?: string
  isDefault?: number
  createTime?: string
  updateTime?: string
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
  return request.get<ApiResult<AddressItem[]>>('/api/user/address')
}

export function addAddress(data: AddressPayload) {
  return request.post<ApiResult<ApiId>>('/api/user/address', data)
}

export function updateAddress(data: AddressPayload) {
  return request.put<ApiResult<void>>('/api/user/address', data)
}

export function deleteAddress(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/user/address/${id}`)
}

export function setDefaultAddress(id: ApiId) {
  return request.put<ApiResult<void>>(`/api/user/address/${id}/default`)
}
