<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">History</text>
        <text class="title">我的足迹</text>
        <text class="subtitle">最近看过的商品会自动保存在这里。</text>
      </view>
      <button v-if="histories.length" :disabled="clearing" @tap="clearAll">清空</button>
    </view>

    <view v-if="!isLoggedIn" class="empty-card">
      <text>登录后查看足迹</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view v-if="loading && !histories.length" class="empty">加载中...</view>
      <view v-else-if="!histories.length" class="empty-card">
        <text>还没有浏览记录</text>
        <button @tap="goHome">去逛逛</button>
      </view>

      <view v-else class="list">
        <view v-for="item in histories" :key="item.spuId" class="history-card">
          <image class="cover" :src="normalizeImage(item.mainImage, String(item.spuId))" mode="aspectFill" @tap="goDetail(item.spuId)" />
          <view class="info" @tap="goDetail(item.spuId)">
            <view class="tag-row">
              <text>{{ item.brandName || item.categoryName || '精选' }}</text>
              <text>{{ formatTime(item.browseTime) }}</text>
            </view>
            <text class="name">{{ item.spuName || '商品' }}</text>
            <text class="subtitle">{{ item.subTitle || '继续看看这个商品' }}</text>
            <view class="meta-row">
              <text>{{ formatPrice(item.price) }}</text>
              <text>已售 {{ item.saleCount || 0 }}</text>
            </view>
          </view>
          <button class="remove-btn" :disabled="removingId === String(item.spuId)" @tap="remove(item)">删除</button>
        </view>
      </view>

      <view v-if="histories.length" class="load-state">
        <text>{{ finished ? '已经到底啦' : loading ? '继续加载...' : '上拉加载更多' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { clearHistory, getHistoryPage, removeHistory, type HistoryItem } from '@/api/history'
import type { ApiId } from '@/api/product'
import { requireSession, syncSession } from '@/utils/session'

const isLoggedIn = ref(false)
const loading = ref(false)
const clearing = ref(false)
const histories = ref<HistoryItem[]>([])
const pageNum = ref(1)
const total = ref(0)
const finished = ref(false)
const removingId = ref('')

onShow(async () => {
  isLoggedIn.value = await syncSession()
  if (!isLoggedIn.value) {
    histories.value = []
    return
  }
  await refresh()
})

onPullDownRefresh(async () => {
  if (isLoggedIn.value) await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value && !loading.value) {
    loadHistories(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await loadHistories(true)
}

async function loadHistories(reset: boolean) {
  loading.value = true
  try {
    const res = await getHistoryPage({ current: pageNum.value, size: 10 })
    const page = res.data
    const list = page?.list || []
    histories.value = reset ? list : [...histories.value, ...list]
    total.value = Number(page?.total || 0)
    finished.value = histories.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

async function remove(item: HistoryItem) {
  if (!(await requireSession())) return
  removingId.value = String(item.spuId)
  try {
    await removeHistory(item.spuId)
    histories.value = histories.value.filter((history) => String(history.spuId) !== String(item.spuId))
    total.value = Math.max(0, total.value - 1)
    uni.showToast({ title: '已删除', icon: 'success' })
  } finally {
    removingId.value = ''
  }
}

async function clearAll() {
  if (!(await requireSession())) return
  clearing.value = true
  try {
    await clearHistory()
    histories.value = []
    total.value = 0
    finished.value = true
    uni.showToast({ title: '足迹已清空', icon: 'success' })
  } finally {
    clearing.value = false
  }
}

function goDetail(id: ApiId) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function formatTime(value?: string) {
  if (!value) return '刚刚看过'
  const text = String(value).replace('T', ' ')
  return text.slice(5, 16)
}

function normalizeImage(url?: string, seed = 'history') {
  if (!url || url.indexOf('x.com/') >= 0 || url.indexOf('seed:') === 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/480/480`
  }
  return url
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
.history-card,
.empty-card {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22rpx;
  padding: 36rpx 30rpx;
  background: #111827;
  color: #fff;
}

.kicker,
.title,
.subtitle,
.name {
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

.hero .subtitle {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
}

.hero button {
  flex: 0 0 auto;
  height: 60rpx;
  margin: 0;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #111827;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 60rpx;
}

.list {
  display: grid;
  gap: 18rpx;
  margin-top: 22rpx;
}

.history-card {
  position: relative;
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
}

.cover {
  width: 190rpx;
  height: 190rpx;
  flex: 0 0 auto;
  border-radius: 14rpx;
  background: #eef2f7;
}

.info {
  min-width: 0;
  flex: 1;
  padding-right: 92rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag-row text {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 21rpx;
  font-weight: 900;
}

.tag-row text:nth-child(2) {
  background: #f3f4f6;
  color: #6b7280;
}

.name {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 12rpx;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.info .subtitle {
  overflow: hidden;
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
}

.meta-row text:first-child {
  color: #e5484d;
  font-size: 32rpx;
  font-weight: 900;
}

.meta-row text:last-child {
  color: #9ca3af;
  font-size: 23rpx;
}

.remove-btn {
  position: absolute;
  top: 18rpx;
  right: 18rpx;
  height: 54rpx;
  margin: 0;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #374151;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 54rpx;
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
  height: 58rpx;
  margin: 0;
  padding: 0 30rpx;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.load-state {
  padding: 28rpx 0 10rpx;
  color: #9ca3af;
  font-size: 23rpx;
  text-align: center;
}
</style>
