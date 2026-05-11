<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { getMyCoupons, type MyCoupon } from '@/api/coupon'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const activeStatus = ref<'all' | '0' | '1' | '2'>('all')
const allCoupons = ref<MyCoupon[]>([])

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '未使用', value: '0' },
  { label: '已使用', value: '1' },
  { label: '已过期', value: '2' },
]

const coupons = computed(() => {
  if (activeStatus.value === 'all') {
    return allCoupons.value
  }
  return allCoupons.value.filter((item) => String(item.status) === activeStatus.value)
})
const availableCount = computed(() => allCoupons.value.filter((item) => Number(item.status) === 0).length)
const usedCount = computed(() => allCoupons.value.filter((item) => Number(item.status) === 1).length)
const expiredCount = computed(() => allCoupons.value.filter((item) => Number(item.status) === 2).length)

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatCouponValue(coupon: MyCoupon) {
  if (coupon.type === 2) {
    const rate = Number(coupon.discountRate ?? 1)
    return `${(rate * 10).toFixed(rate * 10 === Math.trunc(rate * 10) ? 0 : 1)}折`
  }
  return `减 ${formatPrice(coupon.discountAmount)}`
}

function formatCouponLimit(coupon: MyCoupon) {
  const minAmount = Number(coupon.minAmount || 0)
  return minAmount > 0 ? `满 ${formatPrice(minAmount)} 可用` : '无门槛'
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(parsed)
}

function statusClass(status?: number) {
  const code = Number(status)
  if (code === 0) return 'available'
  if (code === 1) return 'used'
  if (code === 2) return 'expired'
  return 'unknown'
}

async function loadCoupons() {
  if (!auth.isLoggedIn) {
    allCoupons.value = []
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getMyCoupons()
    allCoupons.value = res.data || []
  } finally {
    loading.value = false
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadCoupons()
    } else {
      allCoupons.value = []
    }
  },
)

onMounted(loadCoupons)
</script>

<template>
  <div class="coupon-page">
    <ShopHeader />

    <main class="coupon-shell">
      <section class="coupon-hero">
        <div>
          <span>Coupons</span>
          <h1>我的优惠券</h1>
          <p>查看已领取优惠券，并在确认订单时选择可用优惠。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/')">继续逛逛</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看优惠券</h2>
        <p>优惠券会绑定到会员账户，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="coupon-layout">
        <aside class="coupon-summary">
          <span>Overview</span>
          <h2>券包概览</h2>
          <dl>
            <div>
              <dt>未使用</dt>
              <dd>{{ availableCount }}</dd>
            </div>
            <div>
              <dt>已使用</dt>
              <dd>{{ usedCount }}</dd>
            </div>
            <div>
              <dt>已过期</dt>
              <dd>{{ expiredCount }}</dd>
            </div>
          </dl>
          <el-button type="primary" size="large" :disabled="availableCount === 0" @click="router.push('/cart')">
            去结算使用
          </el-button>
        </aside>

        <section class="coupon-main">
          <div class="toolbar">
            <div>
              <span>Wallet</span>
              <h2>优惠券列表</h2>
            </div>
            <el-segmented v-model="activeStatus" :options="statusOptions" />
          </div>

          <div v-loading="loading" class="coupon-grid">
            <article
              v-for="coupon in coupons"
              :key="coupon.id"
              class="coupon-card"
              :class="statusClass(coupon.status)"
            >
              <div class="coupon-value">
                <strong>{{ formatCouponValue(coupon) }}</strong>
                <span>{{ formatCouponLimit(coupon) }}</span>
              </div>
              <div class="coupon-copy">
                <div class="coupon-title">
                  <h3>{{ coupon.name }}</h3>
                  <em>{{ coupon.statusDesc || coupon.typeDesc || '优惠券' }}</em>
                </div>
                <p>{{ coupon.typeDesc || '平台优惠券' }} · 有效期至 {{ formatDate(coupon.endTime) }}</p>
                <small>领取时间 {{ formatDate(coupon.createTime) }}</small>
              </div>
            </article>

            <el-empty v-if="!loading && !coupons.length" description="暂无优惠券">
              <el-button type="primary" @click="router.push('/')">去挑选商品</el-button>
            </el-empty>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.coupon-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.coupon-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.coupon-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.coupon-hero span,
.coupon-summary > span,
.toolbar span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.coupon-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.coupon-hero p {
  margin: 12px 0 0;
  color: #6b7280;
}

.back-link {
  border: 0;
  background: transparent;
  color: #e5484d;
  cursor: pointer;
  font: inherit;
  font-weight: 900;
}

.login-needed,
.coupon-summary,
.toolbar,
.coupon-card {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
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

.coupon-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.coupon-summary {
  position: sticky;
  top: 96px;
  padding: 24px;
}

.coupon-summary h2 {
  margin: 10px 0 20px;
  font-size: 26px;
}

.coupon-summary dl,
.coupon-summary dd {
  margin: 0;
}

.coupon-summary dl > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.coupon-summary dt {
  color: #6b7280;
}

.coupon-summary dd {
  font-size: 22px;
  font-weight: 900;
}

.coupon-summary :deep(.el-button) {
  width: 100%;
  height: 46px;
  margin-top: 20px;
  border-radius: 8px;
  font-weight: 900;
}

.coupon-main {
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
}

.toolbar h2 {
  margin: 0;
  font-size: 26px;
}

.toolbar :deep(.el-segmented) {
  border-radius: 8px;
  background: #f3f4f6;
}

.coupon-grid {
  display: grid;
  gap: 14px;
  min-height: 320px;
}

.coupon-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  overflow: hidden;
}

.coupon-card.used,
.coupon-card.expired {
  opacity: 0.62;
}

.coupon-value {
  display: grid;
  place-content: center;
  gap: 8px;
  min-height: 150px;
  padding: 18px;
  background: #111827;
  color: #fff;
  text-align: center;
}

.coupon-card.available .coupon-value {
  background: #e5484d;
}

.coupon-card.used .coupon-value,
.coupon-card.expired .coupon-value {
  background: #6b7280;
}

.coupon-value strong {
  color: #ffd166;
  font-size: 30px;
  line-height: 1;
}

.coupon-value span {
  font-size: 13px;
  font-weight: 900;
}

.coupon-copy {
  min-width: 0;
  padding: 22px;
}

.coupon-title {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.coupon-title h3 {
  margin: 0;
  font-size: 20px;
  line-height: 1.35;
}

.coupon-title em {
  flex: 0 0 auto;
  padding: 4px 9px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.coupon-card.used .coupon-title em,
.coupon-card.expired .coupon-title em {
  background: #f3f4f6;
  color: #6b7280;
}

.coupon-copy p {
  margin: 16px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.coupon-copy small {
  display: block;
  margin-top: 12px;
  color: #6b7280;
}

.coupon-summary :deep(.el-button--primary),
.login-needed :deep(.el-button--primary),
.coupon-grid :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

@media (max-width: 980px) {
  .coupon-layout {
    grid-template-columns: 1fr;
  }

  .coupon-summary {
    position: static;
  }
}

@media (max-width: 720px) {
  .coupon-shell {
    padding: 28px 16px 52px;
  }

  .coupon-hero,
  .toolbar {
    align-items: start;
    flex-direction: column;
  }

  .coupon-card {
    grid-template-columns: 1fr;
  }

  .coupon-value {
    min-height: 118px;
  }
}
</style>
