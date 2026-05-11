<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  cancelOrder,
  deliverOrder,
  getOrderFulfillmentSummary,
  getOrderPage,
  type ApiId,
  type OrderFulfillmentSummary,
  type OrderItem,
  type OrderQueryParams,
  type OrderRecord,
} from '@/api/order'

const loading = ref(false)
const summaryLoading = ref(false)
const delivering = ref(false)
const actionId = ref<ApiId>()
const detailOpen = ref(false)
const deliverOpen = ref(false)
const currentOrder = ref<OrderRecord>()
const orderList = ref<OrderRecord[]>([])
const summary = reactive<OrderFulfillmentSummary>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<OrderQueryParams>({
  orderNo: '',
  userId: undefined,
  status: undefined,
  deliveryNo: '',
})

const deliverForm = reactive({
  deliveryCompany: '顺丰速运',
  deliveryNo: '',
  deliveryRemark: '',
})

const columns: TableColumnsType<OrderRecord> = [
  { title: '订单信息', dataIndex: 'orderNo', key: 'orderNo', width: 280 },
  { title: '用户', dataIndex: 'userId', key: 'userId', width: 120 },
  { title: '收货人', dataIndex: 'receiver', key: 'receiver', width: 180 },
  { title: '金额', dataIndex: 'payAmount', key: 'payAmount', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '履约', dataIndex: 'deliveryNo', key: 'fulfillment', width: 180 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 240 },
]

const itemColumns: TableColumnsType<OrderItem> = [
  { title: '商品', dataIndex: 'skuName', key: 'skuName' },
  { title: '单价', dataIndex: 'price', key: 'price', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90 },
  { title: '小计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
]

const statusOptions = [
  { label: '待付款', value: 0, color: 'warning' },
  { label: '待发货', value: 1, color: 'processing' },
  { label: '待收货', value: 2, color: 'blue' },
  { label: '已完成', value: 3, color: 'success' },
  { label: '已取消', value: 4, color: 'default' },
  { label: '退款中', value: 5, color: 'purple' },
  { label: '已退款', value: 6, color: 'default' },
]

const statusMap = computed(() => {
  return statusOptions.reduce<Record<number, { label: string; color: string }>>((acc, item) => {
    acc[item.value] = { label: item.label, color: item.color }
    return acc
  }, {})
})

onMounted(fetchOrderData)

async function fetchOrderData() {
  await Promise.all([fetchList(), fetchSummary()])
}

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const res = await getOrderFulfillmentSummary()
    Object.assign(summary, res.data || {})
  } finally {
    summaryLoading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getOrderPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      orderNo: filters.orderNo?.trim() || undefined,
      userId: normalizeId(filters.userId),
      status: filters.status,
      deliveryNo: filters.deliveryNo?.trim() || undefined,
    })
    orderList.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchOrderData()
}

async function handleReset() {
  filters.orderNo = ''
  filters.userId = undefined
  filters.status = undefined
  filters.deliveryNo = ''
  pagination.current = 1
  await fetchOrderData()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchList()
}

async function applyStatus(status?: number) {
  filters.status = status
  pagination.current = 1
  await fetchList()
}

function handleView(record: OrderRecord) {
  currentOrder.value = record
  detailOpen.value = true
}

function handleDeliver(record: OrderRecord) {
  currentOrder.value = record
  deliverForm.deliveryCompany = record.deliveryCompany || '顺丰速运'
  deliverForm.deliveryNo = ''
  deliverForm.deliveryRemark = ''
  deliverOpen.value = true
}

