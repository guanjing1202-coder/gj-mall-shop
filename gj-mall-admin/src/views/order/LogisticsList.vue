<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
  ShopOutlined,
} from '@ant-design/icons-vue'
import type { ApiId, OrderRecord } from '@/api/order'
import {
  createDeliveryCompany,
  deleteDeliveryCompany,
  deliverLogisticsOrder,
  getDeliveryCompanyOptions,
  getDeliveryCompanyPage,
  getLogisticsOrders,
  getLogisticsSummary,
  updateDeliveryCompany,
  updateDeliveryCompanyStatus,
  type DeliveryCompanyPayload,
  type DeliveryCompanyRecord,
  type DeliveryCompanyQuery,
  type LogisticsOrderQuery,
  type LogisticsSummary,
} from '@/api/logistics'

const activeTab = ref('orders')
const statusSegment = ref(-1)
const orderLoading = ref(false)
const companyLoading = ref(false)
const summaryLoading = ref(false)
const deliverOpen = ref(false)
const delivering = ref(false)
const companyModalOpen = ref(false)
const companySaving = ref(false)
const companyEditing = ref(false)
const currentOrder = ref<OrderRecord>()
const companyFormRef = ref<FormInstance>()
const orders = ref<OrderRecord[]>([])
const companies = ref<DeliveryCompanyRecord[]>([])
const companyOptions = ref<DeliveryCompanyRecord[]>([])
const summary = reactive<LogisticsSummary>({})

const orderPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
})

const companyPagination = reactive({
  current: 1,
  pageSize: 8,
  total: 0,
  showSizeChanger: true,
})

const orderFilters = reactive<LogisticsOrderQuery>({
  status: undefined,
  orderNo: '',
  userId: undefined,
  deliveryNo: '',
  deliveryCompany: undefined,
})

const companyFilters = reactive<DeliveryCompanyQuery>({
  keyword: '',
  status: undefined,
})

const deliverForm = reactive({
  deliveryCompany: '',
  deliveryNo: '',
  deliveryRemark: '',
})

const companyForm = reactive<DeliveryCompanyPayload>({
  code: '',
  name: '',
  contactPhone: '',
  sort: 100,
  status: 1,
})

