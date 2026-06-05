<template>
  <view class="page">
    <view class="result-card" :class="resultState.kind">
      <view class="status-icon">{{ resultState.iconText }}</view>
      <text class="badge">{{ resultState.badgeText }}</text>
      <text class="title">{{ resultState.title }}</text>
      <text class="subtitle">{{ resultState.subtitle }}</text>
      <text v-if="bridgeHint" class="bridge-hint">{{ bridgeHint }}</text>
      <text v-if="pollHint" class="poll-hint">{{ pollHint }}</text>
    </view>

    <view class="panel">
      <view class="info-row">
        <text>订单号</text>
        <text>{{ displayOrderNo || '-' }}</text>
      </view>
      <view class="info-row">
        <text>应付金额</text>
        <text>{{ displayAmount }}</text>
      </view>
      <view v-if="displayOrderStatus" class="info-row">
        <text>订单状态</text>
        <text>{{ displayOrderStatus }}</text>
      </view>
      <view v-if="displayPayTime" class="info-row">
        <text>支付时间</text>
        <text>{{ formatTime(displayPayTime) }}</text>
      </view>
      <view v-if="displayPayNo" class="info-row">
        <text>支付流水</text>
        <text>{{ displayPayNo }}</text>
      </view>
      <view v-if="resultState.messageText || loadError" class="info-row">
        <text>提示</text>
        <text>{{ loadError || resultState.messageText }}</text>
      </view>
    </view>

    <view class="action-grid" :class="{ single: resultState.kind === 'success' && !resultState.canRetryPay }">
      <button class="primary" :disabled="loading" @tap="handlePrimary">{{ resultState.primaryText }}</button>
      <button v-if="resultState.canRetryPay" @tap="retryPay">重新支付</button>
      <button v-if="resultState.primaryAction !== 'orders'" @tap="goOrders">我的订单</button>
      <button @tap="goHome">继续逛逛</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOrderDetail, type OrderDetail } from '@/api/order'
import { getPayStatus, type PayStatus } from '@/api/pay'
import { hasRecoverableLoginState } from '@/utils/auth'
import { hasPaymentResultLookup, resolvePaymentResultState } from '@/utils/payment-result-ui'

const queryStatus = ref<'success' | 'pending' | 'fail' | undefined>()
const orderId = ref('')
const orderNo = ref('')
const payNo = ref('')
const message = ref('')
const channel = ref('')
const bridge = ref('')
const order = ref<OrderDetail>()
const payStatus = ref<PayStatus>()
const loading = ref(false)
const loadError = ref('')
const pollRemaining = ref(6)
const resolvedOrderId = ref('')
let pollTimer: ReturnType<typeof setTimeout> | undefined
const effectiveOrderId = computed(() => orderId.value || resolvedOrderId.value)
const canLookupStatus = computed(() => hasPaymentResultLookup({ orderId: effectiveOrderId.value, payNo: payNo.value }))

const initialQueryStatus = computed(() => {
  if (canLookupStatus.value && loading.value && !order.value && !payStatus.value) {
    return 'pending'
  }
  if (canLookupStatus.value && loadError.value && !order.value && !payStatus.value && queryStatus.value !== 'fail') {
    return 'pending'
  }
  return queryStatus.value
})

const resultState = computed(() => resolvePaymentResultState({
  queryStatus: initialQueryStatus.value,
  orderStatus: order.value?.status ?? payStatus.value?.orderStatus,
  orderStatusDesc: order.value?.statusDesc || payStatus.value?.orderStatusDesc,
  message: message.value,
  hasOrderId: canLookupStatus.value,
}))
const displayOrderNo = computed(() => order.value?.orderNo || payStatus.value?.orderNo || orderNo.value)
const displayAmount = computed(() => formatPrice(order.value?.payAmount ?? payStatus.value?.amount))
const displayOrderStatus = computed(() => order.value?.statusDesc || payStatus.value?.orderStatusDesc)
const displayPayTime = computed(() => order.value?.payTime || payStatus.value?.payTime)
const displayPayNo = computed(() => payNo.value || payStatus.value?.payNo)
const bridgeHint = computed(() => {
  if (resultState.value.kind !== 'pending' || !bridge.value) return ''
  const channelText = channel.value === 'alipay' ? '支付宝' : channel.value === 'wechat' ? '微信支付' : '第三方支付'
  if (bridge.value === 'form') return `已打开${channelText}收银页，请完成支付后回到这里查看状态。`
  if (bridge.value === 'external') return `已跳转到${channelText}收银页，请完成支付后回到这里查看状态。`
  return `已收到${channelText}调起参数，请按渠道提示继续支付。`
})
const pollHint = computed(() => {
  if (loading.value) return '正在读取订单最新状态...'
  if (resultState.value.shouldPoll && pollRemaining.value > 0) {
    return `仍在等待回调确认，系统还会自动刷新 ${pollRemaining.value} 次。`
  }
  if (resultState.value.shouldPoll) {
    return '自动刷新暂未等到成功回调，可以稍后手动刷新或返回订单查看。'
  }
  return ''
})

