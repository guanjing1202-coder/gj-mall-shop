import request from '@/utils/request'

export type ApiId = string | number

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string | number
}

export interface PageResult<T> {
  total: string | number
  pageNum: string | number
  pageSize: string | number
  list: T[]
}

export interface MemberQuery {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface MemberAddress {
  id: ApiId
  userId: ApiId
  receiver?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  detail?: string
  postCode?: string
  isDefault?: number
  createTime?: string
  updateTime?: string
}

export interface MemberRecord {
  id: ApiId
  username: string
  phone?: string
  email?: string
  nickname?: string
  avatar?: string
  gender?: number
  birthday?: string
  status: number
  lastLoginAt?: string
  lastLoginIp?: string
  createTime?: string
  updateTime?: string
  addressCount?: number
  orderCount?: number
  paidAmount?: number
  addresses?: MemberAddress[]
}

export function getMemberPage(params: MemberQuery) {
  return request.get<ApiResult<PageResult<MemberRecord>>>('/api/admin/member/page', { params })
}

export function getMemberDetail(id: ApiId) {
  return request.get<ApiResult<MemberRecord>>(`/api/admin/member/${id}`)
}

export function updateMemberStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/member/${id}/status`, null, {
    params: { status },
  })
}

export function deleteMember(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/member/${id}`)
}
