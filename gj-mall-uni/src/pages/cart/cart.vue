<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">Cart</text>
        <text class="title">购物车</text>
      </view>
      <button v-if="cart.items.length" @tap="clearAll">清空</button>
    </view>

    <view v-if="!isLoggedIn" class="login-card">
      <text>登录后查看购物车</text>
      <text>商品会保存在账户下，可以继续结算和管理。</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!cart.items.length" class="empty-card">
        <text>购物车还没有商品</text>
        <button @tap="goHome">去逛逛</button>
      </view>

      <view v-else class="cart-list">
        <view v-for="item in cart.items" :key="item.skuId" class="cart-item" :class="{ invalid: item.invalid }">
          <view class="select-dot" :class="{ active: item.selected === 1 && !item.invalid }" @tap="toggleItem(item)" />
          <image class="thumb" :src="normalizeImage(item.image, String(item.skuId))" mode="aspectFill" @tap="goDetail(item)" />
          <view class="copy">
            <text class="name" @tap="goDetail(item)">{{ item.spuName || item.skuName || '商品' }}</text>
            <text class="spec">{{ specText(item) }}</text>
            <view class="price-line">
              <text>{{ formatPrice(item.price) }}</text>
              <view class="counter">
                <button :disabled="item.quantity <= 1 || item.invalid" @tap="changeQuantity(item, -1)">-</button>
                <text>{{ item.quantity }}</text>
                <button :disabled="item.invalid || item.quantity >= Number(item.stock || 1)" @tap="changeQuantity(item, 1)">+</button>
              </view>
            </view>
            <view class="item-actions">
              <text v-if="item.invalid">{{ item.invalidReason || '商品已失效' }}</text>
              <text v-else>库存 {{ item.stock ?? 0 }}</text>
              <button @tap="removeItem(item)">删除</button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="isLoggedIn && cart.items.length" class="settle-bar">
      <view class="select-all" @tap="toggleAll">
        <view class="select-dot" :class="{ active: allSelected }" />
        <text>全选</text>
      </view>
      <view class="amount">
        <text>已选 {{ cart.selectedCount || 0 }} 件</text>
        <text>{{ formatPrice(cart.selectedAmount) }}</text>
      </view>
      <button :disabled="!cart.selectedCount" @tap="checkout">结算</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  clearCart,
  getCart,
  removeCartItem,
  selectAllCart,
  updateCart,
  type CartInfo,
  type CartItem,
} from '@/api/cart'
import { syncSession } from '@/utils/session'

const emptyCart = (): CartInfo => ({
  items: [],
  totalCount: 0,
  selectedCount: 0,
  selectedAmount: 0,
})

const loading = ref(false)
const isLoggedIn = ref(false)
const cart = ref<CartInfo>(emptyCart())

const allSelected = computed(() => {
  const validItems = cart.value.items.filter((item) => !item.invalid)
  return Boolean(validItems.length) && validItems.every((item) => item.selected === 1)
})

onShow(async () => {
  isLoggedIn.value = await syncSession()
  if (!isLoggedIn.value) {
    cart.value = emptyCart()
    return
  }
  await loadCart()
})

async function loadCart() {
  loading.value = true
  try {
    const res = await getCart()
    cart.value = res.data || emptyCart()
  } finally {
    loading.value = false
  }
}

async function toggleItem(item: CartItem) {
  if (item.invalid) return
  await updateCart({ skuId: item.skuId, selected: item.selected === 1 ? 0 : 1 })
  await loadCart()
}

async function toggleAll() {
  await selectAllCart(!allSelected.value)
  await loadCart()
}

async function changeQuantity(item: CartItem, step: number) {
  const next = Math.max(1, Math.min(Number(item.stock || 1), item.quantity + step))
  if (next === item.quantity) return
  await updateCart({ skuId: item.skuId, quantity: next })
  await loadCart()
}

async function removeItem(item: CartItem) {
  await removeCartItem(item.skuId)
  await loadCart()
}

async function clearAll() {
  await clearCart()
  await loadCart()
}

