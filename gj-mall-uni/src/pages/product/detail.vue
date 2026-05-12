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
        <text class="title">{{ product.name }}</text>
        <text class="subtitle">{{ product.subTitle || '平台精选商品，支持移动端浏览和加购。' }}</text>
        <view class="price-row">
          <text class="price">{{ formatPrice(currentSku?.price || product.price) }}</text>
          <text class="sales">已售 {{ product.saleCount || 0 }}</text>
        </view>
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
import { getProductDetail, type ApiId, type ProductDetail, type ProductSku } from '@/api/product'

const loading = ref(false)
const submitting = ref(false)
const product = ref<ProductDetail>()
const selectedSkuId = ref<ApiId>()
const quantity = ref(1)

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
  } finally {
    loading.value = false
  }
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

function changeQuantity(step: number) {
  quantity.value = Math.min(maxQuantity.value, Math.max(1, quantity.value + step))
}

async function addToCart(redirect = false) {
  const token = uni.getStorageSync('mall_token')
  if (!token) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.switchTab({ url: '/pages/user/user' })
    return
  }
  if (!currentSku.value) {
    uni.showToast({ title: '请选择商品规格', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await addCart({ skuId: currentSku.value.id, quantity: quantity.value })
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    if (redirect) {
      uni.switchTab({ url: '/pages/cart/cart' })
    }
  } finally {
    submitting.value = false
  }
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/cart' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
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

.title,
.subtitle {
  display: block;
}

.title {
  margin-top: 18rpx;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.25;
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

.detail-image {
  width: 100%;
  margin-top: 16rpx;
  border-radius: 12rpx;
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
