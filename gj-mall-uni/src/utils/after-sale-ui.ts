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

export interface AfterSaleRefundLike {
  refundNo?: string
  status?: number
  channelDesc?: string
  statusDesc?: string
  amount?: number
  reason?: string
  successTime?: string
  createTime?: string
}

function formatRefundAmount(amount?: number) {
  if (amount === undefined || amount === null || Number.isNaN(Number(amount))) return ''
  return `¥${Number(amount).toFixed(2)}`
}

export function refundRecordTitle(refund?: AfterSaleRefundLike): string {
  if (!refund?.refundNo) return ''
  return `${refund.statusDesc || '退款记录'} ${refund.refundNo}`
}

export function refundRecordMeta(refund?: AfterSaleRefundLike): string {
  if (!refund?.refundNo) return ''
  return [
    formatRefundAmount(refund.amount),
    refund.channelDesc,
    refund.successTime || refund.createTime,
    refund.reason ? `原因：${refund.reason}` : '',
  ].filter(Boolean).join(' · ')
}

export function refundRecordNotice(refund?: AfterSaleRefundLike): string {
  if (!refund?.refundNo) return ''
  if (Number(refund.status) === 0) return '退款正在处理中，请等待商家或支付渠道确认。'
  if (Number(refund.status) === 2) {
    return refund.reason
      ? `退款失败：${refund.reason}。商家会重新处理，请留意售后进度。`
      : '退款失败，商家会重新处理，请留意售后进度。'
  }
  return ''
}
