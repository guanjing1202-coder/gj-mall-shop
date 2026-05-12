<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Search, ShoppingBag, UserFilled } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const submitting = ref(false)
const authMode = ref<'login' | 'register'>('login')
const loginForm = reactive({
  account: 'test',
  password: '123456',
})
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
})

const navItems = [
  { label: '首页', path: '/' },
  { label: '全部商品', path: '/products' },
  { label: '新品', path: '/products?newStatus=1' },
  { label: '热卖', path: '/products?sort=sales' },
  { label: '品牌馆', path: '/products?focus=brands' },
  { label: '领券', path: '/coupon-center' },
]

const activePath = computed(() => route.path)
const cartCount = computed(() => cart.totalCount)

function go(path: string) {
  router.push(path)
}

function openLogin(mode: 'login' | 'register' = 'login') {
  authMode.value = mode
  auth.openLoginDialog()
}

async function submitAuth() {
  submitting.value = true
  try {
    if (authMode.value === 'login') {
      await auth.loginByPassword(loginForm.account.trim(), loginForm.password)
    } else {
      if (!registerForm.username.trim() || !registerForm.password) {
        ElMessage.warning('请填写用户名和密码')
        return
      }
      await auth.registerAndLogin(
        registerForm.username.trim(),
        registerForm.password,
        registerForm.nickname.trim() || undefined,
        registerForm.phone.trim() || undefined,
      )
    }
    await cart.fetchCart()
    ElMessage.success('登录成功')
  } finally {
    submitting.value = false
  }
}

async function handleLogout() {
  await auth.logout()
  cart.reset()
  ElMessage.success('已退出登录')
}

function handleAuthExpired() {
  auth.clearSession()
  cart.reset()
  auth.openLoginDialog()
}

function goCart() {
  if (!auth.isLoggedIn) {
    openLogin('login')
    return
  }
  router.push('/cart')
}

function goOrders() {
  if (!auth.isLoggedIn) {
    openLogin('login')
    return
  }
  router.push('/orders')
}

function goCoupons() {
  if (!auth.isLoggedIn) {
    openLogin('login')
    return
  }
  router.push('/coupons')
}

function goAfterSales() {
  if (!auth.isLoggedIn) {
    openLogin('login')
    return
  }
  router.push('/after-sales')
}

function goProfile() {
  if (!auth.isLoggedIn) {
    openLogin('login')
    return
  }
  router.push('/profile')
}

