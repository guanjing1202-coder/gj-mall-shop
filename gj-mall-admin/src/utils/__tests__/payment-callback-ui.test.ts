import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  canReplayPaymentCallback,
  formatCallbackSignatureSample,
  paymentCallbackReplayReason,
} from '../payment-callback-ui.ts'

describe('payment-callback-ui', () => {
  it('allows replay only for process failed callbacks with valid signature', () => {
    assert.equal(canReplayPaymentCallback({ processStatus: 3, signatureStatus: 1 }), true)
    assert.equal(canReplayPaymentCallback({ processStatus: 3, signatureStatus: 0 }), true)
    assert.equal(canReplayPaymentCallback({ processStatus: 1, signatureStatus: 1 }), false)
    assert.equal(canReplayPaymentCallback({ processStatus: 3, signatureStatus: 2 }), false)
  })

  it('explains why a callback cannot be replayed', () => {
    assert.equal(paymentCallbackReplayReason({ processStatus: 3, signatureStatus: 2 }), '验签失败回调不可重放')
    assert.equal(paymentCallbackReplayReason({ processStatus: 1, signatureStatus: 1 }), '仅处理失败的回调可重放')
    assert.equal(paymentCallbackReplayReason({ processStatus: 3, signatureStatus: 1 }), '')
  })

  it('formats callback signature sample for operations panel', () => {
    const sample = formatCallbackSignatureSample({
      canonicalPayload: 'amount=99.00&payNo=P1',
      signatureHeader: 'x-gj-pay-signature',
      signature: 'abc123',
    })

    assert.deepEqual(sample, [
      '签名原文：amount=99.00&payNo=P1',
      '请求头：x-gj-pay-signature: abc123',
    ])
  })
})
