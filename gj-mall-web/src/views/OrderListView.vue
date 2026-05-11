<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { cancelOrder, getOrderPage, receiveOrder, type OrderDetail, type OrderItem } from '@/api/order'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const activeStatus = ref('')
const pageNum = ref(1)
const pageSize = ref(6)
const total = ref(0)
const orders = ref<OrderDetail[]>([])
const operatingId = ref<string | number>('')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待付款', value: '0' },
  { label: '待发货', value: '1' },
  { label: '待收货', value: '2' },
  { label: '已完成', value: '3' },
  { label: '已取消', value: '4' },
]

const hasOrders = computed(() => orders.value.length > 0)

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatTime(value?: string) {
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
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'orders') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/280/280`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/280/280`
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

function itemCount(order: OrderDetail) {
  return (order.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
}

function statusClass(status?: number) {
  const code = Number(status)
  if (code === 0) return 'pending'
  if (code === 1 || code === 2) return 'processing'
  if (code === 3) return 'done'
  if (code === 4 || code === 6) return 'closed'
  return 'neutral'
}

function switchStatus(value: string) {
  activeStatus.value = value
  pageNum.value = 1
  loadOrders()
}

async function loadOrders() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getOrderPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeStatus.value === '' ? undefined : Number(activeStatus.value),
    })
    orders.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function cancelCurrentOrder(order: OrderDetail) {
  try {
    await ElMessageBox.confirm('确认取消这个订单？库存会自动释放。', '取消订单', {
      confirmButtonText: '取消订单',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  operatingId.value = order.id
  try {
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } finally {
    operatingId.value = ''
  }
}

async function receiveCurrentOrder(order: OrderDetail) {
  try {
    await ElMessageBox.confirm('确认已经收到商品？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '返回',
      type: 'info',
    })
  } catch {
    return
  }
  operatingId.value = order.id
  try {
    await receiveOrder(order.id)
    ElMessage.success('已确认收货')
    await loadOrders()
  } finally {
    operatingId.value = ''
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadOrders()
    }
  },
)

onMounted(loadOrders)
</script>

<template>
  <div class="orders-page">
    <ShopHeader />

    <main class="orders-shell">
      <section class="orders-hero">
        <div>
          <span>Orders</span>
          <h1>我的订单</h1>
          <p>查看订单状态、继续支付、确认收货或进入详情处理后续流程。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/')">继续购物</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看订单</h2>
        <p>订单数据绑定到会员账户，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="orders-panel">
        <div class="status-tabs" aria-label="订单状态筛选">
          <button
            v-for="item in statusOptions"
            :key="item.value || 'all'"
            type="button"
            :class="{ active: activeStatus === item.value }"
            @click="switchStatus(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div v-loading="loading" class="order-list">
          <article v-for="order in orders" :key="order.id" class="order-card">
            <div class="order-card__top">
              <div>
                <strong>{{ order.orderNo }}</strong>
                <span>{{ formatTime(order.createTime) }}</span>
              </div>
              <em :class="statusClass(order.status)">{{ order.statusDesc || '-' }}</em>
            </div>

            <div class="order-card__body">
              <div class="thumb-stack">
                <img
                  v-for="item in (order.items || []).slice(0, 3)"
                  :key="item.id"
                  :src="normalizeImage(item.skuImage, String(item.skuId))"
                  :alt="item.skuName"
                  @error="handleImageError($event, String(item.skuId))"
                />
              </div>
              <div class="order-copy">
                <strong>{{ (order.items || [])[0]?.skuName || '订单商品' }}</strong>
                <span v-if="(order.items || [])[0]">{{ specText((order.items || [])[0]) }}</span>
                <small>共 {{ itemCount(order) }} 件商品</small>
              </div>
              <div class="order-amount">
                <span>实付</span>
                <strong>{{ formatPrice(order.payAmount) }}</strong>
              </div>
            </div>

            <div class="order-card__actions">
              <el-button @click="router.push(`/order/${order.id}`)">查看详情</el-button>
              <el-button v-if="Number(order.status) === 0" type="primary" @click="router.push(`/order/${order.id}?pay=1`)">
                去支付
              </el-button>
              <el-button
                v-if="Number(order.status) === 0"
                :loading="String(operatingId) === String(order.id)"
                @click="cancelCurrentOrder(order)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="Number(order.status) === 2"
                type="primary"
                :loading="String(operatingId) === String(order.id)"
                @click="receiveCurrentOrder(order)"
              >
                确认收货
              </el-button>
            </div>
          </article>

          <el-empty v-if="!loading && !hasOrders" description="暂无订单">
            <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
          </el-empty>
        </div>

        <div v-if="total > pageSize" class="pagination-row">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadOrders"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.orders-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.orders-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.orders-hero span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.orders-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.orders-hero p {
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
.orders-panel {
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

.orders-panel {
  padding: 20px;
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
  padding: 4px;
  border-radius: 8px;
  background: #f3f4f6;
}

.status-tabs button {
  min-height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  font: inherit;
  font-weight: 900;
}

.status-tabs button.active,
.status-tabs button:hover {
  background: #111827;
  color: #fff;
}

.order-list {
  min-height: 360px;
}

.order-card {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.order-card + .order-card {
  margin-top: 14px;
}

.order-card__top,
.order-card__body,
.order-card__actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.order-card__top {
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.order-card__top strong,
.order-card__top span {
  display: block;
}

.order-card__top span {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.order-card__top em {
  min-width: 72px;
  padding: 7px 12px;
  border-radius: 8px;
  font-style: normal;
  font-weight: 900;
  text-align: center;
}

.order-card__top em.pending {
  background: #fff4dc;
  color: #b7791f;
}

.order-card__top em.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.order-card__top em.done {
  background: #eef8f2;
  color: #2f8f67;
}

.order-card__top em.closed {
  background: #f3f4f6;
  color: #6b7280;
}

.order-card__body {
  padding: 18px;
}

.thumb-stack {
  display: flex;
  min-width: 98px;
}

.thumb-stack img {
  width: 72px;
  height: 72px;
  border: 3px solid #fff;
  border-radius: 8px;
  object-fit: cover;
  background: #f3f4f6;
}

.thumb-stack img + img {
  margin-left: -34px;
}

.order-copy {
  min-width: 0;
  flex: 1;
}

.order-copy strong,
.order-copy span,
.order-copy small {
  display: block;
}

.order-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-copy span,
.order-copy small {
  margin-top: 7px;
  color: #6b7280;
  font-size: 13px;
}

.order-amount {
  min-width: 120px;
  text-align: right;
}

.order-amount span,
.order-amount strong {
  display: block;
}

.order-amount span {
  color: #6b7280;
  font-size: 13px;
}

.order-amount strong {
  margin-top: 6px;
  color: #e5484d;
  font-size: 20px;
}

.order-card__actions {
  justify-content: flex-end;
  padding: 14px 18px 18px;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.order-card__actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.order-card__actions :deep(.el-button--primary),
.login-needed :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 22px;
}

@media (max-width: 760px) {
  .orders-shell {
    padding: 28px 16px 52px;
  }

  .orders-hero,
  .order-card__body {
    align-items: start;
    flex-direction: column;
  }

  .order-card__top {
    align-items: start;
    flex-direction: column;
  }

  .order-amount {
    text-align: left;
  }

  .order-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .order-card__actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
