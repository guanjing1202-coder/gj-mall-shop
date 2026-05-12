<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  getBrandPage,
  getCategoryTree,
  type ApiId,
  type BrandItem,
  type CategoryTreeItem,
} from '@/api/product'
import {
  adjustInventoryStock,
  batchAdjustInventoryStock,
  getInventoryLogPage,
  getInventoryPage,
  getInventorySummary,
  updateInventoryWarnStock,
  type InventoryLogRecord,
  type InventoryQuery,
  type InventoryRecord,
  type InventorySummary,
} from '@/api/inventory'

const route = useRoute()

interface CategoryOption {
  title: string
  value: ApiId
  key: ApiId
  children?: CategoryOption[]
}

const loading = ref(false)
const summaryLoading = ref(false)
const adjusting = ref(false)
const batchAdjusting = ref(false)
const warnSaving = ref(false)
const logLoading = ref(false)
const records = ref<InventoryRecord[]>([])
const logs = ref<InventoryLogRecord[]>([])
const brandList = ref<BrandItem[]>([])
const categoryTree = ref<CategoryTreeItem[]>([])
const selectedRowKeys = ref<ApiId[]>([])
const adjustModalOpen = ref(false)
const batchModalOpen = ref(false)
const warnModalOpen = ref(false)
const logDrawerOpen = ref(false)
const currentRecord = ref<InventoryRecord>()
const summary = reactive<InventorySummary>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const logPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<InventoryQuery>({
  keyword: '',
  brandId: undefined,
  categoryId: undefined,
  publishStatus: undefined,
  stockStatus: undefined,
})

const adjustForm = reactive({
  stockDelta: undefined as number | undefined,
  remark: '',
})

const batchForm = reactive({
  stockDelta: undefined as number | undefined,
  remark: '',
})

const warnForm = reactive({
  warnStock: undefined as number | undefined,
})

const columns: TableColumnsType<InventoryRecord> = [
  { title: '商品 / SKU', dataIndex: 'spuName', key: 'product', width: 360 },
  { title: '品牌 / 分类', dataIndex: 'brandName', key: 'meta', width: 180 },
  { title: '库存', dataIndex: 'stock', key: 'stock', width: 260 },
  { title: '预警线', dataIndex: 'warnStock', key: 'warnStock', width: 130 },
  { title: '售价 / 销量', dataIndex: 'price', key: 'price', width: 150 },
  { title: '状态', dataIndex: 'publishStatus', key: 'status', width: 150 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 230 },
]

const logColumns: TableColumnsType<InventoryLogRecord> = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
  { title: '数量', dataIndex: 'changeQuantity', key: 'changeQuantity', width: 110 },
  { title: '可用库存', dataIndex: 'stockBefore', key: 'stock', width: 170 },
  { title: '锁定库存', dataIndex: 'lockedStockBefore', key: 'lockedStock', width: 170 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 260 },
]

const categoryOptions = computed<CategoryOption[]>(() => {
  const toOption = (nodes: CategoryTreeItem[]): CategoryOption[] =>
    nodes.map((node) => ({
      title: node.name,
      value: node.id,
      key: node.id,
      children: node.children?.length ? toOption(node.children) : undefined,
    }))
  return toOption(categoryTree.value)
})

const adjustedStock = computed(() => {
  if (!currentRecord.value || adjustForm.stockDelta === undefined || adjustForm.stockDelta === null) {
    return undefined
  }
  return Number(currentRecord.value.stock || 0) + Number(adjustForm.stockDelta || 0)
})

const selectedRecords = computed(() =>
  records.value.filter((item) => selectedRowKeys.value.map(String).includes(String(item.skuId)))
)

const batchAdjustedPreview = computed(() => {
  if (batchForm.stockDelta === undefined || batchForm.stockDelta === null) {
    return undefined
  }
  const delta = Number(batchForm.stockDelta || 0)
  const minStock = selectedRecords.value.reduce((min, item) => Math.min(min, Number(item.stock || 0)), Number.MAX_SAFE_INTEGER)
  return minStock === Number.MAX_SAFE_INTEGER ? undefined : minStock + delta
})

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: ApiId[]) => {
    selectedRowKeys.value = keys
  },
}))

