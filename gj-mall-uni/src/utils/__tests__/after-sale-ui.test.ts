import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  canCancelAfterSale,
  canSubmitReturnLogistics,
  refundRecordNotice,
  refundRecordMeta,
  refundRecordTitle,
  statusBadgeClass,
} from '../after-sale-ui.ts'

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

  it('builds refund record text for compact mobile cards', () => {
    const refund = {
      refundNo: 'RF202605280001',
      statusDesc: '退款成功',
      channelDesc: 'MOCK 模拟支付',
      amount: 128.5,
      reason: '用户取消订单',
      successTime: '2026-05-28 18:10:00',
    }

    assert.equal(refundRecordTitle(refund), '退款成功 RF202605280001')
    assert.equal(refundRecordMeta(refund), '¥128.50 · MOCK 模拟支付 · 2026-05-28 18:10:00 · 原因：用户取消订单')
  })

  it('explains refund processing and failure status to users', () => {
    assert.equal(refundRecordNotice({ refundNo: 'RF1', status: 0 }), '退款正在处理中，请等待商家或支付渠道确认。')
    assert.equal(refundRecordNotice({ refundNo: 'RF2', status: 2, reason: '渠道余额不足' }), '退款失败：渠道余额不足。商家会重新处理，请留意售后进度。')
    assert.equal(refundRecordNotice({ refundNo: 'RF3', status: 2 }), '退款失败，商家会重新处理，请留意售后进度。')
    assert.equal(refundRecordNotice({ refundNo: 'RF4', status: 1 }), '')
  })

  it('does not show refund notice without refund number', () => {
    assert.equal(refundRecordNotice({ status: 2, reason: '渠道超时' }), '')
  })
})
