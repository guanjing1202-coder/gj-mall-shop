<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  cancelAfterSale,
  getAfterSalePage,
  submitAfterSaleReturn,
  type AfterSale,
  type OrderItem,
} from '@/api/order'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const activeStatus = ref('')
const pageNum = ref(1)
const pageSize = ref(6)
const total = ref(0)
const afterSales = ref<AfterSale[]>([])
const operatingId = ref<string | number>('')
const returnDialogOpen = ref(false)
const returnSubmitting = ref(false)
const returnTarget = ref<AfterSale>()
const returnForm = ref({
  returnCompany: '',
  returnNo: '',
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: '0' },
  { label: '待退货', value: '1' },
  { label: '待退款', value: '2' },
  { label: '已完成', value: '4' },
]

const hasAfterSales = computed(() => afterSales.value.length > 0)

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

function normalizeImage(url?: string, seed = 'service') {
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

function statusClass(status?: number) {
  const code = Number(status)
  if ([0, 1, 2].includes(code)) return 'processing'
  if (code === 4) return 'done'
  if (code === 3 || code === 5) return 'closed'
  return 'neutral'
}

function switchStatus(value: string) {
  activeStatus.value = value
  pageNum.value = 1
  loadAfterSales()
}

async function loadAfterSales() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getAfterSalePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeStatus.value === '' ? undefined : Number(activeStatus.value),
    })
    afterSales.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function cancelCurrentAfterSale(item: AfterSale) {
  try {
    await ElMessageBox.confirm('确认取消这个售后申请？取消后订单会恢复到申请前状态。', '取消售后', {
      confirmButtonText: '取消售后',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  operatingId.value = item.id
  try {
    await cancelAfterSale(item.id)
    ElMessage.success('售后申请已取消')
    await loadAfterSales()
  } finally {
    operatingId.value = ''
  }
}

function openReturnDialog(item: AfterSale) {
  returnTarget.value = item
  returnForm.value = {
    returnCompany: item.returnCompany || '',
    returnNo: item.returnNo || '',
  }
  returnDialogOpen.value = true
}

async function submitReturnInfo() {
  if (!returnTarget.value) {
    return
  }
  if (!returnForm.value.returnCompany.trim() || !returnForm.value.returnNo.trim()) {
    ElMessage.warning('请填写退货物流公司和单号')
    return
  }
  returnSubmitting.value = true
  try {
    await submitAfterSaleReturn(returnTarget.value.id, {
      returnCompany: returnForm.value.returnCompany.trim(),
      returnNo: returnForm.value.returnNo.trim(),
    })
    ElMessage.success('退货物流已提交，等待商家确认收货')
    returnDialogOpen.value = false
    await loadAfterSales()
  } finally {
    returnSubmitting.value = false
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadAfterSales()
    }
  },
)

onMounted(loadAfterSales)
</script>

<template>
  <div class="service-page">
    <ShopHeader />

    <main class="service-shell">
      <section class="service-hero">
        <div>
          <span>Service</span>
          <h1>我的售后</h1>
          <p>跟踪退款、退货和审核进度，待退货时提交物流单号。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/orders')">返回订单</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看售后</h2>
        <p>售后记录属于会员账户资产，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="service-panel">
        <div class="status-tabs" aria-label="售后状态筛选">
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

        <div v-loading="loading" class="service-list">
          <article v-for="item in afterSales" :key="item.id" class="service-card">
            <div class="service-card__top">
              <div>
                <strong>{{ item.afterSaleNo }}</strong>
                <span>{{ item.typeDesc || '售后申请' }} · {{ formatTime(item.createTime) }}</span>
              </div>
              <em :class="statusClass(item.status)">{{ item.statusDesc || '-' }}</em>
            </div>

            <div class="service-card__body">
              <div class="thumb-stack">
                <img
                  v-for="goods in (item.items || []).slice(0, 3)"
                  :key="goods.id"
                  :src="normalizeImage(goods.skuImage, String(goods.skuId))"
                  :alt="goods.skuName"
                  @error="handleImageError($event, String(goods.skuId))"
                />
              </div>
              <div class="service-copy">
                <strong>{{ (item.items || [])[0]?.skuName || item.orderNo }}</strong>
                <span v-if="(item.items || [])[0]">{{ specText((item.items || [])[0]) }}</span>
                <small>{{ item.reason || '售后申请' }}</small>
              </div>
              <div class="service-amount">
                <span>退款金额</span>
                <strong>{{ formatPrice(item.amount) }}</strong>
              </div>
            </div>

            <div class="service-card__meta">
              <span v-if="item.auditRemark">审核备注：{{ item.auditRemark }}</span>
              <span v-if="item.rejectReason">拒绝原因：{{ item.rejectReason }}</span>
              <span v-if="item.returnCompany || item.returnNo">退货物流：{{ item.returnCompany || '-' }} {{ item.returnNo || '' }}</span>
            </div>

            <div class="service-card__actions">
              <el-button @click="router.push(`/order/${item.orderId}`)">查看订单</el-button>
              <el-button v-if="Number(item.status) === 1" type="primary" @click="openReturnDialog(item)">
                填写退货物流
              </el-button>
              <el-button
                v-if="[0, 1, 2].includes(Number(item.status))"
                :loading="String(operatingId) === String(item.id)"
                @click="cancelCurrentAfterSale(item)"
              >
                取消申请
              </el-button>
            </div>
          </article>

          <el-empty v-if="!loading && !hasAfterSales" description="暂无售后记录">
            <el-button type="primary" @click="router.push('/orders')">查看订单</el-button>
          </el-empty>
        </div>

        <div v-if="total > pageSize" class="pagination-row">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadAfterSales"
          />
        </div>
      </section>
    </main>

    <el-dialog v-model="returnDialogOpen" title="填写退货物流" width="480px" class="service-dialog" append-to-body>
      <div v-if="returnTarget" class="return-summary">
        <strong>{{ returnTarget.afterSaleNo }}</strong>
        <span>提交后售后单将进入待退款状态。</span>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="物流公司">
          <el-input v-model="returnForm.returnCompany" placeholder="例如：顺丰速运" />
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="returnForm.returnNo" placeholder="填写退回包裹的物流单号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="returnDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="returnSubmitting" @click="submitReturnInfo">提交物流</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.service-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.service-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.service-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.service-hero span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.service-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.service-hero p {
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
.service-panel {
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

.service-panel {
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

.service-list {
  min-height: 360px;
}

.service-card {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.service-card + .service-card {
  margin-top: 14px;
}

.service-card__top,
.service-card__body,
.service-card__actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.service-card__top {
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.service-card__top strong,
.service-card__top span {
  display: block;
}

.service-card__top span {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.service-card__top em {
  min-width: 72px;
  padding: 7px 12px;
  border-radius: 8px;
  font-style: normal;
  font-weight: 900;
  text-align: center;
}

.service-card__top em.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.service-card__top em.done {
  background: #eef8f2;
  color: #2f8f67;
}

.service-card__top em.closed {
  background: #f3f4f6;
  color: #6b7280;
}

.service-card__body {
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

.service-copy {
  min-width: 0;
  flex: 1;
}

.service-copy strong,
.service-copy span,
.service-copy small {
  display: block;
}

.service-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-copy span,
.service-copy small {
  margin-top: 7px;
  color: #6b7280;
  font-size: 13px;
}

.service-amount {
  min-width: 120px;
  text-align: right;
}

.service-amount span,
.service-amount strong {
  display: block;
}

.service-amount span {
  color: #6b7280;
  font-size: 13px;
}

.service-amount strong {
  margin-top: 6px;
  color: #e5484d;
  font-size: 20px;
}

.service-card__meta {
  display: grid;
  gap: 8px;
  padding: 0 18px 16px;
  color: #4b5563;
  font-size: 13px;
}

.service-card__actions {
  justify-content: flex-end;
  padding: 14px 18px 18px;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.service-card__actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.service-card__actions :deep(.el-button--primary),
.login-needed :deep(.el-button--primary),
.service-list :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 22px;
}

.return-summary {
  display: grid;
  gap: 7px;
  margin-bottom: 18px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f8f5;
}

.return-summary span {
  color: #6b7280;
  font-size: 13px;
}

:global(.service-dialog.el-dialog) {
  width: min(480px, calc(100vw - 32px));
  border-radius: 14px;
  overflow: hidden;
}

:global(.service-dialog .el-dialog__title) {
  font-weight: 900;
}

:global(.service-dialog .el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

@media (max-width: 760px) {
  .service-shell {
    padding: 28px 16px 52px;
  }

  .service-hero,
  .service-card__body,
  .service-card__top {
    align-items: start;
    flex-direction: column;
  }

  .service-amount {
    text-align: left;
  }

  .service-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .service-card__actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
