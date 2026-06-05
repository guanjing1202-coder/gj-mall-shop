<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  cancelAfterSale,
  getAfterSaleDetail,
  submitAfterSaleReturn,
  type AfterSale,
  type OrderItem,
} from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import {
  canCancelAfterSale,
  canSubmitReturnLogistics,
  refundRecordNotice,
  refundRecordMeta,
  refundRecordTitle,
  statusBadgeClass,
} from '@/utils/after-sale-ui'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const actionLoading = ref(false)
const returnDialogOpen = ref(false)
const returnSubmitting = ref(false)
const afterSale = ref<AfterSale>()
const returnForm = reactive({
  returnCompany: '',
  returnNo: '',
})

onMounted(loadDetail)

async function loadDetail() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getAfterSaleDetail(String(route.params.id))
    afterSale.value = res.data
  } finally {
    loading.value = false
  }
}

function openReturnDialog() {
  if (!afterSale.value) return
  returnForm.returnCompany = afterSale.value.returnCompany || ''
  returnForm.returnNo = afterSale.value.returnNo || ''
  returnDialogOpen.value = true
}

async function submitReturnInfo() {
  if (!afterSale.value) return
  if (!returnForm.returnCompany.trim() || !returnForm.returnNo.trim()) {
    ElMessage.warning('请填写退货物流公司和单号')
    return
  }
  returnSubmitting.value = true
  try {
    await submitAfterSaleReturn(afterSale.value.id, {
      returnCompany: returnForm.returnCompany.trim(),
      returnNo: returnForm.returnNo.trim(),
    })
    ElMessage.success('退货物流已提交')
    returnDialogOpen.value = false
    await loadDetail()
  } finally {
    returnSubmitting.value = false
  }
}

