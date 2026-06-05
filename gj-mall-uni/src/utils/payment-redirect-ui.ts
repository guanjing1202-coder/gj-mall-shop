export interface PaymentRedirectInput {
  paid?: boolean
  payInfo?: string
}

export type PaymentRedirectAction =
  | { kind: 'none' }
  | { kind: 'url'; target: string }
  | { kind: 'html-form'; html: string }
  | { kind: 'info'; text: string }

export function resolvePaymentRedirect(input: PaymentRedirectInput): PaymentRedirectAction {
  if (input.paid) {
    return { kind: 'none' }
  }
  const payInfo = input.payInfo?.trim()
  if (!payInfo) {
    return { kind: 'none' }
  }
  if (/<form[\s>]/i.test(payInfo)) {
    return { kind: 'html-form', html: payInfo }
  }
  if (/^(https?:\/\/|alipay:\/\/|weixin:\/\/|weixin:\/\/)/i.test(payInfo)) {
    return { kind: 'url', target: payInfo }
  }
  return { kind: 'info', text: payInfo }
}

export function paymentRedirectTakesOverCurrentPage(action: PaymentRedirectAction): boolean {
  return action.kind === 'url' || action.kind === 'html-form'
}
