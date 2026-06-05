export interface PaymentActionState {
  status?: number
  syncStatusAllowed?: boolean
  syncStatusReason?: string
  syncCallbackNo?: string
  syncThirdPayNo?: string
  orderStatus?: number
  refundAllowed?: boolean
  refundReason?: string
  refundRecord?: RefundActionState
}

export interface RefundActionState {
  id?: string | number
  status?: number
  reason?: string
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

export function canRetryRefund(record?: PaymentActionState) {
  const status = record?.refundRecord?.status
  return status === 0 || status === 2
}

export function canMarkRefundFailed(record?: PaymentActionState) {
  return record?.refundRecord?.status === 0
}

export function refundFailureHint(refund?: RefundActionState) {
  if (refund?.status !== 2) {
    return ''
  }
  return refund.reason ? `退款失败：${refund.reason}` : '退款失败，请检查渠道返回结果后重试'
}

export function isFullRefundAmount(inputAmount?: number, paymentAmount?: number) {
  if (inputAmount === undefined || inputAmount === null || paymentAmount === undefined || paymentAmount === null) {
    return false
  }
  return Math.round(Number(inputAmount) * 100) === Math.round(Number(paymentAmount) * 100)
}
