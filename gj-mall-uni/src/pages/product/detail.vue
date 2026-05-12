<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中...</view>

    <view v-else-if="product">
      <swiper class="gallery" circular :indicator-dots="gallery.length > 1" indicator-color="rgba(255,255,255,.55)" indicator-active-color="#fff">
        <swiper-item v-for="image in gallery" :key="image">
          <image class="gallery-image" :src="normalizeImage(image, String(product.id))" mode="aspectFill" />
        </swiper-item>
      </swiper>

      <view class="panel product-panel">
        <view class="tag-row">
          <text v-if="product.newStatus === 1">新品</text>
          <text v-if="product.recommendStatus === 1">推荐</text>
          <text>{{ product.brandName || '精选品牌' }}</text>
        </view>
        <view class="title-row">
          <text class="title">{{ product.name }}</text>
          <button class="favorite-btn" :class="{ active: favoriteActive }" :disabled="favoriteLoading" @tap="toggleFavorite">
            <text>{{ favoriteActive ? '♥' : '♡' }}</text>
            <text>{{ favoriteActive ? '已收藏' : '收藏' }}</text>
          </button>
        </view>
        <text class="subtitle">{{ product.subTitle || '平台精选商品，支持移动端浏览和加购。' }}</text>
        <view class="price-row">
          <text class="price">{{ formatPrice(currentSku?.price || product.price) }}</text>
          <text class="sales">已售 {{ product.saleCount || 0 }}</text>
        </view>
      </view>

      <view class="panel promo-panel" @tap="goCoupons">
        <view>
          <text>可领优惠</text>
          <text>领券后结算自动抵扣</text>
        </view>
        <button>去领券</button>
      </view>

      <view v-if="product.skus?.length" class="panel">
        <view class="section-head">
          <text>选择规格</text>
          <text class="muted">库存 {{ currentSku?.stock ?? 0 }}</text>
        </view>
        <view class="sku-grid">
          <view
            v-for="sku in product.skus"
            :key="sku.id"
            class="sku-chip"
            :class="{ active: String(selectedSkuId) === String(sku.id), disabled: !Number(sku.stock || 0) }"
            @tap="selectSku(sku)"
          >
            <text>{{ skuLabel(sku) }}</text>
            <text>{{ formatPrice(sku.price || product.price) }}</text>
          </view>
        </view>
        <view v-if="currentSku" class="selected-sku">
          <text>已选</text>
          <text>{{ skuLabel(currentSku) }} · {{ formatPrice(currentSku.price || product.price) }}</text>
        </view>
      </view>

      <view class="panel quantity-panel">
        <view class="section-head">
          <text>购买数量</text>
          <text class="muted">最多 {{ maxQuantity }} 件</text>
        </view>
        <view class="counter">
          <button :disabled="quantity <= 1" @tap="changeQuantity(-1)">-</button>
          <text>{{ quantity }}</text>
          <button :disabled="quantity >= maxQuantity" @tap="changeQuantity(1)">+</button>
        </view>
      </view>

      <view class="panel service-panel">
        <view class="service-item">
          <text>正品保障</text>
          <text>平台严选在售商品</text>
        </view>
        <view class="service-item">
          <text>售后服务</text>
          <text>{{ product.afterSale || '支持订单售后、退款退货流程' }}</text>
        </view>
        <view class="service-item">
          <text>包装清单</text>
          <text>{{ product.packingList || '商品主件、说明资料、官方包装' }}</text>
        </view>
      </view>

      <view class="panel comment-panel">
        <view class="section-head">
          <text>买家评价</text>
          <button class="head-action" @tap="goComments">{{ commentSummary.total || 0 }} 条</button>
        </view>
        <view class="comment-summary">
          <view>
            <text>{{ Number(commentSummary.averageScore || 0).toFixed(1) }}</text>
            <text>综合评分</text>
          </view>
          <view>
            <text>{{ Number(commentSummary.goodRate || 0).toFixed(1) }}%</text>
            <text>好评率</text>
          </view>
          <view>
            <text>{{ commentSummary.imageCount || 0 }}</text>
            <text>有图评价</text>
          </view>
        </view>
        <view class="comment-tabs">
          <view :class="{ active: !commentHasImage }" @tap="switchCommentFilter(false)">全部</view>
          <view :class="{ active: commentHasImage }" @tap="switchCommentFilter(true)">有图</view>
        </view>
        <view v-if="commentLoading" class="comment-empty">评价加载中...</view>
        <view v-else-if="!comments.length" class="comment-empty">暂无评价</view>
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
            <scroll-view v-if="item.images?.length" scroll-x class="comment-images">
              <view class="comment-image-row">
                <image
                  v-for="image in item.images"
                  :key="image"
                  class="comment-image"
                  :src="normalizeImage(image, `comment-${item.id}`)"
                  mode="aspectFill"
                  @tap="previewImages(item.images, image)"
                />
              </view>
            </scroll-view>
            <view v-if="item.replyContent" class="merchant-reply">
              <text>商家回复</text>
              <text>{{ item.replyContent }}</text>
            </view>
          </view>
        </view>
        <button v-if="Number(commentSummary.total || 0) > comments.length" class="more-comment-btn" @tap="goComments">查看全部评价</button>
      </view>

      <view v-if="relatedProducts.length" class="panel related-panel">
        <view class="section-head">
          <text>同类推荐</text>
          <text class="muted">猜你喜欢</text>
        </view>
        <scroll-view scroll-x class="related-scroll">
          <view class="related-row">
            <view v-for="item in relatedProducts" :key="item.id" class="related-card" @tap="goDetail(item.id)">
              <image class="related-image" :src="normalizeImage(item.mainImage, String(item.id))" mode="aspectFill" />
              <text>{{ item.name }}</text>
              <text>{{ formatPrice(item.price) }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view v-if="product.detailImages?.length || product.detailHtml" class="panel detail-panel">
        <view class="section-head">
          <text>商品详情</text>
        </view>
        <rich-text v-if="product.detailHtml" :nodes="product.detailHtml" />
        <image
          v-for="image in product.detailImages"
          :key="image"
          class="detail-image"
          :src="normalizeImage(image, String(product.id))"
          mode="widthFix"
        />
      </view>
    </view>

    <view v-else class="empty">
      <text>商品不存在</text>
      <button @tap="goHome">返回首页</button>
    </view>

    <view v-if="product" class="bottom-bar">
      <button class="ghost-btn" @tap="goCart">购物车</button>
      <button class="secondary-btn" :disabled="submitting" @tap="addToCart(false)">加入购物车</button>
      <button class="primary-btn" :disabled="submitting" @tap="addToCart(true)">立即购买</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addCart } from '@/api/cart'
