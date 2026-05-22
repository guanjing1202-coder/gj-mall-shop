export interface PaymentActionState {
  status?: number
  refundRecord?: unknown
}

export function canSyncPaymentStatus(record?: PaymentActionState) {
  return record?.status === 0 || record?.status === 2
}
