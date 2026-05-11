import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

const service: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 15000,
})

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
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 200) {
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
