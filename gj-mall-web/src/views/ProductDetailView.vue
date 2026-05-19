<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElImageViewer } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { addFavorite, getFavoriteStatus, removeFavorite } from '@/api/favorite'
import { recordHistory } from '@/api/history'
import {
  getProductCommentSummary,
  getProductComments,
  getProductDetail,
  type ProductComment,
  type ProductCommentSummary,
  type ProductDetail,
  type ProductSku,
} from '@/api/product'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const loading = ref(false)
const adding = ref(false)
const favoriteLoading = ref(false)
const commentsLoading = ref(false)
const quantity = ref(1)
const product = ref<ProductDetail>()
const isFavorite = ref(false)
const commentSummary = ref<ProductCommentSummary>()
const comments = ref<ProductComment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentFilter = ref<'all' | 'image'>('all')
const previewImages = ref<string[]>([])
const previewIndex = ref(0)
const selectedSkuId = ref<string | number>()
const activeImage = ref('')

const currentSku = computed(() => {
  return product.value?.skus?.find((sku) => String(sku.id) === String(selectedSkuId.value))
})
const currentStock = computed(() => Number(currentSku.value?.stock || 0))
const currentLockedStock = computed(() => Number(currentSku.value?.lockedStock || 0))
const canBuyCurrentSku = computed(() => Boolean(currentSku.value) && currentStock.value > 0)
const stockHint = computed(() => {
  if (!currentSku.value) {
    return '请选择商品规格'
  }
  if (currentStock.value <= 0) {
    return '该规格暂时无库存'
  }
  if (currentStock.value <= 5) {
    return `库存紧张，仅剩 ${currentStock.value} 件`
  }
  return `库存 ${currentStock.value} 件`
})

const gallery = computed(() => {
  const detail = product.value
  if (!detail) {
    return []
  }
  const images = [detail.mainImage, ...(detail.images || []), ...(detail.detailImages || [])]
  return Array.from(new Set(images.filter(Boolean))) as string[]
})

const hasMoreComments = computed(() => comments.value.length < commentTotal.value)
const goodRateText = computed(() => `${Number(commentSummary.value?.goodRate || 0).toFixed(1)}%`)
const averageScore = computed(() => Number(commentSummary.value?.averageScore || 0))

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
  if (!value) {
    return '-'
  }
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'gjmall') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/720/720`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/720/720`
}

function skuLabel(sku: ProductSku) {
  const specs = sku.specData ? Object.values(sku.specData).filter(Boolean).join(' / ') : ''
  return specs || sku.name || sku.skuCode || '默认规格'
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id as string)
    product.value = res.data
    selectedSkuId.value = res.data.skus?.[0]?.id
    activeImage.value = normalizeImage(res.data.mainImage || res.data.images?.[0], String(res.data.id))
    await Promise.all([loadComments(true), loadCommentSummary(), loadFavoriteStatus(), recordBrowseHistory()])
  } finally {
    loading.value = false
  }
}

async function loadCommentSummary() {
  if (!product.value?.id) return
  const res = await getProductCommentSummary(product.value.id)
  commentSummary.value = res.data
}

async function loadComments(reset = false) {
  if (!product.value?.id && !route.params.id) return
  if (reset) {
    commentPage.value = 1
    comments.value = []
  }
  commentsLoading.value = true
  try {
    const res = await getProductComments(product.value?.id || (route.params.id as string), {
      current: commentPage.value,
      size: 5,
      hasImage: commentFilter.value === 'image' ? true : undefined,
    })
    const list = res.data?.list || []
    comments.value = reset ? list : [...comments.value, ...list]
    commentTotal.value = Number(res.data?.total || 0)
    commentPage.value += 1
  } finally {
    commentsLoading.value = false
  }
}

async function loadFavoriteStatus() {
  if (!auth.isLoggedIn || !product.value?.id) {
    isFavorite.value = false
    return
  }
  const res = await getFavoriteStatus(product.value.id)
  isFavorite.value = Boolean(res.data)
}

