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
  return request<PageResult<UserMessage>>({ url: '/api/user/message/page', method: 'GET', data: params })
}

export function getUnreadMessageCount() {
  return request<number>({ url: '/api/user/message/unread-count', method: 'GET' })
}

export function getMessageSummary() {
  return request<MessageSummary>({ url: '/api/user/message/summary', method: 'GET' })
}

export function markMessageRead(id: ApiId) {
  return request<void>({ url: `/api/user/message/${id}/read`, method: 'POST' })
}

export function markAllMessagesRead() {
  return request<void>({ url: '/api/user/message/read-all', method: 'POST' })
}

export function clearReadMessages() {
  return request<void>({ url: '/api/user/message/read', method: 'DELETE' })
}

export function deleteMessage(id: ApiId) {
  return request<void>({ url: `/api/user/message/${id}`, method: 'DELETE' })
}
