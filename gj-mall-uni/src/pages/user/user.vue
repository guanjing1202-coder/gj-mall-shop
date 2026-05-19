<template>
  <view class="page">
    <view class="profile-card">
      <image class="avatar" :src="displayAvatar" mode="aspectFill" @tap="isLoggedIn && chooseAvatar()" />
      <view class="profile-copy">
        <text class="name">{{ displayName }}</text>
        <text class="meta">{{ isLoggedIn ? profile?.phone || profile?.email || '欢迎回来' : '登录后解锁订单、购物车和优惠券' }}</text>
      </view>
      <view v-if="isLoggedIn" class="profile-actions">
        <button :disabled="avatarUploading" @tap="chooseAvatar">{{ avatarUploading ? '上传中' : '换头像' }}</button>
        <button @tap="signOut">退出</button>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="login-panel">
      <text class="panel-title">会员登录</text>
      <input v-model="account" class="field" placeholder="账号 / 手机号" />
      <input v-model="password" class="field" placeholder="密码" password />
      <button :disabled="submitting" @tap="submitLogin">登录</button>
    </view>

    <view v-else>
      <view class="stats">
        <view v-for="item in stats" :key="item.label" @tap="openShortcut(item.path)">
          <text>{{ item.value }}</text>
          <text>{{ item.label }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>快捷入口</text>
        </view>
        <view class="shortcut-grid">
          <view v-for="item in shortcuts" :key="item.label" class="shortcut" @tap="openShortcut(item.path)">
            <text class="shortcut-icon">{{ item.icon }}</text>
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="panel info-panel">
        <view class="section-head">
          <text>账户资料</text>
          <button @tap="saveProfile">保存</button>
        </view>
        <input v-model="profileForm.nickname" class="field" placeholder="昵称" />
        <input v-model="profileForm.phone" class="field" placeholder="手机号" />
        <input v-model="profileForm.email" class="field" placeholder="邮箱" />
        <input v-model="profileForm.avatar" class="field" placeholder="头像 URL，可上传后自动填入" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getProfile, login, logout, updateProfile, type UserProfile } from '@/api/auth'
import { getAddressList } from '@/api/address'
import { getCart } from '@/api/cart'
import { getMyCoupons } from '@/api/coupon'
import { uploadImage } from '@/api/file'
import { getFavoritePage } from '@/api/favorite'
import { getHistoryPage } from '@/api/history'
import { getAfterSalePage, getOrderPage } from '@/api/order'
import { clearLoginState, hasRecoverableLoginState, saveLoginTokens } from '@/utils/auth'
import { syncSession } from '@/utils/session'

uni.$on('mall:auth-changed', (event: { loggedIn?: boolean }) => {
  if (!event?.loggedIn) {
    resetUserState()
  }
})

const fallbackAvatar = 'https://picsum.photos/seed/gj-user/180/180'

const account = ref('test')
const password = ref('123456')
const submitting = ref(false)
const avatarUploading = ref(false)
const isLoggedIn = ref(false)
const profile = ref<UserProfile>()

const statValues = reactive({
  orders: 0,
  coupons: 0,
  cart: 0,
  addresses: 0,
  afterSales: 0,
  favorites: 0,
  histories: 0,
})

const profileForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
})

const displayName = computed(() => {
  if (!isLoggedIn.value) return '游客'
  return profile.value?.nickname || profile.value?.username || '会员'
})

const displayAvatar = computed(() => profileForm.avatar || profile.value?.avatar || fallbackAvatar)

const stats = computed(() => [
  { label: '订单', value: statValues.orders, path: '/pages/orders/list' },
  { label: '优惠券', value: statValues.coupons, path: '/pages/coupon/center' },
  { label: '收藏', value: statValues.favorites, path: '/pages/favorite/list' },
  { label: '足迹', value: statValues.histories, path: '/pages/history/list' },
  { label: '购物车', value: statValues.cart, path: '/pages/cart/cart' },
])

const shortcuts = [
  { label: '我的购物车', icon: '购', path: '/pages/cart/cart' },
  { label: '我的订单', icon: '单', path: '/pages/orders/list' },
  { label: '我的收藏', icon: '藏', path: '/pages/favorite/list' },
  { label: '我的足迹', icon: '迹', path: '/pages/history/list' },
  { label: '领券中心', icon: '券', path: '/pages/coupon/center' },
  { label: '限时秒杀', icon: '秒', path: '/pages/seckill/list' },
  { label: '售后进度', icon: '售', path: '/pages/after-sales/list' },
  { label: '收货地址', icon: '址', path: '/pages/address/list' },
  { label: '商品分类', icon: '类', path: '/pages/category/category' },
  { label: '确认订单', icon: '结', path: '/pages/checkout/checkout' },
]

onShow(async () => {
  if (!hasRecoverableLoginState()) {
    resetUserState()
    return
  }
  const ok = await syncSession()
  if (!ok) {
    resetUserState()
    return
  }
  isLoggedIn.value = true
  await loadProfile()
  await loadStats()
})