async function cancelCurrentAfterSale() {
  if (!afterSale.value) return
  try {
    await ElMessageBox.confirm('确认取消这个售后申请？取消后订单会恢复到申请前状态。', '取消售后', {
      confirmButtonText: '取消售后',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await cancelAfterSale(afterSale.value.id)
    ElMessage.success('售后申请已取消')
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatTime(value?: string) {
  if (!value) return '-'
  const parsed = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) return value
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
</script>

<template>
  <div class="service-detail-page">
    <ShopHeader />

    <main class="service-detail-shell" v-loading="loading">
      <section class="detail-hero">
        <div>
          <span>After Sale</span>
          <h1>{{ afterSale?.afterSaleNo || '售后详情' }}</h1>
          <p>{{ afterSale?.typeDesc || '售后申请' }} · {{ formatTime(afterSale?.createTime) }}</p>
        </div>
        <div class="hero-actions">
          <button type="button" @click="router.push('/after-sales')">返回列表</button>
          <button v-if="afterSale" type="button" @click="router.push(`/order/${afterSale.orderId}`)">查看订单</button>
        </div>
      </section>

      <section v-if="afterSale" class="detail-grid">
        <article class="summary-panel">
          <div class="summary-top">
            <div>
              <span>售后状态</span>
              <strong>{{ afterSale.statusDesc || '-' }}</strong>
            </div>
            <em :class="statusBadgeClass(afterSale.status)">{{ afterSale.statusDesc || '-' }}</em>
          </div>
          <div class="summary-amount">
            <span>退款金额</span>
            <strong>{{ formatPrice(afterSale.amount) }}</strong>
          </div>
          <div class="summary-actions">
            <el-button v-if="canSubmitReturnLogistics(afterSale.status)" type="primary" @click="openReturnDialog">
              填写退货物流
            </el-button>
            <el-button
              v-if="canCancelAfterSale(afterSale.status)"
              :loading="actionLoading"
              @click="cancelCurrentAfterSale"
            >
              取消申请
            </el-button>
          </div>
        </article>

        <article class="timeline-panel">
          <h2>处理进度</h2>
          <div class="timeline">
            <div
              v-for="row in afterSale.timeline || []"
              :key="row.title"
              class="timeline-row"
              :class="[{ active: row.active }, row.tone ? `tone-${row.tone}` : '']"
            >
              <i />
              <div>
                <strong>{{ row.title }}</strong>
                <span>{{ row.description }}</span>
                <small>{{ formatTime(row.time) }}</small>
              </div>
            </div>
          </div>
        </article>

        <article class="info-panel">
          <h2>售后信息</h2>
          <dl>
            <div><dt>订单号</dt><dd>{{ afterSale.orderNo }}</dd></div>
            <div><dt>申请原因</dt><dd>{{ afterSale.reason || '-' }}</dd></div>
            <div><dt>问题描述</dt><dd>{{ afterSale.description || '-' }}</dd></div>
            <div><dt>审核备注</dt><dd>{{ afterSale.auditRemark || '-' }}</dd></div>
            <div><dt>拒绝原因</dt><dd>{{ afterSale.rejectReason || '-' }}</dd></div>
            <div><dt>退货物流</dt><dd>{{ [afterSale.returnCompany, afterSale.returnNo].filter(Boolean).join(' / ') || '-' }}</dd></div>
          </dl>
        </article>

        <article class="refund-panel">
          <h2>退款记录</h2>
          <div v-if="afterSale.refundRecord" class="refund-card">
            <strong>{{ refundRecordTitle(afterSale.refundRecord) }}</strong>
            <span>{{ refundRecordMeta(afterSale.refundRecord) }}</span>
            <span v-if="refundRecordNotice(afterSale.refundRecord)">{{ refundRecordNotice(afterSale.refundRecord) }}</span>
            <small>支付流水：{{ afterSale.refundRecord.payNo || '-' }}</small>
          </div>
          <el-empty v-else description="暂无退款记录" />
        </article>

        <article class="goods-panel">
          <h2>售后商品</h2>
          <div class="goods-list">
            <div v-for="goods in afterSale.items || []" :key="goods.id" class="goods-row">
              <img :src="normalizeImage(goods.skuImage, String(goods.skuId))" :alt="goods.skuName" />
              <div>
                <strong>{{ goods.skuName }}</strong>
                <span>{{ specText(goods) }}</span>
              </div>
              <em>x{{ goods.quantity }}</em>
            </div>
          </div>
        </article>

        <article v-if="afterSale.images?.length" class="proof-panel">
          <h2>售后凭证</h2>
          <div class="proof-list">
            <a v-for="(image, index) in afterSale.images" :key="image" :href="image" target="_blank">
              <img :src="normalizeImage(image, `after-sale-proof-${index}`)" alt="售后凭证" />
            </a>
          </div>
        </article>
      </section>
    </main>

    <el-dialog v-model="returnDialogOpen" title="填写退货物流" width="480px" class="service-dialog" append-to-body>
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
.service-detail-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.service-detail-shell {
  max-width: 1180px;
  min-height: 560px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.detail-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.detail-hero span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.detail-hero h1 {
  margin: 10px 0 0;
  font-size: 36px;
  line-height: 1.15;
}

.detail-hero p {
  margin: 10px 0 0;
  color: #6b7280;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.hero-actions button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font: inherit;
  font-weight: 900;
}

.detail-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
}

.summary-panel,
.timeline-panel,
.info-panel,
.refund-panel,
.goods-panel,
.proof-panel {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
  padding: 18px;
}

.timeline-panel,
.info-panel,
.refund-panel,
.goods-panel,
.proof-panel {
  grid-column: 2;
}

.summary-panel {
  grid-row: span 2;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.summary-top span,
.summary-amount span {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.summary-top strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.summary-top em {
  align-self: flex-start;
  padding: 7px 12px;
  border-radius: 8px;
  font-style: normal;
  font-weight: 900;
}

.summary-top em.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.summary-top em.done {
  background: #eef8f2;
  color: #2f8f67;
}

.summary-top em.closed,
.summary-top em.neutral {
  background: #f3f4f6;
  color: #6b7280;
}

.summary-amount {
  margin-top: 26px;
}

.summary-amount strong {
  display: block;
  margin-top: 8px;
  color: #e5484d;
  font-size: 30px;
}

.summary-actions {
  display: grid;
  gap: 10px;
  margin-top: 28px;
}

.summary-actions :deep(.el-button) {
  width: 100%;
  margin-left: 0;
  border-radius: 8px;
  font-weight: 900;
}

.summary-actions :deep(.el-button--primary),
:global(.service-dialog .el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

.timeline {
  display: grid;
  gap: 0;
}

.timeline-row {
  position: relative;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
  padding-bottom: 18px;
}

.timeline-row::before {
  position: absolute;
  top: 18px;
  bottom: 0;
  left: 8px;
  width: 2px;
  background: #e5e7eb;
  content: '';
}

.timeline-row:last-child {
  padding-bottom: 0;
}

.timeline-row:last-child::before {
  display: none;
}

.timeline-row i {
  width: 18px;
  height: 18px;
  border: 4px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  box-sizing: border-box;
}

.timeline-row.active i {
  border-color: #2f8f67;
}

.timeline-row.tone-danger.active i {
  border-color: #e5484d;
}

.timeline-row.tone-warning.active i {
  border-color: #f59e0b;
}

.timeline-row strong,
.timeline-row span,
.timeline-row small {
  display: block;
}

.timeline-row span,
.timeline-row small {
  margin-top: 5px;
  color: #6b7280;
  font-size: 13px;
}

.info-panel dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin: 0;
}

.info-panel dl > div {
  min-width: 0;
}

.info-panel dt,
.info-panel dd {
  margin: 0;
}

.info-panel dt {
  color: #6b7280;
  font-size: 13px;
}

.info-panel dd {
  margin-top: 6px;
  word-break: break-word;
}

.refund-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f8f5;
}

.refund-card span,
.refund-card small {
  color: #6b7280;
  font-size: 13px;
}

.goods-list {
  display: grid;
  gap: 12px;
}

.goods-row {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.goods-row img {
  width: 68px;
  height: 68px;
  border-radius: 8px;
  object-fit: cover;
  background: #f3f4f6;
}

.goods-row strong,
.goods-row span {
  display: block;
}

.goods-row span {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.goods-row em {
  color: #6b7280;
  font-style: normal;
  font-weight: 900;
}

.proof-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.proof-list img {
  width: 112px;
  height: 112px;
  border-radius: 8px;
  object-fit: cover;
  background: #f3f4f6;
}

@media (max-width: 820px) {
  .service-detail-shell {
    padding: 28px 16px 52px;
  }

  .detail-hero,
  .hero-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .timeline-panel,
  .info-panel,
  .refund-panel,
  .goods-panel,
  .proof-panel {
    grid-column: 1;
  }

  .summary-panel {
    grid-row: auto;
  }

  .info-panel dl {
    grid-template-columns: 1fr;
  }
}
</style>
