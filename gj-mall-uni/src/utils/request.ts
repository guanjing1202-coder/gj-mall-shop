/**
 * 统一请求封装（uni.request）
 */
import { clearLoginState, getAccessToken, getRefreshToken, saveLoginTokens } from './auth'

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

export function request<T = any>(opts: RequestOptions): Promise<ApiResult<T>> {
  return doRequest<T>(opts, true)
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
            refreshAccessToken()
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

function refreshAccessToken(): Promise<string | null> {
  if (refreshing) return refreshing
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    handleAuthExpired()
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
          handleAuthExpired()
          resolve(null)
        }
      },
      fail: () => {
        handleAuthExpired()
        resolve(null)
      },
      complete: () => {
        refreshing = null
      },
    })
  })
  return refreshing
}

function handleAuthExpired() {
  clearLoginState()
  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  setTimeout(() => {
    uni.switchTab({ url: '/pages/user/user' })
  }, 500)
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
