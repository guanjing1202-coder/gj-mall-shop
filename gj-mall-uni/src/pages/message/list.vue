<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">Notification</text>
        <text class="title">消息中心</text>
        <text class="subtitle">{{ currentFilterText }} · {{ selectedTypeLabel }}</text>
      </view>
      <button v-if="isLoggedIn" :disabled="!unreadTotal" @tap="readAll">{{ unreadTotal ? '全部已读' : '无未读' }}</button>
    </view>

    <view v-if="isLoggedIn" class="summary-grid">
      <view class="summary-cell" @tap="switchFilter(undefined)">
        <text>全部</text>
        <strong>{{ summary.total }}</strong>
      </view>
      <view class="summary-cell danger" @tap="switchFilter(0)">
        <text>未读</text>
        <strong>{{ summary.unreadTotal }}</strong>
      </view>
      <view class="summary-cell" @tap="switchFilter(1)">
        <text>已读</text>
        <strong>{{ summary.readTotal }}</strong>
      </view>
    </view>

    <scroll-view scroll-x class="tabs">
      <view class="tab-row">
        <view
          v-for="item in filters"
          :key="item.label"
          class="tab"
          :class="{ active: readStatus === item.value }"
          @tap="switchFilter(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <scroll-view v-if="isLoggedIn" scroll-x class="type-tabs">
      <view class="type-row">
        <view
          v-for="item in typeFilters"
          :key="item.type || 'all'"
          class="type-tab"
          :class="{ active: selectedType === item.type }"
          @tap="switchType(item.type)"
        >
          <text>{{ item.typeDesc }}</text>
          <strong>{{ item.total }}</strong>
          <em v-if="item.unreadTotal">{{ item.unreadTotal }} 未读</em>
        </view>
      </view>
    </scroll-view>

    <view v-if="!isLoggedIn" class="empty-card">
      <text>登录后查看消息</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view v-if="loading && !messages.length" class="empty">加载中...</view>
      <view v-else-if="!messages.length" class="empty-card">
        <text>暂无消息</text>
        <button @tap="goHome">去逛逛</button>
      </view>

      <view v-else class="list">
        <view
          v-for="item in messages"
          :key="item.id"
          class="message-card"
          :class="{ unread: Number(item.readStatus) === 0 }"
          @tap="openMessage(item)"
        >
          <view class="message-main">
            <view class="message-head">
              <text>{{ item.typeDesc || '系统通知' }}</text>
              <text>{{ Number(item.readStatus) === 0 ? '未读' : '已读' }}</text>
            </view>
            <text class="message-title">{{ item.title || '消息提醒' }}</text>
            <text class="message-content">{{ item.content || '-' }}</text>
            <text class="message-time">{{ formatTime(item.createTime) }}{{ item.bizNo ? ` · ${item.bizNo}` : '' }}</text>
          </view>
          <view class="message-actions">
            <button @tap.stop="openMessage(item)">查看</button>
            <button class="ghost" @tap.stop="removeMessage(item)">删除</button>
          </view>
        </view>
      </view>

      <view v-if="messages.length" class="load-state">
        <text>{{ finished ? '已经到底啦' : loading ? '继续加载...' : '上拉加载更多' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import {
  deleteMessage,
  getMessagePage,
  getMessageSummary,
  markAllMessagesRead,
  markMessageRead,
  type MessageSummary,
  type UserMessage,
} from '@/api/message'
import { syncSession } from '@/utils/session'

const filters = [
  { label: '全部', value: undefined },
  { label: '未读', value: 0 },
  { label: '已读', value: 1 },
]

const isLoggedIn = ref(false)
const loading = ref(false)
const messages = ref<UserMessage[]>([])
const pageNum = ref(1)
const total = ref(0)
const finished = ref(false)
const readStatus = ref<number | undefined>()
const selectedType = ref<string | undefined>()
const summary = ref<MessageSummary>({
  total: 0,
  unreadTotal: 0,
  readTotal: 0,
  typeItems: [],
})

const unreadTotal = computed(() => Number(summary.value.unreadTotal || 0))
const typeFilters = computed(() => [
  {
    type: undefined,
    typeDesc: '全部',
    total: Number(summary.value.total || 0),
    unreadTotal: Number(summary.value.unreadTotal || 0),
  },
  ...(summary.value.typeItems || []),
])
const currentFilterText = computed(() => {
  if (readStatus.value === 0) return '未读消息'
  if (readStatus.value === 1) return '已读消息'
  return '全部消息'
})
const selectedTypeLabel = computed(() => {
  if (!selectedType.value) return '全部类型'
  return typeFilters.value.find((item) => item.type === selectedType.value)?.typeDesc || '全部类型'
})

onShow(async () => {
  isLoggedIn.value = await syncSession()
  if (!isLoggedIn.value) {
    messages.value = []
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
    loadMessages(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await loadMessages(true)
}

async function loadMessages(reset: boolean) {
  loading.value = true
  try {
    const [res, summaryRes] = await Promise.all([
      getMessagePage({ current: pageNum.value, size: 10, readStatus: readStatus.value, type: selectedType.value }),
      getMessageSummary(),
    ])
    const page = res.data
    const list = page?.list || []
    messages.value = reset ? list : [...messages.value, ...list]
    total.value = Number(page?.total || 0)
    summary.value = summaryRes.data || summary.value
    finished.value = messages.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

async function switchFilter(value?: number) {
  readStatus.value = value
  await refresh()
}

async function switchType(type?: string) {
  selectedType.value = type
  await refresh()
}

async function readAll() {
  if (!unreadTotal.value) return
  await markAllMessagesRead()
  uni.showToast({ title: '已全部已读', icon: 'success' })
  await refresh()
}

async function openMessage(item: UserMessage) {
  if (Number(item.readStatus) === 0) {
    await markMessageRead(item.id)
    item.readStatus = 1
    await refresh()
  }
  if (item.bizType === 'order' && item.bizId) {
    uni.navigateTo({ url: `/pages/order/detail?id=${item.bizId}` })
  } else if (item.bizType === 'after_sale') {
    uni.navigateTo({ url: '/pages/after-sales/list' })
  }
}

async function removeMessage(item: UserMessage) {
  await deleteMessage(item.id)
  messages.value = messages.value.filter((message) => String(message.id) !== String(item.id))
  total.value = Math.max(0, total.value - 1)
  uni.showToast({ title: '消息已删除', icon: 'success' })
  await refresh()
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function formatTime(value?: string) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
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
.summary-grid,
.type-tabs,
.message-card,
.empty-card {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  padding: 34rpx 28rpx;
  background: #111827;
  color: #fff;
}

.kicker,
.title,
.subtitle,
.message-title,
.message-content,
.message-time {
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

.hero button {
  flex: 0 0 auto;
  height: 60rpx;
  margin: 0;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #111827;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 60rpx;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rpx;
  margin-top: 18rpx;
  overflow: hidden;
}

.summary-cell {
  padding: 22rpx 24rpx;
  background: #fff;
}

.summary-cell text {
  display: block;
  color: #6b7280;
  font-size: 22rpx;
  font-weight: 900;
}

.summary-cell strong {
  display: block;
  margin-top: 8rpx;
  color: #111827;
  font-size: 40rpx;
  line-height: 1;
}

.summary-cell.danger strong {
  color: #e5484d;
}

.tabs {
  width: 100%;
  margin: 18rpx 0 14rpx;
  white-space: nowrap;
}

.tab-row,
.type-row {
  display: flex;
  gap: 12rpx;
}

.tab {
  display: inline-flex;
  height: 62rpx;
  align-items: center;
  padding: 0 30rpx;
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

.type-tabs {
  width: 100%;
  margin-bottom: 22rpx;
  padding: 12rpx;
  white-space: nowrap;
}

.type-tab {
  display: inline-flex;
  min-width: 176rpx;
  flex-direction: column;
  gap: 6rpx;
  padding: 18rpx 20rpx;
  border: 2rpx solid transparent;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.type-tab.active {
  border-color: rgba(229, 72, 77, 0.32);
  background: #fff8f8;
}

.type-tab text {
  color: #6b7280;
  font-size: 21rpx;
  font-weight: 900;
}

.type-tab strong {
  color: #111827;
  font-size: 31rpx;
  line-height: 1;
}

.type-tab em {
  color: #e5484d;
  font-size: 20rpx;
  font-style: normal;
  font-weight: 900;
}

.list {
  display: grid;
  gap: 18rpx;
}

.message-card {
  padding: 22rpx;
  border: 2rpx solid transparent;
}

.message-card.unread {
  border-color: rgba(229, 72, 77, 0.3);
  background: #fff8f8;
}

.message-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.message-head text {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 21rpx;
  font-weight: 900;
}

.message-head text:last-child {
  background: #f3f4f6;
  color: #6b7280;
}

.message-card.unread .message-head text:last-child {
  background: #e5484d;
  color: #fff;
}

.message-title {
  margin-top: 14rpx;
  font-size: 31rpx;
  font-weight: 900;
  line-height: 1.35;
}

.message-content {
  margin-top: 10rpx;
  color: #374151;
  font-size: 25rpx;
  line-height: 1.65;
}

.message-time {
  margin-top: 12rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 18rpx;
}

.message-actions button,
.empty-card button {
  height: 58rpx;
  margin: 0;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.message-actions button.ghost {
  background: #f3f4f6;
  color: #374151;
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
}

.load-state {
  padding: 28rpx 0 10rpx;
  color: #9ca3af;
  font-size: 23rpx;
  text-align: center;
}
</style>
