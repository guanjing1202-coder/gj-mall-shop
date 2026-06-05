<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  getPaymentAccess,
  getPaymentCallbackPage,
  getPaymentDetail,
  getPaymentPage,
  getPaymentSummary,
  markRefundFailed,
  markPaymentFailed,
  markPaymentPaid,
  refundPayment,
  replayCallback,
  retryRefund,
  syncPaymentStatus,
  type ApiId,
  type PaymentAccess,
  type PaymentCallbackRecord,
  type PaymentQuery,
  type PaymentRecord,
  type PaymentSummary,
} from '@/api/payment'
import {
  canReplayPaymentCallback,
  formatCallbackSignatureSample,
  formatPaymentCallbackInspectItems,
  paymentCallbackProcessLabel,
  paymentCallbackReplayReason,
} from '@/utils/payment-callback-ui'
import {
  canRefundPayment,
  canMarkRefundFailed as canMarkRefundFailedAction,
  canRetryRefund as canRetryRefundAction,
  canSyncPaymentStatus,
  isFullRefundAmount,
  paymentRefundHint,
  refundFailureHint,
  paymentSyncStatusHint,
} from '@/utils/payment-action-ui'
import {
  paymentAccessTipItems,
  paymentAccessTips,
  paymentAccessTitle,
  paymentAccessTone,
  paymentConfigRouteQuery,
  type PaymentAccessTipItem,
} from '@/utils/payment-access-ui'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const summaryLoading = ref(false)
const detailLoading = ref(false)
const callbackLoading = ref(false)
const actionId = ref<ApiId>()
const callbackActionId = ref<ApiId>()
const payments = ref<PaymentRecord[]>([])
const summary = ref<PaymentSummary>(createEmptySummary())
const access = ref<PaymentAccess>(createEmptyAccess())
const callbacks = ref<PaymentCallbackRecord[]>([])
const detailOpen = ref(false)
const currentPayment = ref<PaymentRecord>()

