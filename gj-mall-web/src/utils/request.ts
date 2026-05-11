import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const service: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 15000,
})

const authExpiredCodes = new Set([2001, 2006, 2007])

function clearExpiredSession() {
  localStorage.removeItem('mall_token')
  localStorage.removeItem('mall_refresh_token')
  localStorage.removeItem('mall_user')
  window.dispatchEvent(new Event('mall-auth-expired'))
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
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 200) {
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
