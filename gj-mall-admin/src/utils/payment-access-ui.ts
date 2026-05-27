export interface PaymentAccessState {
  mode?: string
  ready?: boolean
  readinessText?: string
  readinessTips?: string[]
}

export interface PaymentAccessTipItem {
  text: string
  configKey?: string
}

export function paymentAccessTone(access?: PaymentAccessState) {
  if (!access) {
    return 'muted'
  }
  if (access.mode === 'real') {
    return access.ready ? 'ready' : 'danger'
  }
  return access.ready === false ? 'warning' : 'mock'
}

export function paymentAccessTitle(access?: PaymentAccessState) {
  if (!access) {
    return '支付接入状态'
  }
  if (access.readinessText) {
    return access.readinessText
  }
  return access.mode === 'real' ? '真实支付配置已就绪' : 'Mock 支付模式'
}

export function paymentAccessTips(access?: PaymentAccessState) {
  return (access?.readinessTips || []).filter(Boolean)
}

export function paymentAccessTipItems(access?: PaymentAccessState): PaymentAccessTipItem[] {
  return paymentAccessTips(access).map((text) => ({
    text,
    configKey: extractConfigKey(text),
  }))
}

export function paymentConfigRouteQuery(tip?: PaymentAccessTipItem | string) {
  const configKey = typeof tip === 'string' ? extractConfigKey(tip) : tip?.configKey
  return {
    groupCode: 'payment',
    keyword: configKey || undefined,
  }
}

function extractConfigKey(text?: string) {
  const match = String(text || '').match(/mall\.pay\.[A-Za-z0-9_.-]+/)
  return match?.[0]
}
