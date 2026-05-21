import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from './product'

export interface UserMessage {
  id: ApiId
  type?: string
  typeDesc?: string
  title?: string
  content?: string
  bizType?: string
  bizId?: ApiId
  bizNo?: string
  readStatus?: number
  readTime?: string
  createTime?: string
}

export interface MessageTypeSummary {
  type: string
  typeDesc: string
  total: number
  unreadTotal: number
  readTotal: number
}

export interface MessageSummary {
  total: number
  unreadTotal: number
  readTotal: number
  typeItems: MessageTypeSummary[]
}

export interface MessageQuery {
  current?: number
  size?: number
  readStatus?: number
  type?: string
}

export function getMessagePage(params: MessageQuery) {
  return request.get<ApiResult<PageResult<UserMessage>>>('/api/user/message/page', { params })
}

export function getUnreadMessageCount() {
  return request.get<ApiResult<number>>('/api/user/message/unread-count')
}

export function getMessageSummary() {
  return request.get<ApiResult<MessageSummary>>('/api/user/message/summary')
}

export function markMessageRead(id: ApiId) {
  return request.post<ApiResult<void>>(`/api/user/message/${id}/read`)
}

export function markAllMessagesRead() {
  return request.post<ApiResult<void>>('/api/user/message/read-all')
}

export function clearReadMessages() {
  return request.delete<ApiResult<void>>('/api/user/message/read')
}

export function deleteMessage(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/user/message/${id}`)
}
