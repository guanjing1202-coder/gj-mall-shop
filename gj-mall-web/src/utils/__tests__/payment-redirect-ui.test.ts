import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { resolvePaymentRedirect } from '../payment-redirect-ui.ts'

describe('payment-redirect-ui', () => {
  it('opens http payInfo as an external redirect', () => {
    const action = resolvePaymentRedirect({ paid: false, payInfo: 'https://pay.example.com/order/P1' })

    assert.equal(action.kind, 'url')
    assert.equal(action.target, 'https://pay.example.com/order/P1')
  })

  it('detects alipay auto submit html form', () => {
    const action = resolvePaymentRedirect({
      paid: false,
      payInfo: '<form id="alipaySubmit" action="https://openapi-sandbox.dl.alipaydev.com/gateway.do"></form><script>document.forms[0].submit()</script>',
    })

    assert.equal(action.kind, 'html-form')
    assert.match(action.html || '', /alipaySubmit/)
  })

  it('skips external redirect when mock already paid', () => {
    const action = resolvePaymentRedirect({ paid: true, payInfo: 'mock://paid?orderNo=1' })

    assert.equal(action.kind, 'none')
  })
})
