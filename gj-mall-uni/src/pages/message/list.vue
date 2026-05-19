<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">Notification</text>
        <text class="title">消息中心</text>
        <text class="subtitle">订单、支付、物流和售后状态会同步到这里。</text>
      </view>
      <button v-if="isLoggedIn" :disabled="!unreadTotal" @tap="readAll">{{ unreadTotal ? '全部已读' : '无未读' }}</button>
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
  markAllMessagesRead,
  markMessageRead,
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

const unreadTotal = computed(() => messages.value.filter((item) => Number(item.readStatus) === 0).length)

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
    const res = await getMessagePage({ current: pageNum.value, size: 10, readStatus: readStatus.value })
    const page = res.data
    const list = page?.list || []
    messages.value = reset ? list : [...messages.value, ...list]
    total.value = Number(page?.total || 0)
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
