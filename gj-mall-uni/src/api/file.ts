import { getAccessToken } from '@/utils/auth'
import { ensureSession } from '@/utils/request'

const MAX_IMAGE_SIZE = 10 * 1024 * 1024

// #ifdef H5
const BASE_URL = ''
// #endif
// #ifndef H5
const BASE_URL = 'http://127.0.0.1:8080'
// #endif

export interface UploadFileResult {
  url: string
  fileName: string
  originalName?: string
  contentType?: string
  size?: number
}

export async function uploadImage(filePath: string, scene = 'common'): Promise<UploadFileResult> {
  await validateLocalImage(filePath)
  const sessionReady = await ensureSession({ showToast: true, redirect: true })
  if (!sessionReady) {
    throw new Error('登录已过期')
  }
  return new Promise((resolve, reject) => {
    const token = getAccessToken()
    uni.uploadFile({
      url: `${BASE_URL}/api/file/upload`,
      filePath,
      name: 'file',
      formData: { scene },
      header: {
        'X-Client-Type': 'h5',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (res) => {
        let body: any = res.data
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        } catch (error) {
          reject(new Error('上传响应解析失败'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body?.code === 200 && body.data?.url) {
          resolve(body.data)
          return
        }
        reject(body || new Error('上传失败'))
      },
      fail: reject,
    })
  })
}

function validateLocalImage(filePath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    resolve()
    // #endif
    // #ifndef H5
    uni.getFileInfo({
      filePath,
      success: (info) => {
        if (Number(info.size || 0) > MAX_IMAGE_SIZE) {
          reject(new Error('图片不能超过 10MB'))
          return
        }
        resolve()
      },
      fail: () => resolve(),
    })
    // #endif
  })
}
