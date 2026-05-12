<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  getCouponCenter,
  getMyCoupons,
  receiveCoupon,
  type CouponCenterItem,
  type MyCoupon,
} from '@/api/coupon'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const receivingId = ref<string | number | ''>('')
const coupons = ref<CouponCenterItem[]>([])
const myCoupons = ref<MyCoupon[]>([])

const myCouponIds = computed(() => new Set(myCoupons.value.map((item) => String(item.couponId))))
const availableCount = computed(() => coupons.value.filter((item) => !isReceived(item)).length)
const bestCoupon = computed(() => coupons.value[0])

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatCouponValue(coupon: CouponCenterItem) {
  if (coupon.type === 2) {
    const rate = Number(coupon.discountRate ?? 1)
    return `${(rate * 10).toFixed(rate * 10 === Math.trunc(rate * 10) ? 0 : 1)}折`
  }
  return `减 ${formatPrice(coupon.discountAmount)}`
}

function formatCouponLimit(coupon: CouponCenterItem) {
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

function isReceived(coupon: CouponCenterItem) {
  return myCouponIds.value.has(String(coupon.id))
}

async function loadCenter() {
  loading.value = true
  try {
    const [centerRes, myRes] = await Promise.all([
      getCouponCenter(),
      auth.isLoggedIn ? getMyCoupons() : Promise.resolve({ data: [] as MyCoupon[] }),
    ])
    coupons.value = centerRes.data || []
    myCoupons.value = myRes.data || []
  } finally {
    loading.value = false
  }
}

async function handleReceive(coupon: CouponCenterItem) {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  if (isReceived(coupon)) {
    router.push('/coupons')
    return
  }
  receivingId.value = coupon.id
  try {
    await receiveCoupon(coupon.id)
    ElMessage.success('领取成功，结算时可直接使用')
    await loadCenter()
  } finally {
    receivingId.value = ''
  }
}

watch(
  () => auth.isLoggedIn,
  () => {
    loadCenter()
  },
)

onMounted(loadCenter)
</script>

<template>
  <div class="coupon-center-page">
    <ShopHeader />

    <main class="coupon-center-shell">
      <section class="coupon-hero">
        <div>
          <span>Coupon Center</span>
          <h1>领券中心</h1>
          <p>先领券再下单，系统会在确认订单时为你匹配可用优惠。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/coupons')">我的券包</button>
      </section>

      <section v-if="bestCoupon" class="coupon-feature">
        <div class="feature-copy">
          <span>今日推荐</span>
          <h2>{{ bestCoupon.name }}</h2>
          <p>{{ formatCouponLimit(bestCoupon) }} · 有效期至 {{ formatDate(bestCoupon.endTime) }}</p>
        </div>
        <strong>{{ formatCouponValue(bestCoupon) }}</strong>
        <el-button
          size="large"
          type="primary"
          :loading="receivingId === bestCoupon.id"
          :disabled="isReceived(bestCoupon)"
          @click="handleReceive(bestCoupon)"
        >
          {{ isReceived(bestCoupon) ? '已领取' : '立即领取' }}
        </el-button>
      </section>

      <section class="coupon-layout">
        <aside class="coupon-summary">
          <span>Overview</span>
          <h2>可领优惠</h2>
          <dl>
            <div>
              <dt>可领取</dt>
              <dd>{{ availableCount }}</dd>
            </div>
            <div>
              <dt>已领取</dt>
              <dd>{{ myCouponIds.size }}</dd>
            </div>
            <div>
              <dt>活动券</dt>
              <dd>{{ coupons.length }}</dd>
            </div>
          </dl>
          <el-button size="large" @click="router.push('/')">继续逛逛</el-button>
        </aside>

        <section v-loading="loading" class="coupon-grid">
          <article
            v-for="coupon in coupons"
            :key="coupon.id"
            class="coupon-card"
            :class="{ received: isReceived(coupon) }"
          >
            <div class="coupon-value">
              <strong>{{ formatCouponValue(coupon) }}</strong>
              <span>{{ formatCouponLimit(coupon) }}</span>
            </div>
            <div class="coupon-copy">
              <div class="coupon-title">
                <div>
                  <h3>{{ coupon.name }}</h3>
                  <p>{{ coupon.typeDesc || '平台优惠券' }} · 有效期至 {{ formatDate(coupon.endTime) }}</p>
                </div>
                <em>剩余 {{ coupon.remainCount ?? 0 }}</em>
              </div>
              <div class="coupon-actions">
                <small>已领 {{ coupon.receivedCount ?? 0 }} / {{ coupon.totalCount ?? 0 }}</small>
                <el-button
                  type="primary"
                  :loading="receivingId === coupon.id"
                  :disabled="isReceived(coupon)"
                  @click="handleReceive(coupon)"
                >
                  {{ isReceived(coupon) ? '已领取' : '领取' }}
                </el-button>
              </div>
            </div>
          </article>

          <el-empty v-if="!loading && !coupons.length" description="暂无可领取优惠券">
            <el-button type="primary" @click="router.push('/')">去看商品</el-button>
          </el-empty>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.coupon-center-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 5%, rgba(47, 143, 103, 0.12), transparent 26%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.coupon-center-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.coupon-hero,
.coupon-feature,
.coupon-layout {
  display: grid;
  gap: 24px;
}

.coupon-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  margin-bottom: 24px;
}

