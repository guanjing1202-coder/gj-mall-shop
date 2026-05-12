<template>
  <view class="page">
    <view v-if="loading" class="empty">订单加载中...</view>

    <view v-else-if="order">
      <view class="hero">
        <text class="kicker">Payment</text>
        <text class="title">收银台</text>
        <text class="subtitle">订单号 {{ order.orderNo }}</text>
      </view>

      <view class="amount-card">
        <text>应付金额</text>
        <text>{{ formatPrice(order.payAmount) }}</text>
        <text>{{ order.statusDesc || '待支付' }}</text>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>支付方式</text>
          <text>开发模式</text>
        </view>
        <view
          v-for="item in channels"
          :key="item.value"
          class="channel"
          :class="{ active: selectedChannel === item.value, disabled: item.disabled }"
          @tap="selectChannel(item.value)"
        >
          <view class="channel-icon">{{ item.icon }}</view>
          <view class="channel-copy">
            <text>{{ item.label }}</text>
            <text>{{ item.desc }}</text>
          </view>
          <view class="select-dot" :class="{ active: selectedChannel === item.value }" />
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>订单摘要</text>
          <text>{{ order.items?.length || 0 }} 件</text>
        </view>
        <view v-for="item in order.items || []" :key="item.id" class="order-item">
          <image class="thumb" :src="normalizeImage(item.skuImage, String(item.skuId))" mode="aspectFill" />
          <view>
            <text>{{ item.skuName || '商品' }}</text>
            <text>{{ specText(item) }}</text>
            <text>{{ formatPrice(item.totalAmount) }} x{{ item.quantity }}</text>
          </view>
        </view>
      </view>

      <view class="panel notice">
        <text>当前项目使用 mock 支付，点击确认后会立即把订单推进到待发货状态。</text>
      </view>
    </view>

    <view v-else-if="!isLoggedIn" class="empty">
      <text>请先登录后支付</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else class="empty">
      <text>订单不存在或已失效</text>
      <button @tap="goHome">返回首页</button>
    </view>

    <view v-if="order" class="bottom-bar">
      <view>
        <text>合计</text>
        <text>{{ formatPrice(order.payAmount) }}</text>
      </view>
      <button :disabled="submitting || Number(order.status) !== 0" @tap="submitPay">
        {{ Number(order.status) === 0 ? '确认支付' : '查看订单' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createPay, type PayChannel } from '@/api/pay'
import { getOrderByNo, getOrderDetail, type OrderDetail, type OrderItem } from '@/api/order'
import { hasLoginState } from '@/utils/auth'

const loading = ref(false)
const submitting = ref(false)
const isLoggedIn = ref(false)
const order = ref<OrderDetail>()
const selectedChannel = ref<PayChannel>('mock')

const channels: Array<{ value: PayChannel; label: string; desc: string; icon: string; disabled?: boolean }> = [
  { value: 'mock', label: '余额模拟支付', desc: '开发环境即时支付成功', icon: '¥' },
  { value: 'wechat', label: '微信支付', desc: '待接入商户号与证书', icon: '微', disabled: true },
  { value: 'alipay', label: '支付宝', desc: '待接入 AppID 与密钥', icon: '支', disabled: true },
]

onLoad((options: any) => {
  isLoggedIn.value = hasLoginState()
  if (!isLoggedIn.value) return
  if (options?.orderNo) {
    loadByNo(decodeURIComponent(options.orderNo))
  } else if (options?.id) {
    loadById(options.id)
  }
})

async function loadByNo(orderNo: string) {
  loading.value = true
  try {
    const res = await getOrderByNo(orderNo)
    order.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadById(id: string | number) {
  loading.value = true
  try {
    const res = await getOrderDetail(id)
    order.value = res.data
  } finally {
    loading.value = false
  }
}

function selectChannel(channel: PayChannel) {
  const target = channels.find((item) => item.value === channel)
  if (target?.disabled) {
    uni.showToast({ title: '该支付方式暂未启用', icon: 'none' })
    return
  }
  selectedChannel.value = channel
}

async function submitPay() {
  if (!order.value) return
  if (Number(order.value.status) !== 0) {
    goOrder()
    return
  }
  submitting.value = true
  try {
    const res = await createPay({ orderId: order.value.id, channel: selectedChannel.value })
    uni.showToast({ title: res.data.paid ? '支付成功' : '支付已创建', icon: 'success' })
    uni.redirectTo({ url: `/pages/order/detail?id=${order.value.id}` })
  } finally {
    submitting.value = false
  }
}

function goOrder() {
  if (!order.value) return
  uni.redirectTo({ url: `/pages/order/detail?id=${order.value.id}` })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function normalizeImage(url?: string, seed = 'pay') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 150rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.amount-card,
.panel {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  padding: 36rpx 30rpx;
  background: #111827;
  color: #fff;
}

.kicker,
.title,
.subtitle,
.amount-card text,
.notice text {
  display: block;
}

.kicker {
  color: #ffd166;
  font-size: 23rpx;
  font-weight: 900;
}

.title {
  margin-top: 10rpx;
  font-size: 44rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
}

.amount-card,
.panel {
  margin-top: 22rpx;
  padding: 24rpx;
}

.amount-card {
  text-align: center;
}

.amount-card text:first-child {
  color: #6b7280;
  font-size: 24rpx;
}

.amount-card text:nth-child(2) {
  margin-top: 12rpx;
  color: #e5484d;
  font-size: 58rpx;
  font-weight: 900;
}

.amount-card text:last-child {
  margin-top: 10rpx;
  color: #2f8f67;
  font-size: 24rpx;
  font-weight: 900;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.section-head text:first-child {
  font-size: 30rpx;
  font-weight: 900;
}

.section-head text:last-child {
  color: #9ca3af;
  font-size: 23rpx;
  font-weight: 900;
}

.channel {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 14rpx;
  padding: 20rpx;
  border: 2rpx solid #eef0f3;
  border-radius: 14rpx;
}

.channel.active {
  border-color: #e5484d;
  background: #fff7f7;
}

.channel.disabled {
  opacity: 0.52;
}

.channel-icon {
  width: 72rpx;
  height: 72rpx;
  flex: 0 0 auto;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 72rpx;
  text-align: center;
}

.channel-copy {
  min-width: 0;
  flex: 1;
}

.channel-copy text {
  display: block;
}

.channel-copy text:first-child {
  font-size: 28rpx;
  font-weight: 900;
}

.channel-copy text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.select-dot {
  width: 34rpx;
  height: 34rpx;
  border: 3rpx solid #d1d5db;
  border-radius: 999rpx;
}

.select-dot.active {
  border-color: #e5484d;
  background: #e5484d;
  box-shadow: inset 0 0 0 8rpx #fff;
}

.order-item {
  display: grid;
  grid-template-columns: 112rpx minmax(0, 1fr);
  gap: 16rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;
}

.order-item:first-of-type {
  border-top: 0;
}

.thumb {
  width: 112rpx;
  height: 112rpx;
  border-radius: 12rpx;
  background: #eef2f7;
}

.order-item view {
  min-width: 0;
}

.order-item view text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item view text:first-child {
  font-size: 26rpx;
  font-weight: 900;
}

.order-item view text:nth-child(2) {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.order-item view text:last-child {
  margin-top: 10rpx;
  color: #e5484d;
  font-size: 26rpx;
  font-weight: 900;
}

.notice {
  background: #fff7ed;
  color: #92400e;
  font-size: 24rpx;
  line-height: 1.55;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 28rpx rgba(15, 23, 42, 0.1);
}

.bottom-bar view {
  flex: 1;
}

.bottom-bar text {
  display: block;
}

.bottom-bar text:first-child {
  color: #6b7280;
  font-size: 22rpx;
}

.bottom-bar text:last-child {
  color: #e5484d;
  font-size: 36rpx;
  font-weight: 900;
}

.bottom-bar button,
.empty button {
  height: 78rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-weight: 900;
  line-height: 78rpx;
}

.bottom-bar button {
  width: 220rpx;
}

.empty {
  padding: 120rpx 30rpx;
  color: #6b7280;
  text-align: center;
}

.empty text {
  display: block;
  margin-bottom: 24rpx;
}
</style>