onMounted(() => {
  window.addEventListener('mall-auth-expired', handleAuthExpired)
  if (auth.isLoggedIn) {
    cart.fetchCart()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('mall-auth-expired', handleAuthExpired)
})
</script>

<template>
  <header class="shop-header">
    <div class="shop-header__inner">
      <button class="brand" type="button" @click="go('/')">
        <span class="brand__mark">GJ</span>
        <span>
          <strong>GJ Mall</strong>
          <small>精选商城</small>
        </span>
      </button>

      <nav class="shop-nav" aria-label="商城导航">
        <button
          v-for="item in navItems"
          :key="item.label"
          class="shop-nav__item"
          :class="{ active: activePath === item.path.split('?')[0] || (item.path === '/' && activePath === '/') }"
          type="button"
          @click="go(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="shop-actions">
        <button class="icon-action" type="button" title="搜索" @click="go('/products')">
          <el-icon aria-hidden="true"><Search /></el-icon>
        </button>
        <button class="icon-action cart-action" type="button" title="购物车" aria-label="购物车" @click="goCart">
          <el-icon aria-hidden="true"><ShoppingBag /></el-icon>
          <em v-if="cartCount">{{ cartCount > 99 ? '99+' : cartCount }}</em>
        </button>
        <button v-if="!auth.isLoggedIn" class="account-action" type="button" @click="openLogin('login')">
          <el-icon aria-hidden="true"><UserFilled /></el-icon>
          <span>登录</span>
        </button>
        <el-dropdown v-else trigger="click">
          <button class="account-action account-action--user" type="button">
            <span class="account-avatar">{{ auth.displayName.slice(0, 1).toUpperCase() }}</span>
            <span>{{ auth.displayName }}</span>
            <el-icon aria-hidden="true"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
              <el-dropdown-item @click="goCart">我的购物车</el-dropdown-item>
              <el-dropdown-item @click="goOrders">我的订单</el-dropdown-item>
              <el-dropdown-item @click="goCoupons">我的优惠券</el-dropdown-item>
              <el-dropdown-item @click="goAfterSales">我的售后</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-dialog
      v-model="auth.loginDialogOpen"
      :title="authMode === 'login' ? '会员登录' : '创建账号'"
      width="420px"
      class="auth-dialog"
      append-to-body
      destroy-on-close
    >
      <div class="auth-switch">
        <button :class="{ active: authMode === 'login' }" type="button" @click="authMode = 'login'">登录</button>
        <button :class="{ active: authMode === 'register' }" type="button" @click="authMode = 'register'">注册</button>
      </div>

      <el-form v-if="authMode === 'login'" label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="loginForm.account" placeholder="用户名 / 手机号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </el-form>

      <el-form v-else label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="registerForm.username" placeholder="4-20 位用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="6-32 位密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="registerForm.nickname" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="registerForm.phone" placeholder="选填" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auth.closeLoginDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAuth">
          {{ authMode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </template>
    </el-dialog>
  </header>
</template>

<style scoped>
.shop-header {
  position: sticky;
  top: 0;
  z-index: 20;
  width: 100%;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(20px) saturate(1.25);
}

.shop-header__inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 32px;
  max-width: 1240px;
  height: 72px;
  margin: 0 auto;
  padding: 0 24px;
}

.brand,
.shop-nav__item,
.icon-action,
.account-action {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 178px;
  padding: 5px 0;
  color: #111827;
  text-align: left;
}

.brand__mark {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #111827, #2f8f67);
  box-shadow: 0 12px 24px rgba(17, 24, 39, 0.18);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 18px;
  letter-spacing: 0;
}

.brand small {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.shop-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: fit-content;
  justify-self: center;
  padding: 5px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 999px;
  background: rgba(247, 248, 245, 0.82);
}

.shop-nav__item {
  min-width: 64px;
  padding: 8px 14px;
  border-radius: 999px;
  color: #4b5563;
  font-weight: 600;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.shop-nav__item.active,
.shop-nav__item:hover {
  background: #111827;
  color: #fff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.16);
}

.shop-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.icon-action,
.account-action {
  height: 42px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #111827;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.icon-action {
  position: relative;
  display: grid;
  width: 42px;
  place-items: center;
  color: #374151;
  font-size: 20px;
}

.cart-action em {
  position: absolute;
  top: -8px;
  right: -8px;
  display: grid;
  min-width: 20px;
  height: 20px;
  place-items: center;
  padding: 0 5px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff5a5f, #d92635);
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
}

.account-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 64px;
  padding: 0 15px;
  font-weight: 800;
}

.icon-action:hover,
.account-action:hover {
  border-color: rgba(229, 72, 77, 0.4);
  background: #fff;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.11);
  transform: translateY(-1px);
}

.account-action--user {
  max-width: 176px;
}

.account-action--user > span:not(.account-avatar) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-avatar {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.auth-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 18px;
  padding: 4px;
  border-radius: 8px;
  background: #f3f4f6;
}

.auth-switch button {
  min-height: 38px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
}

.auth-switch button.active {
  background: #111827;
  color: #fff;
}

.auth-dialog :deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

.auth-dialog :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

:global(.auth-dialog.el-dialog) {
  width: min(420px, calc(100vw - 32px));
  max-height: calc(100vh - 48px);
  margin-top: 12vh;
  border-radius: 8px;
  overflow: hidden;
}

:global(.auth-dialog .el-dialog__header) {
  padding: 24px 24px 8px;
  margin-right: 0;
}

:global(.auth-dialog .el-dialog__title) {
  color: #111827;
  font-size: 22px;
  font-weight: 900;
}

:global(.auth-dialog .el-dialog__body) {
  padding: 18px 24px 8px;
}

:global(.auth-dialog .el-dialog__footer) {
  padding: 12px 24px 24px;
}

@media (max-width: 760px) {
  .shop-header__inner {
    grid-template-columns: 1fr auto;
    height: auto;
    gap: 14px;
    padding: 14px 16px;
  }

  .shop-nav {
    grid-column: 1 / -1;
    justify-content: flex-start;
    overflow-x: auto;
  }

  .brand {
    min-width: 0;
  }

  .shop-actions {
    gap: 6px;
  }
}
</style>
