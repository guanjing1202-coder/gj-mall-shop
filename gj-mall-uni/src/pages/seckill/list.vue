<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">Flash Sale</text>
        <text class="title">限时秒杀</text>
        <text class="subtitle">实时库存、限购提醒和快捷下单都在这里。</text>
      </view>
      <button @tap="refresh">刷新</button>
    </view>

    <view v-if="loading" class="empty">活动加载中...</view>

    <view v-else-if="!activities.length" class="empty-card">
      <text>暂无进行中的秒杀</text>
      <button @tap="goHome">去首页逛逛</button>
    </view>

    <view v-else>
      <scroll-view scroll-x class="activity-tabs">
        <view class="activity-row">
          <view
            v-for="item in activities"
            :key="item.id"
            class="activity-tab"
            :class="{ active: String(currentActivityId) === String(item.id) }"
            @tap="selectActivity(item)"
          >
            <text>{{ item.name }}</text>
            <text>{{ item.statusDesc || '进行中' }}</text>
          </view>
        </view>
      </scroll-view>

      <view v-if="currentActivity" class="panel activity-panel">
        <view class="section-head">
          <text>{{ currentActivity.name }}</text>
          <text>{{ currentActivity.statusDesc || '进行中' }}</text>
        </view>
        <view class="time-grid">
          <view>
            <text>开始时间</text>
            <text>{{ formatTime(currentActivity.startTime) }}</text>
          </view>
          <view>
            <text>结束时间</text>
            <text>{{ formatTime(currentActivity.endTime) }}</text>
          </view>
        </view>
      </view>

      <view class="panel address-panel">
        <view class="section-head">
          <text>收货地址</text>
          <button @tap="openAddressForm()">新增</button>
        </view>
        <view v-if="!isLoggedIn" class="login-card">
          <text>登录后参与秒杀</text>
          <button @tap="goLogin">去登录</button>
        </view>
        <view v-else-if="addressLoading" class="empty small">地址加载中...</view>
        <view v-else-if="!addresses.length" class="address-empty" @tap="openAddressForm()">
          <text>新增收货地址</text>
          <text>秒杀下单需要先选择地址</text>
        </view>
        <scroll-view v-else scroll-x class="address-scroll">
          <view class="address-row">
            <view
              v-for="item in addresses"
              :key="item.id"
              class="address-card"
              :class="{ active: String(selectedAddressId) === String(item.id) }"
              @tap="selectedAddressId = item.id"
            >
              <view>
                <text>{{ item.receiver }}</text>
                <text v-if="item.isDefault === 1">默认</text>
              </view>
              <text>{{ item.phone }}</text>
              <text>{{ addressLine(item) }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view v-if="detailLoading" class="empty small">商品加载中...</view>
      <view v-else-if="!skus.length" class="empty-card">
        <text>本场活动暂无商品</text>
        <button @tap="refresh">重新加载</button>
      </view>
      <view v-else class="sku-list">
        <view v-for="item in skus" :key="item.id" class="sku-card">
          <image class="sku-image" :src="normalizeImage(item.image, String(item.skuId))" mode="aspectFill" @tap="goProduct(item)" />
          <view class="sku-copy">
            <view class="sku-head">
              <text>{{ item.spuName || item.skuName || '秒杀商品' }}</text>
              <text>限购 {{ item.seckillLimit || 1 }} 件</text>
            </view>
            <text class="sku-spec">{{ specText(item) }}</text>
            <view class="stock-bar">
              <view :style="{ width: stockPercent(item) + '%' }" />
            </view>
            <view class="sku-meta">
              <text>剩余 {{ item.remainStock || 0 }}</text>
              <text>已抢 {{ item.soldCount || 0 }}</text>
            </view>
            <view class="price-row">
              <view>
                <text>{{ formatPrice(item.seckillPrice) }}</text>
                <text>{{ formatPrice(item.originalPrice) }}</text>
              </view>
              <button :disabled="buyingSkuId === item.id || !canBuy(item)" @tap="buy(item)">
                {{ canBuy(item) ? '立即抢购' : '已抢光' }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="addressDialogOpen" class="dialog-mask" @tap="addressDialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <text>新增地址</text>
          <button @tap="addressDialogOpen = false">关闭</button>
        </view>
        <input v-model="addressForm.receiver" class="field" placeholder="收件人" />
        <input v-model="addressForm.phone" class="field" placeholder="手机号" />
        <view class="city-grid">
          <input v-model="addressForm.province" class="field" placeholder="省份" />
          <input v-model="addressForm.city" class="field" placeholder="城市" />
          <input v-model="addressForm.district" class="field" placeholder="区县" />
        </view>
        <input v-model="addressForm.detail" class="field" placeholder="详细地址" />
        <input v-model="addressForm.postCode" class="field" placeholder="邮编（选填）" />
        <view class="default-row" @tap="addressForm.isDefault = addressForm.isDefault === 1 ? 0 : 1">
          <view class="select-dot" :class="{ active: addressForm.isDefault === 1 }" />
          <text>设为默认地址</text>
        </view>
        <button class="save-btn" :disabled="addressSaving" @tap="saveAddress">保存地址</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { addAddress, getAddressList, type AddressItem, type AddressPayload } from '@/api/address'
import { createSeckillOrder, getActiveSeckills, getSeckillDetail, type SeckillActivity, type SeckillSku } from '@/api/seckill'
import type { ApiId } from '@/api/product'
import { requireSession, syncSession } from '@/utils/session'

const loading = ref(false)
const detailLoading = ref(false)
const addressLoading = ref(false)
const addressSaving = ref(false)
const addressDialogOpen = ref(false)
const isLoggedIn = ref(false)
const activities = ref<SeckillActivity[]>([])
const currentActivityId = ref<ApiId | ''>('')
const currentActivity = ref<SeckillActivity>()
const addresses = ref<AddressItem[]>([])
const selectedAddressId = ref<ApiId | ''>('')
const buyingSkuId = ref<ApiId | ''>('')

const addressForm = reactive<AddressPayload>({
  receiver: '',
  phone: '',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  detail: '',
  postCode: '',
  isDefault: 1,
})

const skus = computed(() => currentActivity.value?.skus || [])

onShow(async () => {
  isLoggedIn.value = await syncSession()
  await refresh()
  if (isLoggedIn.value) {
    await loadAddresses()
  } else {
    addresses.value = []
    selectedAddressId.value = ''
  }
})

onPullDownRefresh(async () => {
  await refresh()
  if (isLoggedIn.value) await loadAddresses()
  uni.stopPullDownRefresh()
})

async function refresh() {
  loading.value = !activities.value.length
  try {
    const res = await getActiveSeckills()
    activities.value = res.data || []
    const exists = activities.value.find((item) => String(item.id) === String(currentActivityId.value))
    const next = exists || activities.value[0]
    if (next) {
      await selectActivity(next)
    } else {
      currentActivity.value = undefined
      currentActivityId.value = ''
    }
  } finally {
    loading.value = false
  }
}

async function selectActivity(activity: SeckillActivity) {
  currentActivityId.value = activity.id
  detailLoading.value = true
  try {
    const res = await getSeckillDetail(activity.id)
    currentActivity.value = res.data || activity
  } finally {
    detailLoading.value = false
  }
}

async function loadAddresses() {
  addressLoading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data || []
    const current = addresses.value.find((item) => String(item.id) === String(selectedAddressId.value))
    if (!current && addresses.value.length) {
      selectedAddressId.value = addresses.value.find((item) => item.isDefault === 1)?.id || addresses.value[0].id
    } else if (!addresses.value.length) {
      selectedAddressId.value = ''
    }
  } finally {
    addressLoading.value = false
  }
}

function openAddressForm() {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  Object.assign(addressForm, {
    receiver: '',
    phone: '',
    province: '广东省',
    city: '深圳市',
    district: '南山区',
    detail: '',
    postCode: '',
    isDefault: addresses.value.length ? 0 : 1,
  })
  addressDialogOpen.value = true
}

async function saveAddress() {
  if (!addressForm.receiver.trim() || !addressForm.phone.trim() || !addressForm.detail.trim()) {
    uni.showToast({ title: '请完整填写地址', icon: 'none' })
    return
  }
  addressSaving.value = true
  try {
    const payload: AddressPayload = {
      receiver: addressForm.receiver.trim(),
      phone: addressForm.phone.trim(),
      province: addressForm.province.trim(),
      city: addressForm.city.trim(),
      district: addressForm.district.trim(),
      detail: addressForm.detail.trim(),
      postCode: addressForm.postCode?.trim() || undefined,
      isDefault: addressForm.isDefault,
    }
    const res = await addAddress(payload)
    selectedAddressId.value = res.data
    addressDialogOpen.value = false
    await loadAddresses()
    uni.showToast({ title: '地址已保存', icon: 'success' })
  } finally {
    addressSaving.value = false
  }
}

async function buy(item: SeckillSku) {
  if (!(await requireSession())) {
    isLoggedIn.value = false
    return
  }
  isLoggedIn.value = true
  if (!selectedAddressId.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  if (!canBuy(item)) return
  buyingSkuId.value = item.id
  try {
    const res = await createSeckillOrder(item.id, selectedAddressId.value)
    uni.showToast({ title: '抢购成功', icon: 'success' })
    uni.redirectTo({ url: `/pages/pay/pay?orderNo=${encodeURIComponent(res.data)}` })
  } finally {
    buyingSkuId.value = ''
  }
}

function canBuy(item: SeckillSku) {
  return Number(item.remainStock || 0) > 0
}

function stockPercent(item: SeckillSku) {
  const remain = Number(item.remainStock || 0)
  const sold = Number(item.soldCount || 0)
  const total = Math.max(remain + sold, Number(item.seckillLimit || 1), 1)
  return Math.max(8, Math.min(100, (remain / total) * 100))
}

function goProduct(item: SeckillSku) {
  if (item.spuId) uni.navigateTo({ url: `/pages/product/detail?id=${item.spuId}` })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function addressLine(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

function specText(item: SeckillSku) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : item.skuName || '默认规格'
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 16).replace('T', ' ')
}

function normalizeImage(url?: string, seed = 'seckill') {
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
.sku-card,
.empty-card {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22rpx;
  padding: 38rpx 30rpx;
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
  font-size: 46rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
  line-height: 1.45;
}

.hero button,
.section-head button,
.empty-card button,
.login-card button,
.price-row button,
.dialog-head button,
.save-btn {
  border-radius: 999rpx;
  font-weight: 900;
}

.hero button {
  flex: 0 0 auto;
  height: 62rpx;
  margin: 0;
  padding: 0 24rpx;
  background: #ffd166;
  color: #111827;
  font-size: 24rpx;
  line-height: 62rpx;
}

.activity-tabs {
  width: 100%;
  margin: 22rpx 0;
  white-space: nowrap;
}

.activity-row {
  display: flex;
  gap: 14rpx;
}

.activity-tab {
  display: inline-flex;
  min-width: 220rpx;
  flex-direction: column;
  gap: 6rpx;
  padding: 18rpx 22rpx;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  background: #fff;
}

.activity-tab.active {
  border-color: #e5484d;
  background: #fff7f7;
}

.activity-tab text:first-child {
  color: #111827;
  font-size: 27rpx;
  font-weight: 900;
}

.activity-tab text:last-child {
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.panel {
  margin-top: 22rpx;
  padding: 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.section-head text:first-child {
  overflow: hidden;
  color: #111827;
  font-size: 30rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-head text:last-child {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.section-head button {
  height: 54rpx;
  margin: 0;
  padding: 0 20rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  line-height: 54rpx;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.time-grid view {
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.time-grid text {
  display: block;
}

.time-grid text:first-child {
  color: #6b7280;
  font-size: 22rpx;
}

.time-grid text:last-child {
  margin-top: 8rpx;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
}

.login-card,
.address-empty {
  padding: 28rpx 20rpx;
  border: 2rpx dashed #d1d5db;
  border-radius: 14rpx;
  text-align: center;
}

.login-card text,
.address-empty text {
  display: block;
}

.login-card text:first-child,
.address-empty text:first-child {
  color: #111827;
  font-size: 28rpx;
  font-weight: 900;
}

.address-empty text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
}

.login-card button,
.empty-card button,
.save-btn {
  display: inline-block;
  height: 62rpx;
  margin-top: 18rpx;
  padding: 0 28rpx;
  background: #e5484d;
  color: #fff;
  font-size: 24rpx;
  line-height: 62rpx;
}

.address-scroll {
  width: 100%;
  white-space: nowrap;
}

.address-row {
  display: flex;
  gap: 14rpx;
}

.address-card {
  display: inline-flex;
  width: 500rpx;
  min-height: 150rpx;
  flex-direction: column;
  padding: 18rpx;
  border: 2rpx solid #eef0f3;
  border-radius: 14rpx;
  white-space: normal;
}

.address-card.active {
  border-color: #e5484d;
  background: #fff7f7;
}

.address-card view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.address-card text {
  display: block;
}

.address-card view text:first-child {
  overflow: hidden;
  font-size: 28rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address-card view text:last-child {
  flex: 0 0 auto;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 21rpx;
  font-weight: 900;
}

.address-card > text:nth-child(2) {
  margin-top: 8rpx;
  color: #111827;
  font-weight: 800;
}

.address-card > text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.4;
}

.sku-list {
  display: grid;
  gap: 18rpx;
  margin-top: 22rpx;
}

.sku-card {
  display: grid;
  grid-template-columns: 210rpx minmax(0, 1fr);
  gap: 18rpx;
  padding: 18rpx;
}

.sku-image {
  width: 210rpx;
  height: 210rpx;
  border-radius: 14rpx;
  background: #eef2f7;
}

.sku-copy {
  min-width: 0;
}

.sku-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
}

.sku-head text:first-child {
  display: -webkit-box;
  overflow: hidden;
  color: #111827;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1.32;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.sku-head text:last-child {
  flex: 0 0 auto;
  padding: 6rpx 10rpx;
  border-radius: 999rpx;
  background: #fff7ed;
  color: #92400e;
  font-size: 21rpx;
  font-weight: 900;
}

.sku-spec {
  display: block;
  overflow: hidden;
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-bar {
  overflow: hidden;
  height: 14rpx;
  margin-top: 16rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
}

.stock-bar view {
  height: 100%;
  border-radius: inherit;
  background: #e5484d;
}

.sku-meta,
.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sku-meta {
  margin-top: 8rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.price-row {
  margin-top: 16rpx;
  gap: 14rpx;
}

.price-row view {
  min-width: 0;
}

.price-row view text {
  display: block;
}

.price-row view text:first-child {
  color: #e5484d;
  font-size: 36rpx;
  font-weight: 900;
}

.price-row view text:last-child {
  margin-top: 2rpx;
  color: #9ca3af;
  font-size: 22rpx;
  text-decoration: line-through;
}

.price-row button {
  width: 168rpx;
  height: 66rpx;
  flex: 0 0 auto;
  margin: 0;
  background: #e5484d;
  color: #fff;
  font-size: 24rpx;
  line-height: 66rpx;
}

.price-row button[disabled] {
  background: #d1d5db;
  color: #fff;
}

.empty,
.empty-card {
  padding: 90rpx 24rpx;
  color: #6b7280;
  text-align: center;
}

.empty.small {
  padding: 34rpx 0;
}

.empty-card text {
  display: block;
  margin-bottom: 22rpx;
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-end;
  background: rgba(17, 24, 39, 0.46);
}

.dialog {
  width: 100%;
  padding: 26rpx;
  border-radius: 24rpx 24rpx 0 0;
  background: #fff;
  box-sizing: border-box;
}

.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.dialog-head text {
  font-size: 32rpx;
  font-weight: 900;
}

.dialog-head button {
  margin: 0;
  background: #f3f4f6;
  color: #374151;
  font-size: 23rpx;
}

.field {
  height: 78rpx;
  margin-top: 14rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  box-sizing: border-box;
  font-size: 26rpx;
}

.city-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.default-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
  color: #374151;
  font-size: 25rpx;
  font-weight: 900;
}

.select-dot {
  width: 34rpx;
  height: 34rpx;
  border: 3rpx solid #d1d5db;
  border-radius: 999rpx;
}

.select-dot.active {
  border-color: #e5484d;
  background: #e5484d;
  box-shadow: inset 0 0 0 8rpx #fff;
}

.save-btn {
  width: 100%;
  height: 82rpx;
  margin-top: 24rpx;
  line-height: 82rpx;
}
</style>
