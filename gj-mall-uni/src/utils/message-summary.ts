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

function toNumber(value?: number | string): number {
  const numberValue = Number(value || 0)
  return Number.isFinite(numberValue) ? numberValue : 0
}