const refundOpen = ref(false)
const refundSaving = ref(false)
const refundTarget = ref<PaymentRecord>()
const refundForm = reactive({
  amount: undefined as number | undefined,
  reason: '',
})
const refundFailedOpen = ref(false)
const refundFailedSaving = ref(false)
const refundFailedTarget = ref<PaymentRecord>()
const refundFailedForm = reactive({
  reason: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<PaymentQuery>({
  keyword: '',
  userId: undefined,
  channel: undefined,
  status: undefined,
})

const columns: TableColumnsType<PaymentRecord> = [
  { title: '支付流水', dataIndex: 'payNo', key: 'payNo', width: 290 },
  { title: '订单', dataIndex: 'orderNo', key: 'orderNo', width: 230 },
  { title: '会员', dataIndex: 'userId', key: 'user', width: 180 },
  { title: '渠道', dataIndex: 'channel', key: 'channel', width: 130 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 130 },
  { title: '支付状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '订单状态', dataIndex: 'orderStatus', key: 'orderStatus', width: 130 },
  { title: '支付时间', dataIndex: 'payTime', key: 'payTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 280 },
]

const channelOptions = [
  { label: '微信支付', value: 1 },
  { label: '支付宝', value: 2 },
  { label: '余额', value: 3 },
  { label: 'Mock 支付', value: 9 },
]

const statusOptions = [
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '支付失败', value: 2 },
  { label: '已退款', value: 3 },
]

onMounted(() => {
  applyRouteFilters()
  fetchSummary()
  fetchAccess()
  fetchCallbacks()
  fetchPayments()
})

const summaryCards = computed(() => [
  {
    key: 'total',
    label: '全部流水',
    value: formatNumber(summary.value.totalCount),
    sub: `总金额 ${formatMoney(summary.value.totalAmount)}`,
    tone: 'slate',
  },
  {
    key: 'pending',
    label: '待支付',
    value: formatNumber(summary.value.pendingCount),
    sub: `待收 ${formatMoney(summary.value.pendingAmount)}`,
    tone: 'blue',
    status: 0,
  },
  {
    key: 'paid',
    label: '已支付',
    value: formatMoney(summary.value.paidAmount),
    sub: `${formatNumber(summary.value.paidCount)} 笔 / 今日 ${formatMoney(summary.value.todayPaidAmount)}`,
    tone: 'green',
    status: 1,
  },
  {
    key: 'failed',
    label: '支付失败',
    value: formatNumber(summary.value.failedCount),
    sub: `失败金额 ${formatMoney(summary.value.failedAmount)}`,
    tone: 'red',
    status: 2,
  },
  {
    key: 'refunded',
    label: '已退款',
    value: formatMoney(summary.value.refundedAmount),
    sub: `${formatNumber(summary.value.refundedCount)} 笔 / 今日 ${formatMoney(summary.value.todayRefundedAmount)}`,
    tone: 'orange',
    status: 3,
  },
])

const healthCards = computed(() => [
  { label: '支付成功率', value: formatPercent(summary.value.successRate), desc: '已支付笔数 / 全部流水' },
  { label: '退款率', value: formatPercent(summary.value.refundRate), desc: '已退款笔数 / 成功支付流水' },
])

const visibleChannels = computed(() => (summary.value.channels || []).filter((item) => Number(item.count || 0) > 0))

const signatureSampleLines = computed(() => formatCallbackSignatureSample({
  canonicalPayload: access.value.devSignaturePayload,
  signatureHeader: access.value.devSignatureHeader,
  signature: access.value.devSignature,
}))

const accessTone = computed(() => paymentAccessTone(access.value))
const accessTitle = computed(() => paymentAccessTitle(access.value))
const accessTips = computed(() => paymentAccessTips(access.value))
const accessTipItems = computed(() => paymentAccessTipItems(access.value))

const callbackStatusCards = computed(() => {
  const handled = callbacks.value.filter((item) => item.processStatus === 1).length
  const ignored = callbacks.value.filter((item) => item.processStatus === 2).length
  const failed = callbacks.value.filter((item) => item.processStatus === 3).length
  return [
    { label: '最近回调', value: formatNumber(callbacks.value.length), tone: 'slate' },
    { label: '已处理', value: formatNumber(handled), tone: 'green' },
    { label: '已忽略', value: formatNumber(ignored), tone: 'blue' },
    { label: '处理失败', value: formatNumber(failed), tone: 'red' },
  ]
})

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
  filters.channel = routeNumber('channel')
  filters.status = routeNumber('status')
}

async function fetchPayments() {
  loading.value = true
  try {
    const res = await getPaymentPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      userId: normalizeId(filters.userId),
      channel: filters.channel,
      status: filters.status,
    })
    payments.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const res = await getPaymentSummary()
    summary.value = res.data || createEmptySummary()
  } finally {
    summaryLoading.value = false
  }
}

async function fetchAccess() {
  const res = await getPaymentAccess()
  access.value = res.data || createEmptyAccess()
}