onMounted(async () => {
  applyRouteFilters()
  await Promise.all([fetchBrands(), fetchCategories()])
  await fetchInventoryData()
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
  filters.brandId = routeValue('brandId') || undefined
  filters.categoryId = routeValue('categoryId') || undefined
  filters.publishStatus = routeNumber('publishStatus')
  filters.stockStatus = normalizeStockStatus(routeValue('stockStatus'))
}

function normalizeStockStatus(value?: string | null) {
  if (value === 'low' || value === 'empty' || value === 'normal' || value === 'locked') {
    return value
  }
  return undefined
}

async function fetchBrands() {
  const res = await getBrandPage({ pageNum: 1, pageSize: 200 })
  brandList.value = res.data.list
}

async function fetchCategories() {
  const res = await getCategoryTree()
  categoryTree.value = res.data
}

function buildQuery() {
  return {
    ...filters,
    keyword: filters.keyword?.trim() || undefined,
  }
}

async function fetchInventoryData() {
  await Promise.all([fetchList(), fetchSummary()])
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getInventoryPage({
      ...buildQuery(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    records.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const res = await getInventorySummary(buildQuery())
    Object.assign(summary, res.data || {})
  } finally {
    summaryLoading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchInventoryData()
}

async function handleReset() {
  filters.keyword = ''
  filters.brandId = undefined
  filters.categoryId = undefined
  filters.publishStatus = undefined
  filters.stockStatus = undefined
  pagination.current = 1
  selectedRowKeys.value = []
  await fetchInventoryData()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchList()
}

async function applyStockStatus(status?: InventoryQuery['stockStatus']) {
  filters.stockStatus = status
  pagination.current = 1
  selectedRowKeys.value = []
  await fetchInventoryData()
}

function openAdjustModal(record: InventoryRecord) {
  currentRecord.value = record
  adjustForm.stockDelta = undefined
  adjustForm.remark = ''
  adjustModalOpen.value = true
}

async function submitAdjust() {
  if (!currentRecord.value) {
    return
  }
  if (adjustForm.stockDelta === undefined || adjustForm.stockDelta === null || Number(adjustForm.stockDelta) === 0) {
    message.warning('请输入非 0 的调整数量')
    return
  }
  if (adjustedStock.value !== undefined && adjustedStock.value < 0) {
    message.warning('调整后可用库存不能小于 0')
    return
  }
  adjusting.value = true
  try {
    const res = await adjustInventoryStock(currentRecord.value.skuId, {
      stockDelta: Number(adjustForm.stockDelta),
      remark: adjustForm.remark.trim() || undefined,
    })
    message.success('库存调整成功')
    replaceRecord(res.data)
    adjustModalOpen.value = false
    await fetchSummary()
    if (logDrawerOpen.value) {
      await fetchLogs()
    }
  } finally {
    adjusting.value = false
  }
}

function openBatchAdjustModal() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先选择需要调整的 SKU')
    return
  }
  batchForm.stockDelta = undefined
  batchForm.remark = ''
  batchModalOpen.value = true
}

async function submitBatchAdjust() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先选择需要调整的 SKU')
    return
  }
  if (batchForm.stockDelta === undefined || batchForm.stockDelta === null || Number(batchForm.stockDelta) === 0) {
    message.warning('请输入非 0 的调整数量')
    return
  }
  if (batchAdjustedPreview.value !== undefined && batchAdjustedPreview.value < 0) {
    message.warning('批量调整后存在可用库存小于 0 的 SKU')
    return
  }
  batchAdjusting.value = true
  try {
    const res = await batchAdjustInventoryStock({
      items: selectedRowKeys.value.map((skuId) => ({
        skuId,
        stockDelta: Number(batchForm.stockDelta),
      })),
      remark: batchForm.remark.trim() || undefined,
    })
    res.data.forEach(replaceRecord)
    message.success(`已调整 ${res.data.length} 个 SKU`)
    selectedRowKeys.value = []
    batchModalOpen.value = false
    await fetchSummary()
    if (logDrawerOpen.value) {
      await fetchLogs()
    }
  } finally {
    batchAdjusting.value = false
  }
}

function openWarnModal(record: InventoryRecord) {
  currentRecord.value = record
  warnForm.warnStock = record.warnStock ?? 10
  warnModalOpen.value = true
}

async function submitWarnStock() {
  if (!currentRecord.value) {
    return
  }
  if (warnForm.warnStock === undefined || warnForm.warnStock === null || warnForm.warnStock < 0) {
    message.warning('请输入不小于 0 的预警库存')
    return
  }
  warnSaving.value = true
  try {
    const res = await updateInventoryWarnStock(currentRecord.value.skuId, {
      warnStock: Number(warnForm.warnStock),
    })
    replaceRecord(res.data)
    message.success('预警库存已更新')
    warnModalOpen.value = false
    await fetchSummary()
  } finally {
    warnSaving.value = false
  }
}

