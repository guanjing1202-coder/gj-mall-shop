import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { buildPaymentChannels, firstEnabledPaymentChannel } from '../payment-channel-ui.ts'

describe('payment-channel-ui', () => {
  it('shows backend disabled reasons for real channels in mock mode', () => {
    const channels = buildPaymentChannels([
      { name: 'mock', desc: 'MOCK 模拟支付', enabled: true, status: '开发环境即时成功' },
      { name: 'wechat', desc: '微信支付', enabled: false, status: '当前为 mock 支付模式，真实微信支付未启用' },
      { name: 'alipay', desc: '支付宝', enabled: false, status: '当前为 mock 支付模式，真实支付宝未启用' },
    ])

    assert.equal(channels[0].disabled, false)
    assert.equal(channels[1].disabled, true)
    assert.equal(channels[1].desc, '当前为 mock 支付模式，真实微信支付未启用')
    assert.equal(firstEnabledPaymentChannel(channels), 'mock')
  })

  it('falls through to the first enabled real channel', () => {
    const channels = buildPaymentChannels([
      { name: 'mock', desc: 'MOCK 模拟支付', enabled: false, status: '真实支付模式下关闭模拟支付' },
      { name: 'wechat', desc: '微信支付', enabled: true, status: '可用' },
      { name: 'alipay', desc: '支付宝', enabled: true, status: '可用' },
    ])

    assert.equal(firstEnabledPaymentChannel(channels), 'wechat')
  })
})
