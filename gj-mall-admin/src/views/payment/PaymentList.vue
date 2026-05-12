<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  getPaymentDetail,
  getPaymentPage,
  markPaymentFailed,
  markPaymentPaid,
  refundPayment,
  type ApiId,
  type PaymentQuery,
  type PaymentRecord,
} from '@/api/payment'

const route = useRoute()
const loading = ref(false)
const detailLoading = ref(false)
const actionId = ref<ApiId>()
const payments = ref<PaymentRecord[]>([])
const detailOpen = ref(false)
const currentPayment = ref<PaymentRecord>()

const refundOpen = ref(false)
const refundSaving = ref(false)
const refundTarget = ref<PaymentRecord>()
const refundForm = reactive({
  amount: undefined as number | undefined,
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
  fetchPayments()
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
        await fetchPayments()
        await refreshCurrentPayment(record.id)
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
  if (!refundTarget.value) {
    return
  }
  if (!refundForm.amount || refundForm.amount <= 0) {
    message.error('请输入退款金额')
    return
  }
  refundSaving.value = true
  try {
    await refundPayment(refundTarget.value.id, {
      amount: refundForm.amount,
      reason: refundForm.reason.trim() || undefined,
    })
    message.success('退款成功')
    refundOpen.value = false
    await fetchPayments()
    await refreshCurrentPayment(refundTarget.value.id)
  } finally {
    refundSaving.value = false
  }
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

function canMarkFailed(record: PaymentRecord) {
  return record.status === 0
}

function canRefund(record: PaymentRecord) {
  return record.status === 1 && record.orderStatus !== 6
}

function toPayment(record: Record<string, any>) {
  return record as PaymentRecord
}
</script>

<template>
  <a-card title="支付退款管理" :bordered="false">
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
        </a-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">回调/处理记录</h4>
          <a-textarea
            :value="currentPayment.callbackData || '--'"
            :rows="8"
            readonly
          />
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: flex-end">
          <a-space>
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
      <a-form-item label="支付流水">
        <a-input :value="refundTarget?.payNo" disabled />
      </a-form-item>
      <a-form-item label="退款金额">
        <a-input-number v-model:value="refundForm.amount" :min="0.01" style="width: 100%" />
      </a-form-item>
      <a-form-item label="退款原因">
        <a-textarea v-model:value="refundForm.reason" :rows="4" placeholder="请输入退款原因" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
