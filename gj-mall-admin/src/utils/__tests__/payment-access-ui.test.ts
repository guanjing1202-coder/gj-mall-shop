import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  paymentAccessTipItems,
  paymentAccessTips,
  paymentAccessTitle,
  paymentAccessTone,
  paymentConfigRouteQuery,
} from '../payment-access-ui.ts'

describe('payment-access-ui', () => {
  it('marks real mode with missing config as dangerous', () => {
    const access = {
      mode: 'real',
      ready: false,
      readinessText: '真实支付配置未就绪',
      readinessTips: ['请配置 mall.pay.callback.secret'],
    }

    assert.equal(paymentAccessTone(access), 'danger')
    assert.equal(paymentAccessTitle(access), '真实支付配置未就绪')
    assert.deepEqual(paymentAccessTips(access), ['请配置 mall.pay.callback.secret'])
    assert.deepEqual(paymentAccessTipItems(access), [
      { text: '请配置 mall.pay.callback.secret', configKey: 'mall.pay.callback.secret' },
    ])
  })

  it('keeps mock mode visually separate from real readiness', () => {
    assert.equal(paymentAccessTone({ mode: 'mock', ready: true }), 'mock')
    assert.equal(paymentAccessTitle({ mode: 'mock', ready: true }), 'Mock 支付模式')
    assert.deepEqual(paymentAccessTips({ mode: 'mock', ready: true }), [])
  })

  it('builds a system config route query from a readiness tip', () => {
    assert.deepEqual(paymentConfigRouteQuery('请配置 mall.pay.wechat.app-id'), {
      groupCode: 'payment',
      keyword: 'mall.pay.wechat.app-id',
    })
  })
})
