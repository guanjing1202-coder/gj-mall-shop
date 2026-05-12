<template>
  <view class="page">
    <view class="category-tabs">
      <scroll-view scroll-y class="tabs-scroll">
        <view
          class="tab"
          :class="{ active: !activeCategoryId }"
          @tap="selectCategory(undefined)"
        >
          全部
        </view>
        <view
          v-for="item in categoryList"
          :key="item.id"
          class="tab"
          :class="{ active: String(activeCategoryId) === String(item.id) }"
          @tap="selectCategory(item.id)"
        >
          {{ item.name }}
        </view>
      </scroll-view>
    </view>

    <view class="products">
      <view class="toolbar">
        <text>{{ activeCategoryName }}</text>
        <view class="toolbar-actions">
          <button @tap="goSearch">筛选</button>
          <button @tap="toggleSort">{{ sortText }}</button>
        </view>
      </view>

      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!products.length" class="empty">暂无商品</view>
      <view v-else>
        <view v-for="item in products" :key="item.id" class="product-row" @tap="goDetail(item.id)">
          <image class="thumb" :src="normalizeImage(item.mainImage, String(item.id))" mode="aspectFill" />
          <view class="copy">
            <text class="title">{{ item.name }}</text>
            <text class="sub">{{ item.subTitle || '精选商城在售商品' }}</text>
            <view class="price-row">
              <text class="price">{{ formatPrice(item.price) }}</text>
              <text class="sales">销量 {{ item.saleCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCategoryTree, getProductPage, type ApiId, type CategoryItem, type ProductItem, type ProductQuery } from '@/api/product'

const loading = ref(false)
const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const activeCategoryId = ref<ApiId>()
const sort = ref<ProductQuery['sort']>('operation')

const categoryList = computed(() => {
  const children = categories.value.flatMap((item) => item.children || [])
  return children.length ? children : categories.value
})

const activeCategoryName = computed(() => {
  return categoryList.value.find((item) => String(item.id) === String(activeCategoryId.value))?.name || '全部商品'
})

const sortText = computed(() => (sort.value === 'sales' ? '销量优先' : '综合推荐'))

onMounted(async () => {
  const res = await getCategoryTree()
  categories.value = res.data || []
  await loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductPage({
      current: 1,
      size: 20,
      categoryId: activeCategoryId.value,
      sort: sort.value,
    })
    products.value = res.data?.list || []
  } finally {
    loading.value = false
  }
}

function selectCategory(id?: ApiId) {
  activeCategoryId.value = id
  loadProducts()
}

function toggleSort() {
  sort.value = sort.value === 'sales' ? 'operation' : 'sales'
  loadProducts()
}

function goSearch() {
  const query = activeCategoryId.value ? `?categoryId=${encodeURIComponent(String(activeCategoryId.value))}` : ''
  uni.navigateTo({ url: `/pages/search/search${query}` })
}

function goDetail(id: ApiId) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function normalizeImage(url?: string, seed = 'category') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}
</script>

<style lang="scss">
.page {
  display: grid;
  grid-template-columns: 176rpx 1fr;
  min-height: 100vh;
  background: #f7f8f5;
}

.category-tabs {
  background: #fff;
}

.tabs-scroll {
  height: 100vh;
}

.tab {
  padding: 30rpx 18rpx;
  color: #4b5563;
  font-size: 26rpx;
  font-weight: 800;
}

.tab.active {
  background: #f7f8f5;
  color: #e5484d;
}

.products {
  min-width: 0;
  padding: 22rpx;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.toolbar text {
  font-size: 34rpx;
  font-weight: 900;
}

.toolbar-actions {
  display: flex;
  gap: 10rpx;
}

.toolbar button {
  margin: 0;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 24rpx;
}

.product-row {
  display: grid;
  grid-template-columns: 160rpx minmax(0, 1fr);
  gap: 18rpx;
  margin-bottom: 18rpx;
  padding: 16rpx;
  border-radius: 16rpx;
  background: #fff;
}

.thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background: #eef2f7;
}

.copy {
  min-width: 0;
}

.title,
.sub {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
}

.title {
  color: #111827;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1.35;
  -webkit-line-clamp: 2;
}

.sub {
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
  margin-top: 16rpx;
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