async function recordBrowseHistory() {
  if (!auth.isLoggedIn || !product.value?.id) return
  try {
    await recordHistory(product.value.id)
  } catch {
    // 浏览足迹不影响商品详情主流程。
  }
}

function selectSku(sku: ProductSku) {
  if (Number(sku.stock || 0) <= 0) {
    ElMessage.warning('该规格暂无库存')
    return
  }
  selectedSkuId.value = sku.id
  quantity.value = 1
  if (sku.image) {
    activeImage.value = normalizeImage(sku.image, String(sku.id))
  }
}

async function addToCart(redirectToCart = false) {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  const sku = currentSku.value
  if (!sku) {
    ElMessage.warning('请选择商品规格')
    return
  }
  if (Number(sku.stock || 0) <= 0) {
    ElMessage.warning('该规格暂无库存')
    return
  }
  if (quantity.value > Number(sku.stock || 0)) {
    quantity.value = Number(sku.stock || 1)
    ElMessage.warning('购买数量已按库存上限调整')
    return
  }
  adding.value = true
  try {
    await cart.add(sku.id, quantity.value)
    if (redirectToCart) {
      router.push('/cart')
    }
  } finally {
    adding.value = false
  }
}

function buyNow() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  const sku = currentSku.value
  if (!sku) {
    ElMessage.warning('请选择商品规格')
    return
  }
  if (Number(sku.stock || 0) <= 0) {
    ElMessage.warning('该规格暂无库存')
    return
  }
  const safeQuantity = Math.min(quantity.value, Number(sku.stock || 1))
  router.push({
    path: '/checkout',
    query: {
      mode: 'direct',
      spuId: String(product.value?.id || ''),
      skuId: String(sku.id),
      quantity: String(safeQuantity),
    },
  })
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  if (!product.value?.id) return
  favoriteLoading.value = true
  try {
    if (isFavorite.value) {
      await removeFavorite(product.value.id)
      isFavorite.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(product.value.id)
      isFavorite.value = true
      ElMessage.success('已收藏')
    }
  } finally {
    favoriteLoading.value = false
  }
}

function switchCommentFilter(filter: 'all' | 'image') {
  commentFilter.value = filter
  loadComments(true)
}

function previewCommentImages(images: string[] = [], index = 0) {
  previewImages.value = images.map((image, imageIndex) => normalizeImage(image, `comment-${imageIndex}`))
  previewIndex.value = index
}

function closePreview() {
  previewImages.value = []
  previewIndex.value = 0
}