function replaceRecord(nextRecord: InventoryRecord) {
  const index = records.value.findIndex((item) => String(item.skuId) === String(nextRecord.skuId))
  if (index >= 0) {
    records.value.splice(index, 1, nextRecord)
  } else {
    fetchList()
  }
}

async function openLogDrawer(record: InventoryRecord) {
  currentRecord.value = record
  logDrawerOpen.value = true
  logPagination.current = 1
  await fetchLogs()
}

async function fetchLogs() {
  if (!currentRecord.value) {
    return
  }
  logLoading.value = true
  try {
    const res = await getInventoryLogPage({
      skuId: currentRecord.value.skuId,
      pageNum: logPagination.current,
      pageSize: logPagination.pageSize,
    })
    logs.value = res.data.list
    logPagination.total = Number(res.data.total || 0)
  } finally {
    logLoading.value = false
  }
}

async function handleLogTableChange(page: { current?: number; pageSize?: number }) {
  logPagination.current = page.current || 1
  logPagination.pageSize = page.pageSize || 10
  await fetchLogs()
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '--'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatAmount(value?: number) {
  if (value === undefined || value === null) {
    return '¥0.00'
  }
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatSpecs(specs?: Record<string, string>) {
  if (!specs || !Object.keys(specs).length) {
    return '--'
  }
  return Object.entries(specs)
    .map(([key, value]) => `${key}: ${value}`)
    .join(' / ')
}

function stockStatusColor(status?: string) {
  if (status === 'empty') {
    return 'error'
  }
  if (status === 'low') {
    return 'warning'
  }
  return 'success'
}

function lockPercent(record: InventoryRecord) {
  const total = Number(record.totalStock || 0)
  if (!total) {
    return 0
  }
  return Math.min(Math.round((Number(record.lockedStock || 0) / total) * 100), 100)
}

function publishColor(status?: number) {
  return status === 1 ? 'success' : 'default'
}

function publishText(status?: number) {
  return status === 1 ? '已上架' : '已下架'
}

function logTypeColor(record: InventoryLogRecord) {
  return record.changeType === 1 ? 'success' : 'error'
}

function logQuantity(record: InventoryLogRecord) {
  const prefix = record.changeType === 1 ? '+' : '-'
  return `${prefix}${record.changeQuantity || 0}`
}

function toInventory(record: Record<string, any>) {
  return record as InventoryRecord
}

function toLog(record: Record<string, any>) {
  return record as InventoryLogRecord
}
</script>

<template>
  <a-card title="库存管理" :bordered="false">
    <template #extra>
      <a-space>
        <a-button :disabled="!selectedRowKeys.length" @click="openBatchAdjustModal">
          批量调整
        </a-button>
        <a-button @click="applyStockStatus('low')">只看预警</a-button>
      </a-space>
    </template>

    <div class="inventory-summary" :class="{ 'inventory-summary-loading': summaryLoading }">
      <button
        type="button"
        class="summary-item"
        :class="{ active: !filters.stockStatus }"
        @click="applyStockStatus(undefined)"
      >
        <span>SKU 总数</span>
        <strong>{{ formatNumber(summary.totalSkuCount) }}</strong>
        <em>{{ formatAmount(summary.stockAmount) }}</em>
      </button>
      <button
        type="button"
        class="summary-item danger"
        :class="{ active: filters.stockStatus === 'empty' }"
        @click="applyStockStatus('empty')"
      >
        <span>无库存</span>
        <strong>{{ formatNumber(summary.emptySkuCount) }}</strong>
        <em>需要补货</em>
      </button>
      <button
        type="button"
        class="summary-item warning"
        :class="{ active: filters.stockStatus === 'low' }"
        @click="applyStockStatus('low')"
      >
        <span>低于预警线</span>
        <strong>{{ formatNumber(summary.lowSkuCount) }}</strong>
        <em>按 SKU 阈值判断</em>
      </button>
      <button
        type="button"
        class="summary-item processing"
        :class="{ active: filters.stockStatus === 'locked' }"
        @click="applyStockStatus('locked')"
      >
        <span>锁定库存 SKU</span>
        <strong>{{ formatNumber(summary.lockedSkuCount) }}</strong>
        <em>锁定 {{ formatNumber(summary.totalLockedStock) }}</em>
      </button>
    </div>

    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索商品、SKU、编码"
          style="width: 240px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.brandId" allow-clear placeholder="品牌" style="width: 180px">
          <a-select-option v-for="brand in brandList" :key="brand.id" :value="brand.id">
            {{ brand.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-tree-select
          v-model:value="filters.categoryId"
          allow-clear
          tree-default-expand-all
          :tree-data="categoryOptions"
          placeholder="分类"
          style="width: 220px"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.publishStatus" allow-clear placeholder="上架状态" style="width: 140px">
          <a-select-option :value="1">已上架</a-select-option>
          <a-select-option :value="0">已下架</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.stockStatus" allow-clear placeholder="库存状态" style="width: 150px">
          <a-select-option value="empty">无库存</a-select-option>
          <a-select-option value="low">低库存</a-select-option>
          <a-select-option value="normal">库存正常</a-select-option>
          <a-select-option value="locked">有锁定库存</a-select-option>
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
      row-key="skuId"
      :columns="columns"
      :data-source="records"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: 1600 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'product'">
          <div class="inventory-product">
            <img
              v-if="record.skuImage || record.mainImage"
              :src="record.skuImage || record.mainImage"
              alt="SKU图"
              class="inventory-thumb"
            />
            <div v-else class="inventory-thumb inventory-thumb-empty">无图</div>
            <div class="inventory-product-text">
              <div class="inventory-title">{{ record.spuName || '--' }}</div>
              <div class="inventory-muted">{{ record.skuName || '--' }}</div>
              <div class="inventory-muted">{{ formatSpecs(record.specData) }}</div>
              <div class="inventory-id">SPU {{ record.spuId }} / SKU {{ record.skuId }}</div>
            </div>
          </div>
        </template>

        <template v-else-if="column.key === 'meta'">
          <div>{{ record.brandName || '--' }}</div>
          <div class="inventory-muted">{{ record.categoryName || '--' }}</div>
        </template>

        <template v-else-if="column.key === 'stock'">
          <div class="stock-line">
            <a-tag :color="stockStatusColor(record.stockStatus)">
              {{ record.stockStatusDesc || '--' }}
            </a-tag>
            <a-tag v-if="record.lockedStock" color="processing">有锁定</a-tag>
          </div>
          <div class="stock-number">可用 {{ record.stock || 0 }}</div>
          <div class="inventory-muted">锁定 {{ record.lockedStock || 0 }} / 总 {{ record.totalStock || 0 }}</div>
          <a-progress
            :percent="lockPercent(toInventory(record))"
            size="small"
            :show-info="false"
            style="margin-top: 6px"
          />
        </template>

        <template v-else-if="column.key === 'warnStock'">
          <div class="inventory-title">{{ record.warnStock ?? 10 }}</div>
          <div v-if="record.alertGap" class="inventory-warning">差 {{ record.alertGap }}</div>
          <div v-else class="inventory-muted">充足</div>
        </template>

        <template v-else-if="column.key === 'price'">
          <div class="inventory-title">{{ formatMoney(record.price) }}</div>
          <div class="inventory-muted">销量 {{ record.saleCount || 0 }}</div>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="publishColor(record.publishStatus)">
            {{ publishText(record.publishStatus) }}
          </a-tag>
          <div class="inventory-muted" style="margin-top: 6px">{{ record.skuCode || '暂无编码' }}</div>
        </template>

        <template v-else-if="column.key === 'updateTime'">
          {{ record.updateTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openAdjustModal(toInventory(record))">调整</a-button>
            <a-button type="link" size="small" @click="openWarnModal(toInventory(record))">预警</a-button>
            <a-button type="link" size="small" @click="openLogDrawer(toInventory(record))">记录</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="adjustModalOpen"
    title="调整可用库存"
    :confirm-loading="adjusting"
    width="560px"
    @ok="submitAdjust"
  >
    <template v-if="currentRecord">
      <a-descriptions :column="1" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="商品">{{ currentRecord.spuName || '--' }}</a-descriptions-item>
        <a-descriptions-item label="SKU">{{ currentRecord.skuName || '--' }}</a-descriptions-item>
        <a-descriptions-item label="当前库存">
          可用 {{ currentRecord.stock || 0 }}，锁定 {{ currentRecord.lockedStock || 0 }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="调整数量">
          <a-input-number
            v-model:value="adjustForm.stockDelta"
            :min="-(currentRecord.stock || 0)"
            :max="999999"
            :step="1"
            placeholder="正数增加，负数减少"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="调整备注">
          <a-textarea
            v-model:value="adjustForm.remark"
            :maxlength="255"
            show-count
            :rows="3"
            placeholder="例如：盘点入库、破损扣减"
          />
        </a-form-item>
      </a-form>
      <a-alert
        v-if="adjustedStock !== undefined"
        :type="adjustedStock < 0 ? 'error' : 'info'"
        show-icon
        :message="`调整后可用库存：${adjustedStock}`"
      />
    </template>
  </a-modal>

  <a-modal
    v-model:open="batchModalOpen"
    title="批量调整库存"
    :confirm-loading="batchAdjusting"
    width="560px"
    @ok="submitBatchAdjust"
  >
    <a-alert
      type="info"
      show-icon
      :message="`已选择 ${selectedRowKeys.length} 个 SKU，批量调整会在一个事务内完成`"
      style="margin-bottom: 16px"
    />
    <a-form layout="vertical">
      <a-form-item label="调整数量">
        <a-input-number
          v-model:value="batchForm.stockDelta"
          :min="-999999"
          :max="999999"
          :step="1"
          placeholder="正数增加，负数减少"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="调整备注">
        <a-textarea
          v-model:value="batchForm.remark"
          :maxlength="255"
          show-count
          :rows="3"
          placeholder="例如：批量补货、盘点扣减"
        />
      </a-form-item>
    </a-form>
    <a-alert
      v-if="batchAdjustedPreview !== undefined"
      :type="batchAdjustedPreview < 0 ? 'error' : 'info'"
      show-icon
      :message="`调整后最低可用库存：${batchAdjustedPreview}`"
    />
  </a-modal>

  <a-modal
    v-model:open="warnModalOpen"
    title="设置预警库存"
    :confirm-loading="warnSaving"
    width="520px"
    @ok="submitWarnStock"
  >
    <template v-if="currentRecord">
      <a-descriptions :column="1" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="商品">{{ currentRecord.spuName || '--' }}</a-descriptions-item>
        <a-descriptions-item label="SKU">{{ currentRecord.skuName || '--' }}</a-descriptions-item>
        <a-descriptions-item label="当前库存">
          可用 {{ currentRecord.stock || 0 }}，锁定 {{ currentRecord.lockedStock || 0 }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="预警库存">
          <a-input-number
            v-model:value="warnForm.warnStock"
            :min="0"
            :max="999999"
            :step="1"
            placeholder="低于或等于该值时标记为低库存"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </template>
  </a-modal>

  <a-drawer
    v-model:open="logDrawerOpen"
    :title="currentRecord ? `${currentRecord.skuName || 'SKU'} - 库存记录` : '库存记录'"
    width="920"
    destroy-on-close
  >
    <a-table
      row-key="id"
      :columns="logColumns"
      :data-source="logs"
      :loading="logLoading"
      :pagination="logPagination"
      :scroll="{ x: 1020 }"
      @change="handleLogTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'changeType'">
          <a-tag :color="logTypeColor(toLog(record))">
            {{ record.changeTypeDesc || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'changeQuantity'">
          <span :class="record.changeType === 1 ? 'change-plus' : 'change-minus'">
            {{ logQuantity(toLog(record)) }}
          </span>
        </template>

        <template v-else-if="column.key === 'stock'">
          {{ record.stockBefore ?? 0 }} -> {{ record.stockAfter ?? 0 }}
        </template>

        <template v-else-if="column.key === 'lockedStock'">
          {{ record.lockedStockBefore ?? 0 }} -> {{ record.lockedStockAfter ?? 0 }}
        </template>

        <template v-else-if="column.key === 'remark'">
          {{ record.remark || '--' }}
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<style scoped>
.inventory-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
  opacity: 1;
  transition: opacity 0.2s ease;
}

.inventory-summary-loading {
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

.summary-item.danger strong {
  color: #cf1322;
}

.summary-item.warning strong {
  color: #d48806;
}

.summary-item.processing strong {
  color: #0958d9;
}

.inventory-product {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.inventory-thumb {
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  object-fit: cover;
}

.inventory-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}

.inventory-product-text {
  min-width: 0;
}

.inventory-title {
  color: #1f1f1f;
  font-weight: 600;
}

.inventory-muted {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.inventory-warning {
  margin-top: 4px;
  color: #d48806;
  font-size: 12px;
  font-weight: 600;
}

.inventory-id {
  margin-top: 3px;
  color: #bfbfbf;
  font-size: 12px;
  word-break: break-all;
}

.stock-line {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.stock-number {
  color: #1f1f1f;
  font-weight: 600;
}

.change-plus {
  color: #389e0d;
  font-weight: 600;
}

.change-minus {
  color: #cf1322;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .inventory-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .inventory-summary {
    grid-template-columns: 1fr;
  }
}
</style>