import { addFavorite, getFavoriteStatus, removeFavorite } from '@/api/favorite'
import { recordHistory } from '@/api/history'
import {
  getProductDetail,
  getProductComments,
  getProductCommentSummary,
  getProductPage,
  type ApiId,
  type ProductComment,
  type ProductCommentSummary,
  type ProductDetail,
  type ProductItem,
  type ProductSku,
} from '@/api/product'
import { hasLoginState } from '@/utils/auth'

const loading = ref(false)
const submitting = ref(false)
const product = ref<ProductDetail>()
const relatedProducts = ref<ProductItem[]>([])
const comments = ref<ProductComment[]>([])
const commentSummary = ref<ProductCommentSummary>({
  total: 0,
  averageScore: 0,
  goodCount: 0,
  goodRate: 0,
  imageCount: 0,
})
const selectedSkuId = ref<ApiId>()
const quantity = ref(1)
const favoriteActive = ref(false)
const favoriteLoading = ref(false)
const commentLoading = ref(false)
const commentHasImage = ref(false)

const currentSku = computed(() => {
  return product.value?.skus?.find((sku) => String(sku.id) === String(selectedSkuId.value))
})

const gallery = computed(() => {
  const detail = product.value
  if (!detail) return []
  const images = [detail.mainImage, ...(detail.images || []), ...(detail.detailImages || [])]
  const unique = Array.from(new Set(images.filter(Boolean))) as string[]
  return unique.length ? unique : [`seed:${detail.id}`]
})

