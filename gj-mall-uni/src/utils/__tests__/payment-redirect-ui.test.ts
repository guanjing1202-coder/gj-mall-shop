import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { paymentRedirectTakesOverCurrentPage, resolvePaymentRedirect } from '../payment-redirect-ui.ts'

describe('payment-redirect-ui', () => {
  it('opens url payInfo externally when payment is pending', () => {
    const action = resolvePaymentRedirect({ paid: false, payInfo: 'alipay://pay?order=P1' })

    assert.equal(action.kind, 'url')
    assert.equal(action.target, 'alipay://pay?order=P1')
  })

  it('keeps html payInfo for an intermediate bridge page', () => {
    const action = resolvePaymentRedirect({ paid: false, payInfo: '<form id="alipaySubmit"></form>' })

    assert.equal(action.kind, 'html-form')
  })

  it('does not redirect when the payment has completed immediately', () => {
    const action = resolvePaymentRedirect({ paid: true, payInfo: 'mock://paid?orderNo=1' })

    assert.equal(action.kind, 'none')
  })

  it('marks external payment bridges as current-page navigation on H5', () => {
    assert.equal(paymentRedirectTakesOverCurrentPage({ kind: 'url', target: 'https://pay.example.com' }), true)
    assert.equal(paymentRedirectTakesOverCurrentPage({ kind: 'html-form', html: '<form></form>' }), true)
    assert.equal(paymentRedirectTakesOverCurrentPage({ kind: 'info', text: 'wait for cashier' }), false)
    assert.equal(paymentRedirectTakesOverCurrentPage({ kind: 'none' }), false)
  })
})
