<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">Address</text>
      <text class="title">收货地址</text>
      <text class="subtitle">管理结算下单时使用的收货信息。</text>
    </view>

    <view v-if="!isLoggedIn" class="empty-card">
      <text>登录后管理地址</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <button class="add-btn" @tap="openForm()">新增地址</button>

      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!addresses.length" class="empty-card">
        <text>暂无收货地址</text>
        <button @tap="openForm()">新增第一个地址</button>
      </view>
      <view v-else class="list">
        <view v-for="item in addresses" :key="item.id" class="address-card">
          <view class="card-head">
            <view>
              <text>{{ item.receiver }}</text>
              <text>{{ item.phone }}</text>
            </view>
            <text v-if="item.isDefault === 1">默认</text>
          </view>
          <text class="address-line">{{ addressLine(item) }}</text>
          <view class="actions">
            <button @tap="openForm(item)">编辑</button>
            <button v-if="item.isDefault !== 1" @tap="makeDefault(item)">设默认</button>
            <button class="danger" @tap="remove(item)">删除</button>
          </view>
        </view>
      </view>
    </view>

    <view v-if="dialogOpen" class="dialog-mask" @tap="dialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <text>{{ editingId ? '编辑地址' : '新增地址' }}</text>
          <button @tap="dialogOpen = false">关闭</button>
        </view>
        <input v-model="form.receiver" class="field" placeholder="收件人" />
        <input v-model="form.phone" class="field" placeholder="手机号" />
        <view class="city-grid">
          <input v-model="form.province" class="field" placeholder="省份" />
          <input v-model="form.city" class="field" placeholder="城市" />
          <input v-model="form.district" class="field" placeholder="区县" />
        </view>
        <input v-model="form.detail" class="field" placeholder="详细地址" />
        <input v-model="form.postCode" class="field" placeholder="邮编（选填）" />
        <view class="default-row" @tap="form.isDefault = form.isDefault === 1 ? 0 : 1">
          <view class="select-dot" :class="{ active: form.isDefault === 1 }" />
          <text>设为默认地址</text>
        </view>
        <button class="save-btn" :disabled="saving" @tap="save">保存地址</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import {
  addAddress,
  deleteAddress,
  getAddressList,
  setDefaultAddress,
  updateAddress,
  type AddressItem,
  type AddressPayload,
} from '@/api/address'
import type { ApiId } from '@/api/product'
import { hasLoginState } from '@/utils/auth'

const isLoggedIn = ref(false)
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editingId = ref<ApiId>()
const addresses = ref<AddressItem[]>([])

const form = reactive<AddressPayload>({
  receiver: '',
  phone: '',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  detail: '',
  postCode: '',
  isDefault: 1,
})

onShow(() => {
  isLoggedIn.value = hasLoginState()
  if (isLoggedIn.value) {
    loadAddresses()
  } else {
    addresses.value = []
    dialogOpen.value = false
  }
})

onPullDownRefresh(async () => {
  if (isLoggedIn.value) await loadAddresses()
  uni.stopPullDownRefresh()
})

async function loadAddresses() {
  loading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openForm(address?: AddressItem) {
  editingId.value = address?.id
  Object.assign(form, {
    receiver: address?.receiver || '',
    phone: address?.phone || '',
    province: address?.province || '广东省',
    city: address?.city || '深圳市',
    district: address?.district || '南山区',
    detail: address?.detail || '',
    postCode: address?.postCode || '',
    isDefault: address?.isDefault ?? (addresses.value.length ? 0 : 1),
  })
  dialogOpen.value = true
}

async function save() {
  if (!form.receiver.trim() || !form.phone.trim() || !form.detail.trim()) {
    uni.showToast({ title: '请完整填写地址', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload: AddressPayload = {
      ...form,
      receiver: form.receiver.trim(),
      phone: form.phone.trim(),
      province: form.province.trim(),
      city: form.city.trim(),
      district: form.district.trim(),
      detail: form.detail.trim(),
      postCode: form.postCode?.trim() || undefined,
    }
    if (editingId.value) {
      await updateAddress({ ...payload, id: editingId.value })
    } else {
      await addAddress(payload)
    }
    dialogOpen.value = false
    uni.showToast({ title: '地址已保存', icon: 'success' })
    await loadAddresses()
  } finally {
    saving.value = false
  }
}

async function makeDefault(address: AddressItem) {
  await setDefaultAddress(address.id)
  uni.showToast({ title: '默认地址已更新', icon: 'success' })
  await loadAddresses()
}

async function remove(address: AddressItem) {
  await deleteAddress(address.id)
  uni.showToast({ title: '地址已删除', icon: 'success' })
  await loadAddresses()
}

function addressLine(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.address-card,
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
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
}

.add-btn {
  width: 100%;
  height: 78rpx;
  margin: 22rpx 0 0;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-weight: 900;
  line-height: 78rpx;
}

.list {
  display: grid;
  gap: 18rpx;
  margin-top: 22rpx;
}

.address-card {
  padding: 22rpx;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.card-head view {
  min-width: 0;
}

.card-head view text {
  display: inline-flex;
  margin-right: 16rpx;
  color: #111827;
  font-size: 29rpx;
  font-weight: 900;
}

.card-head > text {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.address-line {
  display: block;
  margin-top: 14rpx;
  color: #6b7280;
  font-size: 25rpx;
  line-height: 1.55;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 18rpx;
}

.actions button,
.empty-card button,
.dialog-head button,
.save-btn {
  height: 58rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.actions button.danger {
  background: #e5484d;
}

.empty,
.empty-card {
  margin-top: 22rpx;
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
  padding: 0 30rpx;
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
  background: #f3f4f6;
  color: #374151;
}

.field {
  height: 78rpx;
  margin-top: 14rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
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
  background: #e5484d;
  line-height: 82rpx;
}
</style>
