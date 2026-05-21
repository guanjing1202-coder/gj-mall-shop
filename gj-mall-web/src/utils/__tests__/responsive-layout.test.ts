import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { mobileOrderBreakpoints } from '../responsive-layout.ts'

describe('responsive-layout', () => {
  it('keeps the shop header compact on narrow phones', () => {
    assert.deepEqual(mobileOrderBreakpoints.headerRows, ['brand-actions', 'nav'])
    assert.equal(mobileOrderBreakpoints.maxPhoneHeaderHeight, 136)
  })

  it('uses a single column order detail layout on mobile', () => {
    assert.equal(mobileOrderBreakpoints.orderDetailColumns, 1)
    assert.equal(mobileOrderBreakpoints.orderItemColumns, 2)
  })
})
