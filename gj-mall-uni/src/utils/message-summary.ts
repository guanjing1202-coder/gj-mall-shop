export interface RawMessageTypeSummary {
  type?: string
  typeDesc?: string
  total?: number | string
  unreadTotal?: number | string
  readTotal?: number | string
}

export interface RawMessageSummary {
  total?: number | string
  unreadTotal?: number | string
  readTotal?: number | string
  typeItems?: RawMessageTypeSummary[]
}

export interface NormalizedMessageTypeSummary {
  type: string
  typeDesc: string
  total: number
  unreadTotal: number
  readTotal: number
}

export interface NormalizedMessageSummary {
  total: number
  unreadTotal: number
  readTotal: number
  typeItems: NormalizedMessageTypeSummary[]
}

export function emptyMessageSummary(): NormalizedMessageSummary {
  return {
    total: 0,
    unreadTotal: 0,
    readTotal: 0,
    typeItems: [],
  }
}

export function normalizeMessageSummary(summary?: RawMessageSummary | null): NormalizedMessageSummary {
  if (!summary) {
    return emptyMessageSummary()
  }
  return {
    total: toNumber(summary.total),
    unreadTotal: toNumber(summary.unreadTotal),
    readTotal: toNumber(summary.readTotal),
    typeItems: (summary.typeItems || []).map((item) => ({
      type: item.type || 'system',
      typeDesc: item.typeDesc || '系统通知',
      total: toNumber(item.total),
      unreadTotal: toNumber(item.unreadTotal),
      readTotal: toNumber(item.readTotal),
    })),
  }
}

export function messageTypeFilters(summary: NormalizedMessageSummary) {
  return [
    {
      type: undefined,
      typeDesc: '全部',
      total: summary.total,
      unreadTotal: summary.unreadTotal,
      readTotal: summary.readTotal,
    },
    ...summary.typeItems,
  ]
}

export interface MessageNavigationLike {
  bizType?: string
  bizId?: number | string
}

export interface MessagePreviewLike extends MessageNavigationLike {
  title?: string
  content?: string
}

export interface MessagePreviewVO {
  title: string
  content: string
  emphasis: string
  tone: 'default' | 'info' | 'danger'
}

export function messageNavigationTarget(message: MessageNavigationLike): string {
  if (message.bizType === 'order' && message.bizId) {
    return `/pages/order/detail?id=${message.bizId}`
  }
  if (message.bizType === 'after_sale') {
    return message.bizId ? `/pages/after-sales/detail?id=${message.bizId}` : '/pages/after-sales/list'
  }
  return ''
}

export function messagePreview(message: MessagePreviewLike): MessagePreviewVO {
  const title = message.title || '消息提醒'
  const content = message.content || '-'
  if (message.bizType === 'after_sale' && /退款失败|售后失败|拒绝/i.test(title + content)) {
    return { title, content, emphasis: '售后失败', tone: 'danger' }
  }
  if (message.bizType === 'after_sale') {
    return { title, content, emphasis: '售后进度', tone: 'info' }
  }
  if (message.bizType === 'order') {
    return { title, content, emphasis: '订单通知', tone: 'default' }
  }
  return { title, content, emphasis: '系统通知', tone: 'default' }
}

function toNumber(value?: number | string): number {
  const numberValue = Number(value || 0)
  return Number.isFinite(numberValue) ? numberValue : 0
}
