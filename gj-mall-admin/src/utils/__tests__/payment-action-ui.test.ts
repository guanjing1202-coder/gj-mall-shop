import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  canRefundPayment,
  canMarkRefundFailed,
  canRetryRefund,
  isFullRefundAmount,
  canSyncPaymentStatus,
  paymentRefundHint,
  refundFailureHint,
  paymentSyncStatusHint,
} from '../payment-action-ui.ts'

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

  it('uses backend refund eligibility when it is provided', () => {
    assert.equal(canRefundPayment({ status: 1, orderStatus: 1, refundAllowed: true }), true)
    assert.equal(canRefundPayment({ status: 1, orderStatus: 1, refundAllowed: false }), false)
    assert.equal(canRefundPayment({ status: 0, orderStatus: 1, refundAllowed: true }), false)
    assert.equal(canRefundPayment({ status: 1, orderStatus: 6, refundAllowed: true }), false)
    assert.equal(canRefundPayment({ status: 1, orderStatus: 1, refundRecord: { id: 1 }, refundAllowed: true }), false)
  })

  it('formats payment refund hint from backend reason', () => {
    assert.equal(paymentRefundHint({ refundReason: '退款记录已存在，请在退款记录中处理' }), '退款记录已存在，请在退款记录中处理')
    assert.equal(paymentRefundHint({ status: 1, orderStatus: 1 }), '')
  })

  it('allows refund retry only for processing or failed refund records', () => {
    assert.equal(canRetryRefund({ refundRecord: { status: 0 } }), true)
    assert.equal(canRetryRefund({ refundRecord: { status: 2 } }), true)
    assert.equal(canRetryRefund({ refundRecord: { status: 1 } }), false)
    assert.equal(canRetryRefund({}), false)
  })

  it('allows marking refund failed only while refund is processing', () => {
    assert.equal(canMarkRefundFailed({ refundRecord: { status: 0 } }), true)
    assert.equal(canMarkRefundFailed({ refundRecord: { status: 2 } }), false)
    assert.equal(canMarkRefundFailed({ refundRecord: { status: 1 } }), false)
  })

  it('formats refund failure hint with reason fallback', () => {
    assert.equal(refundFailureHint({ status: 2, reason: '渠道余额不足' }), '退款失败：渠道余额不足')
    assert.equal(refundFailureHint({ status: 2 }), '退款失败，请检查渠道返回结果后重试')
    assert.equal(refundFailureHint({ status: 0, reason: '渠道处理中' }), '')
  })

  it('accepts only full refund amount with cent precision', () => {
    assert.equal(isFullRefundAmount(88.88, 88.88), true)
    assert.equal(isFullRefundAmount(88.880001, 88.88), true)
    assert.equal(isFullRefundAmount(80, 88.88), false)
    assert.equal(isFullRefundAmount(undefined, 88.88), false)
  })
})
