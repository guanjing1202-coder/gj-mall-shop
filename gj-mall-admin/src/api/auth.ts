import request from '@/utils/request'

export type ApiId = string | number

export interface AdminLoginParams {
  username: string
  password: string
}

export interface AdminUser {
  id: ApiId
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
}

export interface AdminLoginData {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: AdminUser
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string | number
}

export function login(data: AdminLoginParams) {
  return request.post<ApiResult<AdminLoginData>>('/api/admin/auth/login', data)
}

export function logout() {
  return request.post<ApiResult<void>>('/api/admin/auth/logout')
}
