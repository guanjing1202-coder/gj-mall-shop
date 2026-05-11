<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { cancelOrder, getOrderDetail, receiveOrder, type OrderDetail, type OrderItem } from '@/api/order'
import { createPay, type PayChannel } from '@/api/pay'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const loading = ref(false)
const actionLoading = ref<'pay' | 'cancel' | 'receive' | ''>('')
const order = ref<OrderDetail>()
const payChannel = ref<PayChannel>('mock')

const canPay = computed(() => Number(order.value?.status) === 0)
const canCancel = computed(() => Number(order.value?.status) === 0)
const canReceive = computed(() => Number(order.value?.status) === 2)
const activeStep = computed(() => {
  const status = Number(order.value?.status)
  if (status === 0) return 0
  if (status === 1) return 1
  if (status === 2) return 2
  if (status === 3) return 3
  return 0
})

function statusClass(status?: number) {
  const code = Number(status)
  if (code === 0) return 'pending'
  if (code === 1 || code === 2) return 'processing'
  if (code === 3) return 'done'
  if (code === 4 || code === 6) return 'closed'
  return 'neutral'
}

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

function normalizeImage(url?: string, seed = 'order') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

function receiverLine(detail?: OrderDetail) {
  const receiver = detail?.receiver
  if (!receiver) {
    return '-'
  }
  return `${receiver.province || ''}${receiver.city || ''}${receiver.district || ''}${receiver.detail || ''}`
}

async function loadOrder() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id as string)
    order.value = res.data
    await cart.fetchCart()
  } finally {
    loading.value = false
  }
}

async function payOrder() {
  if (!order.value) {
    return
  }
  actionLoading.value = 'pay'
  try {
    const res = await createPay({ orderId: order.value.id, channel: payChannel.value })
    ElMessage.success(res.data.paid ? '支付成功' : '支付已创建')
    await loadOrder()
  } finally {
    actionLoading.value = ''
  }
}

async function cancelCurrentOrder() {
  if (!order.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认取消这个订单？库存会自动释放。', '取消订单', {
      confirmButtonText: '取消订单',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  actionLoading.value = 'cancel'
  try {
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await loadOrder()
  } finally {
    actionLoading.value = ''
  }
}

async function receiveCurrentOrder() {
  if (!order.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认已经收到商品？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '返回',
      type: 'info',
    })
  } catch {
    return
  }
  actionLoading.value = 'receive'
  try {
    await receiveOrder(order.value.id)
    ElMessage.success('已确认收货')
    await loadOrder()
  } finally {
    actionLoading.value = ''
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadOrder()
    }
  },
)

onMounted(loadOrder)
</script>

<template>
  <div class="order-detail-page">
    <ShopHeader />

    <main class="order-shell">
      <section class="order-hero">
        <div>
          <span>Order</span>
          <h1>订单详情</h1>
          <p v-if="order">订单号 {{ order.orderNo }}</p>
          <p v-else>查看订单支付、物流和商品明细。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/orders')">返回订单列表</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看订单</h2>
        <p>订单属于会员账户资产，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else v-loading="loading" class="detail-layout">
        <div v-if="order" class="detail-main">
          <section class="detail-block status-block">
            <div class="status-heading">
              <div>
                <span>Current Status</span>
                <h2>{{ order.statusDesc || '订单状态' }}</h2>
              </div>
              <em :class="statusClass(order.status)">{{ order.statusDesc || '-' }}</em>
            </div>
            <el-steps :active="activeStep" finish-status="success" align-center>
              <el-step title="提交订单" :description="formatTime(order.createTime)" />
              <el-step title="完成支付" :description="formatTime(order.payTime)" />
              <el-step title="商家发货" :description="formatTime(order.deliveryTime)" />
              <el-step title="交易完成" :description="formatTime(order.receiveTime)" />
            </el-steps>
          </section>

          <section class="detail-block">
            <div class="block-heading">
              <div>
                <span>Items</span>
                <h2>商品明细</h2>
              </div>
              <strong>{{ order.items?.length || 0 }} 件</strong>
            </div>

            <article v-for="item in order.items || []" :key="item.id" class="order-item">
              <img
                :src="normalizeImage(item.skuImage, String(item.skuId))"
                :alt="item.skuName"
                @error="handleImageError($event, String(item.skuId))"
              />
              <div class="item-copy">
                <strong>{{ item.skuName || '商品' }}</strong>
                <span>{{ specText(item) }}</span>
              </div>
              <div class="item-qty">x{{ item.quantity }}</div>
              <div class="item-price">{{ formatPrice(item.totalAmount) }}</div>
            </article>
          </section>

          <section class="info-grid">
            <article class="detail-block">
              <span>Receiver</span>
              <h2>收货信息</h2>
              <dl>
                <div>
                  <dt>收件人</dt>
                  <dd>{{ order.receiver?.receiver || '-' }}</dd>
                </div>
                <div>
                  <dt>手机号</dt>
                  <dd>{{ order.receiver?.phone || '-' }}</dd>
                </div>
                <div>
                  <dt>地址</dt>
                  <dd>{{ receiverLine(order) }}</dd>
                </div>
              </dl>
            </article>

            <article class="detail-block">
              <span>Delivery</span>
              <h2>物流信息</h2>
              <dl>
                <div>
                  <dt>物流公司</dt>
                  <dd>{{ order.deliveryCompany || '-' }}</dd>
                </div>
                <div>
                  <dt>物流单号</dt>
                  <dd>{{ order.deliveryNo || '-' }}</dd>
                </div>
                <div>
                  <dt>备注</dt>
                  <dd>{{ order.deliveryRemark || order.remark || '-' }}</dd>
                </div>
              </dl>
            </article>
          </section>
        </div>

        <aside v-if="order" class="pay-panel">
          <span>Payment</span>
          <h2>{{ formatPrice(order.payAmount) }}</h2>
          <dl>
            <div>
              <dt>商品金额</dt>
              <dd>{{ formatPrice(order.totalAmount) }}</dd>
            </div>
            <div>
              <dt>运费</dt>
              <dd>{{ formatPrice(order.freightAmount) }}</dd>
            </div>
            <div>
              <dt>优惠</dt>
              <dd>- {{ formatPrice(order.couponAmount) }}</dd>
            </div>
            <div>
              <dt>下单时间</dt>
              <dd>{{ formatTime(order.createTime) }}</dd>
            </div>
          </dl>

          <div v-if="canPay" class="channel-box">
            <strong>支付方式</strong>
            <el-radio-group v-model="payChannel">
              <el-radio-button label="mock">模拟支付</el-radio-button>
            </el-radio-group>
          </div>

          <div class="action-stack">
            <el-button
              v-if="canPay"
              size="large"
              type="primary"
              :loading="actionLoading === 'pay'"
              @click="payOrder"
            >
              立即支付
            </el-button>
            <el-button
              v-if="canCancel"
              size="large"
              :loading="actionLoading === 'cancel'"
              @click="cancelCurrentOrder"
            >
              取消订单
            </el-button>
            <el-button
              v-if="canReceive"
              size="large"
              type="primary"
              :loading="actionLoading === 'receive'"
              @click="receiveCurrentOrder"
            >
              确认收货
            </el-button>
            <el-button size="large" @click="router.push('/')">继续逛逛</el-button>
          </div>
        </aside>

        <el-empty v-if="!loading && !order" description="订单不存在">
          <el-button type="primary" @click="router.push('/orders')">返回订单列表</el-button>
        </el-empty>
      </section>
    </main>
  </div>
