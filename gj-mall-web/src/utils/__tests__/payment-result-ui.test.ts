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
    assert.equal(state.primaryAction, 'refresh')
  })

  it('lets the paid order status win over a stale failed query', () => {
    const state = resolvePaymentResultState({
      queryStatus: 'fail',
      orderStatus: 1,
      orderStatusDesc: '待发货',
      message: '支付网关返回失败',
      hasOrderId: true,
    })

    assert.equal(state.kind, 'success')
    assert.equal(state.title, '支付成功')
    assert.equal(state.canRetryPay, false)
    assert.equal(state.messageText, '')
    assert.match(state.subtitle, /待发货/)
  })

  it('treats closed orders as failed and hides retry payment', () => {
    const state = resolvePaymentResultState({
      queryStatus: 'pending',
      orderStatus: 4,
      orderStatusDesc: '已取消',
      hasOrderId: true,
    })

    assert.equal(state.kind, 'fail')
    assert.equal(state.title, '订单已关闭')
    assert.equal(state.canRetryPay, false)
    assert.equal(state.shouldPoll, false)
  })

  it('defaults unknown direct visits to a pending confirmation state', () => {
    const state = resolvePaymentResultState({})

    assert.equal(state.kind, 'pending')
    assert.equal(state.title, '等待支付确认')
    assert.equal(state.canRetryPay, false)
    assert.equal(state.primaryText, '我的订单')
  })

  it('keeps a failed gateway message only on failed result states', () => {
    const state = resolvePaymentResultState({
      queryStatus: 'fail',
      message: '支付网关返回失败',
      hasOrderId: true,
    })

    assert.equal(state.kind, 'fail')
    assert.equal(state.primaryText, '查看订单')
    assert.equal(state.messageText, '支付网关返回失败')
  })

  it('treats payNo-only result links as status lookup capable', () => {
    assert.equal(hasPaymentResultLookup({ payNo: 'P202605210001' }), true)
    assert.equal(hasPaymentResultLookup({ orderId: '30' }), true)
    assert.equal(hasPaymentResultLookup({ payNo: '   ' }), false)
  })
})
