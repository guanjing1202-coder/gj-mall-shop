import request from '@/utils/request'
import type { ApiResult } from './product'

export interface UploadFileResult {
  url: string
  fileName: string
  originalName?: string
  contentType?: string
  size?: number
}

export function uploadImage(file: File, scene = 'common') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)
  return request.post<ApiResult<UploadFileResult>>('/api/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
