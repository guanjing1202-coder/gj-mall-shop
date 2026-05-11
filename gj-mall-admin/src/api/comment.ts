import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from '@/api/product'

export type { ApiId } from '@/api/product'

export interface CommentQuery {
  keyword?: string
  userId?: ApiId
  spuId?: ApiId
  skuId?: ApiId
  score?: number
  status?: number
  hasImage?: boolean
  hasReply?: boolean
  pageNum?: number
  pageSize?: number
}

export interface CommentRecord {
  id: ApiId
  orderId?: ApiId
  orderNo?: string
  orderItemId?: ApiId
  userId: ApiId
  username?: string
  nickname?: string
  phone?: string
  avatar?: string
  spuId: ApiId
  spuName?: string
  subTitle?: string
  mainImage?: string
  skuId: ApiId
  skuName?: string
  skuCode?: string
  skuImage?: string
  specData?: Record<string, string>
  skuPrice?: number
  brandId?: ApiId
  brandName?: string
  categoryId?: ApiId
  categoryName?: string
  score: number
  content?: string
  images?: string[]
  status: number
  statusDesc?: string
  auditRemark?: string
  auditTime?: string
  replyContent?: string
  replyTime?: string
  createTime?: string
  updateTime?: string
}

export interface CommentActionPayload {
  auditRemark?: string
}

export interface CommentReplyPayload {
  replyContent: string
}

export function getCommentPage(params: CommentQuery) {
  return request.get<ApiResult<PageResult<CommentRecord>>>('/api/admin/product/comment/page', { params })
}

export function getCommentDetail(id: ApiId) {
  return request.get<ApiResult<CommentRecord>>(`/api/admin/product/comment/${id}`)
}

export function approveComment(id: ApiId, data: CommentActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/product/comment/${id}/approve`, data)
}

export function rejectComment(id: ApiId, data: CommentActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/product/comment/${id}/reject`, data)
}

export function hideComment(id: ApiId, data: CommentActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/product/comment/${id}/hide`, data)
}

export function showComment(id: ApiId, data: CommentActionPayload) {
  return request.put<ApiResult<void>>(`/api/admin/product/comment/${id}/show`, data)
}

export function replyComment(id: ApiId, data: CommentReplyPayload) {
  return request.put<ApiResult<void>>(`/api/admin/product/comment/${id}/reply`, data)
}

export function deleteComment(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/product/comment/${id}`)
}
