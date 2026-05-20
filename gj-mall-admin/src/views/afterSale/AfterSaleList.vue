<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import {
  approveAfterSale,
  cancelAfterSale,
  createAfterSale,
  getAfterSaleDetail,
  getAfterSalePage,
  getAfterSaleSummary,
  receiveAfterSale,
  refundAfterSale,
  rejectAfterSale,
  type ApiId,
  type AfterSaleActionPayload,
  type AfterSaleCreatePayload,
  type AfterSaleItem,
  type AfterSaleQuery,
  type AfterSaleRecord,
  type AfterSaleSummary,
} from '@/api/afterSale'

const route = useRoute()
const router = useRouter()

interface CreateFormState {
  orderId?: ApiId
  type: number
  amount?: number
  reason: string
  description: string
  imagesText: string
}

type ActionMode = 'approve' | 'reject' | 'receive' | 'refund' | 'cancel'

const loading = ref(false)
const summaryLoading = ref(false)
const detailLoading = ref(false)
const actionLoading = ref(false)
const creating = ref(false)
const afterSales = ref<AfterSaleRecord[]>([])
const summary = ref<AfterSaleSummary>(createEmptySummary())
const currentAfterSale = ref<AfterSaleRecord>()
const detailOpen = ref(false)
const createOpen = ref(false)
const actionOpen = ref(false)
const actionMode = ref<ActionMode>('approve')
const actionTarget = ref<AfterSaleRecord>()
const createFormRef = ref<FormInstance>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<AfterSaleQuery>({
  keyword: '',
  userId: undefined,
  type: undefined,
  status: undefined,
})

const createForm = reactive<CreateFormState>(createEmptyForm())
const actionForm = reactive({
  auditRemark: '',
  rejectReason: '',
  returnCompany: '',
  returnNo: '',
})

const createRules: any = {
  orderId: [{ required: true, message: '请输入订单ID', trigger: 'change' }],
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
}

const columns: TableColumnsType<AfterSaleRecord> = [
  { title: '售后单', dataIndex: 'afterSaleNo', key: 'afterSaleNo', width: 280 },
  { title: '订单', dataIndex: 'orderNo', key: 'orderNo', width: 210 },
  { title: '会员', dataIndex: 'userId', key: 'user', width: 180 },
  { title: '类型/金额', dataIndex: 'type', key: 'typeAmount', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '订单状态', dataIndex: 'currentOrderStatus', key: 'orderStatus', width: 130 },
  { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 330 },
]

const itemColumns: TableColumnsType<AfterSaleItem> = [
  { title: '商品', dataIndex: 'skuName', key: 'skuName' },
  { title: '单价', dataIndex: 'price', key: 'price', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90 },
  { title: '小计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
]

const typeOptions = [
  { label: '仅退款', value: 1 },
  { label: '退货退款', value: 2 },
]

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '待退货', value: 1 },
  { label: '待退款', value: 2 },
  { label: '已拒绝', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 },
]

onMounted(() => {
  applyRouteFilters()
  fetchSummary()
  fetchAfterSales()
})

const summaryCards = computed(() => [
  {
    key: 'all',
    label: '全部售后',
    value: formatNumber(summary.value.totalCount),
    sub: `总金额 ${formatMoney(summary.value.totalAmount)}`,
    tone: 'slate',
  },
  {
    key: 'processing',
    label: '处理中',
    value: formatNumber(summary.value.processingCount),
    sub: `待处理 ${formatMoney(summary.value.processingAmount)}`,
    tone: 'blue',
  },
  {
    key: 'pending',
    label: '待审核',
    value: formatNumber(summary.value.pendingCount),
    sub: `申请金额 ${formatMoney(summary.value.pendingAmount)}`,
    tone: 'orange',
    status: 0,
  },
  {
    key: 'waitReturn',
    label: '待退货',
    value: formatNumber(summary.value.waitReturnCount),
    sub: `涉及 ${formatMoney(summary.value.waitReturnAmount)}`,
    tone: 'cyan',
    status: 1,
  },
  {
    key: 'waitRefund',
    label: '待退款',
    value: formatMoney(summary.value.waitRefundAmount),
    sub: `${formatNumber(summary.value.waitRefundCount)} 笔待退`,
    tone: 'red',
    status: 2,
  },
  {
    key: 'completed',
    label: '已完成',
    value: formatMoney(summary.value.completedAmount),
    sub: `${formatNumber(summary.value.completedCount)} 笔完成`,
    tone: 'green',
    status: 4,
  },
])

