/**
 * 统一请求封装（uni.request）
 */

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

export function request<T = any>(opts: RequestOptions): Promise<ApiResult<T>> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('mall_token')
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
