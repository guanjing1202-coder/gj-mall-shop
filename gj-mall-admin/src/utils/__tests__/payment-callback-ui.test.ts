import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  canReplayPaymentCallback,
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
})
