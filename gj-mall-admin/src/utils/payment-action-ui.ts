export interface PaymentActionState {
  status?: number
  syncStatusAllowed?: boolean
  syncStatusReason?: string
  syncCallbackNo?: string
  syncThirdPayNo?: string
  orderStatus?: number
  refundAllowed?: boolean
  refundReason?: string
  refundRecord?: unknown
}

export function canSyncPaymentStatus(record?: PaymentActionState) {
  if (!record || (record.status !== 0 && record.status !== 2)) {
    return false
  }
  return record.syncStatusAllowed ?? true
}

export function paymentSyncStatusHint(record?: PaymentActionState) {
  if (!record) {
    return ''
  }
  if (record.syncCallbackNo) {
    return `依据回调 ${record.syncCallbackNo}${record.syncThirdPayNo ? ` / ${record.syncThirdPayNo}` : ''}`
  }
  return record.syncStatusReason || ''
}

export function canRefundPayment(record?: PaymentActionState) {
  if (!record || record.status !== 1 || record.orderStatus === 6 || record.refundRecord) {
    return false
  }
  return record.refundAllowed ?? true
}

export function paymentRefundHint(record?: PaymentActionState) {
  return record?.refundReason || ''
}

export function isFullRefundAmount(inputAmount?: number, paymentAmount?: number) {
  if (inputAmount === undefined || inputAmount === null || paymentAmount === undefined || paymentAmount === null) {
    return false
  }
  return Math.round(Number(inputAmount) * 100) === Math.round(Number(paymentAmount) * 100)
}