const healthCards = computed(() => [
  {
    label: '今日新增',
    value: formatNumber(summary.value.todayNewCount),
    desc: `申请金额 ${formatMoney(summary.value.todayNewAmount)}`,
  },
  {
    label: '今日退款',
    value: formatMoney(summary.value.todayRefundedAmount),
    desc: `${formatNumber(summary.value.todayRefundedCount)} 笔完成退款`,
  },
  {
    label: '完成率',
    value: formatPercent(summary.value.completionRate),
    desc: '已完成 / 非取消售后',
  },
  {
    label: '拒绝率',
    value: formatPercent(summary.value.rejectionRate),
    desc: '已拒绝 / 非取消售后',
  },
])

const visibleTypes = computed(() => (summary.value.types || []).filter((item) => Number(item.count || 0) > 0))

function routeValue(name: string) {
  const value = route.query[name]
  return Array.isArray(value) ? value[0] : value
}

function routeNumber(name: string) {
  const value = routeValue(name)
  if (value === undefined || value === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function applyRouteFilters() {
  filters.keyword = routeValue('keyword') || ''
  filters.userId = routeValue('userId') || undefined
  filters.type = routeNumber('type')
  filters.status = routeNumber('status')
}

async function fetchAfterSales() {
  loading.value = true
  try {
    const res = await getAfterSalePage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      userId: normalizeId(filters.userId),
      type: filters.type,
      status: filters.status,
    })
    afterSales.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const res = await getAfterSaleSummary()
    summary.value = res.data || createEmptySummary()
  } finally {
    summaryLoading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchAfterSales()
}

async function handleReset() {
  filters.keyword = ''
  filters.userId = undefined
  filters.type = undefined
  filters.status = undefined
  pagination.current = 1
  await fetchAfterSales()
}

async function applyStatusFilter(status?: number) {
  filters.status = status
  pagination.current = 1
  await fetchAfterSales()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchAfterSales()
}

function createEmptyForm(): CreateFormState {
  return {
    orderId: undefined,
    type: 1,
    amount: undefined,
    reason: '',
    description: '',
    imagesText: '',
  }
}

function openCreate() {
  Object.assign(createForm, createEmptyForm())
  createFormRef.value?.clearValidate()
  createOpen.value = true
}

async function submitCreate() {
  try {
    await createFormRef.value?.validate()
    const payload: AfterSaleCreatePayload = {
      orderId: requireId(createForm.orderId, '请输入正确的订单ID'),
      type: createForm.type,
      amount: createForm.amount,
      reason: normalizeOptional(createForm.reason),
      description: normalizeOptional(createForm.description),
      images: splitLines(createForm.imagesText),
    }
    creating.value = true
    try {
      await createAfterSale(payload)
      message.success('售后单创建成功')
      createOpen.value = false
      pagination.current = 1
      await fetchSummary()
      await fetchAfterSales()
    } finally {
      creating.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

async function handleView(record: AfterSaleRecord) {
  detailOpen.value = true
  detailLoading.value = true
  currentAfterSale.value = record
  try {
    const res = await getAfterSaleDetail(record.id)
    currentAfterSale.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function openAction(mode: ActionMode, record: AfterSaleRecord) {
  actionMode.value = mode
  actionTarget.value = record
  actionForm.auditRemark = ''
  actionForm.rejectReason = ''
  actionForm.returnCompany = record.returnCompany || ''
  actionForm.returnNo = record.returnNo || ''
  actionOpen.value = true
}

function actionTip() {
  if (!actionTarget.value) {
    return ''
  }
  if (actionMode.value === 'approve') {
    return actionTarget.value.type === 2
      ? '通过后用户需要先填写退货物流，商家确认收货后才能退款。'
      : '通过后售后单会进入待退款，下一步可执行全额退款。'
  }
  if (actionMode.value === 'receive') {
    return '确认收货后售后单进入待退款，退货物流会保留到售后详情。'
  }
  if (actionMode.value === 'refund') {
    return `将按售后金额 ${formatMoney(actionTarget.value.amount)} 创建退款流水，并同步支付记录为已退款。`
  }
  if (actionMode.value === 'cancel') {
    return '取消后订单状态会恢复到发起售后前的状态。'
  }
  return '拒绝后订单状态会恢复到发起售后前的状态。'
}

async function submitAction() {
  if (!actionTarget.value) {
    return
  }
  const payload: AfterSaleActionPayload = {
    auditRemark: normalizeOptional(actionForm.auditRemark),
    rejectReason: normalizeOptional(actionForm.rejectReason),
    returnCompany: normalizeOptional(actionForm.returnCompany),
    returnNo: normalizeOptional(actionForm.returnNo),
  }
  if (actionMode.value === 'reject' && !payload.rejectReason) {
    message.error('请输入拒绝原因')
    return
  }
  actionLoading.value = true
  try {
    const id = actionTarget.value.id
    if (actionMode.value === 'approve') {
      await approveAfterSale(id, payload)
      message.success('审核通过')
    } else if (actionMode.value === 'reject') {
      await rejectAfterSale(id, payload)
      message.success('已拒绝')
    } else if (actionMode.value === 'receive') {
      await receiveAfterSale(id, payload)
      message.success('已确认收货')
    } else if (actionMode.value === 'refund') {
      await refundAfterSale(id, payload)
      message.success('退款完成')
    } else {
      await cancelAfterSale(id, payload)
      message.success('售后单已取消')
    }
    actionOpen.value = false
    await fetchSummary()
    await fetchAfterSales()
    await refreshCurrent(id)
  } finally {
    actionLoading.value = false
  }
}

async function refreshCurrent(id: ApiId) {
  if (currentAfterSale.value?.id !== id) {
    return
  }
  const res = await getAfterSaleDetail(id)
  currentAfterSale.value = res.data
}

function statusColor(status?: number) {
  if (status === 0 || status === 1 || status === 2) {
    return 'processing'
  }
  if (status === 3) {
    return 'error'
  }
  if (status === 4) {
    return 'success'
  }
  return 'default'
}

function orderStatusColor(status?: number) {
  if (status === 5) {
    return 'warning'
  }
  if (status === 6) {
    return 'default'
  }
  return 'processing'
}

function actionTitle() {
  const map: Record<ActionMode, string> = {
    approve: '审核通过',
    reject: '审核拒绝',
    receive: '确认退货收货',
    refund: '确认退款',
    cancel: '取消售后单',
  }
  return map[actionMode.value]
}

function canApprove(record: AfterSaleRecord) {
  return record.status === 0
}

function canReject(record: AfterSaleRecord) {
  return record.status === 0
}

function canReceive(record: AfterSaleRecord) {
  return record.status === 1
}

function canRefund(record: AfterSaleRecord) {
  return record.status === 2
}

function canCancel(record: AfterSaleRecord) {
  return record.status === 0 || record.status === 1 || record.status === 2
}

function goPayment(record?: AfterSaleRecord) {
  const keyword = record?.refundRecord?.payNo || record?.orderNo
  router.push({ name: 'Payment', query: keyword ? { keyword } : undefined })
}

function refundOperatorText(value?: string) {
  if (value === 'admin_after_sale') {
    return '售后后台'
  }
  if (value === 'admin_payment') {
    return '支付后台'
  }
  if (value === 'system') {
    return '系统'
  }
  return value || '--'
}

function userText(record?: AfterSaleRecord) {
  if (!record) {
    return '--'
  }
  return record.nickname || record.username || `会员 ${record.userId}`
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '--'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatPercent(value?: number) {
  return `${Number(value || 0).toFixed(2)}%`
}

function formatSpecs(value?: string | Record<string, string>) {
  if (!value) {
    return '--'
  }
  const specs = typeof value === 'string' ? parseSpecString(value) : value
  const items = Object.entries(specs).map(([key, item]) => `${key}: ${item}`)
  return items.length ? items.join(' / ') : '--'
}

function parseSpecString(value: string) {
  try {
    return JSON.parse(value) as Record<string, string>
  } catch {
    return {}
  }
}

function splitLines(value: string) {
  return value
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function requireId(value: ApiId | undefined, errorMessage: string) {
  const id = normalizeId(value)
  if (!id || !/^\d+$/.test(String(id))) {
    throw new Error(errorMessage)
  }
  return id
}

function normalizeOptional(value: string) {
  const text = value.trim()
  return text ? text : undefined
}

function toAfterSale(record: Record<string, any>) {
  return record as AfterSaleRecord
}

function toAfterSaleItem(record: Record<string, any>) {
  return record as AfterSaleItem
}

function createEmptySummary(): AfterSaleSummary {
  return {
    totalCount: 0,
    totalAmount: 0,
    pendingCount: 0,
    pendingAmount: 0,
    waitReturnCount: 0,
    waitReturnAmount: 0,
    waitRefundCount: 0,
    waitRefundAmount: 0,
    rejectedCount: 0,
    rejectedAmount: 0,
    completedCount: 0,
    completedAmount: 0,
    canceledCount: 0,
    canceledAmount: 0,
    processingCount: 0,
    processingAmount: 0,
    todayNewCount: 0,
    todayNewAmount: 0,
    todayRefundedCount: 0,
    todayRefundedAmount: 0,
    completionRate: 0,
    rejectionRate: 0,
    types: [],
  }
}
</script>

<template>
  <a-card title="售后/退货管理" :bordered="false">
    <a-spin :spinning="summaryLoading">
      <div class="after-sale-summary">
        <button
          v-for="card in summaryCards"
          :key="card.key"
          class="summary-card"
          :class="[`summary-card--${card.tone}`, { active: filters.status === card.status || (card.status === undefined && filters.status === undefined) }]"
          type="button"
          @click="applyStatusFilter(card.status)"
        >
          <span class="summary-label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <span class="summary-sub">{{ card.sub }}</span>
        </button>
      </div>

      <div class="after-sale-health">
        <div v-for="card in healthCards" :key="card.label" class="health-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.desc }}</small>
        </div>
        <div class="type-strip">
          <span class="type-title">类型分布</span>
          <a-empty v-if="!visibleTypes.length" :image="false" description="暂无售后单" />
          <a-space v-else wrap>
            <a-tag v-for="item in visibleTypes" :key="item.type" color="blue">
              {{ item.typeDesc || '未知类型' }}：{{ formatNumber(item.count) }} 笔 / {{ formatMoney(item.amount) }}
            </a-tag>
          </a-space>
        </div>
      </div>
    </a-spin>

    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索售后单号/订单号/原因"
          style="width: 260px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.userId" allow-clear placeholder="会员 ID" style="width: 140px" />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.type" allow-clear placeholder="类型" style="width: 140px">
          <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="售后状态" style="width: 140px">
          <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="openCreate">新建售后单</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="afterSales"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1520 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'afterSaleNo'">
          <div style="font-weight: 600">{{ record.afterSaleNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.reason || '暂无原因' }}
          </div>
        </template>

        <template v-else-if="column.key === 'orderNo'">
          <div>{{ record.orderNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            订单 ID {{ record.orderId }}
          </div>
        </template>

        <template v-else-if="column.key === 'user'">
          <div>{{ userText(toAfterSale(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            ID {{ record.userId }} / {{ record.phone || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'typeAmount'">
          <div>{{ record.typeDesc || '--' }}</div>
          <div style="font-weight: 600; margin-top: 4px">{{ formatMoney(record.amount) }}</div>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'orderStatus'">
          <a-tag :color="orderStatusColor(record.currentOrderStatus)">
            {{ record.currentOrderStatusDesc || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toAfterSale(record))">查看</a-button>
            <a-button v-if="canApprove(toAfterSale(record))" type="link" size="small" @click="openAction('approve', toAfterSale(record))">
              通过
            </a-button>
            <a-button v-if="canReject(toAfterSale(record))" type="link" danger size="small" @click="openAction('reject', toAfterSale(record))">
              拒绝
            </a-button>
            <a-button v-if="canReceive(toAfterSale(record))" type="link" size="small" @click="openAction('receive', toAfterSale(record))">
              收货
            </a-button>
            <a-button v-if="canRefund(toAfterSale(record))" type="link" danger size="small" @click="openAction('refund', toAfterSale(record))">
              退款
            </a-button>
            <a-button v-if="canCancel(toAfterSale(record))" type="link" size="small" @click="openAction('cancel', toAfterSale(record))">
              取消
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="createOpen"
    title="新建售后单"
    :confirm-loading="creating"
    width="680px"
    @ok="submitCreate"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="订单 ID" name="orderId">
            <a-input v-model:value="createForm.orderId" placeholder="请输入订单ID" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="售后类型" name="type">
            <a-radio-group v-model:value="createForm.type">
              <a-radio :value="1">仅退款</a-radio>
              <a-radio :value="2">退货退款</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="售后金额">
            <a-input-number v-model:value="createForm.amount" :min="0.01" style="width: 100%" placeholder="留空默认订单实付" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="售后原因">
            <a-input v-model:value="createForm.reason" placeholder="例如：商品破损" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="问题描述">
            <a-textarea v-model:value="createForm.description" :rows="4" placeholder="请输入问题描述" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="凭证图片 URL">
            <a-textarea v-model:value="createForm.imagesText" :rows="3" placeholder="每行一个图片 URL" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-drawer v-model:open="detailOpen" title="售后详情" width="900" destroy-on-close>
    <a-spin :spinning="detailLoading">
      <template v-if="currentAfterSale">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="售后单号" :span="2">{{ currentAfterSale.afterSaleNo }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentAfterSale.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="会员">{{ userText(currentAfterSale) }}</a-descriptions-item>
          <a-descriptions-item label="售后类型">{{ currentAfterSale.typeDesc || '--' }}</a-descriptions-item>
          <a-descriptions-item label="售后金额">{{ formatMoney(currentAfterSale.amount) }}</a-descriptions-item>
          <a-descriptions-item label="售后状态">
            <a-tag :color="statusColor(currentAfterSale.status)">
              {{ currentAfterSale.statusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="订单状态">
            <a-tag :color="orderStatusColor(currentAfterSale.currentOrderStatus)">
              {{ currentAfterSale.currentOrderStatusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="申请原因">{{ currentAfterSale.reason || '--' }}</a-descriptions-item>
          <a-descriptions-item label="原订单状态">{{ currentAfterSale.orderStatusSnapshotDesc || '--' }}</a-descriptions-item>
          <a-descriptions-item label="审核备注">{{ currentAfterSale.auditRemark || '--' }}</a-descriptions-item>
          <a-descriptions-item label="拒绝原因">{{ currentAfterSale.rejectReason || '--' }}</a-descriptions-item>
          <a-descriptions-item label="退货物流">{{ [currentAfterSale.returnCompany, currentAfterSale.returnNo].filter(Boolean).join(' / ') || '--' }}</a-descriptions-item>
          <a-descriptions-item label="退款记录">{{ currentAfterSale.refundRecord?.refundNo || currentAfterSale.refundPaymentId || '--' }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ currentAfterSale.createTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="退款时间">{{ currentAfterSale.refundTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="问题描述" :span="2">{{ currentAfterSale.description || '--' }}</a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">退款记录</h4>
          <div v-if="currentAfterSale.refundRecord" class="refund-record-card">
            <div class="refund-record-card__main">
              <span>退款流水</span>
              <strong>{{ currentAfterSale.refundRecord.refundNo }}</strong>
              <small>
                {{ currentAfterSale.refundRecord.channelDesc || '--' }} ·
                {{ refundOperatorText(currentAfterSale.refundRecord.operatorType) }} ·
                {{ currentAfterSale.refundRecord.successTime || currentAfterSale.refundRecord.createTime || '--' }}
              </small>
            </div>
            <div class="refund-record-card__amount">
              <span>{{ currentAfterSale.refundRecord.statusDesc || '--' }}</span>
              <strong>{{ formatMoney(currentAfterSale.refundRecord.amount) }}</strong>
            </div>
            <div class="refund-record-card__meta">
              <span>支付流水：{{ currentAfterSale.refundRecord.payNo || '--' }}</span>
              <span>退款原因：{{ currentAfterSale.refundRecord.reason || '--' }}</span>
            </div>
            <div class="refund-record-card__actions">
              <a-button size="small" type="link" @click="goPayment(currentAfterSale)">查看支付流水</a-button>
            </div>
          </div>
          <a-empty v-else :image="false" description="暂无退款记录">
            <template #description>
              <span>售后进入待退款后，点击“确认退款”会生成退款流水。</span>
            </template>
          </a-empty>
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">订单商品</h4>
          <a-table
            row-key="id"
            size="small"
            :columns="itemColumns"
            :data-source="currentAfterSale.items || []"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuName'">
                <div style="display: flex; gap: 12px; align-items: center">
                  <img
                    v-if="record.skuImage"
                    :src="record.skuImage"
                    alt="商品图"
                    style="width: 48px; height: 48px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0"
                  />
                  <div>
                    <div style="font-weight: 600">{{ record.skuName }}</div>
                    <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                      {{ formatSpecs(toAfterSaleItem(record).specData) }}
                    </div>
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'price'">
                {{ formatMoney(record.price) }}
              </template>
              <template v-else-if="column.key === 'totalAmount'">
                {{ formatMoney(record.totalAmount) }}
              </template>
            </template>
          </a-table>
        </div>

        <div v-if="currentAfterSale.images?.length" style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">售后凭证</h4>
          <a-space wrap>
            <a v-for="image in currentAfterSale.images" :key="image" :href="image" target="_blank">
              {{ image }}
            </a>
          </a-space>
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: flex-end">
          <a-space>
            <a-button v-if="canApprove(currentAfterSale)" @click="openAction('approve', currentAfterSale)">审核通过</a-button>
            <a-button v-if="canReject(currentAfterSale)" danger @click="openAction('reject', currentAfterSale)">拒绝</a-button>
            <a-button v-if="canReceive(currentAfterSale)" @click="openAction('receive', currentAfterSale)">确认收货</a-button>
            <a-button v-if="canRefund(currentAfterSale)" danger @click="openAction('refund', currentAfterSale)">确认退款</a-button>
            <a-button v-if="canCancel(currentAfterSale)" @click="openAction('cancel', currentAfterSale)">取消售后</a-button>
          </a-space>
        </div>
      </template>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="actionOpen"
    :title="actionTitle()"
    :confirm-loading="actionLoading"
    @ok="submitAction"
  >
    <a-form layout="vertical">
      <a-form-item label="售后单">
        <a-input :value="actionTarget?.afterSaleNo" disabled />
      </a-form-item>
      <a-alert v-if="actionTip()" :message="actionTip()" type="info" show-icon style="margin-bottom: 16px" />
      <a-descriptions v-if="actionTarget" :column="2" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="状态">{{ actionTarget.statusDesc || '--' }}</a-descriptions-item>
        <a-descriptions-item label="金额">{{ formatMoney(actionTarget.amount) }}</a-descriptions-item>
        <a-descriptions-item label="订单">{{ actionTarget.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="会员">{{ userText(actionTarget) }}</a-descriptions-item>
      </a-descriptions>
      <a-form-item v-if="actionMode === 'reject'" label="拒绝原因">
        <a-textarea v-model:value="actionForm.rejectReason" :rows="3" placeholder="请输入拒绝原因" />
      </a-form-item>
      <a-row v-if="actionMode === 'receive'" :gutter="12">
        <a-col :span="12">
          <a-form-item label="退货物流公司">
            <a-input v-model:value="actionForm.returnCompany" placeholder="例如：顺丰" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="退货物流单号">
            <a-input v-model:value="actionForm.returnNo" placeholder="请输入物流单号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="操作备注">
        <a-textarea v-model:value="actionForm.auditRemark" :rows="4" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.after-sale-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-card {
  min-height: 110px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
  color: #111827;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.summary-card:hover,
.summary-card.active {
  border-color: #111827;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.summary-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 21px;
  line-height: 1.15;
}

.summary-label,
.summary-sub {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.summary-card--blue {
  background: #f8fbff;
}

.summary-card--orange {
  background: #fffaf2;
}

.summary-card--cyan {
  background: #f6fdff;
}

.summary-card--red {
  background: #fff8f8;
}

.summary-card--green {
  background: #f6fffb;
}

.after-sale-health {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 180px)) minmax(280px, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.health-card,
.type-strip {
  padding: 12px 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fafafa;
}

.health-card span,
.type-title {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.health-card strong {
  display: block;
  margin: 6px 0 2px;
  font-size: 20px;
  color: #111827;
}

.health-card small {
  color: #94a3b8;
}

.type-strip {
  min-width: 0;
}

.type-title {
  margin-bottom: 8px;
}

.refund-record-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 160px;
  gap: 12px 18px;
  padding: 16px 18px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfcff;
}

.refund-record-card__main span,
.refund-record-card__main small,
.refund-record-card__amount span,
.refund-record-card__meta span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.refund-record-card__main strong {
  display: block;
  margin: 6px 0;
  color: #111827;
  font-size: 18px;
  word-break: break-all;
}

.refund-record-card__amount {
  text-align: right;
}

.refund-record-card__amount span {
  color: #16a34a;
  font-weight: 700;
}

.refund-record-card__amount strong {
  display: block;
  margin-top: 6px;
  color: #dc2626;
  font-size: 22px;
}

.refund-record-card__meta {
  display: grid;
  grid-column: 1 / -1;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid #edf0f5;
}

.refund-record-card__actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1400px) {
  .after-sale-summary {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }

  .after-sale-health {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }

  .type-strip {
    grid-column: 1 / -1;
  }
}
</style>
