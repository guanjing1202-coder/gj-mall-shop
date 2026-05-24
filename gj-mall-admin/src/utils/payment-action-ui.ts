export interface PaymentActionState {
  status?: number
  syncStatusAllowed?: boolean
  syncStatusReason?: string
  syncCallbackNo?: string
  syncThirdPayNo?: string
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