const orderColumns: TableColumnsType<OrderRecord> = [
  { title: '订单', dataIndex: 'orderNo', key: 'order', width: 280 },
  { title: '收货信息', dataIndex: 'receiver', key: 'receiver', width: 260 },
  { title: '金额', dataIndex: 'payAmount', key: 'amount', width: 140 },
  { title: '履约状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '物流信息', dataIndex: 'deliveryNo', key: 'delivery', width: 240 },
  { title: '时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 130 },
]

const companyColumns: TableColumnsType<DeliveryCompanyRecord> = [
  { title: '物流公司', dataIndex: 'name', key: 'company', width: 260 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contact', width: 160 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 },
]

const statusOptions = [
  { label: '全部履约', value: -1 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
]

const defaultCompanyName = computed(() => companyOptions.value[0]?.name || '顺丰速运')

onMounted(async () => {
  await Promise.all([fetchSummary(), fetchOrders(), fetchCompanies(), fetchCompanyOptions()])
})

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const res = await getLogisticsSummary()
    Object.assign(summary, res.data || {})
  } finally {
    summaryLoading.value = false
  }
}

async function fetchOrders() {
  orderLoading.value = true
  try {
    const res = await getLogisticsOrders({
      pageNum: orderPagination.current,
      pageSize: orderPagination.pageSize,
      status: orderFilters.status,
      orderNo: textOrUndefined(orderFilters.orderNo),
      userId: normalizeId(orderFilters.userId),
      deliveryNo: textOrUndefined(orderFilters.deliveryNo),
      deliveryCompany: orderFilters.deliveryCompany,
    })
    orders.value = res.data.list || []
    orderPagination.total = Number(res.data.total || 0)
  } finally {
    orderLoading.value = false
  }
}

async function fetchCompanies() {
  companyLoading.value = true
  try {
    const res = await getDeliveryCompanyPage({
      pageNum: companyPagination.current,
      pageSize: companyPagination.pageSize,
      keyword: textOrUndefined(companyFilters.keyword),
      status: companyFilters.status,
    })
    companies.value = res.data.list || []
    companyPagination.total = Number(res.data.total || 0)
  } finally {
    companyLoading.value = false
  }
}

async function fetchCompanyOptions() {
  const res = await getDeliveryCompanyOptions()
  companyOptions.value = res.data || []
}

async function handleOrderSearch() {
  orderPagination.current = 1
  await Promise.all([fetchOrders(), fetchSummary()])
}

async function handleOrderReset() {
  orderFilters.status = undefined
  statusSegment.value = -1
  orderFilters.orderNo = ''
  orderFilters.userId = undefined
  orderFilters.deliveryNo = ''
  orderFilters.deliveryCompany = undefined
  orderPagination.current = 1
  await handleOrderSearch()
}

async function applyStatus(status?: number) {
  orderFilters.status = status
  statusSegment.value = status ?? -1
  orderPagination.current = 1
  await fetchOrders()
}

async function handleStatusSegmentChange(value: string | number) {
  const nextValue = Number(value)
  await applyStatus(nextValue === -1 ? undefined : nextValue)
}

async function handleOrderTableChange(page: { current?: number; pageSize?: number }) {
  orderPagination.current = page.current || 1
  orderPagination.pageSize = page.pageSize || 10
  await fetchOrders()
}

async function handleCompanySearch() {
  companyPagination.current = 1
  await fetchCompanies()
}

async function handleCompanyReset() {
  companyFilters.keyword = ''
  companyFilters.status = undefined
  companyPagination.current = 1
  await fetchCompanies()
}

async function handleCompanyTableChange(page: { current?: number; pageSize?: number }) {
  companyPagination.current = page.current || 1
  companyPagination.pageSize = page.pageSize || 8
  await fetchCompanies()
}

function openDeliver(record: OrderRecord) {
  currentOrder.value = record
  deliverForm.deliveryCompany = record.deliveryCompany || defaultCompanyName.value
  deliverForm.deliveryNo = ''
  deliverForm.deliveryRemark = ''
  deliverOpen.value = true
}

async function submitDeliver() {
  if (!currentOrder.value) {
    return
  }
  if (!deliverForm.deliveryCompany.trim()) {
    message.warning('请选择物流公司')
    return
  }
  if (!deliverForm.deliveryNo.trim()) {
    message.warning('请输入物流单号')
    return
  }
  delivering.value = true
  try {
    await deliverLogisticsOrder(currentOrder.value.id, {
      deliveryCompany: deliverForm.deliveryCompany.trim(),
      deliveryNo: deliverForm.deliveryNo.trim(),
      deliveryRemark: deliverForm.deliveryRemark.trim() || undefined,
    })
    message.success('订单已发货')
    deliverOpen.value = false
    await Promise.all([fetchOrders(), fetchSummary()])
  } finally {
    delivering.value = false
  }
}

function openCreateCompany() {
  companyEditing.value = false
  resetCompanyForm()
  companyModalOpen.value = true
}

function openEditCompany(record: DeliveryCompanyRecord) {
  companyEditing.value = true
  Object.assign(companyForm, {
    id: record.id,
    code: record.code,
    name: record.name,
    contactPhone: record.contactPhone || '',
    sort: record.sort ?? 100,
    status: record.status ?? 1,
  })
  companyModalOpen.value = true
}

async function submitCompany() {
  await companyFormRef.value?.validate()
  companySaving.value = true
  try {
    if (companyEditing.value) {
      await updateDeliveryCompany({ ...companyForm })
      message.success('物流公司已更新')
    } else {
      await createDeliveryCompany({ ...companyForm })
      message.success('物流公司已新增')
    }
    companyModalOpen.value = false
    await Promise.all([fetchCompanies(), fetchCompanyOptions(), fetchSummary()])
  } finally {
    companySaving.value = false
  }
}

async function handleCompanySwitchChange(record: DeliveryCompanyRecord, checked: unknown) {
  try {
    await updateDeliveryCompanyStatus(record.id, Boolean(checked) ? 1 : 0)
    message.success(Boolean(checked) ? '已启用' : '已停用')
    await Promise.all([fetchCompanies(), fetchCompanyOptions(), fetchSummary()])
  } catch {
    await fetchCompanies()
  }
}

function handleDeleteCompany(record: DeliveryCompanyRecord) {
  Modal.confirm({
    title: '删除物流公司',
    content: `确认删除「${record.name}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteDeliveryCompany(record.id)
      message.success('物流公司已删除')
      await Promise.all([fetchCompanies(), fetchCompanyOptions(), fetchSummary()])
    },
  })
}

function resetCompanyForm() {
  Object.assign(companyForm, {
    id: undefined,
    code: '',
    name: '',
    contactPhone: '',
    sort: 100,
    status: 1,
  })
  companyFormRef.value?.clearValidate()
}

function formatAmount(value?: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatReceiver(record?: OrderRecord) {
  const receiver = record?.receiver
  return [receiver?.receiver, receiver?.phone].filter(Boolean).join(' / ') || '--'
}

function formatAddress(record?: OrderRecord) {
  const receiver = record?.receiver
  return [receiver?.province, receiver?.city, receiver?.district, receiver?.detail].filter(Boolean).join(' ') || '--'
}

function statusColor(status?: number) {
  if (status === 1) return 'warning'
  if (status === 2) return 'processing'
  return 'default'
}

function canDeliver(record: OrderRecord) {
  return record.status === 1
}

function textOrUndefined(value?: string) {
  const text = String(value || '').trim()
  return text || undefined
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function toOrder(record: Record<string, any>) {
  return record as OrderRecord
}

function toCompany(record: Record<string, any>) {
  return record as DeliveryCompanyRecord
}
</script>

<template>
  <div class="logistics-page">
    <section class="logistics-heading">
      <div>
        <div class="page-kicker">Fulfillment</div>
        <h1>物流/发货管理</h1>
      </div>
      <a-space>
        <a-button @click="fetchOrders">
          <template #icon><ReloadOutlined /></template>
          刷新订单
        </a-button>
        <a-button type="primary" @click="openCreateCompany">
          <template #icon><PlusOutlined /></template>
          新增物流公司
        </a-button>
      </a-space>
    </section>

    <section class="metric-grid" :class="{ faded: summaryLoading }">
      <button type="button" class="metric-card accent-amber" :class="{ active: orderFilters.status === 1 }" @click="applyStatus(1)">
        <span>待发货</span>
        <strong>{{ formatNumber(summary.pendingDeliveryCount) }}</strong>
        <em>{{ formatAmount(summary.pendingDeliveryAmount) }}</em>
      </button>
      <button type="button" class="metric-card accent-blue" :class="{ active: orderFilters.status === 2 }" @click="applyStatus(2)">
        <span>待收货</span>
        <strong>{{ formatNumber(summary.pendingReceiveCount) }}</strong>
        <em>运输中订单</em>
      </button>
      <div class="metric-card accent-teal">
        <span>今日发货</span>
        <strong>{{ formatNumber(summary.shippedTodayCount) }}</strong>
        <em>已交付承运</em>
      </div>
      <div class="metric-card accent-slate">
        <span>启用物流公司</span>
        <strong>{{ formatNumber(summary.activeCompanyCount) }}</strong>
        <em>可用于发货</em>
      </div>
    </section>

    <section class="logistics-panel">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="orders" tab="发货订单">
          <div class="filter-row">
            <a-segmented
              v-model:value="statusSegment"
              :options="statusOptions"
              @change="handleStatusSegmentChange"
            />
            <a-input v-model:value="orderFilters.orderNo" allow-clear placeholder="订单号" class="filter-input" @pressEnter="handleOrderSearch">
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-input v-model:value="orderFilters.deliveryNo" allow-clear placeholder="物流单号" class="filter-input" @pressEnter="handleOrderSearch" />
            <a-select v-model:value="orderFilters.deliveryCompany" allow-clear placeholder="物流公司" class="filter-select">
              <a-select-option v-for="item in companyOptions" :key="item.id" :value="item.name">
                {{ item.name }}
              </a-select-option>
            </a-select>
            <a-space class="toolbar-actions">
              <a-button type="primary" @click="handleOrderSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleOrderReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </div>

          <a-table
            row-key="id"
            :columns="orderColumns"
            :data-source="orders"
            :loading="orderLoading"
            :pagination="orderPagination"
            :scroll="{ x: 1380 }"
            class="logistics-table"
            @change="handleOrderTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'">
                <div class="primary-text">{{ record.orderNo }}</div>
                <div class="muted-text">{{ record.items?.length || 0 }} 件商品 / 用户 ID {{ record.userId }}</div>
              </template>
              <template v-else-if="column.key === 'receiver'">
                <div>{{ formatReceiver(toOrder(record)) }}</div>
                <div class="muted-text address-line">{{ formatAddress(toOrder(record)) }}</div>
              </template>
              <template v-else-if="column.key === 'amount'">
                <strong>{{ formatAmount(record.payAmount) }}</strong>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="statusColor(record.status)">{{ record.statusDesc }}</a-tag>
              </template>
              <template v-else-if="column.key === 'delivery'">
                <template v-if="record.deliveryNo">
                  <div class="primary-text">{{ record.deliveryCompany || '--' }}</div>
                  <code class="delivery-no">{{ record.deliveryNo }}</code>
                </template>
                <span v-else class="empty-text">未发货</span>
              </template>
              <template v-else-if="column.key === 'time'">
                <div>{{ record.deliveryTime || record.payTime || record.createTime || '--' }}</div>
                <div class="muted-text">{{ record.deliveryTime ? '发货时间' : '订单时间' }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="canDeliver(toOrder(record))"
                  type="primary"
                  size="small"
                  @click="openDeliver(toOrder(record))"
                >
                  <template #icon><SendOutlined /></template>
                  发货
                </a-button>
                <a-tag v-else color="processing">运输中</a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="companies" tab="物流公司">
          <div class="filter-row">
            <a-input v-model:value="companyFilters.keyword" allow-clear placeholder="搜索编码 / 名称 / 电话" class="filter-input wide" @pressEnter="handleCompanySearch">
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-select v-model:value="companyFilters.status" allow-clear placeholder="状态" class="filter-select compact">
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">停用</a-select-option>
            </a-select>
            <a-space class="toolbar-actions">
              <a-button type="primary" @click="handleCompanySearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleCompanyReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
              <a-button @click="openCreateCompany">
                <template #icon><PlusOutlined /></template>
                新增
              </a-button>
            </a-space>
          </div>

          <a-table
            row-key="id"
            :columns="companyColumns"
            :data-source="companies"
            :loading="companyLoading"
            :pagination="companyPagination"
            :scroll="{ x: 980 }"
            class="logistics-table"
            @change="handleCompanyTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'company'">
                <div class="company-title">
                  <ShopOutlined />
                  <span>{{ record.name }}</span>
                </div>
                <code class="company-code">{{ record.code }}</code>
              </template>
              <template v-else-if="column.key === 'contact'">
                {{ record.contactPhone || '--' }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-switch
                  :checked="record.status === 1"
                  checked-children="启"
                  un-checked-children="停"
                  @change="handleCompanySwitchChange(toCompany(record), $event)"
                />
              </template>
              <template v-else-if="column.key === 'updateTime'">
                <span class="muted-text">{{ record.updateTime || '--' }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="openEditCompany(toCompany(record))">
                    <template #icon><EditOutlined /></template>
                    编辑
                  </a-button>
                  <a-button type="link" size="small" danger @click="handleDeleteCompany(toCompany(record))">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </section>

    <a-modal v-model:open="deliverOpen" title="订单发货" width="620px" :confirm-loading="delivering" @ok="submitDeliver">
      <template v-if="currentOrder">
        <a-descriptions :column="1" size="small" bordered class="deliver-desc">
          <a-descriptions-item label="订单号">{{ currentOrder.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="收货人">{{ formatReceiver(currentOrder) }}</a-descriptions-item>
          <a-descriptions-item label="收货地址">{{ formatAddress(currentOrder) }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical">
          <a-form-item label="物流公司">
            <a-select v-model:value="deliverForm.deliveryCompany" placeholder="请选择物流公司">
              <a-select-option v-for="item in companyOptions" :key="item.id" :value="item.name">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="物流单号">
            <a-input v-model:value="deliverForm.deliveryNo" allow-clear :maxlength="64" placeholder="请输入物流单号" />
          </a-form-item>
          <a-form-item label="发货备注">
            <a-textarea v-model:value="deliverForm.deliveryRemark" :rows="3" :maxlength="255" show-count placeholder="可选" />
          </a-form-item>
        </a-form>
      </template>
    </a-modal>

    <a-modal
      v-model:open="companyModalOpen"
      :title="companyEditing ? '编辑物流公司' : '新增物流公司'"
      width="620px"
      :confirm-loading="companySaving"
      @ok="submitCompany"
    >
      <a-form ref="companyFormRef" :model="companyForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item name="name" label="公司名称" :rules="[{ required: true, message: '请输入公司名称' }]">
              <a-input v-model:value="companyForm.name" placeholder="例如：顺丰速运" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="code" label="公司编码" :rules="[{ required: true, message: '请输入公司编码' }]">
              <a-input v-model:value="companyForm.code" placeholder="例如：SF" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="contactPhone" label="联系电话">
              <a-input v-model:value="companyForm.contactPhone" placeholder="例如：95338" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="sort" label="排序">
              <a-input-number v-model:value="companyForm.sort" :min="0" :max="9999" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="status" label="状态">
              <a-radio-group v-model:value="companyForm.status" button-style="solid">
                <a-radio-button :value="1">启用</a-radio-button>
                <a-radio-button :value="0">停用</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.logistics-page {
  min-height: calc(100vh - 104px);
  padding: 4px;
  color: #1f2933;
}

.logistics-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 22px 24px;
  border: 1px solid #e5ebf3;
  border-left: 4px solid #2563eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(20, 31, 43, 0.06);
}

.page-kicker {
  margin-bottom: 4px;
  color: #5f6f7f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.logistics-heading h1 {
  margin: 0;
  color: #17212b;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card {
  min-height: 92px;
  padding: 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
  text-align: left;
  box-shadow: 0 8px 20px rgba(20, 31, 43, 0.04);
}

button.metric-card {
  cursor: pointer;
}

.metric-card.active,
button.metric-card:hover {
  border-color: #2563eb;
  box-shadow: 0 10px 26px rgba(37, 99, 235, 0.12);
}

.metric-card span,
.metric-card em {
  display: block;
  color: #657384;
  font-size: 13px;
  font-style: normal;
}

.metric-card strong {
  display: block;
  margin: 10px 0 6px;
  color: #17212b;
  font-size: 26px;
  line-height: 1;
}

.accent-amber {
  border-top: 3px solid #b7791f;
}

.accent-blue {
  border-top: 3px solid #2563eb;
}

.accent-teal {
  border-top: 3px solid #0f766e;
}

.accent-slate {
  border-top: 3px solid #526171;
}

.faded {
  opacity: 0.72;
}

.logistics-panel {
  padding: 18px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(20, 31, 43, 0.06);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.filter-input {
  width: 210px;
}

.filter-input.wide {
  width: 300px;
}

.filter-select {
  width: 168px;
}

.filter-select.compact {
  width: 120px;
}

.toolbar-actions {
  margin-left: auto;
}

.logistics-table :deep(.ant-table-thead > tr > th) {
  background: #f7f9fc;
  color: #526171;
  font-weight: 700;
}

.primary-text {
  color: #17212b;
  font-weight: 700;
}

.muted-text {
  margin-top: 4px;
  color: #7b8794;
  font-size: 12px;
}

.address-line {
  max-width: 230px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delivery-no,
.company-code {
  display: inline-block;
  margin-top: 5px;
  padding: 4px 7px;
  border-radius: 6px;
  background: #f0f4ff;
  color: #2850a7;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.empty-text {
  color: #9aa6b2;
}

.company-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #17212b;
  font-weight: 700;
}

.company-title :deep(.anticon) {
  color: #2563eb;
}

.deliver-desc {
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .logistics-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .filter-input,
  .filter-input.wide,
  .filter-select {
    width: 100%;
  }

  .toolbar-actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
