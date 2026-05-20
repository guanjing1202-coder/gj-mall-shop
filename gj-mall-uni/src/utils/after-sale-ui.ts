export const AFTER_SALE_STATUS = {
  pending: 0,
  waitReturn: 1,
  waitRefund: 2,
  rejected: 3,
  completed: 4,
  canceled: 5,
} as const

const ACTIVE_STATUSES = [
  AFTER_SALE_STATUS.pending,
  AFTER_SALE_STATUS.waitReturn,
  AFTER_SALE_STATUS.waitRefund,
]

export function canCancelAfterSale(status?: number) {
  return ACTIVE_STATUSES.includes(Number(status))
}

export function canSubmitReturnLogistics(status?: number) {
  return Number(status) === AFTER_SALE_STATUS.waitReturn
}

export function statusBadgeClass(status?: number) {
  const code = Number(status)
  if (ACTIVE_STATUSES.includes(code)) return 'processing'
  if (code === AFTER_SALE_STATUS.completed) return 'done'
  if (code === AFTER_SALE_STATUS.rejected || code === AFTER_SALE_STATUS.canceled) return 'closed'
  return 'neutral'
}
