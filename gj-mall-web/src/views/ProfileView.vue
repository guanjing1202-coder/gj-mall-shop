<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { getAddressList } from '@/api/address'
import { getMyCoupons } from '@/api/coupon'
import { getOrderPage } from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const loading = ref(false)
const saving = ref(false)
const orderTotal = ref(0)
const addressTotal = ref(0)
const couponTotal = ref(0)
const usableCouponTotal = ref(0)
const profileForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  gender: 0,
})

const displayName = computed(() => auth.user?.nickname || auth.user?.username || '会员')
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())

const quickActions = computed(() => [
  { label: '我的订单', value: orderTotal.value, path: '/orders' },
  { label: '购物车商品', value: cart.totalCount, path: '/cart' },
  { label: '可用优惠券', value: usableCouponTotal.value, path: '/coupons' },
  { label: '收货地址', value: addressTotal.value, path: '/checkout' },
  { label: '售后进度', value: '查看', path: '/after-sales' },
])

function syncForm() {
  profileForm.nickname = auth.user?.nickname || ''
  profileForm.phone = auth.user?.phone || ''
  profileForm.email = auth.user?.email || ''
  profileForm.avatar = auth.user?.avatar || ''
  profileForm.gender = Number(auth.user?.gender || 0)
}

async function loadDashboard() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const [profile, orders, addresses, coupons] = await Promise.all([
      auth.loadProfile(),
      getOrderPage({ pageNum: 1, pageSize: 1 }),
      getAddressList(),
      getMyCoupons(),
      cart.fetchCart(),
    ])
    if (profile) {
      syncForm()
    }
    orderTotal.value = Number(orders.data.total || 0)
    addressTotal.value = addresses.data?.length || 0
    couponTotal.value = coupons.data?.length || 0
    usableCouponTotal.value = (coupons.data || []).filter((item) => Number(item.status) === 0).length
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  saving.value = true
  try {
    await auth.updateProfile({
      nickname: profileForm.nickname.trim() || undefined,
      phone: profileForm.phone.trim() || undefined,
      email: profileForm.email.trim() || undefined,
      avatar: profileForm.avatar.trim() || undefined,
      gender: profileForm.gender,
    })
    syncForm()
    ElMessage.success('个人资料已保存')
  } finally {
    saving.value = false
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadDashboard()
    }
  },
)

onMounted(() => {
  syncForm()
  loadDashboard()
})
</script>

<template>
  <div class="profile-page">
    <ShopHeader />

    <main class="profile-shell">
      <section class="profile-hero">
        <div class="profile-avatar">
          <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="displayName" />
          <span v-else>{{ initials }}</span>
        </div>
        <div>
          <span>Account</span>
          <h1>{{ displayName }}</h1>
          <p>{{ auth.user?.username || '会员账号' }} · 管理资料、订单、券包和收货信息。</p>
        </div>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后进入个人中心</h2>
        <p>登录后可以管理资料、订单、购物车和优惠券。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else v-loading="loading" class="profile-layout">
        <section class="quick-grid">
          <button v-for="item in quickActions" :key="item.label" type="button" @click="router.push(item.path)">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </button>
        </section>

        <section class="profile-card">
          <div class="card-heading">
            <div>
              <span>Profile</span>
              <h2>基础资料</h2>
            </div>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </div>

          <el-form label-position="top" class="profile-form" @submit.prevent>
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="昵称">
                  <el-input v-model="profileForm.nickname" placeholder="设置昵称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号">
                  <el-input v-model="profileForm.phone" placeholder="绑定手机号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="profileForm.email" placeholder="邮箱地址" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-segmented
                    v-model="profileForm.gender"
                    :options="[
                      { label: '保密', value: 0 },
                      { label: '男', value: 1 },
                      { label: '女', value: 2 },
                    ]"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="头像 URL">
              <el-input v-model="profileForm.avatar" placeholder="https://..." />
            </el-form-item>
          </el-form>
        </section>

        <section class="profile-card action-card">
          <div class="card-heading">
            <div>
              <span>Shortcuts</span>
              <h2>常用入口</h2>
            </div>
          </div>
          <div class="shortcut-list">
            <button type="button" @click="router.push('/coupon-center')">
              <strong>领券中心</strong>
              <span>领取满减、新人和折扣优惠</span>
            </button>
            <button type="button" @click="router.push('/orders')">
              <strong>订单售后</strong>
              <span>查看支付、物流、确认收货</span>
            </button>
            <button type="button" @click="router.push('/after-sales')">
              <strong>售后进度</strong>
              <span>跟踪退款、退货和审核状态</span>
            </button>
            <button type="button" @click="router.push('/checkout')">
              <strong>收货地址</strong>
              <span>结算时新增或维护地址</span>
            </button>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 5%, rgba(47, 143, 103, 0.12), transparent 26%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.profile-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.profile-avatar {
  display: grid;
  width: 86px;
  height: 86px;
  place-items: center;
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(135deg, #111827, #2f8f67);
  box-shadow: 0 18px 34px rgba(17, 24, 39, 0.18);
  color: #fff;
  font-size: 32px;
  font-weight: 900;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-hero span,
.card-heading span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.profile-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.profile-hero p {
  margin: 10px 0 0;
  color: #6b7280;
}

.login-needed,
.profile-card,
.quick-grid button {
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.07);
}

.login-needed {
  padding: 44px;
}

.login-needed h2 {
  margin: 0;
  font-size: 28px;
}

.login-needed p {
  margin: 12px 0 24px;
  color: #6b7280;
}

.profile-layout {
  display: grid;
  gap: 18px;
  min-height: 360px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quick-grid button,
.shortcut-list button {
  border: 0;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.quick-grid button {
  padding: 22px;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.quick-grid button:hover,
.shortcut-list button:hover {
  transform: translateY(-2px);
}

.quick-grid strong {
  display: block;
  color: #e5484d;
  font-size: 30px;
  line-height: 1;
}

.quick-grid span {
  display: block;
  margin-top: 10px;
  color: #6b7280;
  font-weight: 900;
}

.profile-card {
  padding: 24px;
}

.card-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.card-heading h2 {
  margin: 9px 0 0;
  font-size: 26px;
}

.card-heading :deep(.el-button),
.login-needed :deep(.el-button--primary) {
  border-radius: 12px;
  font-weight: 900;
}

.card-heading :deep(.el-button--primary),
.login-needed :deep(.el-button--primary) {
  background: linear-gradient(135deg, #e5484d, #c92432);
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(229, 72, 77, 0.22);
}

.profile-form :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(17, 24, 39, 0.08) inset;
}

.profile-form :deep(.el-segmented) {
  border-radius: 999px;
}

.shortcut-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.shortcut-list button {
  min-height: 112px;
  padding: 18px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 16px;
  background: #fbfcf8;
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.shortcut-list button:hover {
  border-color: rgba(229, 72, 77, 0.22);
}

.shortcut-list strong,
.shortcut-list span {
  display: block;
}

.shortcut-list strong {
  font-size: 18px;
}

.shortcut-list span {
  margin-top: 10px;
  color: #6b7280;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .quick-grid,
  .shortcut-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .profile-shell {
    padding: 28px 16px 52px;
  }

  .profile-hero,
  .card-heading {
    align-items: start;
    flex-direction: column;
  }

  .quick-grid,
  .shortcut-list {
    grid-template-columns: 1fr;
  }
}
</style>
