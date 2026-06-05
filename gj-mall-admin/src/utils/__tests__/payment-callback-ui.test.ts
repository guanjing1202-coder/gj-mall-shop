import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  canReplayPaymentCallback,
  formatCallbackSignatureSample,
  formatPaymentCallbackInspectItems,
  paymentCallbackProcessLabel,
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

  it('uses a broad ignored label for callbacks that are recorded but not booked', () => {
    assert.equal(paymentCallbackProcessLabel(2), '已忽略')
    assert.equal(paymentCallbackProcessLabel(1), '已处理')
    assert.equal(paymentCallbackProcessLabel(3), '处理失败')
  })

  it('formats channel payload fields for payment callback inspection', () => {
    const items = formatPaymentCallbackInspectItems({
      channelDesc: '支付宝',
      channelAppId: '2021000123456789',
      channelOrderNo: 'P202605210001',
      channelTradeNo: '2026052222001400000001',
      channelTradeStatus: 'TRADE_SUCCESS',
      channelAmount: '99.00',
      validationSummary: '验签通过 / 处理失败 / 原因：AppID 不一致',
    })

    assert.deepEqual(items, [
      { label: '渠道', value: '支付宝' },
      { label: 'AppID', value: '2021000123456789' },
      { label: '商户单号', value: 'P202605210001' },
      { label: '渠道交易号', value: '2026052222001400000001' },
      { label: '交易状态', value: 'TRADE_SUCCESS' },
      { label: '通知金额', value: '99.00' },
      { label: '校验摘要', value: '验签通过 / 处理失败 / 原因：AppID 不一致' },
    ])
  })
})
