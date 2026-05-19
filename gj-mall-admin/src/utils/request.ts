import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

interface AdminAxiosRequestConfig extends AxiosRequestConfig {
  skipAuthRefresh?: boolean
  _retry?: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 15000,
})

const authExpiredCodes = new Set([2001, 2006, 2007])
let refreshingPromise: Promise<string | null> | null = null

service.interceptors.request.use((config) => {
  config.headers = config.headers || {}
  config.headers['X-Client-Type'] = 'admin'
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  async (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 200) {
        const originalConfig = resp.config as AdminAxiosRequestConfig
        if (
          authExpiredCodes.has(Number(body.code)) &&
          !originalConfig.skipAuthRefresh &&
          !originalConfig._retry
        ) {
          const token = await refreshAccessToken()
          if (token) {
            originalConfig._retry = true
            originalConfig.headers = originalConfig.headers || {}
            originalConfig.headers.Authorization = `Bearer ${token}`
            return service.request(originalConfig)
          }
          clearAdminSession()
          window.dispatchEvent(new CustomEvent('mall-admin-auth-expired'))
        }
        message.error(body.message || '请求失败')
        return Promise.reject(body)
      }
    }
    return body
  },
  (err) => {
    message.error(err?.message || '网络错误')
    return Promise.reject(err)
  }
)

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('admin_refresh_token')
  if (!refreshToken) {
    return null
  }
  if (!refreshingPromise) {
    refreshingPromise = service
      .post<any, any>('/api/admin/auth/refresh', undefined, {
        params: { refreshToken },
        skipAuthRefresh: true,
      } as AdminAxiosRequestConfig)
      .then((res) => {
        const data = res?.data
        if (!data?.accessToken) {
          return null
        }
        localStorage.setItem('admin_token', data.accessToken)
        if (data.refreshToken) {
          localStorage.setItem('admin_refresh_token', data.refreshToken)
        }
        if (data.user) {
          localStorage.setItem('admin_user', JSON.stringify(data.user))
        }
        localStorage.removeItem('admin_permission_codes')
        window.dispatchEvent(new CustomEvent('mall-admin-auth-refreshed'))
        return data.accessToken as string
      })
      .catch(() => null)
      .finally(() => {
        refreshingPromise = null
      })
  }
  return refreshingPromise
}

function clearAdminSession() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_refresh_token')
  localStorage.removeItem('admin_user')
  localStorage.removeItem('admin_permission_codes')
}

export default {
  get: <T = any>(url: string, config?: AxiosRequestConfig) =>
    service.get<any, T>(url, config),
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    service.post<any, T>(url, data, config),
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    service.put<any, T>(url, data, config),
  delete: <T = any>(url: string, config?: AxiosRequestConfig) =>
    service.delete<any, T>(url, config),
}
