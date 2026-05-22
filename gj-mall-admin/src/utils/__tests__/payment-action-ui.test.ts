import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { canSyncPaymentStatus } from '../payment-action-ui.ts'

describe('payment-action-ui', () => {
  it('allows payment status sync only for unsettled payments', () => {
    assert.equal(canSyncPaymentStatus({ status: 0 }), true)
    assert.equal(canSyncPaymentStatus({ status: 2 }), true)
    assert.equal(canSyncPaymentStatus({ status: 1 }), false)
    assert.equal(canSyncPaymentStatus({ status: 3 }), false)
  })
})
