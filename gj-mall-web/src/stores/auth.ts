import { defineStore } from 'pinia'
import {
  getProfile,
  login,
  logout,
  register,
  updateProfile,
  type UpdateProfilePayload,
  type UserProfile,
} from '@/api/auth'

function readStoredUser(): UserProfile | undefined {
  const raw = localStorage.getItem('mall_user')
  if (!raw) {
    return undefined
  }
  try {
    return JSON.parse(raw)
  } catch {
    return undefined
  }
}

export const useAuthStore = defineStore('mall-auth', {
  state: () => ({
    token: localStorage.getItem('mall_token') || '',
    refreshToken: localStorage.getItem('mall_refresh_token') || '',
    user: readStoredUser() as UserProfile | undefined,
    loginDialogOpen: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.nickname || state.user?.username || '会员',
  },
  actions: {
    openLoginDialog() {
      this.loginDialogOpen = true
    },
    closeLoginDialog() {
      this.loginDialogOpen = false
    },
    setSession(accessToken: string, refreshToken: string, user: UserProfile) {
      this.token = accessToken
      this.refreshToken = refreshToken
      this.user = user
      localStorage.setItem('mall_token', accessToken)
      localStorage.setItem('mall_refresh_token', refreshToken)
      localStorage.setItem('mall_user', JSON.stringify(user))
    },
    clearSession() {
      this.token = ''
      this.refreshToken = ''
      this.user = undefined
      localStorage.removeItem('mall_token')
      localStorage.removeItem('mall_refresh_token')
      localStorage.removeItem('mall_user')
    },
    async loginByPassword(account: string, password: string) {
      const res = await login({ account, password })
      this.setSession(res.data.accessToken, res.data.refreshToken, res.data.user)
      this.closeLoginDialog()
      return res.data
    },
    async registerAndLogin(username: string, password: string, nickname?: string, phone?: string) {
      const res = await register({ username, password, nickname, phone })
      this.setSession(res.data.accessToken, res.data.refreshToken, res.data.user)
      this.closeLoginDialog()
      return res.data
    },
    async loadProfile() {
      if (!this.token) {
        return undefined
      }
      const res = await getProfile()
      this.user = res.data
      localStorage.setItem('mall_user', JSON.stringify(res.data))
      return res.data
    },
    async updateProfile(payload: UpdateProfilePayload) {
      await updateProfile(payload)
      return this.loadProfile()
    },
    async logout() {
      try {
        if (this.token) {
          await logout()
        }
      } finally {
        this.clearSession()
      }
    },
  },
})