async function fetchCallbacks(keyword?: string) {
  callbackLoading.value = true
  try {
    const res = await getPaymentCallbackPage({
      pageNum: 1,
      pageSize: 6,
      keyword: keyword || undefined,
    })
    callbacks.value = res.data.list || []
  } finally {
    callbackLoading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchPayments()
}

async function handleReset() {
  filters.keyword = ''
  filters.userId = undefined
  filters.channel = undefined
  filters.status = undefined
  pagination.current = 1
  await fetchPayments()
}

async function applyStatusFilter(status?: number) {
  filters.status = status
  pagination.current = 1
  await fetchPayments()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchPayments()
}

async function handleView(record: PaymentRecord) {
  detailOpen.value = true
  detailLoading.value = true
  currentPayment.value = record
  try {
    const res = await getPaymentDetail(record.id)
    currentPayment.value = res.data
    await fetchCallbacks(record.payNo)
  } finally {
    detailLoading.value = false
  }
}

function handleMarkPaid(record: PaymentRecord) {
  let thirdPayNo = record.thirdPayNo || ''
  Modal.confirm({
    title: '确认同步为已支付吗？',
    content: `支付流水：${record.payNo}`,
    async onOk() {
      actionId.value = record.id
      try {
        await markPaymentPaid(record.id, thirdPayNo.trim() || undefined)
        message.success('已同步为支付成功')
        await fetchSummary()
        await fetchPayments()
        await refreshCurrentPayment(record.id)
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleMarkFailed(record: PaymentRecord) {
  Modal.confirm({
    title: '确认标记为支付失败吗？',
    content: `支付流水：${record.payNo}`,
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await markPaymentFailed(record.id, '后台手动标记失败')
        message.success('已标记为失败')
        await fetchSummary()
        await fetchPayments()
        await refreshCurrentPayment(record.id)
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleSyncStatus(record: PaymentRecord) {
  Modal.confirm({
    title: '确认同步支付状态吗？',
    content: `将根据已记录的成功渠道回调同步支付流水：${record.payNo}`,
    async onOk() {
      actionId.value = record.id
      try {
        await syncPaymentStatus(record.id)
        message.success('支付状态已同步')
        await fetchSummary()
        await fetchPayments()
        await refreshCurrentPayment(record.id)
        await fetchCallbacks(record.payNo)
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function openRefund(record: PaymentRecord) {
  refundTarget.value = record
  refundForm.amount = record.amount
  refundForm.reason = ''
  refundOpen.value = true
}

async function submitRefund() {
  const target = refundTarget.value
  if (!target) {
    return
  }
  if (!canRefund(target)) {
    message.error(refundHint(target) || '当前支付流水不可退款')
    return
  }
  if (!isFullRefundAmount(refundForm.amount, target.amount)) {
    message.error('当前版本仅支持整笔全额退款，请刷新后重试')
    return
  }
  refundSaving.value = true
  try {
    await refundPayment(target.id, {
      amount: target.amount,
      reason: refundForm.reason.trim() || undefined,
    })
    message.success('退款成功')
    refundOpen.value = false
    await fetchSummary()
    await fetchPayments()
    await refreshCurrentPayment(target.id)
  } finally {
    refundSaving.value = false
  }
}

function handleRetryRefund(record: PaymentRecord) {
  const refundRecord = record.refundRecord
  if (!refundRecord) {
    return
  }
  Modal.confirm({
    title: '确认重试退款吗？',
    content: `退款流水：${refundRecord.refundNo}`,
    async onOk() {
      actionId.value = record.id
      try {
        await retryRefund(refundRecord.id)
        message.success('退款重试已成功')
        await fetchSummary()
        await fetchPayments()
        await refreshCurrentPayment(record.id)
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleMarkRefundFailed(record: PaymentRecord) {
  const refundRecord = record.refundRecord
  if (!refundRecord) {
    return
  }
  refundFailedTarget.value = record
  refundFailedForm.reason = refundRecord.reason || ''
  refundFailedOpen.value = true
}

async function submitMarkRefundFailed() {
  const target = refundFailedTarget.value
  const refundRecord = target?.refundRecord
  if (!target || !refundRecord) {
    return
  }
  refundFailedSaving.value = true
  actionId.value = target.id
  try {
    await markRefundFailed(refundRecord.id, refundFailedForm.reason.trim() || undefined)
    message.success('已标记退款失败')
    refundFailedOpen.value = false
    await fetchPayments()
    await refreshCurrentPayment(target.id)
  } finally {
    refundFailedSaving.value = false
    actionId.value = undefined
  }
}

function handleReplayCallback(item: PaymentCallbackRecord) {
  const disabledReason = paymentCallbackReplayReason(item)
  if (disabledReason) {
    message.warning(disabledReason)
    return
  }
  Modal.confirm({
    title: '确认重放支付回调吗？',
    content: `回调流水：${item.callbackNo}`,
    async onOk() {
      callbackActionId.value = item.id
      try {
        await replayCallback(item.id)
        message.success('支付回调已重放')
        const keyword = currentPayment.value?.payNo || item.payNo
        await fetchCallbacks(keyword)
        await fetchSummary()
        await fetchPayments()
        if (currentPayment.value?.id) {
          await refreshCurrentPayment(currentPayment.value.id)
        }
      } finally {
        callbackActionId.value = undefined
      }
    },
  })
}

async function refreshCurrentPayment(id: ApiId) {
  if (currentPayment.value?.id !== id) {
    return
  }
  const res = await getPaymentDetail(id)
  currentPayment.value = res.data
}

function paymentStatusColor(status?: number) {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'error'
  }
  if (status === 3) {
    return 'default'
  }
  return 'processing'
}

function orderStatusColor(status?: number) {
  if (status === 1 || status === 2 || status === 3) {
    return 'success'
  }
  if (status === 5) {
    return 'warning'
  }
  if (status === 6) {
    return 'default'
  }
  return 'processing'
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

function userText(record?: PaymentRecord) {
  if (!record) {
    return '--'
  }
  return record.nickname || record.username || `会员 ${record.userId}`
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function canMarkPaid(record: PaymentRecord) {
  return record.status === 0 || record.status === 2
}

function canSyncStatus(record: PaymentRecord) {
  return canSyncPaymentStatus(record)
}

function syncStatusHint(record?: PaymentRecord) {
  return paymentSyncStatusHint(record)
}

function canMarkFailed(record: PaymentRecord) {
  return record.status === 0
}

function canRefund(record: PaymentRecord) {
  return canRefundPayment(record)
}

function refundHint(record?: PaymentRecord) {
  return paymentRefundHint(record)
}

function canRetryRefund(record: PaymentRecord) {
  return canRetryRefundAction(record)
}

function canMarkRefundFailed(record: PaymentRecord) {
  return canMarkRefundFailedAction(record)
}

function canReplayCallback(item: PaymentCallbackRecord) {
  return canReplayPaymentCallback(item)
}

function callbackInspectItems(item: PaymentCallbackRecord) {
  return formatPaymentCallbackInspectItems(item)
}

function refundStatusColor(status?: number) {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'error'
  }
  return 'processing'
}

function refundErrorHint(record?: PaymentRecord) {
  return refundFailureHint(record?.refundRecord)
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

function toPayment(record: Record<string, any>) {
  return record as PaymentRecord
}

function createEmptySummary(): PaymentSummary {
  return {
    totalCount: 0,
    totalAmount: 0,
    pendingCount: 0,
    pendingAmount: 0,
    paidCount: 0,
    paidAmount: 0,
    failedCount: 0,
    failedAmount: 0,
    refundedCount: 0,
    refundedAmount: 0,
    todayPaidAmount: 0,
    todayRefundedAmount: 0,
    successRate: 0,
    refundRate: 0,
    channels: [],
  }
}

function createEmptyAccess(): PaymentAccess {
  return {
    mode: 'mock',
    ready: true,
    readinessText: 'Mock 支付模式',
    readinessTips: [],
    callbackRequireSignature: false,
    callbackPath: '/api/pay/callback/{channel}',
    devSignatureAlgorithm: 'HmacSHA256(sortedPayload, mall.pay.callback.secret)',
    devSignatureHeader: 'x-gj-pay-signature',
    devSignaturePayload: '',
    devSignature: '',
    devCallbackExample: '',
    channels: [],
  }
}

function processStatusColor(status?: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'processing'
  if (status === 3) return 'error'
  return 'default'
}

function signatureStatusColor(status?: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'error'
  return 'default'
}

function goPaymentConfig(tip?: PaymentAccessTipItem) {
  router.push({
    name: 'SystemConfig',
    query: paymentConfigRouteQuery(tip),
  })
}
</script>

<template>
  <a-card title="支付退款管理" :bordered="false">
    <a-spin :spinning="summaryLoading">
      <section class="access-panel" :class="`access-panel--${accessTone}`">
        <div class="access-main">
          <span>支付接入状态</span>
          <strong>{{ accessTitle }}</strong>
          <small>回调入口：{{ access.callbackPath }} / {{ access.callbackRequireSignature ? '强制验签' : '开发环境允许无签名' }}</small>
        </div>
        <div class="access-channels">
          <a-tag v-for="item in access.channels" :key="item.name" :color="item.enabled ? 'green' : 'default'">
            {{ item.desc }} · {{ item.status }}
          </a-tag>
        </div>
        <div v-if="accessTipItems.length" class="access-tips">
          <button
            v-for="tip in accessTipItems.slice(0, 6)"
            :key="tip.text"
            type="button"
            @click="goPaymentConfig(tip)"
          >
            {{ tip.text }}
          </button>
          <em v-if="accessTips.length > 6">还有 {{ accessTips.length - 6 }} 项配置待补齐</em>
          <a-button size="small" type="link" @click="goPaymentConfig()">打开支付配置</a-button>
        </div>
      </section>

      <div class="payment-summary">
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

      <div class="payment-health">
        <div v-for="card in healthCards" :key="card.label" class="health-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.desc }}</small>
        </div>
        <div class="channel-strip">
          <span class="channel-title">渠道分布</span>
          <a-empty v-if="!visibleChannels.length" :image="false" description="暂无支付流水" />
          <a-space v-else wrap>
            <a-tag v-for="item in visibleChannels" :key="item.channel" color="blue">
              {{ item.channelDesc || '未知渠道' }}：{{ formatNumber(item.count) }} 笔 / {{ formatMoney(item.amount) }}
            </a-tag>
          </a-space>
        </div>
      </div>

      <div class="callback-health">
        <div v-for="card in callbackStatusCards" :key="card.label" class="callback-card" :class="`callback-card--${card.tone}`">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </div>
        <div class="callback-tip">
          <span>回调验签</span>
          <strong>{{ access.devSignatureAlgorithm }}</strong>
          <small>真实微信/支付宝 SDK 接入后，这里会记录每次异步通知的验签和处理结果。</small>
        </div>
      </div>

      <section class="signature-sample">
        <div class="signature-sample__head">
          <span>开发回调签名样例</span>
          <strong>{{ access.devSignatureHeader || 'x-gj-pay-signature' }}</strong>
        </div>
        <div class="signature-sample__body">
          <code v-for="line in signatureSampleLines" :key="line">{{ line }}</code>
          <code v-if="access.devCallbackExample">请求体：{{ access.devCallbackExample }}</code>
          <small>本样例用于 Mock/开发联调；真实支付接入后以微信、支付宝 SDK 验签结果为准。</small>
        </div>
      </section>
    </a-spin>

    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索支付流水/订单号/第三方流水"
          style="width: 280px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input
          v-model:value="filters.userId"
          allow-clear
          placeholder="会员 ID"
          style="width: 140px"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.channel" allow-clear placeholder="渠道" style="width: 140px">
          <a-select-option v-for="item in channelOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="支付状态" style="width: 140px">
          <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="payments"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1650 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'payNo'">
          <div style="font-weight: 600">{{ record.payNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.thirdPayNo || '暂无第三方流水' }}
          </div>
        </template>

        <template v-else-if="column.key === 'orderNo'">
          <div>{{ record.orderNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            订单 ID {{ record.orderId }}
          </div>
        </template>

        <template v-else-if="column.key === 'user'">
          <div>{{ userText(toPayment(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            ID {{ record.userId }} / {{ record.phone || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'channel'">
          {{ record.channelDesc || '--' }}
        </template>

        <template v-else-if="column.key === 'amount'">
          <span style="font-weight: 600">{{ formatMoney(record.amount) }}</span>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="paymentStatusColor(record.status)">
            {{ record.statusDesc || '--' }}
          </a-tag>
          <div v-if="record.status === 0 || record.status === 2" class="sync-status-hint">
            {{ syncStatusHint(toPayment(record)) || '等待渠道回调' }}
          </div>
        </template>

        <template v-else-if="column.key === 'orderStatus'">
          <a-tag :color="orderStatusColor(record.orderStatus)">
            {{ record.orderStatusDesc || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'payTime'">
          {{ record.payTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toPayment(record))">查看</a-button>
            <a-button
              v-if="canSyncStatus(toPayment(record))"
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleSyncStatus(toPayment(record))"
            >
              同步状态
            </a-button>
            <a-button
              v-if="canMarkPaid(toPayment(record))"
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleMarkPaid(toPayment(record))"
            >
              同步成功
            </a-button>
            <a-button
              v-if="canMarkFailed(toPayment(record))"
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleMarkFailed(toPayment(record))"
            >
              标失败
            </a-button>
            <a-button
              v-if="canRefund(toPayment(record))"
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="openRefund(toPayment(record))"
            >
              退款
            </a-button>
            <a-button
              v-if="canRetryRefund(toPayment(record))"
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleRetryRefund(toPayment(record))"
            >
              重试退款
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-drawer v-model:open="detailOpen" title="支付详情" width="820" destroy-on-close>
    <a-spin :spinning="detailLoading">
      <template v-if="currentPayment">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="支付流水" :span="2">{{ currentPayment.payNo }}</a-descriptions-item>
          <a-descriptions-item label="第三方流水" :span="2">{{ currentPayment.thirdPayNo || '--' }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentPayment.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="会员">{{ userText(currentPayment) }}</a-descriptions-item>
          <a-descriptions-item label="支付渠道">{{ currentPayment.channelDesc || '--' }}</a-descriptions-item>
          <a-descriptions-item label="支付金额">{{ formatMoney(currentPayment.amount) }}</a-descriptions-item>
          <a-descriptions-item label="支付状态">
            <a-tag :color="paymentStatusColor(currentPayment.status)">
              {{ currentPayment.statusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="订单状态">
            <a-tag :color="orderStatusColor(currentPayment.orderStatus)">
              {{ currentPayment.orderStatusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="支付时间">{{ currentPayment.payTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentPayment.createTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="同步资格" :span="2">
            <a-space wrap>
              <a-tag :color="currentPayment.syncStatusAllowed ? 'green' : 'default'">
                {{ currentPayment.syncStatusAllowed ? '可同步' : '不可同步' }}
              </a-tag>
              <span>{{ currentPayment.syncStatusReason || '--' }}</span>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item v-if="currentPayment.syncCallbackNo" label="同步依据" :span="2">
            <div class="sync-basis">
              <strong>{{ currentPayment.syncCallbackNo }}</strong>
              <span>{{ currentPayment.syncThirdPayNo || '暂无第三方流水' }}</span>
              <small>{{ currentPayment.syncCallbackTime || '--' }}</small>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="退款资格" :span="2">
            <a-space wrap>
              <a-tag :color="currentPayment.refundAllowed ? 'green' : 'default'">
                {{ currentPayment.refundAllowed ? '可退款' : '不可退款' }}
              </a-tag>
              <span>{{ refundHint(currentPayment) || '--' }}</span>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="最近用户通知" :span="2">
            <div
              v-if="currentPayment.latestUserMessageTitle || currentPayment.latestUserMessageContent"
              class="latest-user-message"
            >
              <div class="latest-user-message__head">
                <strong>{{ currentPayment.latestUserMessageTitle || '用户通知' }}</strong>
                <small>{{ currentPayment.latestUserMessageTime || '--' }}</small>
              </div>
              <p>{{ currentPayment.latestUserMessageContent || '--' }}</p>
            </div>
            <span v-else>--</span>
          </a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">回调/处理记录</h4>
          <a-textarea
            :value="currentPayment.callbackData || '--'"
            :rows="8"
            readonly
          />
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">退款记录</h4>
          <div v-if="currentPayment.refundRecord" class="refund-record-card">
            <div class="refund-record-card__main">
              <span>退款流水</span>
              <strong>{{ currentPayment.refundRecord.refundNo }}</strong>
              <small>
                {{ currentPayment.refundRecord.channelDesc || '--' }} ·
                {{ refundOperatorText(currentPayment.refundRecord.operatorType) }} ·
                {{ currentPayment.refundRecord.successTime || currentPayment.refundRecord.createTime || '--' }}
              </small>
            </div>
            <div class="refund-record-card__amount">
              <a-tag :color="refundStatusColor(currentPayment.refundRecord.status)">
                {{ currentPayment.refundRecord.statusDesc || '--' }}
              </a-tag>
              <strong>{{ formatMoney(currentPayment.refundRecord.amount) }}</strong>
            </div>
            <div class="refund-record-card__meta">
              <span v-if="currentPayment.refundRecord.afterSaleNo">售后单：{{ currentPayment.refundRecord.afterSaleNo }}</span>
              <span>退款原因：{{ currentPayment.refundRecord.reason || '--' }}</span>
              <span v-if="refundErrorHint(currentPayment)" class="refund-record-card__error">{{ refundErrorHint(currentPayment) }}</span>
              <span v-if="currentPayment.refundRecord.callbackData">处理记录：{{ currentPayment.refundRecord.callbackData }}</span>
              <a-space v-if="canRetryRefund(currentPayment) || canMarkRefundFailed(currentPayment)" wrap>
                <a-button
                  v-if="canRetryRefund(currentPayment)"
                  size="small"
                  type="primary"
                  :loading="actionId === currentPayment.id"
                  @click="handleRetryRefund(currentPayment)"
                >
                  重试退款
                </a-button>
                <a-button
                  v-if="canMarkRefundFailed(currentPayment)"
                  size="small"
                  danger
                  :loading="actionId === currentPayment.id"
                  @click="handleMarkRefundFailed(currentPayment)"
                >
                  标记失败
                </a-button>
              </a-space>
            </div>
          </div>
          <a-empty v-else :image="false" description="暂无退款记录" />
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">最近渠道回调</h4>
          <a-spin :spinning="callbackLoading">
            <a-empty v-if="!callbacks.length" :image="false" description="暂无回调记录" />
            <a-timeline v-else>
              <a-timeline-item v-for="item in callbacks" :key="item.id">
                <div class="callback-line">
                  <div>
                    <strong>{{ item.callbackNo }}</strong>
                    <span>{{ item.createTime || '--' }}</span>
                  </div>
                  <p>
                    {{ item.channelDesc || '--' }} / {{ item.eventType || '支付通知' }} / {{ formatMoney(item.amount) }}
                  </p>
                  <dl v-if="callbackInspectItems(item).length" class="callback-inspect">
                    <div v-for="field in callbackInspectItems(item)" :key="field.label">
                      <dt>{{ field.label }}</dt>
                      <dd>{{ field.value }}</dd>
                    </div>
                  </dl>
                  <a-space wrap>
                    <a-tag :color="signatureStatusColor(item.signatureStatus)">
                      {{ item.signatureStatusDesc || '--' }}
                    </a-tag>
                    <a-tag :color="processStatusColor(item.processStatus)">
                      {{ paymentCallbackProcessLabel(item.processStatus, item.processStatusDesc) }}
                    </a-tag>
                    <a-tag v-if="item.errorMessage" color="red">{{ item.errorMessage }}</a-tag>
                    <a-button
                      v-if="canReplayCallback(item)"
                      size="small"
                      type="primary"
                      :loading="callbackActionId === item.id"
                      @click="handleReplayCallback(item)"
                    >
                      重放回调
                    </a-button>
                  </a-space>
                </div>
              </a-timeline-item>
            </a-timeline>
          </a-spin>
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: flex-end">
          <a-space>
            <a-button
              v-if="canSyncStatus(currentPayment)"
              type="primary"
              :loading="actionId === currentPayment.id"
              @click="handleSyncStatus(currentPayment)"
            >
              按成功回调同步
            </a-button>
            <a-button
              v-if="canMarkPaid(currentPayment)"
              :loading="actionId === currentPayment.id"
              @click="handleMarkPaid(currentPayment)"
            >
              同步支付成功
            </a-button>
            <a-button
              v-if="canRefund(currentPayment)"
              danger
              :loading="actionId === currentPayment.id"
              @click="openRefund(currentPayment)"
            >
              全额退款
            </a-button>
          </a-space>
        </div>
      </template>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="refundOpen"
    title="全额退款"
    :confirm-loading="refundSaving"
    @ok="submitRefund"
  >
    <a-form layout="vertical">
      <a-alert
        class="refund-confirm-alert"
        type="warning"
        show-icon
        message="请确认该支付流水需要整笔退回"
        description="当前后台退款入口仅支持全额退款；售后部分退款会在售后单处理链路中发起。"
      />
      <a-form-item label="支付流水">
        <a-input :value="refundTarget?.payNo" disabled />
      </a-form-item>
      <a-form-item label="退款金额">
        <div class="refund-confirm-amount">
          <span>原路退回金额</span>
          <strong>{{ formatMoney(refundTarget?.amount) }}</strong>
          <small>{{ refundHint(refundTarget) || '已支付且未生成退款记录，可发起全额退款' }}</small>
        </div>
      </a-form-item>
      <a-form-item label="退款原因">
        <a-textarea v-model:value="refundForm.reason" :rows="4" placeholder="请输入退款原因" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="refundFailedOpen"
    title="标记退款失败"
    :confirm-loading="refundFailedSaving"
    ok-text="确认失败"
    ok-type="danger"
    @ok="submitMarkRefundFailed"
  >
    <a-form layout="vertical">
      <a-form-item label="退款流水">
        <a-input :value="refundFailedTarget?.refundRecord?.refundNo" disabled />
      </a-form-item>
      <a-form-item label="失败原因">
        <a-textarea
          v-model:value="refundFailedForm.reason"
          :rows="4"
          placeholder="请输入渠道返回原因、人工判定原因等"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.payment-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.access-panel {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(320px, 2fr);
  gap: 12px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfcff;
}

.access-panel--mock {
  border-color: #dbeafe;
  background: #f8fbff;
}

.access-panel--ready {
  border-color: #bbf7d0;
  background: #f7fef9;
}

.access-panel--warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.access-panel--danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.access-main span,
.access-main small,
.callback-tip span,
.callback-tip small {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.access-main strong,
.callback-tip strong {
  display: block;
  margin: 6px 0;
  color: #111827;
}

.access-channels {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.access-tips {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
}

.access-tips button,
.access-tips em {
  padding: 5px 9px;
  border: 0;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.78);
  color: #991b1b;
  cursor: pointer;
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
}

.access-tips button:hover {
  background: #fff1f2;
  color: #be123c;
}

.access-tips em {
  color: #64748b;
  cursor: default;
}

.callback-health {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 160px)) minmax(280px, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.signature-sample {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.signature-sample__head span,
.signature-sample__body small {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.signature-sample__head strong {
  display: block;
  margin-top: 6px;
  color: #111827;
}

.signature-sample__body {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.signature-sample__body code {
  display: block;
  max-width: 100%;
  padding: 8px 10px;
  overflow-x: auto;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
}

.callback-card,
.callback-tip {
  min-height: 78px;
  padding: 12px 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}

.callback-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.callback-card strong {
  display: block;
  margin-top: 10px;
  color: #111827;
  font-size: 22px;
}

.callback-card--green {
  background: #f6fffb;
}

.callback-card--blue {
  background: #f8fbff;
}

.callback-card--red {
  background: #fff8f8;
}

.callback-line strong,
.callback-line span,
.callback-line p {
  display: block;
}

.callback-line span {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.callback-line p {
  margin: 8px 0;
  color: #475569;
}

.callback-inspect {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin: 10px 0 12px;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
}

.callback-inspect div {
  min-width: 0;
}

.callback-inspect dt {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.4;
}

.callback-inspect dd {
  margin: 3px 0 0;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.45;
  word-break: break-all;
}

.sync-status-hint {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.sync-basis {
  display: grid;
  gap: 4px;
}

.sync-basis strong {
  color: #111827;
  word-break: break-all;
}

.sync-basis span,
.sync-basis small {
  color: #64748b;
}

.latest-user-message {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fbfcff;
}

.latest-user-message__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.latest-user-message__head strong {
  color: #111827;
  line-height: 1.45;
}

.latest-user-message__head small {
  flex-shrink: 0;
  color: #94a3b8;
  font-size: 12px;
}

.latest-user-message p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
  word-break: break-word;
}

.refund-record-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
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

.refund-record-card__meta .refund-record-card__error {
  color: #dc2626;
  font-weight: 600;
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

.refund-confirm-alert {
  margin-bottom: 16px;
}

.refund-confirm-amount {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  background: #fff8f8;
}

.refund-confirm-amount span,
.refund-confirm-amount small {
  color: #64748b;
  font-size: 13px;
}

.refund-confirm-amount strong {
  color: #dc2626;
  font-size: 28px;
  line-height: 1.15;
}

.summary-card {
  min-height: 112px;
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
  font-size: 22px;
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

.summary-card--green {
  background: #f6fffb;
}

.summary-card--red {
  background: #fff8f8;
}

.summary-card--orange {
  background: #fffaf2;
}

.payment-health {
  display: grid;
  grid-template-columns: 180px 180px minmax(280px, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.health-card,
.channel-strip {
  padding: 12px 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fafafa;
}

.health-card span,
.channel-title {
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

.channel-strip {
  min-width: 0;
}

.channel-title {
  margin-bottom: 8px;
}

@media (max-width: 1200px) {
  .payment-summary {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }

  .payment-health {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }

  .channel-strip {
    grid-column: 1 / -1;
  }

  .access-panel,
  .callback-health,
  .signature-sample {
    grid-template-columns: 1fr;
  }
}
</style>
