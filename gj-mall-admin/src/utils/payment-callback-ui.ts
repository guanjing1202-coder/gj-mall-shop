export interface PaymentCallbackReplayState {
  processStatus?: number
  signatureStatus?: number
}

export function canReplayPaymentCallback(callback?: PaymentCallbackReplayState) {
  return callback?.processStatus === 3 && callback.signatureStatus !== 2
}

export function paymentCallbackReplayReason(callback?: PaymentCallbackReplayState) {
  if (!callback) {
    return '回调记录不存在'
  }
  if (callback.signatureStatus === 2) {
    return '验签失败回调不可重放'
  }
  if (callback.processStatus !== 3) {
    return '仅处理失败的回调可重放'
  }
  return ''
}
