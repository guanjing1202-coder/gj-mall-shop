export interface PaymentCallbackReplayState {
  processStatus?: number
  signatureStatus?: number
}

export interface PaymentCallbackSignatureSample {
  canonicalPayload?: string
  signatureHeader?: string
  signature?: string
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

export function formatCallbackSignatureSample(sample?: PaymentCallbackSignatureSample) {
  const lines: string[] = []
  const payload = sample?.canonicalPayload?.trim()
  const header = sample?.signatureHeader?.trim()
  const signature = sample?.signature?.trim()
  if (payload) {
    lines.push(`签名原文：${payload}`)
  }
  if (header && signature) {
    lines.push(`请求头：${header}: ${signature}`)
  }
  return lines
}