function commentSpec(comment: ProductComment) {
  const values = comment.specData ? Object.values(comment.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : comment.skuName || '默认规格'
}

function maskUser(userId?: string | number) {
  const raw = String(userId || '会员')
  return raw === '会员' ? raw : `会员 ${raw.slice(-4)}`
}

onMounted(loadDetail)
</script>

<template>
  <div class="detail-page">
    <ShopHeader />

    <main v-loading="loading">
      <section v-if="product" class="detail-shell">
        <div class="breadcrumb">
          <button type="button" @click="router.push('/')">首页</button>
          <span>/</span>
          <span>{{ product.categoryName || '商品详情' }}</span>
          <span>/</span>
          <strong>{{ product.name }}</strong>
        </div>

        <section class="product-hero">
          <div class="gallery-panel">
            <div class="main-image">
              <img
                :src="normalizeImage(activeImage || product.mainImage, String(product.id))"
                :alt="product.name"
                @error="handleImageError($event, String(product.id))"
              />
            </div>
            <div class="thumb-row">
              <button
                v-for="image in gallery.slice(0, 6)"
                :key="image"
                class="thumb"
                :class="{ active: normalizeImage(image, String(product?.id)) === activeImage }"
                type="button"
                @click="activeImage = normalizeImage(image, String(product?.id))"
              >
                <img
                  :src="normalizeImage(image, String(product.id))"
                  :alt="product.name"
                  @error="handleImageError($event, String(product.id))"
                />
              </button>
            </div>
          </div>

          <div class="buy-panel">
            <div class="tag-row">
              <span v-if="product.newStatus === 1">新品</span>
              <span v-if="product.recommendStatus === 1" class="hot">推荐</span>
              <span>{{ product.brandName || '精选品牌' }}</span>
            </div>

            <h1>{{ product.name }}</h1>
            <p>{{ product.subTitle }}</p>

            <div class="price-box">
              <strong>{{ formatPrice(currentSku?.price || product.price) }}</strong>
              <span>已售 {{ formatSales(product.saleCount) }}</span>
            </div>

            <div v-if="product.skus?.length" class="sku-section">
              <div class="section-title">选择规格</div>
              <div class="sku-grid">
                <button
                  v-for="sku in product.skus"
                  :key="sku.id"
                  class="sku-chip"
                  :class="{ active: String(selectedSkuId) === String(sku.id), disabled: Number(sku.stock || 0) <= 0 }"
                  type="button"
                  @click="selectSku(sku)"
                >
                  <strong>{{ skuLabel(sku) }}</strong>
                  <small>{{ Number(sku.stock || 0) > 0 ? `库存 ${sku.stock ?? 0}` : '暂无库存' }}</small>
                </button>
              </div>
            </div>

            <div class="quantity-section">
              <div class="section-title">购买数量</div>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="Math.max(1, currentStock || 1)"
                :disabled="!canBuyCurrentSku"
                controls-position="right"
              />
              <div class="stock-tip" :class="{ warning: currentStock <= 5, empty: currentStock <= 0 }">
                {{ stockHint }}
                <span v-if="currentLockedStock > 0">，{{ currentLockedStock }} 件正在锁定待支付</span>
              </div>
            </div>

            <div class="service-row">
              <span>正品保障</span>
              <span>极速发货</span>
              <span>售后无忧</span>
            </div>

            <div class="buy-actions">
              <el-button size="large" type="primary" :disabled="!canBuyCurrentSku" @click="buyNow">立即购买</el-button>
              <el-button size="large" :loading="adding" :disabled="!canBuyCurrentSku" @click="addToCart(false)">加入购物车</el-button>
              <el-button class="favorite-action" size="large" :loading="favoriteLoading" @click="toggleFavorite">
                {{ isFavorite ? '已收藏' : '收藏商品' }}
              </el-button>
            </div>
          </div>
        </section>

        <section class="detail-content">
          <div class="detail-main">
            <section class="comment-section">
              <div class="section-heading comment-heading">
                <div>
                  <span>Reviews</span>
                  <h2>买家评价</h2>
                </div>
                <div class="comment-score">
                  <strong>{{ averageScore.toFixed(1) }}</strong>
                  <span>{{ goodRateText }} 好评</span>
                </div>
              </div>

              <div class="comment-tabs">
                <button :class="{ active: commentFilter === 'all' }" type="button" @click="switchCommentFilter('all')">
                  全部 {{ commentSummary?.total || 0 }}
                </button>
                <button :class="{ active: commentFilter === 'image' }" type="button" @click="switchCommentFilter('image')">
                  有图 {{ commentSummary?.imageCount || 0 }}
                </button>
              </div>

              <div v-loading="commentsLoading" class="comment-list">
                <article v-for="comment in comments" :key="comment.id" class="comment-card">
                  <div class="comment-user">
                    <span>{{ maskUser(comment.userId) }}</span>
                    <el-rate :model-value="comment.score" disabled />
                  </div>
                  <p>{{ comment.content }}</p>
                  <small>{{ commentSpec(comment) }} · {{ formatTime(comment.createTime) }}</small>
                  <div v-if="comment.images?.length" class="comment-images">
                    <button
                      v-for="(image, index) in comment.images"
                      :key="image"
                      type="button"
                      @click="previewCommentImages(comment.images, index)"
                    >
                      <img
                        :src="normalizeImage(image, `comment-${comment.id}-${index}`)"
                        :alt="comment.content"
                        @error="handleImageError($event, `comment-${comment.id}-${index}`)"
                      />
                    </button>
                  </div>
                  <div v-if="comment.replyContent" class="merchant-reply">
                    <strong>商家回复</strong>
                    <span>{{ comment.replyContent }}</span>
                  </div>
                </article>

                <el-empty v-if="!commentsLoading && !comments.length" description="暂无评价" />
              </div>

              <button v-if="hasMoreComments" class="more-comment" type="button" @click="loadComments(false)">
                查看更多评价
              </button>
            </section>

            <div class="section-heading">
              <span>Details</span>
              <h2>商品详情</h2>
            </div>
            <div v-if="product.detailHtml" class="rich-detail" v-html="product.detailHtml" />
            <div v-else class="detail-fallback">
              <p>{{ product.subTitle || '这件商品暂未配置图文详情，基础信息已经接入真实商品接口。' }}</p>
            </div>

            <div v-if="product.detailImages?.length" class="detail-images">
              <img
                v-for="image in product.detailImages"
                :key="image"
                :src="normalizeImage(image, String(product.id))"
                :alt="product.name"
                @error="handleImageError($event, String(product.id))"
              />
            </div>
          </div>

          <aside class="promise-panel">
            <div class="section-heading compact">
              <span>Service</span>
              <h2>购买保障</h2>
            </div>
            <dl>
              <div>
                <dt>包装清单</dt>
                <dd>{{ product.packingList || '商品主件、说明资料、官方包装' }}</dd>
              </div>
              <div>
                <dt>售后服务</dt>
                <dd>{{ product.afterSale || '支持平台售后流程，订单、退款、退货可在商城系统内闭环处理。' }}</dd>
              </div>
              <div>
                <dt>库存状态</dt>
                <dd>{{ stockHint }}</dd>
              </div>
            </dl>
          </aside>
        </section>
      </section>

      <el-empty v-else-if="!loading" description="商品不存在">
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </el-empty>
    </main>

    <el-image-viewer
      v-if="previewImages.length"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="closePreview"
    />
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

main {
  min-height: calc(100vh - 72px);
}

.detail-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px;
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 22px;
  color: #6b7280;
  font-size: 14px;
}