onLoad((options: any) => {
  const incoming = String(options?.status || '')
  queryStatus.value = incoming === 'success' || incoming === 'pending' || incoming === 'fail' ? incoming : undefined
  orderId.value = String(options?.orderId || '')
  orderNo.value = decodeURIComponent(String(options?.orderNo || ''))
  payNo.value = decodeURIComponent(String(options?.payNo || ''))
  message.value = decodeURIComponent(String(options?.message || ''))
  channel.value = decodeURIComponent(String(options?.channel || ''))
  bridge.value = decodeURIComponent(String(options?.bridge || ''))
  refreshOrder().then(schedulePoll)
})

onUnmounted(stopPoll)

async function refreshOrder(manual = false) {
  if (!effectiveOrderId.value && !payNo.value) return
  if (!hasRecoverableLoginState()) {
    loadError.value = '登录状态已过期，请登录后查看最新订单状态。'
    return
  }
  if (manual) {
    pollRemaining.value = 6
  }
  loading.value = true
  loadError.value = ''
  try {
    if (effectiveOrderId.value) {
      const res = await getOrderDetail(effectiveOrderId.value)
      order.value = res.data
      return
    }
    const res = await getPayStatus(payNo.value)
    payStatus.value = res.data
    if (res.data.orderId) {
      resolvedOrderId.value = String(res.data.orderId)
    }
  } catch (error: any) {
    loadError.value = error?.message || '订单状态刷新失败，请稍后再试。'
  } finally {
    loading.value = false
  }
}

function schedulePoll() {
  stopPoll()
  if (!resultState.value.shouldPoll || pollRemaining.value <= 0) {
    return
  }
  pollTimer = setTimeout(async () => {
    pollRemaining.value -= 1
    await refreshOrder(false)
    schedulePoll()
  }, 3000)
}

function stopPoll() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

function handlePrimary() {
  if (resultState.value.primaryAction === 'refresh') {
    refreshOrder(true).then(schedulePoll)
    return
  }
  goOrder()
}

function goOrder() {
  if (effectiveOrderId.value) {
    uni.redirectTo({ url: `/pages/order/detail?id=${effectiveOrderId.value}` })
  } else {
    goOrders()
  }
}

function retryPay() {
  if (effectiveOrderId.value) {
    uni.redirectTo({ url: `/pages/pay/pay?id=${effectiveOrderId.value}` })
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

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '-'
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) return value
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  const hour = String(parsed.getHours()).padStart(2, '0')
  const minute = String(parsed.getMinutes()).padStart(2, '0')
  return `${month}/${day} ${hour}:${minute}`
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

.badge {
  display: inline-block;
  margin-bottom: 18rpx;
  padding: 10rpx 18rpx;
  border-radius: 14rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 23rpx;
  font-weight: 900;
}

.result-card.pending .status-icon {
  background: #f59e0b;
}

.result-card.pending .badge {
  background: #fff4dc;
  color: #b7791f;
}

.result-card.fail .status-icon {
  background: #e5484d;
}

.result-card.fail .badge {
  background: #fff1f2;
  color: #be123c;
}

.title,
.subtitle,
.poll-hint {
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

.poll-hint,
.bridge-hint {
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.5;
}

.poll-hint {
  color: #9ca3af;
}

.bridge-hint {
  color: #b7791f;
  font-weight: 900;
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

.action-grid button[disabled] {
  opacity: 0.58;
}

.action-grid.single button.primary {
  grid-column: 1 / -1;
}

.action-grid button.primary {
  background: #e5484d;
  color: #fff;
}
</style>
