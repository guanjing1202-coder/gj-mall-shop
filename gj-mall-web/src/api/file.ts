import request from '@/utils/request'
import type { ApiResult } from './product'

const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

export interface UploadFileResult {
  url: string
  fileName: string
  originalName?: string
  contentType?: string
  size?: number
}

export function validateImageFile(file: File) {
  if (!file) {
    throw new Error('请选择要上传的图片')
  }
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    throw new Error('仅支持 JPG、PNG、WEBP、GIF 图片')
  }
  if (file.size > MAX_IMAGE_SIZE) {
    throw new Error('图片不能超过 10MB')
  }
}

export function uploadImage(file: File, scene = 'common') {
  validateImageFile(file)
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)
  return request.post<ApiResult<UploadFileResult>>('/api/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
