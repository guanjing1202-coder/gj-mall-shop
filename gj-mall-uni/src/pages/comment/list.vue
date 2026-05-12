<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">Reviews</text>
      <text class="title">商品评价</text>
      <text class="subtitle">{{ productTitle || '真实买家反馈、晒图和商家回复' }}</text>
    </view>

    <view class="panel summary-panel">
      <view class="summary-grid">
        <view>
          <text>{{ Number(summary.averageScore || 0).toFixed(1) }}</text>
          <text>综合评分</text>
        </view>
        <view>
          <text>{{ Number(summary.goodRate || 0).toFixed(1) }}%</text>
          <text>好评率</text>
        </view>
        <view>
          <text>{{ summary.imageCount || 0 }}</text>
          <text>有图评价</text>
        </view>
      </view>
    </view>

    <view class="filter-row">
      <view :class="{ active: !hasImage }" @tap="switchFilter(false)">全部 {{ summary.total || 0 }}</view>
      <view :class="{ active: hasImage }" @tap="switchFilter(true)">有图 {{ summary.imageCount || 0 }}</view>
    </view>

    <view v-if="loading && !comments.length" class="empty">评价加载中...</view>
    <view v-else-if="!comments.length" class="empty-card">
      <text>暂无评价</text>
      <button @tap="goBack">返回商品</button>
    </view>
    <view v-else class="comment-list">
      <view v-for="item in comments" :key="item.id" class="comment-card">
        <view class="comment-head">
          <view>
            <text>{{ maskUser(item.userId) }}</text>
            <text>{{ formatTime(item.createTime) }}</text>
          </view>
          <text>{{ stars(item.score) }}</text>
        </view>
        <text class="comment-spec">{{ commentSpec(item) }}</text>
        <text class="comment-content">{{ item.content }}</text>
        <view v-if="item.images?.length" class="image-grid">
          <image
            v-for="image in item.images"
            :key="image"
            class="comment-image"
            :src="normalizeImage(image, `comment-${item.id}`)"
            mode="aspectFill"
            @tap="previewImages(item.images, image)"
          />
        </view>
        <view v-if="item.replyContent" class="merchant-reply">
          <text>商家回复</text>
          <text>{{ item.replyContent }}</text>
        </view>
      </view>
    </view>

    <view v-if="comments.length" class="load-state">
      <text v-if="loading">继续加载中...</text>
      <text v-else-if="finished">已经到底了</text>
      <button v-else @tap="loadComments(false)">加载更多</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import {
  getProductComments,
  getProductCommentSummary,
  type ApiId,
  type ProductComment,
  type ProductCommentSummary,
} from '@/api/product'

const summary = reactive<ProductCommentSummary>({
  total: 0,
  averageScore: 0,
  goodCount: 0,
  goodRate: 0,
  imageCount: 0,
})

const spuId = ref<ApiId>()
const productTitle = ref('')
const comments = ref<ProductComment[]>([])
const loading = ref(false)
const hasImage = ref(false)
const pageNum = ref(1)
const total = ref(0)
const finished = ref(false)

onLoad((options: any) => {
  spuId.value = options?.spuId
  productTitle.value = options?.title ? decodeURIComponent(options.title) : ''
  refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!loading.value && !finished.value) {
    loadComments(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await Promise.all([loadSummary(), loadComments(true)])
}

async function loadSummary() {
  if (!spuId.value) return
  const res = await getProductCommentSummary(spuId.value)
  Object.assign(summary, res.data || {})
}

async function loadComments(reset: boolean) {
  if (!spuId.value) return
  loading.value = true
  try {
    const res = await getProductComments(spuId.value, {
      current: pageNum.value,
      size: 8,
      hasImage: hasImage.value || undefined,
    })
    const page = res.data
    const list = page?.list || []
    comments.value = reset ? list : [...comments.value, ...list]
    total.value = Number(page?.total || 0)
    finished.value = comments.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

async function switchFilter(nextHasImage: boolean) {
  if (hasImage.value === nextHasImage) return
  hasImage.value = nextHasImage
  comments.value = []
  pageNum.value = 1
  finished.value = false
  await loadComments(true)
}

function previewImages(images?: string[], current?: string) {
  const urls = (images || []).map((image) => normalizeImage(image, 'comment')).filter(Boolean)
  if (!urls.length) return
  uni.previewImage({ urls, current: current ? normalizeImage(current, 'comment') : urls[0] })
}

function commentSpec(comment: ProductComment) {
  const specs = comment.specData ? Object.values(comment.specData).filter(Boolean).join(' / ') : ''
  return specs || comment.skuName || '默认规格'
}

function maskUser(userId?: ApiId) {
  const raw = String(userId || 'GJ')
  return `GJ会员 ${raw.slice(-4)}`
}

function stars(score?: number) {
  const count = Math.max(0, Math.min(5, Number(score || 0)))
  return '★★★★★'.slice(0, count) + '☆☆☆☆☆'.slice(0, 5 - count)
}

function goBack() {
  uni.navigateBack()
}

function formatTime(value?: string) {
  if (!value) return ''
  return String(value).slice(0, 16).replace('T', ' ')
}

function normalizeImage(url?: string, seed = 'comment') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/520/520`
  }
  return url
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 56rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.panel,
.comment-card,
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
  display: -webkit-box;
  overflow: hidden;
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.panel {
  margin-top: 22rpx;
  padding: 24rpx;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.summary-grid view {
  padding: 18rpx 8rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
  text-align: center;
}

.summary-grid text {
  display: block;
}

.summary-grid text:first-child {
  color: #e5484d;
  font-size: 34rpx;
  font-weight: 900;
}

.summary-grid text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 22rpx;
  font-weight: 800;
}

.filter-row {
  display: flex;
  gap: 12rpx;
  margin: 22rpx 0;
}

.filter-row view {
  height: 62rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #6b7280;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 62rpx;
}

.filter-row view.active {
  background: #111827;
  color: #fff;
}

.comment-list {
  display: grid;
  gap: 18rpx;
}

.comment-card {
  padding: 24rpx;
}

.comment-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.comment-head view text,
.comment-spec,
.comment-content,
.merchant-reply text {
  display: block;
}

.comment-head view text:first-child {
  color: #111827;
  font-size: 27rpx;
  font-weight: 900;
}

.comment-head view text:last-child,
.comment-spec {
  margin-top: 6rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.comment-head > text {
  color: #ffd166;
  font-size: 24rpx;
  letter-spacing: 0;
}

.comment-content {
  margin-top: 14rpx;
  color: #374151;
  font-size: 26rpx;
  line-height: 1.6;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 16rpx;
}

.comment-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 12rpx;
  background: #eef2f7;
}

.merchant-reply {
  margin-top: 16rpx;
  padding: 16rpx;
  border-radius: 12rpx;
  background: #fff7ed;
}

.merchant-reply text:first-child {
  color: #92400e;
  font-size: 22rpx;
  font-weight: 900;
}

.merchant-reply text:last-child {
  margin-top: 8rpx;
  color: #78350f;
  font-size: 24rpx;
  line-height: 1.5;
}

.load-state,
.empty,
.empty-card {
  padding: 76rpx 24rpx;
  color: #6b7280;
  text-align: center;
}

.load-state {
  padding: 28rpx 0 10rpx;
}

.load-state text,
.empty-card text {
  display: block;
}

.load-state button,
.empty-card button {
  height: 62rpx;
  margin: 0;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 62rpx;
}

.empty-card text {
  margin-bottom: 22rpx;
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}
</style>
