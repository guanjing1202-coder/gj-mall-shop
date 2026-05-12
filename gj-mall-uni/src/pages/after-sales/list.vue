<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">After Sale</text>
      <text class="title">售后进度</text>
      <text class="subtitle">查看退款、退货和审核状态。</text>
    </view>

    <view v-if="!isLoggedIn" class="empty-card">
      <text>登录后查看售后</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!afterSales.length" class="empty-card">
        <text>暂无售后记录</text>
        <button @tap="goOrders">查看订单</button>
      </view>
      <view v-else class="list">
        <view v-for="item in afterSales" :key="item.id" class="sale-card" @tap="goOrder(item)">
          <view class="card-head">
            <view>
              <text>{{ item.typeDesc || '售后申请' }}</text>
              <text>{{ item.afterSaleNo }}</text>
            </view>
            <text>{{ item.statusDesc || '处理中' }}</text>
          </view>
          <view class="card-main">
            <view>
              <text>订单号</text>
              <text>{{ item.orderNo }}</text>
            </view>
            <view>
              <text>退款金额</text>
              <text>{{ formatPrice(item.amount) }}</text>
            </view>
            <view>
              <text>申请原因</text>
              <text>{{ item.reason || '-' }}</text>
            </view>
          </view>
          <view class="card-foot">
            <text>{{ formatTime(item.createTime) }}</text>
            <button @tap.stop="goOrder(item)">查看订单</button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { getAfterSalePage, type AfterSale } from '@/api/order'
import { hasLoginState } from '@/utils/auth'

const isLoggedIn = ref(false)
const loading = ref(false)
const afterSales = ref<AfterSale[]>([])
const pageNum = ref(1)
const total = ref(0)
const finished = ref(false)

onShow(() => {
  isLoggedIn.value = hasLoginState()
  if (isLoggedIn.value) {
    refresh()
  } else {
    afterSales.value = []
  }
})

onPullDownRefresh(async () => {
  if (isLoggedIn.value) await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value && !loading.value) {
    loadAfterSales(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await loadAfterSales(true)
}

async function loadAfterSales(reset: boolean) {
  loading.value = true
  try {
    const res = await getAfterSalePage({ pageNum: pageNum.value, pageSize: 10 })
    const page = res.data
    const list = page?.list || []
    afterSales.value = reset ? list : [...afterSales.value, ...list]
    total.value = Number(page?.total || 0)
    finished.value = afterSales.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

function goOrder(item: AfterSale) {
  uni.navigateTo({ url: `/pages/order/detail?id=${item.orderId}` })
}

function goOrders() {
  uni.navigateTo({ url: '/pages/orders/list' })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 16).replace('T', ' ')
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.sale-card,
.empty-card {
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
.subtitle {
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

.list {
  display: grid;
  gap: 18rpx;
  margin-top: 22rpx;
}

.sale-card {
  padding: 22rpx;
}

.card-head,
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.card-head view {
  min-width: 0;
}

.card-head view text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-head view text:first-child {
  color: #111827;
  font-size: 30rpx;
  font-weight: 900;
}

.card-head view text:last-child {
  margin-top: 6rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.card-head > text {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.card-main {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.card-main view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.card-main text:first-child,
.card-foot text {
  color: #6b7280;
  font-size: 23rpx;
}

.card-main text:last-child {
  max-width: 450rpx;
  overflow: hidden;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-main view:nth-child(2) text:last-child {
  color: #e5484d;
}

.card-foot {
  margin-top: 18rpx;
}

.card-foot button,
.empty-card button {
  height: 58rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.empty,
.empty-card {
  margin-top: 22rpx;
  padding: 80rpx 24rpx;
  color: #6b7280;
  text-align: center;
}

.empty-card text {
  display: block;
  margin-bottom: 22rpx;
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}

.empty-card button {
  display: inline-block;
  padding: 0 30rpx;
}
</style>
