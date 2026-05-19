import { request } from '@/utils/request'
import type { ApiId, PageResult } from './product'

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

export interface MessageQuery {
  current?: number
  size?: number
  readStatus?: number
}

export function getMessagePage(params: MessageQuery) {
  return request<PageResult<UserMessage>>({ url: '/api/user/message/page', method: 'GET', data: params })
}

export function getUnreadMessageCount() {
  return request<number>({ url: '/api/user/message/unread-count', method: 'GET' })
}

export function markMessageRead(id: ApiId) {
  return request<void>({ url: `/api/user/message/${id}/read`, method: 'POST' })
}

export function markAllMessagesRead() {
  return request<void>({ url: '/api/user/message/read-all', method: 'POST' })
}

export function deleteMessage(id: ApiId) {
  return request<void>({ url: `/api/user/message/${id}`, method: 'DELETE' })
}
