export interface SystemConfigUiRecord {
  configKey?: string
  configValue?: string
  sensitive?: boolean
  masked?: boolean
}

export interface SystemConfigPayloadLike {
  configKey?: string
  configValue?: string
  sensitive?: boolean
  masked?: boolean
}

export interface ConfigSummaryLike {
  groupCode?: string
  totalCount?: number
  requiredCount?: number
  readyCount?: number
  enabledCount?: number
  missingCount?: number
  uninitializedCount?: number
}

export interface ConfigInitResultLike {
  createdCount?: number
  restoredCount?: number
  existingCount?: number
}

export function normalizeConfigSummaryMetrics(summary?: ConfigSummaryLike) {
  return {
    requiredCount: Number(summary?.requiredCount || summary?.totalCount || 0),
    readyCount: Number(summary?.readyCount || 0),
    enabledCount: Number(summary?.enabledCount || 0),
    missingCount: Number(summary?.missingCount || 0),
  }
}

export function canInitializePaymentConfig(summary?: ConfigSummaryLike) {
  return summary?.groupCode === 'payment' && Number(summary?.uninitializedCount || 0) > 0
}

export function formatConfigInitResult(result?: ConfigInitResultLike) {
  const created = Number(result?.createdCount || 0)
  const restored = Number(result?.restoredCount || 0)
  const existing = Number(result?.existingCount || 0)
  const parts: string[] = []
  if (created > 0) {
    parts.push(`创建 ${created} 项`)
  }
  if (restored > 0) {
    parts.push(`恢复 ${restored} 项`)
  }
  if (existing > 0) {
    parts.push(`保留 ${existing} 项`)
  }
  return parts.length ? `支付配置已初始化：${parts.join('，')}` : '支付配置已是最新'
}

export function isSensitiveConfig(record?: SystemConfigUiRecord) {
  if (record?.sensitive) {
    return true
  }
  const key = String(record?.configKey || '').toLowerCase()
  return key.startsWith('mall.pay.')
    && ['secret', 'api-v3-key', 'private-key', 'alipay-public-key'].some((token) => key.includes(token))
}

export function isMaskedConfigValue(value?: string) {
  return String(value || '').includes('***')
}

export function shouldConfirmSensitiveConfigSave(record?: SystemConfigUiRecord, payload?: SystemConfigPayloadLike) {
  return isSensitiveConfig(record || payload) && !isMaskedConfigValue(payload?.configValue)
}

export function sensitiveConfigHint(record?: SystemConfigUiRecord) {
  if (!isSensitiveConfig(record)) {
    return ''
  }
  return record?.masked
    ? '当前值已脱敏展示；不修改配置值时可直接保存，系统会保留原始密钥。'
    : '该配置属于敏感支付参数，保存后列表与详情将脱敏展示。'
}
