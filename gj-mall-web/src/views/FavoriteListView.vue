<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { getFavoritePage, removeFavorite, type FavoriteItem } from '@/api/favorite'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const removingId = ref<string | number>('')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const favorites = ref<FavoriteItem[]>([])

const hasFavorites = computed(() => favorites.value.length > 0)

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}

function formatSales(value?: number) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`
  }
  return String(count)
}

function formatTime(value?: string) {
  if (!value) return '-'
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'favorite') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/480/480`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/480/480`
}

async function loadFavorites() {
  if (!auth.isLoggedIn) {
    favorites.value = []
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getFavoritePage({
      current: pageNum.value,
      size: pageSize.value,
    })
    favorites.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function removeItem(item: FavoriteItem) {
  try {
    await ElMessageBox.confirm(`确认取消收藏「${item.spuName || '该商品'}」？`, '取消收藏', {
      confirmButtonText: '取消收藏',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  removingId.value = item.spuId
  try {
    await removeFavorite(item.spuId)
    ElMessage.success('已取消收藏')
    await loadFavorites()
  } finally {
    removingId.value = ''
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadFavorites()
    } else {
      favorites.value = []
    }
  },
)

onMounted(loadFavorites)
</script>

<template>
  <div class="favorite-page">
    <ShopHeader />

    <main class="favorite-shell">
      <section class="favorite-hero">
        <div>
          <span>Favorites</span>
          <h1>我的收藏</h1>
          <p>把想反复比较的商品先收起来，之后可以直接继续加购。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/products')">继续挑选</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看收藏</h2>
        <p>收藏商品会绑定到会员账户，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="favorite-panel">
        <div class="panel-heading">
          <div>
            <span>Saved Items</span>
            <h2>{{ total }} 件收藏</h2>
          </div>
        </div>

        <div v-loading="loading" class="favorite-grid">
          <article v-for="item in favorites" :key="item.spuId" class="favorite-card">
            <button class="cover" type="button" @click="router.push(`/product/${item.spuId}`)">
              <img
                :src="normalizeImage(item.mainImage, String(item.spuId))"
                :alt="item.spuName"
                @error="handleImageError($event, String(item.spuId))"
              />
            </button>
            <div class="card-copy">
              <strong>{{ item.spuName || '商品' }}</strong>
              <span>{{ item.subTitle || item.categoryName || '精选商品' }}</span>
              <small>{{ item.brandName || '精选品牌' }} · 收藏于 {{ formatTime(item.createTime) }}</small>
            </div>
            <div class="card-foot">
              <div>
                <strong>{{ formatPrice(item.price) }}</strong>
                <span>已售 {{ formatSales(item.saleCount) }}</span>
              </div>
              <div>
                <el-button @click="router.push(`/product/${item.spuId}`)">查看</el-button>
                <el-button
                  :loading="String(removingId) === String(item.spuId)"
                  @click="removeItem(item)"
                >
                  取消收藏
                </el-button>
              </div>
            </div>
          </article>

          <el-empty v-if="!loading && !hasFavorites" description="暂无收藏商品">
            <el-button type="primary" @click="router.push('/products')">去挑选</el-button>
          </el-empty>
        </div>

        <div v-if="total > pageSize" class="pagination-row">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadFavorites"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.favorite-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.favorite-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.favorite-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.favorite-hero span,
.panel-heading span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.favorite-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.favorite-hero p {
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
.favorite-panel,
.favorite-card {
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

.favorite-panel {
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

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 320px;
}

.favorite-card {
  overflow: hidden;
}

.cover {
  display: block;
  width: 100%;
  padding: 0;
  background: #f3f4f6;
}

.cover img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.card-copy {
  display: grid;
  gap: 8px;
  padding: 14px;
}

.card-copy strong {
  display: -webkit-box;
  min-height: 44px;
  overflow: hidden;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-copy span,
.card-copy small {
  display: -webkit-box;
  overflow: hidden;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-foot {
  display: grid;
  gap: 12px;
  padding: 0 14px 14px;
}

.card-foot > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-foot > div:first-child strong {
  color: #e5484d;
  font-size: 20px;
}

.card-foot > div:first-child span {
  color: #6b7280;
  font-size: 12px;
}

.card-foot > div:last-child {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.card-foot :deep(.el-button) {
  width: 100%;
  margin-left: 0;
  border-radius: 8px;
  font-weight: 900;
}

.login-needed :deep(.el-button--primary),
.favorite-grid :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 22px;
}

@media (max-width: 1080px) {
  .favorite-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .favorite-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .favorite-shell {
    padding: 28px 16px 52px;
  }

  .favorite-hero,
  .panel-heading {
    align-items: start;
    flex-direction: column;
  }

  .favorite-grid {
    grid-template-columns: 1fr;
  }
}
</style>