async function submitDeliver() {
  if (!currentOrder.value) {
    return
  }
  if (!deliverForm.deliveryCompany.trim()) {
    message.warning('请输入物流公司')
    return
  }
  if (!deliverForm.deliveryNo.trim()) {
    message.warning('请输入物流单号')
    return
  }
  delivering.value = true
  actionId.value = currentOrder.value.id
  try {
    const res = await deliverOrder(currentOrder.value.id, {
      deliveryCompany: deliverForm.deliveryCompany.trim(),
      deliveryNo: deliverForm.deliveryNo.trim(),
      deliveryRemark: deliverForm.deliveryRemark.trim() || undefined,
    })
    message.success('发货成功')
    deliverOpen.value = false
    replaceOrder(res.data)
    currentOrder.value = res.data
    await fetchSummary()
  } finally {
    delivering.value = false
    actionId.value = undefined
  }
}

function handleCancel(record: OrderRecord) {
  Modal.confirm({
    title: '确认取消这个待付款订单吗？',
    content: '取消后会释放已锁定库存。',
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await cancelOrder(record.id)
        message.success('订单已取消')
        await fetchList()
        await fetchSummary()
        if (currentOrder.value?.id === record.id) {
          currentOrder.value = orderList.value.find((item) => item.id === record.id)
        }
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function replaceOrder(nextOrder: OrderRecord) {
  const index = orderList.value.findIndex((item) => String(item.id) === String(nextOrder.id))
  if (index >= 0) {
    orderList.value.splice(index, 1, nextOrder)
  } else {
    fetchList()
  }
}

function formatPrice(price?: number) {
  if (price === undefined || price === null) {
    return '--'
  }
  return `¥${Number(price).toFixed(2)}`
}

function formatAmount(price?: number) {
  if (price === undefined || price === null) {
    return '¥0.00'
  }
  return `¥${Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatSpecs(specs?: Record<string, string>) {
  if (!specs) {
    return '--'
  }
  const items = Object.entries(specs).map(([key, value]) => `${key}: ${value}`)
  return items.length ? items.join(' / ') : '--'
}

function formatReceiver(record?: OrderRecord) {
  const receiver = record?.receiver
  if (!receiver) {
    return '--'
  }
  return [receiver.receiver, receiver.phone].filter(Boolean).join(' / ') || '--'
}

function formatAddress(record?: OrderRecord) {
  const receiver = record?.receiver
  if (!receiver) {
    return '--'
  }
  return [receiver.province, receiver.city, receiver.district, receiver.detail]
    .filter(Boolean)
    .join(' ') || '--'
}

function statusLabel(status?: number, fallback?: string) {
  if (status === undefined || status === null) {
    return fallback || '未知'
  }
  return statusMap.value[status]?.label || fallback || '未知'
}

function statusColor(status?: number) {
  if (status === undefined || status === null) {
    return 'default'
  }
  return statusMap.value[status]?.color || 'default'
}

function payTypeLabel(payType?: number) {
  const map: Record<number, string> = {
    1: '微信支付',
    2: '支付宝',
    3: '余额',
    9: 'Mock 支付',
  }
  return payType === undefined || payType === null ? '--' : map[payType] || `渠道 ${payType}`
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function canDeliver(record: OrderRecord) {
  return record.status === 1
}

function canCancel(record: OrderRecord) {
  return record.status === 0
}

function toOrder(record: Record<string, any>) {
  return record as OrderRecord
}
</script>

<template>
  <a-card title="订单管理" :bordered="false">
    <div class="order-summary" :class="{ 'order-summary-loading': summaryLoading }">
      <button type="button" class="summary-item" :class="{ active: filters.status === undefined }" @click="applyStatus(undefined)">
        <span>订单总数</span>
        <strong>{{ formatNumber(summary.totalOrderCount) }}</strong>
        <em>今日 {{ formatNumber(summary.todayOrderCount) }}</em>
      </button>
      <button type="button" class="summary-item warning" :class="{ active: filters.status === 1 }" @click="applyStatus(1)">
        <span>待发货</span>
        <strong>{{ formatNumber(summary.pendingDeliveryCount) }}</strong>
        <em>{{ formatAmount(summary.pendingDeliveryAmount) }}</em>
      </button>
      <button type="button" class="summary-item processing" :class="{ active: filters.status === 2 }" @click="applyStatus(2)">
        <span>待收货</span>
        <strong>{{ formatNumber(summary.pendingReceiveCount) }}</strong>
        <em>物流跟进</em>
      </button>
      <button type="button" class="summary-item success" :class="{ active: filters.status === 3 }" @click="applyStatus(3)">
        <span>已完成</span>
        <strong>{{ formatNumber(summary.completedCount) }}</strong>
        <em>{{ formatAmount(summary.totalPayAmount) }}</em>
      </button>
    </div>

    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.orderNo"
          allow-clear
          placeholder="搜索订单号"
          style="width: 240px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input
          v-model:value="filters.userId"
          allow-clear
          placeholder="用户 ID"
          style="width: 160px"
        />
      </a-form-item>
      <a-form-item>
        <a-input
          v-model:value="filters.deliveryNo"
          allow-clear
          placeholder="物流单号"
          style="width: 180px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="订单状态"
          style="width: 160px"
        >
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
      :data-source="orderList"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1450 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'orderNo'">
          <div style="font-weight: 600; color: #1f1f1f">{{ record.orderNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.items?.length || 0 }} 件商品
          </div>
        </template>

        <template v-else-if="column.key === 'userId'">
          ID {{ record.userId }}
        </template>

        <template v-else-if="column.key === 'receiver'">
          <div>{{ formatReceiver(toOrder(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ formatAddress(toOrder(record)) }}
          </div>
        </template>

        <template v-else-if="column.key === 'payAmount'">
          <div style="font-weight: 600">{{ formatPrice(record.payAmount) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            总额 {{ formatPrice(record.totalAmount) }}
          </div>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ statusLabel(record.status, record.statusDesc) }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'fulfillment'">
          <template v-if="record.deliveryNo">
            <div style="font-weight: 600; color: #1f1f1f">{{ record.deliveryCompany || '--' }}</div>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">{{ record.deliveryNo }}</div>
          </template>
          <span v-else style="color: #bfbfbf">未发货</span>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toOrder(record))">查看</a-button>
            <a-button
              v-if="canDeliver(toOrder(record))"
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleDeliver(toOrder(record))"
            >
              发货
            </a-button>
            <a-button
              v-if="canCancel(toOrder(record))"
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleCancel(toOrder(record))"
            >
              取消
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="deliverOpen"
    title="订单发货"
    :confirm-loading="delivering"
    width="560px"
    @ok="submitDeliver"
  >
    <template v-if="currentOrder">
      <a-descriptions :column="1" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="订单号">{{ currentOrder.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="收货人">{{ formatReceiver(currentOrder) }}</a-descriptions-item>
        <a-descriptions-item label="收货地址">{{ formatAddress(currentOrder) }}</a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="物流公司">
          <a-select v-model:value="deliverForm.deliveryCompany" placeholder="请选择物流公司">
            <a-select-option value="顺丰速运">顺丰速运</a-select-option>
            <a-select-option value="京东物流">京东物流</a-select-option>
            <a-select-option value="中通快递">中通快递</a-select-option>
            <a-select-option value="圆通速递">圆通速递</a-select-option>
            <a-select-option value="韵达快递">韵达快递</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="物流单号">
          <a-input
            v-model:value="deliverForm.deliveryNo"
            allow-clear
            :maxlength="64"
            placeholder="请输入物流单号"
          />
        </a-form-item>
        <a-form-item label="发货备注">
          <a-textarea
            v-model:value="deliverForm.deliveryRemark"
            :maxlength="255"
            show-count
            :rows="3"
            placeholder="例如：仓库已复核，易碎品已加固"
          />
        </a-form-item>
      </a-form>
    </template>
  </a-modal>

  <a-drawer v-model:open="detailOpen" title="订单详情" width="860" destroy-on-close>
    <template v-if="currentOrder">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="用户 ID">{{ currentOrder.userId }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(currentOrder.status)">
            {{ statusLabel(currentOrder.status, currentOrder.statusDesc) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="支付金额">{{ formatPrice(currentOrder.payAmount) }}</a-descriptions-item>
        <a-descriptions-item label="商品总额">{{ formatPrice(currentOrder.totalAmount) }}</a-descriptions-item>
        <a-descriptions-item label="运费">{{ formatPrice(currentOrder.freightAmount) }}</a-descriptions-item>
        <a-descriptions-item label="优惠">{{ formatPrice(currentOrder.couponAmount) }}</a-descriptions-item>
        <a-descriptions-item label="支付方式">{{ payTypeLabel(currentOrder.payType) }}</a-descriptions-item>
        <a-descriptions-item label="支付时间">{{ currentOrder.payTime || '--' }}</a-descriptions-item>
        <a-descriptions-item label="物流公司">{{ currentOrder.deliveryCompany || '--' }}</a-descriptions-item>
        <a-descriptions-item label="物流单号">{{ currentOrder.deliveryNo || '--' }}</a-descriptions-item>
        <a-descriptions-item label="发货时间">{{ currentOrder.deliveryTime || '--' }}</a-descriptions-item>
        <a-descriptions-item label="收货时间">{{ currentOrder.receiveTime || '--' }}</a-descriptions-item>
        <a-descriptions-item label="下单时间">{{ currentOrder.createTime || '--' }}</a-descriptions-item>
        <a-descriptions-item label="发货备注">{{ currentOrder.deliveryRemark || '--' }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{ currentOrder.remark || '--' }}</a-descriptions-item>
      </a-descriptions>

      <div style="margin-top: 20px">
        <h4 style="margin-bottom: 12px">收货信息</h4>
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="收货人">{{ currentOrder.receiver?.receiver || '--' }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ currentOrder.receiver?.phone || '--' }}</a-descriptions-item>
          <a-descriptions-item label="邮编">{{ currentOrder.receiver?.postCode || '--' }}</a-descriptions-item>
          <a-descriptions-item label="地址">{{ formatAddress(currentOrder) }}</a-descriptions-item>
        </a-descriptions>
      </div>

      <div style="margin-top: 20px">
        <h4 style="margin-bottom: 12px">商品明细</h4>
        <a-table
          row-key="id"
          size="small"
          :columns="itemColumns"
          :data-source="currentOrder.items || []"
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
                    {{ formatSpecs(record.specData) }}
                  </div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'price'">
              {{ formatPrice(record.price) }}
            </template>
            <template v-else-if="column.key === 'totalAmount'">
              {{ formatPrice(record.totalAmount) }}
            </template>
          </template>
        </a-table>
      </div>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end">
        <a-space>
          <a-button
            v-if="canDeliver(currentOrder)"
            type="primary"
            :loading="actionId === currentOrder.id"
            @click="handleDeliver(currentOrder)"
          >
            发货
          </a-button>
          <a-button
            v-if="canCancel(currentOrder)"
            danger
            :loading="actionId === currentOrder.id"
            @click="handleCancel(currentOrder)"
          >
            取消订单
          </a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<style scoped>
.order-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
  opacity: 1;
  transition: opacity 0.2s ease;
}

.order-summary-loading {
  opacity: 0.72;
}

.summary-item {
  min-height: 92px;
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.summary-item:hover,
.summary-item.active {
  border-color: #1677ff;
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.12);
}

.summary-item span,
.summary-item em {
  display: block;
  color: #8c8c8c;
  font-style: normal;
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin: 7px 0 5px;
  color: #1f1f1f;
  font-size: 24px;
  line-height: 1.1;
}

.summary-item.warning strong {
  color: #d48806;
}

.summary-item.processing strong {
  color: #0958d9;
}

.summary-item.success strong {
  color: #389e0d;
}

@media (max-width: 1200px) {
  .order-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .order-summary {
    grid-template-columns: 1fr;
  }
}
</style>
