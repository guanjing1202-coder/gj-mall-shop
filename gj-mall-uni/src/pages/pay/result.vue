<template>
  <view class="page">
    <view class="result-card" :class="status">
      <view class="status-icon">{{ iconText }}</view>
      <text class="title">{{ titleText }}</text>
      <text class="subtitle">{{ subtitleText }}</text>
    </view>

    <view class="panel">
      <view class="info-row">
        <text>订单号</text>
        <text>{{ orderNo || '-' }}</text>
      </view>
      <view v-if="payNo" class="info-row">
        <text>支付流水</text>
        <text>{{ payNo }}</text>
      </view>
      <view v-if="message" class="info-row">
        <text>提示</text>
        <text>{{ message }}</text>
      </view>
    </view>

  <view class="action-grid" :class="{ single: status === 'success' }">
      <button class="primary" @tap="goOrder">{{ primaryText }}</button>
      <button v-if="status !== 'success'" @tap="retryPay">重新支付</button>
      <button @tap="goOrders">我的订单</button>
      <button @tap="goHome">继续逛逛</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const status = ref<'success' | 'pending' | 'fail'>('success')
const orderId = ref('')
const orderNo = ref('')
const payNo = ref('')
const message = ref('')

const iconText = computed(() => {
  if (status.value === 'success') return '✓'
  if (status.value === 'pending') return '…'
  return '!'
})

const titleText = computed(() => {
  if (status.value === 'success') return '支付成功'
  if (status.value === 'pending') return '支付已创建'
  return '支付失败'
})

const subtitleText = computed(() => {
  if (status.value === 'success') return '订单已经进入待发货，商家会尽快处理。'
  if (status.value === 'pending') return '请在第三方支付完成后回到订单查看状态。'
  return message.value || '本次支付没有完成，可以重新支付或稍后再试。'
})

const primaryText = computed(() => (status.value === 'success' ? '查看订单详情' : '查看订单状态'))

onLoad((options: any) => {
  const incoming = String(options?.status || 'success')
  status.value = incoming === 'pending' || incoming === 'fail' ? incoming : 'success'
  orderId.value = String(options?.orderId || '')
  orderNo.value = decodeURIComponent(String(options?.orderNo || ''))
  payNo.value = decodeURIComponent(String(options?.payNo || ''))
  message.value = decodeURIComponent(String(options?.message || ''))
})

function goOrder() {
  if (orderId.value) {
    uni.redirectTo({ url: `/pages/order/detail?id=${orderId.value}` })
  } else {
    goOrders()
  }
}

function retryPay() {
  if (orderId.value) {
    uni.redirectTo({ url: `/pages/pay/pay?id=${orderId.value}` })
  } else if (orderNo.value) {
    uni.redirectTo({ url: `/pages/pay/pay?orderNo=${encodeURIComponent(orderNo.value)}` })
  } else {
    goOrders()
  }
}

function goOrders() {
  uni.redirectTo({ url: '/pages/orders/list' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 28rpx 24rpx;
  background: #f7f8f5;
  color: #111827;
}

.result-card,
.panel {
  border-radius: 18rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.result-card {
  padding: 60rpx 34rpx;
  text-align: center;
}

.status-icon {
  width: 108rpx;
  height: 108rpx;
  margin: 0 auto 24rpx;
  border-radius: 999rpx;
  background: #2f8f67;
  color: #fff;
  font-size: 62rpx;
  font-weight: 900;
  line-height: 108rpx;
}

.result-card.pending .status-icon {
  background: #f59e0b;
}

.result-card.fail .status-icon {
  background: #e5484d;
}

.title,
.subtitle {
  display: block;
}

.title {
  font-size: 44rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 14rpx;
  color: #6b7280;
  font-size: 25rpx;
  line-height: 1.55;
}

.panel {
  margin-top: 22rpx;
  padding: 24rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;
}

.info-row:first-child {
  border-top: 0;
}

.info-row text:first-child {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 24rpx;
}

.info-row text:last-child {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 24rpx;
}

.action-grid button {
  height: 78rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #fff;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.action-grid.single button.primary {
  grid-column: 1 / -1;
}

.action-grid button.primary {
  background: #e5484d;
  color: #fff;
}
</style>
