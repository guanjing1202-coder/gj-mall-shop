export interface PaymentCallbackReplayState {
  processStatus?: number
  signatureStatus?: number
}

export interface PaymentCallbackSignatureSample {
  canonicalPayload?: string
  signatureHeader?: string
  signature?: string
}

export interface PaymentCallbackInspectState {
  channelDesc?: string
  channelAppId?: string
  channelOrderNo?: string
  channelTradeNo?: string
  channelNotifyId?: string
  channelTradeStatus?: string
  channelAmount?: string
  validationSummary?: string
}

export interface PaymentCallbackInspectItem {
  label: string
  value: string
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

export function paymentCallbackProcessLabel(status?: number, fallback?: string) {
  if (status === 0) {
    return '已接收'
  }
  if (status === 1) {
    return '已处理'
  }
  if (status === 2) {
    return '已忽略'
  }
  if (status === 3) {
    return '处理失败'
  }
  return fallback || '--'
}

export function formatPaymentCallbackInspectItems(callback?: PaymentCallbackInspectState) {
  if (!callback) {
    return []
  }
  const items: Array<{ label: string; value?: string }> = [
    { label: '渠道', value: callback.channelDesc },
    { label: 'AppID', value: callback.channelAppId },
    { label: '商户单号', value: callback.channelOrderNo },
    { label: '渠道交易号', value: callback.channelTradeNo },
    { label: '通知ID', value: callback.channelNotifyId },
    { label: '交易状态', value: callback.channelTradeStatus },
    { label: '通知金额', value: callback.channelAmount },
    { label: '校验摘要', value: callback.validationSummary },
  ]
  return items.reduce<PaymentCallbackInspectItem[]>((result, item) => {
    const value = item.value?.trim()
    if (value) {
      result.push({ label: item.label, value })
    }
    return result
  }, [])
}
