import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { canCancelAfterSale, canSubmitReturnLogistics, statusBadgeClass } from '../after-sale-ui.ts'

describe('after-sale-ui', () => {
  it('allows cancel only while an after-sale request is still active', () => {
    assert.equal(canCancelAfterSale(0), true)
    assert.equal(canCancelAfterSale(1), true)
    assert.equal(canCancelAfterSale(2), true)
    assert.equal(canCancelAfterSale(3), false)
    assert.equal(canCancelAfterSale(4), false)
    assert.equal(canCancelAfterSale(5), false)
  })

  it('allows return logistics only for wait-return status', () => {
    assert.equal(canSubmitReturnLogistics(1), true)
    assert.equal(canSubmitReturnLogistics(0), false)
    assert.equal(canSubmitReturnLogistics(2), false)
  })

  it('maps statuses to stable badge classes', () => {
    assert.equal(statusBadgeClass(0), 'processing')
    assert.equal(statusBadgeClass(1), 'processing')
    assert.equal(statusBadgeClass(2), 'processing')
    assert.equal(statusBadgeClass(4), 'done')
    assert.equal(statusBadgeClass(3), 'closed')
    assert.equal(statusBadgeClass(5), 'closed')
    assert.equal(statusBadgeClass(undefined), 'neutral')
  })
})
