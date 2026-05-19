import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const service: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 15000,
})

const authExpiredCodes = new Set([2001, 2006, 2007])
let refreshing: Promise<boolean> | null = null

function clearExpiredSession() {
  localStorage.removeItem('mall_token')
  localStorage.removeItem('mall_refresh_token')
  localStorage.removeItem('mall_user')
  window.dispatchEvent(new Event('mall-auth-expired'))
}

function saveRefreshedSession(data?: {
  accessToken?: string
  refreshToken?: string
  user?: unknown
}) {
  if (!data?.accessToken) {
    return false
  }
  localStorage.setItem('mall_token', data.accessToken)
  if (data.refreshToken) {
    localStorage.setItem('mall_refresh_token', data.refreshToken)
  }
  if (data.user) {
    localStorage.setItem('mall_user', JSON.stringify(data.user))
  }
  window.dispatchEvent(new Event('mall-auth-refreshed'))
  return true
}

function refreshSession() {
  if (refreshing) {
    return refreshing
  }
  const refreshToken = localStorage.getItem('mall_refresh_token')
  if (!refreshToken) {
    clearExpiredSession()
    return Promise.resolve(false)
  }
  refreshing = axios
    .post(
      `/api/user/auth/refresh?refreshToken=${encodeURIComponent(refreshToken)}`,
      undefined,
      {
        headers: {
          'Content-Type': 'application/json',
          'X-Client-Type': 'pc',
        },
      },
    )
    .then((resp) => {
      const body = resp.data
      if (body?.code === 200 && saveRefreshedSession(body.data)) {
        return true
      }
      clearExpiredSession()
      return false
    })
    .catch(() => {
      clearExpiredSession()
      return false
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

service.interceptors.request.use((config) => {
  config.headers = config.headers || {}
  config.headers['X-Client-Type'] = 'pc'
  const token = localStorage.getItem('mall_token')
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
        const originalConfig = resp.config as AxiosRequestConfig & { _retry?: boolean }
        if (authExpiredCodes.has(Number(body.code)) && !originalConfig._retry) {
          originalConfig._retry = true
          const refreshed = await refreshSession()
          if (refreshed) {
            return service(originalConfig)
          }
        }
        if (authExpiredCodes.has(Number(body.code))) {
          clearExpiredSession()
        }
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(body)
      }
    }
    return body
  },
  (err) => {
    if (err?.response?.status === 401) {
      clearExpiredSession()
    }
    ElMessage.error(err?.message || '网络错误')
    return Promise.reject(err)
  }
)

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
