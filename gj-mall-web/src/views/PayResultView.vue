<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'

const route = useRoute()
const router = useRouter()

const status = computed(() => {
  const raw = String(route.query.status || 'success')
  return raw === 'pending' || raw === 'fail' ? raw : 'success'
})
const orderId = computed(() => String(route.query.orderId || ''))
const orderNo = computed(() => String(route.query.orderNo || ''))
const payNo = computed(() => String(route.query.payNo || ''))
const message = computed(() => String(route.query.message || ''))

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
  return message.value || '本次支付没有完成，可以重新发起支付或稍后再试。'
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
        <div class="status-icon">{{ iconText }}</div>
        <h1>{{ titleText }}</h1>
        <p>{{ subtitleText }}</p>
      </section>

      <section class="info-panel">
        <div>
          <span>订单号</span>
          <strong>{{ orderNo || '-' }}</strong>
        </div>
        <div v-if="payNo">
          <span>支付流水</span>
          <strong>{{ payNo }}</strong>
        </div>
        <div v-if="message">
          <span>提示</span>
          <strong>{{ message }}</strong>
        </div>
      </section>

      <section class="action-row">
        <button class="primary" @click="goOrder">查看订单</button>
        <button v-if="status !== 'success'" @click="retryPay">重新支付</button>
        <button @click="router.replace('/orders')">我的订单</button>
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

.result-card.pending .status-icon {
  background: #f59e0b;
}

.result-card.fail .status-icon {
  background: #e5484d;
}

h1 {
  margin: 22px 0 0;
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
