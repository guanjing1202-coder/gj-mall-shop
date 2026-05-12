<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">GJ Mall</text>
        <text class="title">精选好物</text>
        <text class="subtitle">手机端商城入口，商品、分类和领券都能直接使用。</text>
      </view>
      <view class="hero-actions">
        <button class="coupon-btn" @tap="goCoupons">领券</button>
        <button class="seckill-btn" @tap="goSeckill">秒杀</button>
      </view>
    </view>

    <view class="search-card">
      <input v-model="keyword" class="search-input" placeholder="搜索手机、耳机、护肤" confirm-type="search" @confirm="search" />
      <button class="search-btn" @tap="search">搜索</button>
    </view>

    <view v-if="activeSeckill" class="seckill-card" @tap="goSeckill">
      <view>
        <text class="seckill-kicker">限时秒杀</text>
        <text class="seckill-title">{{ activeSeckill.name }}</text>
        <text class="seckill-sub">{{ formatTime(activeSeckill.endTime) }} 截止</text>
      </view>
      <button>去抢购</button>
    </view>

    <view class="section-head">
      <text>推荐商品</text>
      <button @tap="loadProducts">刷新</button>
    </view>

    <view v-if="loading" class="empty">加载中...</view>
    <view v-else-if="!products.length" class="empty">暂无商品</view>
    <view v-else class="grid">
      <view v-for="item in products" :key="item.id" class="product-card" @tap="goDetail(item.id)">
        <image class="product-image" :src="normalizeImage(item.mainImage, String(item.id))" mode="aspectFill" />
        <view class="product-copy">
          <text class="product-title">{{ item.name }}</text>
          <text class="product-sub">{{ item.subTitle || '精选商城在售商品' }}</text>
          <view class="price-row">
            <text class="price">{{ formatPrice(item.price) }}</text>
            <text class="sales">已售 {{ item.saleCount || 0 }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getProductPage, type ApiId, type ProductItem } from '@/api/product'
import { getActiveSeckills, type SeckillActivity } from '@/api/seckill'

const loading = ref(false)
const seckillLoading = ref(false)
const keyword = ref('')
const products = ref<ProductItem[]>([])
const activeSeckill = ref<SeckillActivity>()

onMounted(() => {
  loadProducts()
  loadSeckill()
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductPage({
      current: 1,
      size: 10,
      keyword: keyword.value.trim() || undefined,
      sort: 'operation',
    })
    products.value = res.data?.list || []
  } finally {
    loading.value = false
  }
}

async function loadSeckill() {
  seckillLoading.value = true
  try {
    const res = await getActiveSeckills()
    activeSeckill.value = (res.data || [])[0]
  } finally {
    seckillLoading.value = false
  }
}

function search() {
  const query = keyword.value.trim()
  uni.navigateTo({ url: `/pages/search/search${query ? `?keyword=${encodeURIComponent(query)}` : ''}` })
}

function goDetail(id: ApiId) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function goCoupons() {
  uni.navigateTo({ url: '/pages/coupon/center' })
}

function goSeckill() {
  uni.navigateTo({ url: '/pages/seckill/list' })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function formatTime(value?: string) {
  if (!value) return '活动进行中'
  return String(value).slice(5, 16).replace('T', ' ')
}

function normalizeImage(url?: string, seed = 'mall') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/520/520`
  }
  return url
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8f5;
}

.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  padding: 42rpx 30rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #111827, #2f8f67);
  color: #fff;
}

.kicker,
.title,
.subtitle {
  display: block;
}

.kicker {
  font-size: 24rpx;
  opacity: 0.8;
}

.title {
  margin-top: 12rpx;
  font-size: 48rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.6;
  opacity: 0.86;
}

.coupon-btn,
.seckill-btn,
.search-btn,
.section-head button {
  border: 0;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 800;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 12rpx;
}

.coupon-btn,
.seckill-btn {
  flex: 0 0 auto;
  margin: 0;
}

.coupon-btn {
  background: #ffd166;
  color: #111827;
}

.seckill-btn {
  background: #fff;
  color: #111827;
}

.search-card {
  display: grid;
  grid-template-columns: 1fr 150rpx;
  gap: 16rpx;
  margin-top: 24rpx;
  padding: 12rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.06);
}

.search-input {
  height: 76rpx;
  padding: 0 22rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  font-size: 28rpx;
}

.search-btn {
  height: 76rpx;
  background: #e5484d;
  color: #fff;
  line-height: 76rpx;
}

.seckill-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #111827;
  color: #fff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.08);
}

.seckill-card text {
  display: block;
}

.seckill-kicker {
  color: #ffd166;
  font-size: 23rpx;
  font-weight: 900;
}

.seckill-title {
  margin-top: 8rpx;
  font-size: 32rpx;
  font-weight: 900;
}

.seckill-sub {
  margin-top: 6rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 23rpx;
}

.seckill-card button {
  flex: 0 0 auto;
  height: 62rpx;
  margin: 0;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 62rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 34rpx 0 18rpx;
}

.section-head text {
  font-size: 34rpx;
  font-weight: 900;
}

.section-head button {
  margin: 0;
  padding: 0 22rpx;
  background: #111827;
  color: #fff;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.product-card {
  overflow: hidden;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.05);
}

.product-image {
  width: 100%;
  height: 320rpx;
  background: #eef2f7;
}

.product-copy {
  padding: 18rpx;
}

.product-title,
.product-sub {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
}

.product-title {
  min-height: 72rpx;
  color: #111827;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1.3;
  -webkit-line-clamp: 2;
}

.product-sub {
  min-height: 62rpx;
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.35;
  -webkit-line-clamp: 2;
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.price {
  color: #e5484d;
  font-size: 32rpx;
  font-weight: 900;
}

.sales {
  color: #9ca3af;
  font-size: 22rpx;
}

.empty {
  padding: 80rpx 0;
  color: #6b7280;
  text-align: center;
}
</style>
