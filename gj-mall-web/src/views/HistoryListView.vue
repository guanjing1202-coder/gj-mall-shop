<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { clearHistory, getHistoryPage, removeHistory, type HistoryItem } from '@/api/history'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const operating = ref(false)
const removingId = ref<string | number>('')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const histories = ref<HistoryItem[]>([])

const hasHistories = computed(() => histories.value.length > 0)

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}

function formatTime(value?: string) {
  if (!value) return '-'
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'history') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/480/480`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/480/480`
}

async function loadHistories() {
  if (!auth.isLoggedIn) {
    histories.value = []
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getHistoryPage({
      current: pageNum.value,
      size: pageSize.value,
    })
    histories.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function removeItem(item: HistoryItem) {
  removingId.value = item.spuId
  try {
    await removeHistory(item.spuId)
    ElMessage.success('已移除足迹')
    await loadHistories()
  } finally {
    removingId.value = ''
  }
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('确认清空全部浏览足迹？', '清空足迹', {
      confirmButtonText: '清空',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  operating.value = true
  try {
    await clearHistory()
    ElMessage.success('足迹已清空')
    pageNum.value = 1
    await loadHistories()
  } finally {
    operating.value = false
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadHistories()
    } else {
      histories.value = []
    }
  },
)

onMounted(loadHistories)
</script>

<template>
  <div class="history-page">
    <ShopHeader />

    <main class="history-shell">
      <section class="history-hero">
        <div>
          <span>History</span>
          <h1>浏览足迹</h1>
          <p>最近看过的商品会记录在这里，方便回头比较价格、库存和评价。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/products')">继续挑选</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看足迹</h2>
        <p>足迹会同步到当前会员账户，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="history-panel">
        <div class="panel-heading">
          <div>
            <span>Recently Viewed</span>
            <h2>{{ total }} 条记录</h2>
          </div>
          <el-button :disabled="!hasHistories" :loading="operating" @click="clearAll">清空足迹</el-button>
        </div>

        <div v-loading="loading" class="history-list">
          <article v-for="item in histories" :key="item.spuId" class="history-card">
            <button class="cover" type="button" @click="router.push(`/product/${item.spuId}`)">
              <img
                :src="normalizeImage(item.mainImage, String(item.spuId))"
                :alt="item.spuName"
                @error="handleImageError($event, String(item.spuId))"
              />
            </button>
            <div class="card-copy">
              <span>{{ item.categoryName || item.brandName || '精选商品' }}</span>
              <strong>{{ item.spuName || '商品' }}</strong>
              <p>{{ item.subTitle || '继续查看商品详情、规格和评价。' }}</p>
              <small>浏览时间 {{ formatTime(item.browseTime) }}</small>
            </div>
            <div class="card-side">
              <strong>{{ formatPrice(item.price) }}</strong>
              <el-button type="primary" @click="router.push(`/product/${item.spuId}`)">继续看</el-button>
              <el-button :loading="String(removingId) === String(item.spuId)" @click="removeItem(item)">
                移除
              </el-button>
            </div>
          </article>

          <el-empty v-if="!loading && !hasHistories" description="暂无浏览足迹">
            <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
          </el-empty>
        </div>

        <div v-if="total > pageSize" class="pagination-row">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadHistories"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.history-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.history-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.history-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.history-hero span,
.panel-heading span,
.card-copy > span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.history-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.history-hero p {
  margin: 12px 0 0;
  color: #6b7280;
}

.back-link,
.cover {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.back-link {
  color: #e5484d;
  font-weight: 900;
}

.login-needed,
.history-panel,
.history-card {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.login-needed {
  padding: 44px;
}

.login-needed h2 {
  margin: 0;
  font-size: 28px;
}

.login-needed p {
  margin: 12px 0 24px;
  color: #6b7280;
}

.history-panel {
  padding: 20px;
}

.panel-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  margin: 8px 0 0;
  font-size: 26px;
}

.panel-heading :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.history-list {
  display: grid;
  gap: 14px;
  min-height: 320px;
}

.history-card {
  display: grid;
  grid-template-columns: 136px minmax(0, 1fr) 150px;
  gap: 18px;
  align-items: center;
  padding: 14px;
}

.cover {
  overflow: hidden;
  padding: 0;
  border-radius: 8px;
  background: #f3f4f6;
}

.cover img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.card-copy {
  min-width: 0;
}

.card-copy strong,
.card-copy p,
.card-copy small {
  display: block;
}

.card-copy strong {
  margin-top: 8px;
  overflow: hidden;
  font-size: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-copy p {
  display: -webkit-box;
  margin: 10px 0 0;
  overflow: hidden;
  color: #4b5563;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-copy small {
  margin-top: 10px;
  color: #6b7280;
}

.card-side {
  display: grid;
  gap: 10px;
}

.card-side strong {
  color: #e5484d;
  font-size: 24px;
  text-align: right;
}

.card-side :deep(.el-button) {
  width: 100%;
  margin-left: 0;
  border-radius: 8px;
  font-weight: 900;
}

.login-needed :deep(.el-button--primary),
.history-list :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 22px;
}

@media (max-width: 760px) {
  .history-shell {
    padding: 28px 16px 52px;
  }

  .history-hero,
  .panel-heading {
    align-items: start;
    flex-direction: column;
  }

  .history-card {
    grid-template-columns: 1fr;
  }

  .card-side strong {
    text-align: left;
  }
}
</style>
