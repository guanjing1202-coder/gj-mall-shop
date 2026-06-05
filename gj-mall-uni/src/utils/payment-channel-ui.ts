import type { PayChannel, PayChannelStatus } from '@/api/pay'

export interface PaymentChannelOption {
  value: PayChannel
  label: string
  desc: string
  icon: string
  disabled: boolean
}

const CHANNEL_META: Record<PayChannel, { label: string; icon: string; fallback: string }> = {
  mock: { label: '余额模拟支付', icon: '¥', fallback: '开发环境即时支付成功' },
  wechat: { label: '微信支付', icon: '微', fallback: '等待商户配置' },
  alipay: { label: '支付宝', icon: '支', fallback: '等待应用配置' },
}

const FALLBACK_CHANNELS: PayChannelStatus[] = [
  { name: 'mock', desc: 'MOCK 模拟支付', enabled: true, status: '开发环境即时成功' },
  { name: 'wechat', desc: '微信支付', enabled: false, status: '等待后端返回支付渠道状态' },
  { name: 'alipay', desc: '支付宝', enabled: false, status: '等待后端返回支付渠道状态' },
]

export function buildPaymentChannels(source: PayChannelStatus[] = FALLBACK_CHANNELS): PaymentChannelOption[] {
  const byName = new Map(source.map((item) => [item.name, item]))
  return (Object.keys(CHANNEL_META) as PayChannel[]).map((value) => {
    const meta = CHANNEL_META[value]
    const remote = byName.get(value)
    return {
      value,
      label: remote?.desc || meta.label,
      desc: remote?.status || meta.fallback,
      icon: meta.icon,
      disabled: remote?.enabled !== true,
    }
  })
}

export function firstEnabledPaymentChannel(channels: PaymentChannelOption[]): PayChannel | undefined {
  return channels.find((item) => !item.disabled)?.value
}
