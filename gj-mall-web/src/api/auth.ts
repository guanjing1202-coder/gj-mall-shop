import request from '@/utils/request'
import type { ApiResult, ApiId } from './product'

export interface UserProfile {
  id: ApiId
  username: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
  gender?: number
}

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  phone?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}

export function login(data: LoginPayload) {
  return request.post<ApiResult<LoginResult>>('/api/user/auth/login', data)
}

export function register(data: RegisterPayload) {
  return request.post<ApiResult<LoginResult>>('/api/user/auth/register', data)
}

export function logout() {
  return request.post<ApiResult<void>>('/api/user/auth/logout')
}

export function getProfile() {
  return request.get<ApiResult<UserProfile>>('/api/user/me')
}