</template>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.order-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.order-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.order-hero span,
.detail-block > span,
.block-heading span,
.status-heading span,
.pay-panel > span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.order-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.order-hero p {
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
.detail-block,
.pay-panel {
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

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
  min-height: 420px;
}

.detail-main {
  display: grid;
  gap: 18px;
}

.detail-block {
  padding: 24px;
}

.status-heading,
.block-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.status-heading h2,
.block-heading h2,
.detail-block h2 {
  margin: 9px 0 0;
  font-size: 24px;
}

.status-heading em {
  min-width: 72px;
  padding: 7px 12px;
  border-radius: 8px;
  font-style: normal;
  font-weight: 900;
  text-align: center;
}

.status-heading em.pending {
  background: #fff4dc;
  color: #b7791f;
}

.status-heading em.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.status-heading em.done {
  background: #eef8f2;
  color: #2f8f67;
}

.status-heading em.closed {
  background: #f3f4f6;
  color: #6b7280;
}

.status-block :deep(.el-step__title) {
  font-weight: 900;
}

.order-item {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) 50px 116px;
  gap: 14px;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.order-item img {
  width: 78px;
  height: 78px;
  border-radius: 8px;
  object-fit: cover;
  background: #f3f4f6;
}

.item-copy {
  min-width: 0;
}

.item-copy strong,
.item-copy span {
  display: block;
}

.item-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-copy span {
  margin-top: 7px;
  color: #6b7280;
  font-size: 13px;
}

.item-qty {
  color: #6b7280;
  font-weight: 900;
}

.item-price {
  color: #e5484d;
  font-weight: 900;
  text-align: right;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.detail-block dl,
.detail-block dd,
.pay-panel dl,
.pay-panel dd {
  margin: 0;
}

.detail-block dl {
  margin-top: 18px;
}

.detail-block dl > div,
.pay-panel dl > div {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.detail-block dt,
.pay-panel dt {
  color: #6b7280;
}

.detail-block dd,
.pay-panel dd {
  max-width: 68%;
  color: #111827;
  font-weight: 900;
  text-align: right;
}

.pay-panel {
  position: sticky;
  top: 96px;
  padding: 24px;
}

.pay-panel h2 {
  margin: 10px 0 20px;
  color: #e5484d;
  font-size: 30px;
}

.channel-box {
  margin-top: 18px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f8f5;
}

.channel-box strong {
  display: block;
  margin-bottom: 12px;
}

.action-stack {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.action-stack :deep(.el-button) {
  width: 100%;
  height: 46px;
  margin-left: 0;
  border-radius: 8px;
  font-weight: 900;
}

.action-stack :deep(.el-button--primary),
.login-needed :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

@media (max-width: 1040px) {
  .detail-layout,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .pay-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .order-shell {
    padding: 28px 16px 52px;
  }

  .order-hero,
  .status-heading,
  .block-heading {
    align-items: start;
    flex-direction: column;
  }

  .order-item {
    grid-template-columns: 70px minmax(0, 1fr);
  }

  .order-item img {
    width: 70px;
    height: 70px;
  }

  .item-qty,
  .item-price {
    grid-column: 2;
    text-align: left;
  }

  .detail-block dd,
  .pay-panel dd {
    max-width: 60%;
  }
}
</style>
