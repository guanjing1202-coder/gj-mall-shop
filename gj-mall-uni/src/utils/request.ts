/**
 * 统一请求封装（uni.request）
 */
import { clearLoginState, getAccessToken, getRefreshToken, isTokenExpired, saveLoginTokens } from './auth'

// #ifdef H5
const BASE_URL = '' // dev 走 vite 代理；生产改为后端地址
// #endif
// #ifndef H5
const BASE_URL = 'http://127.0.0.1:8080'
// #endif

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
}

export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

let refreshing: Promise<string | null> | null = null
let lastExpiredNoticeAt = 0

export function request<T = any>(opts: RequestOptions): Promise<ApiResult<T>> {
  return doRequest<T>(opts, true)
}

export async function ensureSession(options: { showToast?: boolean; redirect?: boolean } = {}): Promise<boolean> {
  const token = getAccessToken()
  if (token && !isTokenExpired(token)) return true

  const refreshToken = getRefreshToken()
  if (!refreshToken || isTokenExpired(refreshToken, 0)) {
    handleAuthExpired(options)
    return false
  }

  const newToken = await refreshAccessToken(options)
  return Boolean(newToken)
}

function doRequest<T = any>(opts: RequestOptions, allowRefresh: boolean): Promise<ApiResult<T>> {
  return new Promise((resolve, reject) => {
    const token = getAccessToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-Client-Type': getClientType(),
      ...(opts.header || {}),
    }
    if (token) header['Authorization'] = `Bearer ${token}`

    uni.request({
      url: BASE_URL + opts.url,
      method: opts.method || 'GET',
      data: opts.data,
      header,
      success: (res) => {
        const body = res.data as ApiResult<T>
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (body && body.code === 200) {
            resolve(body)
          } else if (allowRefresh && shouldRefresh(body)) {
            refreshAccessToken({ showToast: true, redirect: true })
              .then((newToken) => {
                if (!newToken) {
                  reject(body)
                  return
                }
                doRequest<T>(opts, false).then(resolve).catch(reject)
              })
              .catch((error) => {
                reject(error)
              })
          } else {
            uni.showToast({ title: body?.message || '请求失败', icon: 'none' })
            reject(body)
          }
        } else {
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      },
    })
  })
}

function shouldRefresh(body?: ApiResult<any>) {
  return body?.code === 2001 || body?.code === 2006 || body?.code === 2007
}

function refreshAccessToken(options: { showToast?: boolean; redirect?: boolean } = {}): Promise<string | null> {
  if (refreshing) return refreshing
  const refreshToken = getRefreshToken()
  if (!refreshToken || isTokenExpired(refreshToken, 0)) {
    handleAuthExpired(options)
    return Promise.resolve(null)
  }
  refreshing = new Promise((resolve) => {
    uni.request({
      url: `${BASE_URL}/api/user/auth/refresh?refreshToken=${encodeURIComponent(refreshToken)}`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'X-Client-Type': getClientType(),
      },
      success: (res) => {
        const body = res.data as ApiResult<{ accessToken: string; refreshToken: string }>
        if (res.statusCode >= 200 && res.statusCode < 300 && body?.code === 200 && body.data?.accessToken) {
          saveLoginTokens(body.data.accessToken, body.data.refreshToken)
          resolve(body.data.accessToken)
        } else {
          handleAuthExpired(options)
          resolve(null)
        }
      },
      fail: () => {
        handleAuthExpired(options)
        resolve(null)
      },
      complete: () => {
        refreshing = null
      },
    })
  })
  return refreshing
}

function handleAuthExpired(options: { showToast?: boolean; redirect?: boolean } = {}) {
  const showToast = options.showToast !== false
  const redirect = options.redirect !== false
  clearLoginState()
  if (showToast) {
    const now = Date.now()
    if (now - lastExpiredNoticeAt > 1500) {
      lastExpiredNoticeAt = now
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
    }
  }
  if (redirect) {
    setTimeout(() => {
      uni.switchTab({ url: '/pages/user/user' })
    }, 500)
  }
}

function getClientType(): string {
  // #ifdef H5
  return 'h5'
  // #endif
  // #ifdef MP-WEIXIN
  return 'mp-weixin'
  // #endif
  // #ifdef APP-PLUS
  return 'app'
  // #endif
  return 'unknown'
}
