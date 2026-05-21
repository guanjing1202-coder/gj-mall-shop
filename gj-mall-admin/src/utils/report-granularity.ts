export type ReportGranularity = 'day' | 'week' | 'month'

export const reportGranularityOptions: Array<{ label: string, value: ReportGranularity }> = [
  { label: '按日', value: 'day' },
  { label: '按周', value: 'week' },
  { label: '按月', value: 'month' },
]

export function normalizeReportGranularity(value?: string): ReportGranularity {
  const normalized = String(value || 'day').trim().toLowerCase()
  if (normalized === 'week' || normalized === 'month') {
    return normalized
  }
  return 'day'
}

export function formatReportPeriod(value?: string, granularity?: string) {
  if (!value) {
    return '--'
  }
  const normalized = normalizeReportGranularity(granularity)
  if (normalized === 'month') {
    return value.slice(0, 7)
  }
  if (normalized === 'week') {
    return `${value.slice(5)} 周`
  }
  return value.slice(5)
}

export function reportGranularityName(value?: string) {
  const normalized = normalizeReportGranularity(value)
  return reportGranularityOptions.find((item) => item.value === normalized)?.label || '按日'
}
