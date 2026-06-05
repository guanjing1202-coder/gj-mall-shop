import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { hasPaymentResultLookup, resolvePaymentResultState } from '../payment-result-ui.ts'

describe('payment-result-ui', () => {
  it('keeps a successful query pending until the order is paid', () => {
    const state = resolvePaymentResultState({
      queryStatus: 'success',
      orderStatus: 0,
      orderStatusDesc: '待付款',
      hasOrderId: true,
    })

    assert.equal(state.kind, 'pending')
    assert.equal(state.title, '等待支付确认')
    assert.equal(state.shouldPoll, true)
  })

  it('lets a paid order override a failed gateway query', () => {
    const state = resolvePaymentResultState({
      queryStatus: 'fail',
      orderStatus: 2,
      orderStatusDesc: '待收货',
      message: '支付失败',
      hasOrderId: true,
    })

    assert.equal(state.kind, 'success')
    assert.equal(state.title, '支付成功')
    assert.equal(state.canRetryPay, false)
    assert.equal(state.messageText, '')
    assert.match(state.subtitle, /待收货/)
  })

  it('marks canceled orders as closed without retry', () => {
    const state = resolvePaymentResultState({
      queryStatus: 'pending',
      orderStatus: 4,
      orderStatusDesc: '已取消',
      hasOrderId: true,
    })

    assert.equal(state.kind, 'fail')
    assert.equal(state.title, '订单已关闭')
    assert.equal(state.canRetryPay, false)
    assert.equal(state.messageText, '')
  })

  it('labels direct visits without an order as order list navigation', () => {
    const state = resolvePaymentResultState({})

    assert.equal(state.kind, 'pending')
    assert.equal(state.primaryText, '我的订单')
  })

  it('treats payNo-only result links as status lookup capable', () => {
    assert.equal(hasPaymentResultLookup({ payNo: 'P202605210001' }), true)
    assert.equal(hasPaymentResultLookup({ orderId: '30' }), true)
    assert.equal(hasPaymentResultLookup({ payNo: '   ' }), false)
  })
})