function checkout() {
  uni.navigateTo({ url: '/pages/checkout/checkout' })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function goDetail(item: CartItem) {
  if (item.spuId) {
    uni.navigateTo({ url: `/pages/product/detail?id=${item.spuId}` })
  }
}

function specText(item: CartItem) {
  const specs = item.specData ? Object.entries(item.specData).map(([key, value]) => `${key}: ${value}`).join(' / ') : ''
  return specs || item.skuName || '默认规格'
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function normalizeImage(url?: string, seed = 'cart') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 144rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.login-card,
.empty-card,
.cart-item {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  background: #111827;
  color: #fff;
}

.kicker,
.title,
.login-card text,
.empty-card text {
  display: block;
}

.kicker {
  color: #ffd166;
  font-size: 23rpx;
  font-weight: 900;
}

.title {
  margin-top: 8rpx;
  font-size: 42rpx;
  font-weight: 900;
}

.hero button {
  margin: 0;
  border-radius: 999rpx;
  background: #fff;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
}

.login-card,
.empty-card {
  margin-top: 22rpx;
  padding: 46rpx 28rpx;
  text-align: center;
}

.login-card text:first-child,
.empty-card text {
  font-size: 34rpx;
  font-weight: 900;
}

.login-card text:nth-child(2) {
  margin-top: 12rpx;
  color: #6b7280;
  font-size: 25rpx;
  line-height: 1.6;
}

.login-card button,
.empty-card button {
  margin-top: 28rpx;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-weight: 900;
}

.cart-list {
  margin-top: 22rpx;
}

.cart-item {
  display: grid;
  grid-template-columns: 44rpx 156rpx minmax(0, 1fr);
  gap: 16rpx;
  align-items: center;
  margin-bottom: 18rpx;
  padding: 18rpx;
}

.cart-item.invalid {
  opacity: 0.58;
}

.select-dot {
  width: 34rpx;
  height: 34rpx;
  border: 3rpx solid #d1d5db;
  border-radius: 999rpx;
  background: #fff;
}

.select-dot.active {
  border-color: #e5484d;
  background: #e5484d;
  box-shadow: inset 0 0 0 8rpx #fff;
}

.thumb {
  width: 156rpx;
  height: 156rpx;
  border-radius: 14rpx;
  background: #eef2f7;
}

.copy {
  min-width: 0;
}

.name,
.spec {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
}

.name {
  color: #111827;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1.35;
  -webkit-line-clamp: 2;
}

.spec {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.3;
  -webkit-line-clamp: 1;
}

.price-line,
.item-actions,
.settle-bar,
.select-all,
.amount {
  display: flex;
  align-items: center;
}

.price-line {
  justify-content: space-between;
  margin-top: 14rpx;
}

.price-line > text {
  color: #e5484d;
  font-size: 30rpx;
  font-weight: 900;
}

.counter {
  display: grid;
  grid-template-columns: 48rpx 56rpx 48rpx;
  align-items: center;
  overflow: hidden;
  border-radius: 999rpx;
  background: #f3f4f6;
}

.counter button {
  width: 48rpx;
  height: 48rpx;
  margin: 0;
  border-radius: 0;
  background: #111827;
  color: #fff;
  font-size: 24rpx;
  line-height: 48rpx;
}

.counter text {
  text-align: center;
  font-size: 24rpx;
  font-weight: 900;
}

.item-actions {
  justify-content: space-between;
  margin-top: 12rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.item-actions button {
  height: 44rpx;
  margin: 0;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 22rpx;
  line-height: 44rpx;
}

.settle-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  gap: 18rpx;
  padding: 18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 28rpx rgba(15, 23, 42, 0.1);
}

.select-all {
  gap: 10rpx;
  color: #374151;
  font-size: 24rpx;
  font-weight: 900;
}

.amount {
  flex: 1;
  min-width: 0;
  flex-direction: column;
  align-items: flex-end;
}

.amount text:first-child {
  color: #6b7280;
  font-size: 22rpx;
}

.amount text:last-child {
  color: #e5484d;
  font-size: 34rpx;
  font-weight: 900;
}

.settle-bar button {
  width: 180rpx;
  height: 76rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-weight: 900;
  line-height: 76rpx;
}

.empty {
  padding: 90rpx 0;
  color: #6b7280;
  text-align: center;
}
</style>
