import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { messageNavigationTarget, messagePreview, messageTypeFilters, normalizeMessageSummary } from '../message-summary.ts'

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

  it('routes after-sale messages to detail when biz id is present', () => {
    assert.equal(messageNavigationTarget({ bizType: 'after_sale', bizId: 200 }), '/pages/after-sales/detail?id=200')
    assert.equal(messageNavigationTarget({ bizType: 'after_sale' }), '/pages/after-sales/list')
    assert.equal(messageNavigationTarget({ bizType: 'order', bizId: 100 }), '/pages/order/detail?id=100')
  })

  it('builds focused preview for after-sale failure messages', () => {
    assert.deepEqual(
      messagePreview({
        bizType: 'after_sale',
        title: '退款失败',
        content: '售后单 AS202606010001 退款失败，原因：渠道余额不足，请尽快处理。',
      }),
      {
        title: '退款失败',
        content: '售后单 AS202606010001 退款失败，原因：渠道余额不足，请尽快处理。',
        emphasis: '售后失败',
        tone: 'danger',
      },
    )
  })
})
