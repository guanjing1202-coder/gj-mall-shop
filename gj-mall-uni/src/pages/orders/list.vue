<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">Orders</text>
      <text class="title">我的订单</text>
      <text class="subtitle">查看支付、发货和交易完成状态。</text>
    </view>

    <scroll-view scroll-x class="tabs">
      <view class="tab-row">
        <view
          v-for="item in statusTabs"
          :key="item.label"
          class="tab"
          :class="{ active: currentStatus === item.value }"
          @tap="selectStatus(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="!isLoggedIn" class="empty-card">
      <text>登录后查看订单</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!orders.length" class="empty-card">
        <text>暂无订单</text>
        <button @tap="goHome">去逛逛</button>
      </view>
      <view v-else class="list">
        <view v-for="item in orders" :key="item.id" class="order-card" @tap="goDetail(item)">
          <view class="card-head">
            <text>{{ item.orderNo }}</text>
            <text>{{ item.statusDesc || '订单状态' }}</text>
          </view>
          <view class="card-main">
            <view>
              <text>下单时间</text>
              <text>{{ formatTime(item.createTime) }}</text>
            </view>
            <view>
              <text>实付金额</text>
              <text>{{ formatPrice(item.payAmount) }}</text>
            </view>
          </view>
          <view class="card-foot">
            <text>共 {{ item.items?.length || 0 }} 件商品</text>
            <button @tap.stop="goDetail(item)">查看详情</button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { getOrderPage, type OrderDetail } from '@/api/order'
import { hasLoginState } from '@/utils/auth'

const statusTabs = [
  { label: '全部', value: undefined },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
]

const isLoggedIn = ref(false)
const loading = ref(false)
const orders = ref<OrderDetail[]>([])
const currentStatus = ref<number | undefined>()
const pageNum = ref(1)
const total = ref(0)
const finished = ref(false)

onShow(() => {
  isLoggedIn.value = hasLoginState()
  if (isLoggedIn.value) {
    refresh()
  } else {
    orders.value = []
  }
})

onPullDownRefresh(async () => {
  if (isLoggedIn.value) await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value && !loading.value) {
    loadOrders(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await loadOrders(true)
}

async function loadOrders(reset: boolean) {
  loading.value = true
  try {
    const res = await getOrderPage({
      pageNum: pageNum.value,
      pageSize: 10,
      status: currentStatus.value,
    })
    const page = res.data
    const list = page?.list || []
    orders.value = reset ? list : [...orders.value, ...list]
    total.value = Number(page?.total || 0)
    finished.value = orders.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

function selectStatus(status?: number) {
  currentStatus.value = status
  refresh()
}

function goDetail(item: OrderDetail) {
  uni.navigateTo({ url: `/pages/order/detail?id=${item.id}` })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
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
.order-card,
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

.tabs {
  width: 100%;
  margin: 22rpx 0;
  white-space: nowrap;
}

.tab-row {
  display: flex;
  gap: 12rpx;
}

.tab {
  display: inline-flex;
  height: 62rpx;
  align-items: center;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #6b7280;
  font-size: 25rpx;
  font-weight: 900;
}

.tab.active {
  background: #e5484d;
  color: #fff;
}

.list {
  display: grid;
  gap: 18rpx;
}

.order-card {
  padding: 22rpx;
}

.card-head,
.card-main,
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.card-head text:first-child {
  overflow: hidden;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-head text:last-child {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.card-main {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.card-main view text {
  display: block;
}

.card-main view text:first-child,
.card-foot text {
  color: #6b7280;
  font-size: 23rpx;
}

.card-main view text:last-child {
  margin-top: 8rpx;
  color: #111827;
  font-size: 27rpx;
  font-weight: 900;
}

.card-main view:last-child text:last-child {
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
