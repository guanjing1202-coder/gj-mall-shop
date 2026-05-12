<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">Coupons</text>
      <text class="title">领券中心</text>
      <text class="subtitle">把可用优惠先领到账号，结算时可以直接选择抵扣。</text>
    </view>

    <view class="tabs">
      <view :class="{ active: tab === 'center' }" @tap="tab = 'center'">可领取</view>
      <view :class="{ active: tab === 'mine' }" @tap="tab = 'mine'">我的券</view>
    </view>

    <view v-if="tab === 'center'">
      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!coupons.length" class="empty">暂无可领取优惠券</view>
      <view v-else class="coupon-list">
        <view v-for="item in coupons" :key="item.id" class="coupon-card">
          <view class="coupon-value">
            <text>{{ couponValue(item) }}</text>
            <text>{{ couponType(item) }}</text>
          </view>
          <view class="coupon-copy">
            <text class="coupon-name">{{ item.name }}</text>
            <text class="coupon-rule">{{ couponRule(item) }}</text>
            <text class="coupon-time">{{ item.startTime || '即刻' }} - {{ item.endTime || '长期有效' }}</text>
          </view>
          <button :disabled="receivingId === item.id" @tap="receive(item.id)">领取</button>
        </view>
      </view>
    </view>

    <view v-else>
      <view v-if="mineLoading" class="empty">加载中...</view>
      <view v-else-if="!myCoupons.length" class="empty">你还没有优惠券</view>
      <view v-else class="coupon-list">
        <view v-for="item in myCoupons" :key="item.couponId || item.id" class="coupon-card mine">
          <view class="coupon-value">
            <text>{{ couponValue(item) }}</text>
            <text>{{ item.statusDesc || '可使用' }}</text>
          </view>
          <view class="coupon-copy">
            <text class="coupon-name">{{ item.name }}</text>
            <text class="coupon-rule">{{ couponRule(item) }}</text>
            <text class="coupon-time">{{ item.endTime || '长期有效' }} 前有效</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCouponCenter, getMyCoupons, receiveCoupon, type CouponCenterItem, type MyCoupon } from '@/api/coupon'
import type { ApiId } from '@/api/product'

const tab = ref<'center' | 'mine'>('center')
const loading = ref(false)
const mineLoading = ref(false)
const receivingId = ref<ApiId>()
const coupons = ref<CouponCenterItem[]>([])
const myCoupons = ref<MyCoupon[]>([])

onShow(() => {
  loadCoupons()
  if (uni.getStorageSync('mall_token')) {
    loadMyCoupons()
  }
})

watch(tab, (value) => {
  if (value === 'mine') {
    if (!uni.getStorageSync('mall_token')) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      uni.switchTab({ url: '/pages/user/user' })
      return
    }
    loadMyCoupons()
  }
})

async function loadCoupons() {
  loading.value = true
  try {
    const res = await getCouponCenter()
    coupons.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadMyCoupons() {
  mineLoading.value = true
  try {
    const res = await getMyCoupons()
    myCoupons.value = res.data || []
  } finally {
    mineLoading.value = false
  }
}

async function receive(id: ApiId) {
  if (!uni.getStorageSync('mall_token')) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.switchTab({ url: '/pages/user/user' })
    return
  }
  receivingId.value = id
  try {
    await receiveCoupon(id)
    uni.showToast({ title: '领取成功', icon: 'success' })
    await Promise.all([loadCoupons(), loadMyCoupons()])
  } finally {
    receivingId.value = undefined
  }
}

function couponValue(item: CouponCenterItem | MyCoupon) {
  if (item.type === 2 && item.discountRate) {
    return `${Number(item.discountRate).toFixed(1)}折`
  }
  return `¥${Number(item.discountAmount || 0).toFixed(0)}`
}

function couponType(item: CouponCenterItem | MyCoupon) {
  return item.typeDesc || (item.type === 2 ? '折扣券' : '满减券')
}

function couponRule(item: CouponCenterItem | MyCoupon) {
  const minAmount = Number(item.minAmount || 0)
  return minAmount > 0 ? `满 ${minAmount.toFixed(0)} 元可用` : '无门槛可用'
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero {
  padding: 38rpx 30rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #111827, #2f8f67);
  color: #fff;
}

.kicker,
.title,
.subtitle,
.coupon-value text,
.coupon-name,
.coupon-rule,
.coupon-time {
  display: block;
}

.kicker {
  color: #ffd166;
  font-size: 23rpx;
  font-weight: 900;
}

.title {
  margin-top: 10rpx;
  font-size: 46rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 25rpx;
  line-height: 1.55;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin: 22rpx 0;
  padding: 8rpx;
  border-radius: 999rpx;
  background: #fff;
}

.tabs view {
  height: 64rpx;
  border-radius: 999rpx;
  color: #6b7280;
  font-size: 26rpx;
  font-weight: 900;
  line-height: 64rpx;
  text-align: center;
}

.tabs view.active {
  background: #111827;
  color: #fff;
}

.coupon-list {
  display: grid;
  gap: 18rpx;
}

.coupon-card {
  display: grid;
  grid-template-columns: 172rpx minmax(0, 1fr) 130rpx;
  gap: 18rpx;
  align-items: center;
  overflow: hidden;
  min-height: 174rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.coupon-card.mine {
  grid-template-columns: 172rpx minmax(0, 1fr);
}

.coupon-value {
  display: flex;
  min-height: 138rpx;
  flex-direction: column;
  justify-content: center;
  border-radius: 14rpx;
  background: #fff0f0;
  color: #e5484d;
  text-align: center;
}

.coupon-value text:first-child {
  font-size: 42rpx;
  font-weight: 900;
}

.coupon-value text:last-child {
  margin-top: 6rpx;
  font-size: 22rpx;
  font-weight: 800;
}

.coupon-copy {
  min-width: 0;
}

.coupon-name {
  overflow: hidden;
  color: #111827;
  font-size: 29rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-rule {
  margin-top: 10rpx;
  color: #374151;
  font-size: 24rpx;
  font-weight: 800;
}

.coupon-time {
  margin-top: 10rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.coupon-card button {
  height: 70rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 70rpx;
}

.empty {
  padding: 90rpx 0;
  color: #6b7280;
  text-align: center;
}
</style>