const maxQuantity = computed(() => Math.max(1, Number(currentSku.value?.stock || 1)))

onLoad((options: any) => {
  loadDetail(options?.id)
})

async function loadDetail(id?: ApiId) {
  if (!id) return
  loading.value = true
  try {
    const res = await getProductDetail(id)
    product.value = res.data
    selectedSkuId.value = res.data.skus?.find((sku) => Number(sku.stock || 0) > 0)?.id || res.data.skus?.[0]?.id
    quantity.value = 1
    favoriteActive.value = false
    comments.value = []
    commentHasImage.value = false
    loadFavoriteStatus(res.data.id)
    recordBrowseHistory(res.data.id)
    await loadComments(res.data.id)
    await loadRelated(res.data)
  } finally {
    loading.value = false
  }
}

async function loadComments(spuId: ApiId) {
  commentLoading.value = true
  try {
    const [summaryRes, commentRes] = await Promise.all([
      getProductCommentSummary(spuId),
      getProductComments(spuId, { current: 1, size: 3, hasImage: commentHasImage.value || undefined }),
    ])
    commentSummary.value = summaryRes.data || commentSummary.value
    comments.value = commentRes.data?.list || []
  } finally {
    commentLoading.value = false
  }
}

async function switchCommentFilter(hasImage: boolean) {
  if (!product.value || commentHasImage.value === hasImage) return
  commentHasImage.value = hasImage
  await loadComments(product.value.id)
}

function previewImages(images?: string[], current?: string) {
  const urls = (images || []).map((image) => normalizeImage(image, 'comment')).filter(Boolean)
  if (!urls.length) return
  uni.previewImage({ urls, current: current ? normalizeImage(current, 'comment') : urls[0] })
}

async function loadFavoriteStatus(spuId: ApiId) {
  if (!hasLoginState()) return
  try {
    const res = await getFavoriteStatus(spuId)
    favoriteActive.value = Boolean(res.data)
  } catch (error) {
    console.warn(error)
  }
}

async function recordBrowseHistory(spuId: ApiId) {
  if (!hasLoginState()) return
  try {
    await recordHistory(spuId)
  } catch (error) {
    console.warn(error)
  }
}

async function toggleFavorite() {
  if (!product.value) return
  if (!hasLoginState()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.switchTab({ url: '/pages/user/user' })
    return
  }
  favoriteLoading.value = true
  try {
    if (favoriteActive.value) {
      await removeFavorite(product.value.id)
      favoriteActive.value = false
      uni.showToast({ title: '已取消收藏', icon: 'success' })
    } else {
      await addFavorite(product.value.id)
      favoriteActive.value = true
      uni.showToast({ title: '收藏成功', icon: 'success' })
    }
  } finally {
    favoriteLoading.value = false
  }
}

async function loadRelated(detail: ProductDetail) {
  const res = await getProductPage({
    current: 1,
    size: 8,
    categoryId: detail.categoryId,
    sort: 'sales',
  })
  relatedProducts.value = (res.data?.list || []).filter((item) => String(item.id) !== String(detail.id)).slice(0, 6)
}

function selectSku(sku: ProductSku) {
  if (!Number(sku.stock || 0)) {
    uni.showToast({ title: '该规格暂无库存', icon: 'none' })
    return
  }
  selectedSkuId.value = sku.id
  quantity.value = 1
}

