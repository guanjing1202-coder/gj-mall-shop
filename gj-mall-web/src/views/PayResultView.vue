<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { getOrderDetail, type OrderDetail } from '@/api/order'
import { getPayStatus, type PayStatus } from '@/api/pay'
import { hasPaymentResultLookup, resolvePaymentResultState } from '@/utils/payment-result-ui'

const route = useRoute()
const router = useRouter()

const order = ref<OrderDetail>()
const payStatus = ref<PayStatus>()
const loading = ref(false)
const loadError = ref('')
const pollRemaining = ref(6)
const resolvedOrderId = ref('')
let pollTimer: ReturnType<typeof setTimeout> | undefined

const queryStatus = computed(() => {
  const raw = String(route.query.status || '')
  if (raw === 'success' || raw === 'pending' || raw === 'fail') {
    return raw
  }
  return undefined
})
const routeOrderId = computed(() => String(route.query.orderId || ''))
const orderId = computed(() => routeOrderId.value || resolvedOrderId.value)
const orderNo = computed(() => String(route.query.orderNo || ''))
const payNo = computed(() => String(route.query.payNo || ''))
const message = computed(() => String(route.query.message || ''))
const channel = computed(() => String(route.query.channel || ''))
const bridge = computed(() => String(route.query.bridge || ''))
const canLookupStatus = computed(() => hasPaymentResultLookup({ orderId: orderId.value, payNo: payNo.value }))
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
const status = computed(() => resultState.value.kind)
const displayOrderNo = computed(() => order.value?.orderNo || payStatus.value?.orderNo || orderNo.value)
const displayAmount = computed(() => formatPrice(order.value?.payAmount ?? payStatus.value?.amount))
const displayOrderStatus = computed(() => order.value?.statusDesc || payStatus.value?.orderStatusDesc)
const displayPayTime = computed(() => order.value?.payTime || payStatus.value?.payTime)
const displayPayNo = computed(() => payNo.value || payStatus.value?.payNo)
const bridgeHint = computed(() => {
  if (resultState.value.kind !== 'pending' || !bridge.value) {
    return ''
  }
  const channelText = channel.value === 'alipay' ? '支付宝' : channel.value === 'wechat' ? '微信支付' : '第三方支付'
  if (bridge.value === 'form') {
    return `已打开${channelText}收银页面，请在新窗口完成支付后回到这里查看状态。`
  }
  if (bridge.value === 'external') {
    return `已跳转到${channelText}收银页面，请完成支付后回到这里查看状态。`
  }
  return `已收到${channelText}调起参数，请按渠道页面提示继续完成支付。`
})

async function refreshOrder(manual = false) {
  if (!orderId.value && !payNo.value) {
    return
  }
  if (manual) {
    pollRemaining.value = 6
  }
  loading.value = true
  loadError.value = ''
  try {
    if (orderId.value) {
      const res = await getOrderDetail(orderId.value)
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

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

const pollHint = computed(() => {
  if (loading.value) {
    return '正在读取订单最新状态...'
  }
  if (resultState.value.shouldPoll && pollRemaining.value > 0) {
    return `仍在等待回调确认，系统还会自动刷新 ${pollRemaining.value} 次。`
  }
  if (resultState.value.shouldPoll) {
    return '自动刷新暂未等到成功回调，可以稍后手动刷新或返回订单查看。'
  }
  return ''
})

watch(
  () => resultState.value.shouldPoll,
  () => schedulePoll(),
)

onMounted(async () => {
  await refreshOrder(false)
  schedulePoll()
})

onUnmounted(() => {
  stopPoll()
})

function goOrder() {
  if (orderId.value) {
    router.replace(`/order/${orderId.value}`)
  } else {
    router.replace('/orders')
  }
}

function retryPay() {
  if (orderId.value) {
    router.replace(`/order/${orderId.value}?pay=1`)
  } else {
    router.replace('/orders')
  }
}
</script>

<template>
  <div class="page-shell">
    <ShopHeader />

    <main class="result-page">
      <section class="result-card" :class="status">
        <div class="status-icon">{{ resultState.iconText }}</div>
        <span class="status-badge">{{ resultState.badgeText }}</span>
        <h1>{{ resultState.title }}</h1>
        <p>{{ resultState.subtitle }}</p>
        <p v-if="bridgeHint" class="bridge-hint">{{ bridgeHint }}</p>
        <p v-if="pollHint" class="poll-hint">{{ pollHint }}</p>
      </section>

      <section class="info-panel">
        <div>
          <span>订单号</span>
          <strong>{{ displayOrderNo || '-' }}</strong>
        </div>
        <div>
          <span>应付金额</span>
          <strong>{{ displayAmount }}</strong>
        </div>
        <div v-if="displayOrderStatus">
          <span>订单状态</span>
          <strong>{{ displayOrderStatus }}</strong>
        </div>
        <div v-if="displayPayTime">
          <span>支付时间</span>
          <strong>{{ formatTime(displayPayTime) }}</strong>
        </div>
        <div v-if="displayPayNo">
          <span>支付流水</span>
          <strong>{{ displayPayNo }}</strong>
        </div>
        <div v-if="resultState.messageText || loadError">
          <span>提示</span>
          <strong>{{ loadError || resultState.messageText }}</strong>
        </div>
      </section>

      <section class="action-row">
        <button class="primary" :disabled="loading" @click="handlePrimary">{{ resultState.primaryText }}</button>
        <button v-if="resultState.canRetryPay" @click="retryPay">重新支付</button>
        <button v-if="resultState.primaryAction !== 'orders'" @click="router.replace('/orders')">我的订单</button>
        <button @click="router.replace('/')">继续逛逛</button>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page-shell {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.result-page {
  width: min(760px, calc(100% - 32px));
  margin: 0 auto;
  padding: 42px 0 72px;
}

.result-card,
.info-panel {
  border: 1px solid #eef0f3;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.result-card {
  padding: 52px 36px;
  text-align: center;
}

.status-icon {
  display: inline-flex;
  width: 78px;
  height: 78px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #2f8f67;
  color: #fff;
  font-size: 44px;
  font-weight: 900;
}

.status-badge {
  display: table;
  margin: 18px auto 0;
  padding: 7px 12px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
}

.result-card.pending .status-icon {
  background: #f59e0b;
}

.result-card.pending .status-badge {
  background: #fff4dc;
  color: #b7791f;
}

.result-card.fail .status-icon {
  background: #e5484d;
}

.result-card.fail .status-badge {
  background: #fff1f2;
  color: #be123c;
}

h1 {
  margin: 14px 0 0;
  font-size: 34px;
  line-height: 1.2;
}

p {
  margin: 12px auto 0;
  max-width: 460px;
  color: #6b7280;
  font-size: 16px;
  line-height: 1.7;
}

.poll-hint {
  color: #9ca3af;
  font-size: 14px;
}

.bridge-hint {
  color: #b7791f;
  font-size: 14px;
  font-weight: 800;
}

.info-panel {
  display: grid;
  gap: 0;
  margin-top: 18px;
  padding: 10px 24px;
}

.info-panel > div {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid #eef0f3;
}

.info-panel > div:first-child {
  border-top: 0;
}

.info-panel span {
  flex: 0 0 auto;
  color: #6b7280;
}

.info-panel strong {
  min-width: 0;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

button {
  height: 46px;
  border: 0;
  border-radius: 999px;
  background: #fff;
  color: #111827;
  font-weight: 900;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

button.primary {
  background: #e5484d;
  color: #fff;
}

@media (max-width: 640px) {
  .action-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