async function submitLogin() {
  if (!account.value.trim() || !password.value.trim()) {
    uni.showToast({ title: '请输入账号和密码', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const res = await login(account.value.trim(), password.value.trim())
    saveLoginTokens(res.data.accessToken, res.data.refreshToken)
    profile.value = res.data.user
    fillProfileForm(res.data.user)
    isLoggedIn.value = true
    uni.showToast({ title: '登录成功', icon: 'success' })
    await loadStats()
  } finally {
    submitting.value = false
  }
}

async function loadProfile() {
  try {
    const res = await getProfile()
    profile.value = res.data
    fillProfileForm(res.data)
    isLoggedIn.value = true
  } catch (error) {
    resetUserState()
  }
}

async function loadStats() {
  const jobs = await Promise.allSettled([
    getOrderPage({ pageNum: 1, pageSize: 1 }),
    getMyCoupons(),
    getCart(),
    getAddressList(),
    getAfterSalePage({ pageNum: 1, pageSize: 1 }),
    getFavoritePage({ current: 1, size: 1 }),
    getHistoryPage({ current: 1, size: 1 }),
  ])
  if (jobs[0].status === 'fulfilled') statValues.orders = Number(jobs[0].value.data?.total || 0)
  if (jobs[1].status === 'fulfilled') statValues.coupons = jobs[1].value.data?.length || 0
  if (jobs[2].status === 'fulfilled') statValues.cart = jobs[2].value.data?.totalCount || 0
  if (jobs[3].status === 'fulfilled') statValues.addresses = jobs[3].value.data?.length || 0
  if (jobs[4].status === 'fulfilled') statValues.afterSales = Number(jobs[4].value.data?.total || 0)
  if (jobs[5].status === 'fulfilled') statValues.favorites = Number(jobs[5].value.data?.total || 0)
  if (jobs[6].status === 'fulfilled') statValues.histories = Number(jobs[6].value.data?.total || 0)
}

async function saveProfile() {
  await updateProfile({
    nickname: profileForm.nickname.trim(),
    phone: profileForm.phone.trim(),
    email: profileForm.email.trim(),
    avatar: profileForm.avatar.trim(),
  })
  uni.showToast({ title: '已保存', icon: 'success' })
  await loadProfile()
}

async function chooseAvatar() {
  if (!isLoggedIn.value || avatarUploading.value) return
  try {
    const files = await chooseImageFiles()
    const filePath = files[0]
    if (!filePath) return
    avatarUploading.value = true
    const uploaded = await uploadImage(filePath, 'avatar')
    profileForm.avatar = uploaded.url
    await updateProfile({
      nickname: profileForm.nickname.trim(),
      phone: profileForm.phone.trim(),
      email: profileForm.email.trim(),
      avatar: uploaded.url,
    })
    await loadProfile()
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch (error) {
    if ((error as any)?.errMsg?.includes('cancel')) return
    uni.showToast({ title: '头像上传失败', icon: 'none' })
  } finally {
    avatarUploading.value = false
  }
}

async function signOut() {
  try {
    await logout()
  } catch (error) {
    console.warn(error)
  }
  clearLoginState('logout')
  resetUserState()
  uni.showToast({ title: '已退出', icon: 'success' })
}

function resetUserState() {
  profile.value = undefined
  isLoggedIn.value = false
  Object.assign(statValues, { orders: 0, coupons: 0, cart: 0, addresses: 0, afterSales: 0, favorites: 0, histories: 0 })
  fillProfileForm()
}

function fillProfileForm(user?: UserProfile) {
  profileForm.nickname = user?.nickname || ''
  profileForm.phone = user?.phone || ''
  profileForm.email = user?.email || ''
  profileForm.avatar = user?.avatar || ''
}

function chooseImageFiles(): Promise<string[]> {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => resolve(res.tempFilePaths || []),
      fail: reject,
    })
  })
}

function openShortcut(path: string) {
  if (!path) {
    uni.showToast({ title: '移动端页面继续开发中', icon: 'none' })
    return
  }
  if (path.indexOf('/pages/cart') === 0 || path.indexOf('/pages/category') === 0) {
    uni.switchTab({ url: path })
    return
  }
  uni.navigateTo({ url: path })
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8f5;
  color: #111827;
}

.profile-card,
.login-panel,
.panel,
.stats {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 26rpx;
  background: #111827;
  color: #fff;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.24);
  border-radius: 999rpx;
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.name,
.meta,
.panel-title {
  display: block;
}

.name {
  overflow: hidden;
  font-size: 36rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 24rpx;
}

.profile-card button {
  margin: 0;
  border-radius: 999rpx;
  background: #fff;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
}

.profile-actions {
  display: grid;
  gap: 10rpx;
}

.profile-actions button {
  min-width: 120rpx;
  padding: 0 20rpx;
}

.login-panel,
.panel {
  margin-top: 22rpx;
  padding: 26rpx;
}

.panel-title {
  margin-bottom: 20rpx;
  font-size: 32rpx;
  font-weight: 900;
}

.field {
  height: 82rpx;
  margin-top: 16rpx;
  padding: 0 22rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  color: #111827;
  font-size: 27rpx;
}

.login-panel button {
  height: 82rpx;
  margin-top: 22rpx;
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-weight: 900;
  line-height: 82rpx;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10rpx;
  margin-top: 22rpx;
  padding: 22rpx 10rpx;
}

.stats view {
  text-align: center;
}

.stats text {
  display: block;
}

.stats text:first-child {
  color: #e5484d;
  font-size: 34rpx;
  font-weight: 900;
}

.stats text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  font-weight: 800;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.section-head text {
  font-size: 30rpx;
  font-weight: 900;
}

.section-head button {
  margin: 0;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.shortcut {
  min-width: 0;
  padding: 20rpx 8rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  color: #374151;
  text-align: center;
}

.shortcut text {
  display: block;
  overflow: hidden;
  font-size: 22rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut .shortcut-icon {
  margin-bottom: 10rpx;
  width: 54rpx;
  height: 54rpx;
  margin-right: auto;
  margin-left: auto;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 28rpx;
  line-height: 54rpx;
}

.info-panel {
  margin-bottom: 24rpx;
}
</style>