.breadcrumb button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #e5484d;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
}

.breadcrumb strong {
  color: #111827;
}

.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 440px;
  gap: 30px;
  align-items: start;
}

.gallery-panel,
.buy-panel,
.detail-main,
.promise-panel {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.gallery-panel {
  padding: 18px;
}

.main-image {
  overflow: hidden;
  border-radius: 8px;
  background: #f3f4f6;
}

.main-image img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.thumb-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.thumb,
.sku-chip {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.thumb {
  overflow: hidden;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 8px;
  background: #f3f4f6;
}

.thumb.active,
.thumb:hover {
  border-color: #e5484d;
}

.thumb img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.buy-panel {
  padding: 28px;
}

.tag-row,
.service-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span,
.service-row span {
  min-height: 28px;
  padding: 5px 10px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-weight: 900;
}

.tag-row span.hot {
  background: #fff0f0;
  color: #e5484d;
}

.buy-panel h1 {
  margin: 18px 0 0;
  font-size: 34px;
  line-height: 1.18;
  letter-spacing: 0;
}

.buy-panel p {
  margin: 14px 0 0;
  color: #4b5563;
  font-size: 16px;
  line-height: 1.75;
}

.price-box {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  padding: 18px;
  border-radius: 8px;
  background: #111827;
  color: #fff;
}

.price-box strong {
  color: #ffd166;
  font-size: 32px;
}

.price-box span {
  color: rgba(255, 255, 255, 0.76);
}

.sku-section {
  margin-top: 24px;
}

.quantity-section {
  margin-top: 22px;
}

.stock-tip {
  margin-top: 10px;
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
}

.stock-tip.warning {
  color: #b7791f;
}

.stock-tip.empty {
  color: #e5484d;
}

.section-title {
  margin-bottom: 12px;
  color: #111827;
  font-weight: 900;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sku-chip {
  min-height: 68px;
  padding: 12px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 8px;
  background: #fff;
  color: #111827;
  text-align: left;
}

.sku-chip.active,
.sku-chip:hover {
  border-color: #e5484d;
  box-shadow: 0 0 0 2px rgba(229, 72, 77, 0.08);
}

.sku-chip.disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.sku-chip.disabled:hover {
  border-color: rgba(17, 24, 39, 0.1);
  box-shadow: none;
}

.sku-chip strong,
.sku-chip small {
  display: block;
}

.sku-chip small {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
}

.service-row {
  margin-top: 22px;
}

.buy-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.buy-actions :deep(.el-button) {
  height: 46px;
  border-radius: 8px;
  font-weight: 900;
}

.buy-actions :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.buy-actions :deep(.favorite-action) {
  margin-left: 0;
}

.comment-section {
  margin-bottom: 28px;
  padding-bottom: 28px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.comment-heading {
  align-items: center;
}

.comment-score {
  min-width: 112px;
  text-align: right;
}

.comment-score strong,
.comment-score span {
  display: block;
}

.comment-score strong {
  color: #e5484d;
  font-size: 32px;
  line-height: 1;
}

.comment-score span {
  margin-top: 7px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 900;
}

.comment-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding: 4px;
  border-radius: 8px;
  background: #f3f4f6;
}

.comment-tabs button,
.more-comment,
.comment-images button {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.comment-tabs button {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  background: transparent;
  color: #4b5563;
  font-weight: 900;
}

.comment-tabs button.active,
.comment-tabs button:hover {
  background: #111827;
  color: #fff;
}

.comment-list {
  min-height: 120px;
}

.comment-card {
  padding: 18px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.comment-card:first-child {
  border-top: 0;
}

.comment-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.comment-user span {
  color: #111827;
  font-weight: 900;
}

.comment-card p {
  margin: 12px 0 0;
  color: #374151;
  line-height: 1.75;
}

.comment-card small {
  display: block;
  margin-top: 9px;
  color: #6b7280;
}

.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.comment-images button {
  overflow: hidden;
  width: 78px;
  height: 78px;
  padding: 0;
  border-radius: 8px;
  background: #f3f4f6;
}

.comment-images img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.merchant-reply {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f8f5;
  color: #4b5563;
  font-size: 13px;
}

.merchant-reply strong {
  color: #2f8f67;
}

.more-comment {
  width: 100%;
  min-height: 42px;
  margin-top: 10px;
  border-radius: 8px;
  background: #111827;
  color: #fff;
  font-weight: 900;
}

.detail-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  margin-top: 28px;
  align-items: start;
}

.detail-main,
.promise-panel {
  padding: 24px;
}

.section-heading {
  margin-bottom: 18px;
}

.section-heading span {
  display: inline-flex;
  margin-bottom: 10px;
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.section-heading h2 {
  margin: 0;
  font-size: 26px;
}

.section-heading.compact h2 {
  font-size: 22px;
}

.rich-detail,
.detail-fallback {
  color: #374151;
  line-height: 1.9;
}

.rich-detail :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.detail-images {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.detail-images img {
  width: 100%;
  border-radius: 8px;
}

.promise-panel {
  position: sticky;
  top: 96px;
}

.promise-panel dl,
.promise-panel dd {
  margin: 0;
}

.promise-panel div + div {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.promise-panel dt {
  color: #111827;
  font-weight: 900;
}

.promise-panel dd {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .product-hero,
  .detail-content {
    grid-template-columns: 1fr;
  }

  .promise-panel {
    position: static;
  }
}

@media (max-width: 680px) {
  .detail-shell {
    padding: 18px 16px 32px;
  }

  .buy-panel {
    padding: 20px;
  }

  .buy-panel h1 {
    font-size: 28px;
  }

  .thumb-row,
  .sku-grid,
  .buy-actions {
    grid-template-columns: 1fr;
  }

  .comment-heading,
  .comment-user {
    align-items: start;
    flex-direction: column;
  }

  .comment-score {
    text-align: left;
  }
}
</style>
