import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { canSyncPaymentStatus, paymentSyncStatusHint } from '../payment-action-ui.ts'

describe('payment-action-ui', () => {
  it('allows payment status sync only for unsettled payments', () => {
    assert.equal(canSyncPaymentStatus({ status: 0 }), true)
    assert.equal(canSyncPaymentStatus({ status: 2 }), true)
    assert.equal(canSyncPaymentStatus({ status: 1 }), false)
    assert.equal(canSyncPaymentStatus({ status: 3 }), false)
  })

  it('requires backend sync eligibility when it is provided', () => {
    assert.equal(canSyncPaymentStatus({ status: 0, syncStatusAllowed: true }), true)
    assert.equal(canSyncPaymentStatus({ status: 0, syncStatusAllowed: false }), false)
    assert.equal(canSyncPaymentStatus({ status: 2, syncStatusAllowed: true }), true)
    assert.equal(canSyncPaymentStatus({ status: 1, syncStatusAllowed: true }), false)
  })

  it('formats payment sync status hint with callback basis first', () => {
    assert.equal(
      paymentSyncStatusHint({
        syncStatusReason: '发现成功回调，可同步支付状态',
        syncCallbackNo: 'CB202605240001',
        syncThirdPayNo: 'WX202605240001',
      }),
      '依据回调 CB202605240001 / WX202605240001',
    )
    assert.equal(paymentSyncStatusHint({ syncStatusReason: '未找到可入账的成功支付回调' }), '未找到可入账的成功支付回调')
  })
})
