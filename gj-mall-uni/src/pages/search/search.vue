<template>
  <view class="page">
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索商品" confirm-type="search" @confirm="refresh" />
      <button @tap="refresh">搜索</button>
    </view>

    <scroll-view scroll-x class="sort-tabs">
      <view class="sort-row">
        <view
          v-for="item in sortOptions"
          :key="item.value"
          class="sort-tab"
          :class="{ active: sort === item.value }"
          @tap="changeSort(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="loading && !products.length" class="empty">加载中...</view>
    <view v-else-if="!products.length" class="empty-card">
      <text>没有找到相关商品</text>
      <button @tap="clearKeyword">查看全部</button>
    </view>

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

    <view v-if="products.length" class="load-state">
      <text>{{ finished ? '已经到底啦' : loading ? '继续加载...' : '上拉加载更多' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { getProductPage, type ApiId, type ProductItem, type ProductQuery } from '@/api/product'

const sortOptions: Array<{ label: string; value: ProductQuery['sort'] }> = [
  { label: '综合推荐', value: 'operation' },
  { label: '销量优先', value: 'sales' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' },
]

const loading = ref(false)
const finished = ref(false)
const products = ref<ProductItem[]>([])
const keyword = ref('')
const categoryId = ref<ApiId>()
const brandId = ref<ApiId>()
const sort = ref<ProductQuery['sort']>('operation')
const pageNum = ref(1)
const total = ref(0)
const routeKeyword = ref('')

onLoad((options: any) => {
  routeKeyword.value = decodeURIComponent(options?.keyword || '')
  keyword.value = routeKeyword.value
  categoryId.value = options?.categoryId
  brandId.value = options?.brandId
  refresh()
})

onShow(() => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const nextKeyword = decodeURIComponent(current?.options?.keyword || '')
  if (nextKeyword !== routeKeyword.value) {
    routeKeyword.value = nextKeyword
    keyword.value = nextKeyword
    refresh()
  }
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value && !loading.value) {
    loadProducts(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await loadProducts(true)
}

async function loadProducts(reset: boolean) {
  loading.value = true
  try {
    const res = await getProductPage({
      current: pageNum.value,
      size: 10,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value,
      brandId: brandId.value,
      sort: sort.value,
    })
    const page = res.data
    const list = page?.list || []
    products.value = reset ? list : [...products.value, ...list]
    total.value = Number(page?.total || 0)
    finished.value = products.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

function changeSort(value: ProductQuery['sort']) {
  sort.value = value
  refresh()
}

function clearKeyword() {
  keyword.value = ''
  categoryId.value = undefined
  brandId.value = undefined
  refresh()
}

function goDetail(id: ApiId) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function normalizeImage(url?: string, seed = 'search') {
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
  color: #111827;
}

.search-bar {
  display: grid;
  grid-template-columns: 1fr 140rpx;
  gap: 14rpx;
  padding: 12rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.search-input {
  height: 76rpx;
  padding: 0 22rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  font-size: 27rpx;
}

.search-bar button {
  height: 76rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 76rpx;
}

.sort-tabs {
  width: 100%;
  margin: 22rpx 0;
  white-space: nowrap;
}

.sort-row {
  display: flex;
  gap: 12rpx;
}

.sort-tab {
  display: inline-flex;
  height: 62rpx;
  align-items: center;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #6b7280;
  font-size: 24rpx;
  font-weight: 900;
}

.sort-tab.active {
  background: #111827;
  color: #fff;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.product-card,
.empty-card {
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
  gap: 12rpx;
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
