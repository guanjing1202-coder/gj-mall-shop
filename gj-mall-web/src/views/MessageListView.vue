<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  deleteMessage,
  getMessagePage,
  markAllMessagesRead,
  markMessageRead,
  type UserMessage,
} from '@/api/message'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const messages = ref<UserMessage[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const readStatus = ref<number | undefined>()

const unreadTotal = computed(() => messages.value.filter((item) => Number(item.readStatus) === 0).length)

onMounted(loadMessages)

async function loadMessages() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getMessagePage({ current: current.value, size: pageSize.value, readStatus: readStatus.value })
    messages.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function readMessage(item: UserMessage) {
  if (Number(item.readStatus) === 0) {
    await markMessageRead(item.id)
    item.readStatus = 1
  }
  openBiz(item)
}

async function readAll() {
  await markAllMessagesRead()
  ElMessage.success('已全部标记为已读')
  await loadMessages()
}

async function removeMessage(item: UserMessage) {
  await deleteMessage(item.id)
  ElMessage.success('消息已删除')
  await loadMessages()
}

function openBiz(item: UserMessage) {
  if (item.bizType === 'order' && item.bizId) {
    router.push(`/order/${item.bizId}`)
  } else if (item.bizType === 'after_sale') {
    router.push('/after-sales')
  }
}

function switchFilter(value?: number) {
  readStatus.value = value
  current.value = 1
  loadMessages()
}

function onPageChange(page: number) {
  current.value = page
  loadMessages()
}

function formatTime(value?: string) {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <div class="message-page">
    <ShopHeader />

    <main class="message-shell">
      <section class="message-hero">
        <div>
          <span>Notification</span>
          <h1>消息中心</h1>
          <p>订单、支付、物流和售后状态都会在这里同步。</p>
        </div>
        <div class="hero-actions">
          <el-button @click="switchFilter(undefined)">全部</el-button>
          <el-button @click="switchFilter(0)">未读</el-button>
          <el-button @click="switchFilter(1)">已读</el-button>
          <el-button type="primary" :disabled="!unreadTotal" @click="readAll">全部已读</el-button>
        </div>
      </section>

      <section v-loading="loading" class="message-panel">
        <div v-if="messages.length" class="message-list">
          <article
            v-for="item in messages"
            :key="item.id"
            class="message-card"
            :class="{ unread: Number(item.readStatus) === 0 }"
          >
            <button type="button" class="message-main" @click="readMessage(item)">
              <span>{{ item.typeDesc || '系统通知' }}</span>
              <strong>{{ item.title || '消息提醒' }}</strong>
              <p>{{ item.content || '-' }}</p>
              <em>{{ formatTime(item.createTime) }} <template v-if="item.bizNo">· {{ item.bizNo }}</template></em>
            </button>
            <div class="message-actions">
              <el-tag v-if="Number(item.readStatus) === 0" type="danger">未读</el-tag>
              <el-tag v-else>已读</el-tag>
              <el-button text type="primary" @click="readMessage(item)">查看</el-button>
              <el-button text type="danger" @click="removeMessage(item)">删除</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无消息" />

        <div v-if="total > pageSize" class="pager">
          <el-pagination
            layout="prev, pager, next"
            :total="total"
            :current-page="current"
            :page-size="pageSize"
            @current-change="onPageChange"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.message-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.message-shell {
  max-width: 1040px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.message-hero,
.message-panel {
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.07);
}

.message-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 26px;
}

.message-hero span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.message-hero h1 {
  margin: 12px 0 0;
  font-size: 38px;
  line-height: 1.1;
}

.message-hero p {
  margin: 10px 0 0;
  color: #6b7280;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.message-panel {
  min-height: 360px;
  padding: 18px;
}

.message-list {
  display: grid;
  gap: 12px;
}

.message-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 14px;
  background: #fbfcf8;
}

.message-card.unread {
  border-color: rgba(229, 72, 77, 0.28);
  background: #fff8f8;
}

.message-main {
  min-width: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.message-main span,
.message-main em {
  display: block;
  color: #6b7280;
  font-size: 12px;
  font-style: normal;
}

.message-main strong {
  display: block;
  margin: 8px 0;
  color: #111827;
  font-size: 18px;
}

.message-main p {
  margin: 0;
  color: #374151;
  line-height: 1.7;
}

.message-main em {
  margin-top: 10px;
}

.message-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .message-shell {
    padding: 28px 16px 52px;
  }

  .message-card {
    grid-template-columns: 1fr;
  }

  .message-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .message-actions {
    justify-content: flex-start;
  }
}
</style>