function skuLabel(sku: ProductSku) {
  const specs = sku.specData ? Object.values(sku.specData).filter(Boolean).join(' / ') : ''
  return specs || sku.name || sku.skuCode || '默认规格'
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

function changeQuantity(step: number) {
  quantity.value = Math.min(maxQuantity.value, Math.max(1, quantity.value + step))
}

async function addToCart(redirect = false) {
  if (!hasLoginState()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.switchTab({ url: '/pages/user/user' })
    return
  }
  if (!currentSku.value) {
    uni.showToast({ title: '请选择商品规格', icon: 'none' })
    return
  }
  if (redirect) {
    uni.navigateTo({ url: `/pages/checkout/checkout?spuId=${product.value.id}&skuId=${currentSku.value.id}&quantity=${quantity.value}` })
    return
  }
  submitting.value = true
  try {
    await addCart({ skuId: currentSku.value.id, quantity: quantity.value })
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } finally {
    submitting.value = false
  }
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/cart' })
}

function goCoupons() {
  uni.navigateTo({ url: '/pages/coupon/center' })
}

function goComments() {
  if (!product.value) return
  uni.navigateTo({ url: `/pages/comment/list?spuId=${product.value.id}&title=${encodeURIComponent(product.value.name)}` })
}

function goDetail(id: ApiId) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function formatTime(value?: string) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

function normalizeImage(url?: string, seed = 'detail') {
  if (!url || url.indexOf('x.com/') >= 0 || url.indexOf('seed:') === 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/760/760`
  }
  return url
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 132rpx;
  background: #f7f8f5;
  color: #111827;
}

.gallery {
  height: 750rpx;
  background: #e5e7eb;
}

.gallery-image {
  width: 100%;
  height: 750rpx;
}

.panel {
  margin: 20rpx 24rpx 0;
  padding: 26rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag-row text {
  padding: 8rpx 14rpx;
  border-radius: 10rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 18rpx;
}

.title,
.subtitle {
  display: block;
}

.title {
  min-width: 0;
  flex: 1;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.25;
}

.favorite-btn {
  width: 126rpx;
  height: 92rpx;
  margin: 0;
  padding: 0;
  border-radius: 18rpx;
  background: #f3f4f6;
  color: #6b7280;
  line-height: 1;
}

.favorite-btn text {
  display: block;
}

.favorite-btn text:first-child {
  margin-top: 12rpx;
  color: #9ca3af;
  font-size: 34rpx;
  line-height: 36rpx;
}

.favorite-btn text:last-child {
  margin-top: 8rpx;
  font-size: 21rpx;
  font-weight: 900;
}

.favorite-btn.active {
  background: #fff1f2;
  color: #e5484d;
}

.favorite-btn.active text:first-child {
  color: #e5484d;
}

.subtitle {
  margin-top: 14rpx;
  color: #6b7280;
  font-size: 26rpx;
  line-height: 1.55;
}

.price-row,
.section-head,
.counter,
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price-row {
  margin-top: 20rpx;
}

.promo-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  background: #fff7ed;
}

.promo-panel view text {
  display: block;
}

.promo-panel view text:first-child {
  color: #e5484d;
  font-size: 28rpx;
  font-weight: 900;
}

.promo-panel view text:last-child {
  margin-top: 6rpx;
  color: #92400e;
  font-size: 24rpx;
}

.promo-panel button {
  height: 60rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 60rpx;
}

.price {
  color: #e5484d;
  font-size: 44rpx;
  font-weight: 900;
}

.sales,
.muted {
  color: #9ca3af;
  font-size: 24rpx;
}

.section-head {
  margin-bottom: 18rpx;
  font-size: 30rpx;
  font-weight: 900;
}

.section-head .head-action {
  height: 54rpx;
  margin: 0;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 54rpx;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.sku-chip {
  min-width: 0;
  padding: 18rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 14rpx;
  background: #fff;
}

.sku-chip text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-chip text:first-child {
  color: #111827;
  font-size: 26rpx;
  font-weight: 900;
}

.sku-chip text:last-child {
  margin-top: 8rpx;
  color: #e5484d;
  font-size: 24rpx;
  font-weight: 800;
}

.sku-chip.active {
  border-color: #e5484d;
  background: #fff7f7;
}

.sku-chip.disabled {
  opacity: 0.45;
}

.selected-sku {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  padding: 16rpx;
  border-radius: 12rpx;
  background: #f7f8f5;
}

.selected-sku text:first-child {
  color: #6b7280;
  font-size: 24rpx;
  font-weight: 900;
}

.selected-sku text:last-child {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.counter {
  width: 260rpx;
}

.counter button {
  width: 76rpx;
  height: 64rpx;
  margin: 0;
  border-radius: 12rpx;
  background: #111827;
  color: #fff;
  line-height: 64rpx;
}

.counter text {
  width: 88rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 900;
}

.service-item + .service-item {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eef0f3;
}

.service-item text {
  display: block;
}

.service-item text:first-child {
  font-size: 28rpx;
  font-weight: 900;
}

.service-item text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 25rpx;
  line-height: 1.55;
}

.comment-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.comment-summary view {
  padding: 18rpx 8rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
  text-align: center;
}

.comment-summary text {
  display: block;
}

.comment-summary text:first-child {
  color: #e5484d;
  font-size: 34rpx;
  font-weight: 900;
}

.comment-summary text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 22rpx;
  font-weight: 800;
}

.comment-tabs {
  display: flex;
  gap: 12rpx;
  margin-top: 20rpx;
}

.comment-tabs view {
  height: 56rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 56rpx;
}

.comment-tabs view.active {
  background: #111827;
  color: #fff;
}

.comment-list {
  display: grid;
  gap: 18rpx;
  margin-top: 20rpx;
}

.comment-card {
  padding-top: 18rpx;
  border-top: 1rpx solid #eef0f3;
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
  font-size: 26rpx;
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
  font-size: 25rpx;
  line-height: 1.6;
}

.comment-images {
  width: 100%;
  margin-top: 14rpx;
  white-space: nowrap;
}

.comment-image-row {
  display: flex;
  gap: 12rpx;
}

.comment-image {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  background: #eef2f7;
}

.merchant-reply {
  margin-top: 14rpx;
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

.comment-empty {
  margin-top: 18rpx;
  padding: 36rpx 0 12rpx;
  color: #9ca3af;
  font-size: 24rpx;
  text-align: center;
}

.more-comment-btn {
  width: 100%;
  height: 68rpx;
  margin-top: 20rpx;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 68rpx;
}

.detail-image {
  width: 100%;
  margin-top: 16rpx;
  border-radius: 12rpx;
}

.related-scroll {
  width: 100%;
  white-space: nowrap;
}

.related-row {
  display: flex;
  gap: 16rpx;
}

.related-card {
  display: inline-flex;
  width: 214rpx;
  flex-direction: column;
  overflow: hidden;
  border-radius: 14rpx;
  background: #f7f8f5;
  white-space: normal;
}

.related-image {
  width: 214rpx;
  height: 214rpx;
  background: #eef2f7;
}

.related-card text {
  display: -webkit-box;
  overflow: hidden;
  padding: 0 14rpx;
  -webkit-box-orient: vertical;
}

.related-card text:nth-child(2) {
  min-height: 64rpx;
  margin-top: 12rpx;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 1.35;
  -webkit-line-clamp: 2;
}

.related-card text:last-child {
  padding-bottom: 14rpx;
  color: #e5484d;
  font-size: 26rpx;
  font-weight: 900;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  gap: 14rpx;
  padding: 18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 28rpx rgba(15, 23, 42, 0.1);
}

.bottom-bar button {
  height: 76rpx;
  margin: 0;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 76rpx;
}

.ghost-btn {
  width: 132rpx;
  background: #f3f4f6;
  color: #111827;
}

.secondary-btn {
  flex: 1;
  background: #111827;
  color: #fff;
}

.primary-btn {
  flex: 1;
  background: #e5484d;
  color: #fff;
}

.empty {
  padding: 120rpx 30rpx;
  color: #6b7280;
  text-align: center;
}

.empty text {
  display: block;
  margin-bottom: 24rpx;
}

.empty button {
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
}
</style>
