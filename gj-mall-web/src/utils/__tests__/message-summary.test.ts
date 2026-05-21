import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { messageTypeFilters, normalizeMessageSummary } from '../message-summary.ts'

describe('message-summary', () => {
  it('normalizes numeric strings from api responses', () => {
    const summary = normalizeMessageSummary({
      total: '10',
      unreadTotal: '9',
      readTotal: '1',
      typeItems: [
        { type: 'order', typeDesc: '订单通知', total: '6', unreadTotal: '6', readTotal: '0' },
        { type: 'logistics', typeDesc: '物流通知', total: '0', unreadTotal: '0', readTotal: '0' },
      ],
    })

    assert.equal(summary.total, 10)
    assert.equal(summary.unreadTotal, 9)
    assert.equal(summary.readTotal, 1)
    assert.equal(summary.typeItems[0].unreadTotal, 6)
    assert.equal(summary.typeItems[1].unreadTotal, 0)
  })

  it('keeps zero unread type filters falsy for badges', () => {
    const filters = messageTypeFilters(normalizeMessageSummary({
      total: '9',
      unreadTotal: '9',
      readTotal: '0',
      typeItems: [
        { type: 'payment', typeDesc: '支付通知', total: '3', unreadTotal: '3', readTotal: '0' },
        { type: 'system', typeDesc: '系统通知', total: '0', unreadTotal: '0', readTotal: '0' },
      ],
    }))

    assert.equal(Boolean(filters[1].unreadTotal), true)
    assert.equal(Boolean(filters[2].unreadTotal), false)
  })
})
