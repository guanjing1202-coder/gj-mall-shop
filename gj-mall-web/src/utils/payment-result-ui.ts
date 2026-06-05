export type PaymentResultKind = 'success' | 'pending' | 'fail'
export type PaymentPrimaryAction = 'order' | 'refresh' | 'orders'

export interface PaymentResultInput {
  queryStatus?: string
  orderStatus?: number | string | null
  orderStatusDesc?: string
  message?: string
  hasOrderId?: boolean
}

export interface PaymentResultLookupInput {
  orderId?: number | string | null
  payNo?: string | null
}

export interface PaymentResultState {
  kind: PaymentResultKind
  iconText: string
  title: string
  subtitle: string
  badgeText: string
  messageText: string
  canRetryPay: boolean
  shouldPoll: boolean
  primaryAction: PaymentPrimaryAction
  primaryText: string
}

const paidOrderStatuses = new Set([1, 2, 3, 5, 6])
const closedOrderStatuses = new Set([4])

export function hasPaymentResultLookup(input: PaymentResultLookupInput) {
  return Boolean(String(input.orderId || '').trim() || String(input.payNo || '').trim())
}

export function resolvePaymentResultState(input: PaymentResultInput): PaymentResultState {
  const queryStatus = normalizeQueryStatus(input.queryStatus)
  const orderStatus = normalizeOrderStatus(input.orderStatus)
  const statusDesc = input.orderStatusDesc?.trim()

  if (orderStatus !== undefined && paidOrderStatuses.has(orderStatus)) {
    return {
      kind: 'success',
      iconText: '✓',
      title: '支付成功',
      subtitle: `订单当前状态：${statusDesc || '已完成支付'}，商家会继续处理后续履约。`,
      badgeText: statusDesc || '已支付',
      messageText: '',
      canRetryPay: false,
      shouldPoll: false,
      primaryAction: 'order',
      primaryText: '查看订单',
    }
  }

  if (orderStatus !== undefined && closedOrderStatuses.has(orderStatus)) {
    return {
      kind: 'fail',
      iconText: '!',
      title: '订单已关闭',
      subtitle: statusDesc ? `订单当前状态：${statusDesc}，无法继续发起支付。` : '订单已经关闭，无法继续发起支付。',
      badgeText: statusDesc || '已关闭',
      messageText: '',
      canRetryPay: false,
      shouldPoll: false,
      primaryAction: 'order',
      primaryText: '查看订单',
    }
  }

  if (orderStatus === 0) {
    return {
      kind: 'pending',
      iconText: '…',
      title: '等待支付确认',
      subtitle: '支付结果可能需要几秒钟同步，系统会自动刷新订单状态。',
      badgeText: statusDesc || '待付款',
      messageText: '',
      canRetryPay: true,
      shouldPoll: Boolean(input.hasOrderId),
      primaryAction: 'refresh',
      primaryText: '刷新状态',
    }
  }

  if (queryStatus === 'fail') {
    return {
      kind: 'fail',
      iconText: '!',
      title: '支付未完成',
      subtitle: input.message?.trim() || '本次支付没有完成，可以重新发起支付或稍后再试。',
      badgeText: '支付失败',
      messageText: input.message?.trim() || '',
      canRetryPay: Boolean(input.hasOrderId),
      shouldPoll: false,
      primaryAction: input.hasOrderId ? 'order' : 'orders',
      primaryText: input.hasOrderId ? '查看订单' : '我的订单',
    }
  }

  if (queryStatus === 'success') {
    return {
      kind: 'success',
      iconText: '✓',
      title: '支付成功',
      subtitle: '订单已经进入待发货，商家会尽快处理。',
      badgeText: '已支付',
      messageText: '',
      canRetryPay: false,
      shouldPoll: false,
      primaryAction: input.hasOrderId ? 'order' : 'orders',
      primaryText: input.hasOrderId ? '查看订单' : '我的订单',
    }
  }

  return {
    kind: 'pending',
    iconText: '…',
    title: '等待支付确认',
    subtitle: input.hasOrderId ? '正在确认支付结果，请稍候刷新订单状态。' : '请回到订单列表查看最新支付状态。',
    badgeText: '确认中',
    messageText: '',
    canRetryPay: Boolean(input.hasOrderId && queryStatus === 'pending'),
    shouldPoll: Boolean(input.hasOrderId),
    primaryAction: input.hasOrderId ? 'refresh' : 'orders',
    primaryText: input.hasOrderId ? '刷新状态' : '我的订单',
  }
}

function normalizeQueryStatus(status?: string) {
  const value = String(status || '').trim().toLowerCase()
  return value === 'success' || value === 'pending' || value === 'fail' ? value : undefined
}

function normalizeOrderStatus(status?: number | string | null) {
  if (status === undefined || status === null || status === '') {
    return undefined
  }
  const value = Number(status)
  return Number.isFinite(value) ? value : undefined
}
