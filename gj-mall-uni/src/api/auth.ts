import { request } from '@/utils/request'
import type { ApiId } from './product'

export interface UserProfile {
  id: ApiId
  username: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
  gender?: number
}

export interface UpdateProfilePayload {
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
  gender?: number
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}

export function login(account: string, password: string) {
  return request<LoginResult>({
    url: '/api/user/auth/login',
    method: 'POST',
    data: { account, password },
  })
}

export function getProfile() {
  return request<UserProfile>({ url: '/api/user/me', method: 'GET' })
}

export function updateProfile(data: UpdateProfilePayload) {
  return request<void>({ url: '/api/user/me', method: 'PUT', data })
}

export function logout() {
  return request<void>({ url: '/api/user/auth/logout', method: 'POST' })
}