.coupon-hero span,
.feature-copy span,
.coupon-summary > span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
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

.coupon-feature,
.coupon-summary,
.coupon-card {
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.07);
}

.coupon-feature {
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  margin-bottom: 24px;
  padding: 26px;
  overflow: hidden;
}

.feature-copy h2 {
  margin: 12px 0 0;
  font-size: 28px;
}

.feature-copy p {
  margin: 10px 0 0;
  color: #6b7280;
}

.coupon-feature > strong {
  color: #e5484d;
  font-size: 36px;
  line-height: 1;
  white-space: nowrap;
}

.coupon-feature :deep(.el-button),
.coupon-actions :deep(.el-button),
.coupon-grid :deep(.el-button),
.coupon-summary :deep(.el-button) {
  border-radius: 12px;
  font-weight: 900;
}

.coupon-feature :deep(.el-button--primary),
.coupon-actions :deep(.el-button--primary),
.coupon-grid :deep(.el-button--primary) {
  background: linear-gradient(135deg, #e5484d, #c92432);
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(229, 72, 77, 0.22);
}

.coupon-layout {
  grid-template-columns: 300px minmax(0, 1fr);
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
}

.coupon-grid {
  display: grid;
  gap: 14px;
  min-height: 320px;
}

.coupon-card {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  overflow: hidden;
}

.coupon-card.received {
  opacity: 0.68;
}

.coupon-value {
  display: grid;
  place-content: center;
  gap: 8px;
  min-height: 154px;
  padding: 18px;
  background: linear-gradient(135deg, #e5484d, #111827);
  color: #fff;
  text-align: center;
}

.coupon-card.received .coupon-value {
  background: #6b7280;
}

.coupon-value strong {
  color: #ffd166;
  font-size: 31px;
  line-height: 1;
}

.coupon-value span {
  font-size: 13px;
  font-weight: 900;
}

.coupon-copy {
  display: grid;
  gap: 18px;
  min-width: 0;
  padding: 22px;
}

.coupon-title {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 18px;
}

.coupon-title h3 {
  margin: 0;
  font-size: 21px;
  line-height: 1.35;
}

.coupon-title p {
  margin: 10px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.coupon-title em {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: 999px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.coupon-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.coupon-actions small {
  color: #6b7280;
}

@media (max-width: 980px) {
  .coupon-feature,
  .coupon-layout {
    grid-template-columns: 1fr;
  }

  .coupon-summary {
    position: static;
  }
}

@media (max-width: 720px) {
  .coupon-center-shell {
    padding: 28px 16px 52px;
  }

  .coupon-hero,
  .coupon-card {
    grid-template-columns: 1fr;
  }

  .coupon-hero {
    align-items: start;
  }

  .coupon-feature {
    align-items: start;
  }

  .coupon-value {
    min-height: 118px;
  }
}
</style>
