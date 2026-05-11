<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { getProductDetail, getProductPage, type ApiId, type ProductDetail, type ProductItem, type ProductSku } from '@/api/product'
import {
  createSeckill,
  deleteSeckill,
  deleteSeckillSku,
  getSeckillDetail,
  getSeckillPage,
  saveSeckillSku,
  updateSeckill,
  updateSeckillStatus,
  warmUpSeckill,
  type SeckillPayload,
  type SeckillQuery,
  type SeckillRecord,
  type SeckillSkuPayload,
  type SeckillSkuRecord,
} from '@/api/seckill'

interface SeckillFormState {
  id?: ApiId
  name: string
  validityRange?: [string, string]
  status: number
}

interface SkuFormState {
  id?: ApiId
  seckillId?: ApiId
  spuId?: ApiId
  skuId?: ApiId
  seckillPrice?: number
  seckillStock?: number
  seckillLimit?: number
}

const loading = ref(false)
const saving = ref(false)
const actionId = ref<ApiId>()
const seckills = ref<SeckillRecord[]>([])
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()

const skuDrawerOpen = ref(false)
const detailLoading = ref(false)
const currentActivity = ref<SeckillRecord>()
const skuModalOpen = ref(false)
const skuModalMode = ref<'create' | 'edit'>('create')
const skuSaving = ref(false)
const skuFormRef = ref<FormInstance>()
const productLoading = ref(false)
const productKeyword = ref('')
const productOptions = ref<ProductItem[]>([])
const selectedProduct = ref<ProductDetail>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<SeckillQuery>({
  keyword: '',
  status: undefined,
})

const seckillForm = reactive<SeckillFormState>(createEmptySeckillForm())
const skuForm = reactive<SkuFormState>(createEmptySkuForm())

const formRules: any = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  validityRange: [{ required: true, type: 'array', message: '请选择活动时间', trigger: 'change' }],
}

const skuFormRules: any = {
  spuId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  skuId: [{ required: true, message: '请选择 SKU', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'change' }],
  seckillStock: [{ required: true, message: '请输入秒杀库存', trigger: 'change' }],
  seckillLimit: [{ required: true, message: '请输入限购数量', trigger: 'change' }],
}

const columns: TableColumnsType<SeckillRecord> = [
  { title: '活动', dataIndex: 'name', key: 'name', width: 260 },
  { title: '活动时间', dataIndex: 'startTime', key: 'time', width: 260 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 150 },
  { title: '商品/库存', dataIndex: 'skuCount', key: 'stock', width: 220 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 330 },
]

const skuColumns: TableColumnsType<SeckillSkuRecord> = [
  { title: '商品 SKU', dataIndex: 'skuName', key: 'skuName', width: 300 },
  { title: '价格', dataIndex: 'seckillPrice', key: 'price', width: 160 },
  { title: '库存', dataIndex: 'seckillStock', key: 'stock', width: 190 },
  { title: '限购', dataIndex: 'seckillLimit', key: 'seckillLimit', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 },
]

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '上线', value: 1 },
  { label: '结束', value: 2 },
]

const productSkuOptions = computed(() => selectedProduct.value?.skus || [])

onMounted(fetchSeckills)

async function fetchSeckills() {
  loading.value = true
  try {
    const res = await getSeckillPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      status: filters.status,
    })
    seckills.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } catch {
    seckills.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchSeckills()
}

async function handleReset() {
  filters.keyword = ''
  filters.status = undefined
  pagination.current = 1
  await fetchSeckills()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchSeckills()
}

function createEmptySeckillForm(): SeckillFormState {
  return {
    id: undefined,
    name: '',
    validityRange: undefined,
    status: 0,
  }
}

function createEmptySkuForm(): SkuFormState {
  return {
    id: undefined,
    seckillId: undefined,
    spuId: undefined,
    skuId: undefined,
    seckillPrice: undefined,
    seckillStock: 1,
    seckillLimit: 1,
  }
}

function resetSeckillForm() {
  Object.assign(seckillForm, createEmptySeckillForm())
  formRef.value?.clearValidate()
}

function handleCreate() {
  modalMode.value = 'create'
  resetSeckillForm()
  modalOpen.value = true
}

function handleEdit(record: SeckillRecord) {
  modalMode.value = 'edit'
  Object.assign(seckillForm, {
    id: record.id,
    name: record.name || '',
    validityRange: record.startTime && record.endTime ? [record.startTime, record.endTime] : undefined,
    status: record.status ?? 0,
  })
  formRef.value?.clearValidate()
  modalOpen.value = true
}

async function submitSeckill() {
  if (saving.value) {
    return
  }
  try {
    await formRef.value?.validate()
    const payload = buildSeckillPayload()
    saving.value = true
    try {
      if (modalMode.value === 'create') {
        await createSeckill(payload)
        message.success('秒杀活动创建成功')
      } else {
        await updateSeckill(payload)
        message.success('秒杀活动更新成功')
      }
      modalOpen.value = false
      pagination.current = 1
      await fetchSeckills()
      await refreshCurrentActivity()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (!error?.code && error?.message) {
      message.error(error.message)
    }
  }
}

function buildSeckillPayload(): SeckillPayload {
  if (!seckillForm.validityRange) {
    throw new Error('请选择活动时间')
  }
  if (modalMode.value === 'create' && seckillForm.status === 1) {
    throw new Error('请先保存草稿并配置秒杀 SKU 后再上线')
  }
  return {
    id: seckillForm.id,
    name: seckillForm.name.trim(),
    startTime: seckillForm.validityRange[0],
    endTime: seckillForm.validityRange[1],
    status: seckillForm.status,
  }
}

function handleStatus(record: SeckillRecord, status: number) {
  const text = statusLabel(status)
  Modal.confirm({
    title: `确认将活动改为${text}吗？`,
    content: record.name,
    async onOk() {
      actionId.value = record.id
      try {
        await updateSeckillStatus(record.id, status)
        message.success('状态更新成功')
        await fetchSeckills()
        await refreshCurrentActivity()
      } catch {
        // Error feedback is already handled by the shared request interceptor.
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleWarmUp(record: SeckillRecord) {
  Modal.confirm({
    title: '确认预热该活动的秒杀库存吗？',
    content: '预热会把当前秒杀库存同步到 Redis。',
    async onOk() {
      actionId.value = record.id
      try {
        await warmUpSeckill(record.id)
        message.success('库存预热成功')
        await fetchSeckills()
        await refreshCurrentActivity()
      } catch {
        // Error feedback is already handled by the shared request interceptor.
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleDelete(record: SeckillRecord) {
  Modal.confirm({
    title: '确认删除这个秒杀活动吗？',
    content: '上线中或已有销量的活动后端会拒绝删除，可改为结束。',
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteSeckill(record.id)
        message.success('秒杀活动删除成功')
        if (seckills.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchSeckills()
        if (currentActivity.value?.id === record.id) {
          skuDrawerOpen.value = false
          currentActivity.value = undefined
        }
      } catch {
        // Error feedback is already handled by the shared request interceptor.
      } finally {
        actionId.value = undefined
      }
    },
  })
}

async function openSkuDrawer(record: SeckillRecord) {
  skuDrawerOpen.value = true
  currentActivity.value = record
  await refreshCurrentActivity()
}

async function refreshCurrentActivity() {
  if (!currentActivity.value) {
    return
  }
  detailLoading.value = true
  try {
    const res = await getSeckillDetail(currentActivity.value.id)
    currentActivity.value = res.data
  } catch {
    skuDrawerOpen.value = false
    currentActivity.value = undefined
  } finally {
    detailLoading.value = false
  }
}

async function openSkuModal(record?: SeckillSkuRecord) {
  if (!currentActivity.value) {
    return
  }
  skuModalMode.value = record ? 'edit' : 'create'
  Object.assign(skuForm, createEmptySkuForm(), {
    id: record?.id,
    seckillId: currentActivity.value.id,
    spuId: record?.spuId,
    skuId: record?.skuId,
    seckillPrice: record?.seckillPrice,
    seckillStock: record?.seckillStock ?? 1,
    seckillLimit: record?.seckillLimit ?? 1,
  })
  selectedProduct.value = undefined
  productKeyword.value = record?.spuName || ''
  skuFormRef.value?.clearValidate()
  skuModalOpen.value = true
  await fetchProducts(productKeyword.value)
  if (record?.spuId) {
    try {
      await handleProductChange(record.spuId, false)
      skuForm.skuId = record.skuId
    } catch {
      skuModalOpen.value = false
    }
  }
}

async function fetchProducts(keyword?: string) {
  productLoading.value = true
  try {
    const res = await getProductPage({
      current: 1,
      size: 50,
      keyword: keyword?.trim() || undefined,
      publishStatus: 1,
    })
    productOptions.value = res.data.list
  } catch {
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

async function handleProductSearch(value: string) {
  productKeyword.value = value
  await fetchProducts(value)
}

async function handleProductSelect(value: unknown) {
  try {
    await handleProductChange(value as ApiId)
  } catch {
    selectedProduct.value = undefined
  }
}

async function handleProductChange(value: ApiId, resetSku = true) {
  skuForm.spuId = value
  if (resetSku) {
    skuForm.skuId = undefined
  }
  if (!value) {
    selectedProduct.value = undefined
    return
  }
  const res = await getProductDetail(value)
  selectedProduct.value = res.data
}

function handleSkuSelect(value: unknown) {
  handleSkuChange(value as ApiId)
}

function handleSkuChange(value: ApiId) {
  skuForm.skuId = value
  const sku = currentProductSku()
  if (!sku) {
    return
  }
  if (!skuForm.seckillPrice && sku.price) {
    skuForm.seckillPrice = Number(sku.price)
  }
  if (!skuForm.seckillStock && sku.stock) {
    skuForm.seckillStock = Math.max(Number(sku.stock), 1)
  }
}

async function submitSku() {
  if (skuSaving.value) {
    return
  }
  try {
    await skuFormRef.value?.validate()
    const payload = buildSkuPayload()
    skuSaving.value = true
    try {
      await saveSeckillSku(payload)
      message.success(skuModalMode.value === 'create' ? '秒杀 SKU 添加成功' : '秒杀 SKU 更新成功')
      skuModalOpen.value = false
      await refreshCurrentActivity()
      await fetchSeckills()
    } finally {
      skuSaving.value = false
    }
  } catch (error: any) {
    if (!error?.code && error?.message) {
      message.error(error.message)
    }
  }
}

function buildSkuPayload(): SeckillSkuPayload {
  if (!skuForm.seckillId || !skuForm.spuId || !skuForm.skuId) {
    throw new Error('请选择商品 SKU')
  }
  if (!skuForm.seckillPrice || skuForm.seckillPrice <= 0) {
    throw new Error('秒杀价必须大于0')
  }
  if (!skuForm.seckillStock || skuForm.seckillStock <= 0) {
    throw new Error('秒杀库存必须大于0')
  }
  if (!skuForm.seckillLimit || skuForm.seckillLimit <= 0) {
    throw new Error('限购数量必须大于0')
  }
  return {
    id: skuForm.id,
    seckillId: skuForm.seckillId,
    spuId: skuForm.spuId,
    skuId: skuForm.skuId,
    seckillPrice: skuForm.seckillPrice,
    seckillStock: skuForm.seckillStock,
    seckillLimit: skuForm.seckillLimit,
  }
}

function handleDeleteSku(record: SeckillSkuRecord) {
  Modal.confirm({
    title: '确认删除这个秒杀 SKU 吗？',
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteSeckillSku(record.id)
        message.success('秒杀 SKU 删除成功')
        await refreshCurrentActivity()
        await fetchSeckills()
      } catch {
        // Error feedback is already handled by the shared request interceptor.
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function currentProductSku(): ProductSku | undefined {
  return productSkuOptions.value.find((item) => String(item.id) === String(skuForm.skuId))
}

function statusLabel(status?: number) {
  const found = statusOptions.find((item) => item.value === status)
  return found?.label || '未知'
}

function statusColor(status?: number) {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'default'
  }
  return 'warning'
}

function timeStatusColor(status?: string) {
  if (status === '进行中') {
    return 'processing'
  }
  if (status === '未开始') {
    return 'warning'
  }
  return 'default'
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '--'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatSpecs(specs?: Record<string, string>) {
  if (!specs) {
    return '--'
  }
  const items = Object.entries(specs).map(([key, value]) => `${key}: ${value}`)
  return items.length ? items.join(' / ') : '--'
}

function stockPercent(record: SeckillRecord | SeckillSkuRecord) {
  const total = (record as SeckillRecord).totalStock !== undefined
    ? (record as SeckillRecord).totalStock
    : (record as SeckillSkuRecord).seckillStock
  const sold = record.soldCount || 0
  if (!total) {
    return 0
  }
  return Math.min(Math.round((sold / total) * 100), 100)
}

function canEditSku() {
  return currentActivity.value?.status !== 1
}

function toSeckill(record: Record<string, any>) {
  return record as SeckillRecord
}

function toSeckillSku(record: Record<string, any>) {
  return record as SeckillSkuRecord
}
</script>

<template>
  <a-card title="秒杀活动管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索活动名称"
          style="width: 240px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="状态" style="width: 140px">
          <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="handleCreate">新增活动</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="seckills"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1400 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div style="font-weight: 600">{{ record.name }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            ID {{ record.id }}
          </div>
        </template>

        <template v-else-if="column.key === 'time'">
          <div>{{ record.startTime || '--' }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin: 4px 0">
            至 {{ record.endTime || '--' }}
          </div>
          <a-tag :color="timeStatusColor(record.timeStatus)">
            {{ record.timeStatus || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || statusLabel(record.status) }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'stock'">
          <div>{{ record.skuCount || 0 }} 个 SKU</div>
          <a-progress
            :percent="stockPercent(toSeckill(record))"
            size="small"
            :show-info="false"
            style="margin-top: 6px"
          />
          <div style="font-size: 12px; color: #8c8c8c">
            已售 {{ record.soldCount || 0 }} / 总量 {{ record.totalStock || 0 }}，剩余 {{ record.remainStock || 0 }}
          </div>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(toSeckill(record))">编辑</a-button>
            <a-button type="link" size="small" @click="openSkuDrawer(toSeckill(record))">商品</a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleStatus(toSeckill(record), record.status === 1 ? 2 : 1)"
            >
              {{ record.status === 1 ? '结束' : '上线' }}
            </a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleWarmUp(toSeckill(record))"
            >
              预热
            </a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toSeckill(record))"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新增秒杀活动' : '编辑秒杀活动'"
    :confirm-loading="saving"
    width="680px"
    @ok="submitSeckill"
  >
    <a-form ref="formRef" :model="seckillForm" :rules="formRules" layout="vertical">
      <a-form-item label="活动名称" name="name">
        <a-input v-model:value="seckillForm.name" placeholder="请输入活动名称" />
      </a-form-item>
      <a-form-item label="活动时间" name="validityRange">
        <a-range-picker
          v-model:value="seckillForm.validityRange"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="活动状态">
        <a-radio-group v-model:value="seckillForm.status">
          <a-radio :value="0">草稿</a-radio>
          <a-radio :value="1" :disabled="modalMode === 'create'">上线</a-radio>
          <a-radio :value="2">结束</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </a-modal>

  <a-drawer
    v-model:open="skuDrawerOpen"
    :title="currentActivity ? `${currentActivity.name} - 秒杀商品` : '秒杀商品'"
    width="980"
    destroy-on-close
  >
    <a-spin :spinning="detailLoading">
      <template v-if="currentActivity">
        <a-descriptions :column="3" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(currentActivity.status)">
              {{ currentActivity.statusDesc || statusLabel(currentActivity.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="时间状态">
            <a-tag :color="timeStatusColor(currentActivity.timeStatus)">
              {{ currentActivity.timeStatus || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="SKU 数">{{ currentActivity.skuCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="总库存">{{ currentActivity.totalStock || 0 }}</a-descriptions-item>
          <a-descriptions-item label="已售">{{ currentActivity.soldCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="剩余">{{ currentActivity.remainStock || 0 }}</a-descriptions-item>
        </a-descriptions>

        <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
          <a-space>
            <a-button @click="refreshCurrentActivity">刷新</a-button>
            <a-button @click="handleWarmUp(currentActivity)">预热库存</a-button>
            <a-button type="primary" :disabled="!canEditSku()" @click="openSkuModal()">新增 SKU</a-button>
          </a-space>
        </div>

        <a-table
          row-key="id"
          :columns="skuColumns"
          :data-source="currentActivity.skus || []"
          :pagination="false"
          :scroll="{ x: 940 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'skuName'">
              <div style="display: flex; gap: 12px; align-items: center">
                <img
                  v-if="record.skuImage"
                  :src="record.skuImage"
                  alt="SKU图"
                  style="width: 48px; height: 48px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0"
                />
                <div>
                  <div style="font-weight: 600">{{ record.spuName || '--' }}</div>
                  <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                    {{ record.skuName || '--' }}
                  </div>
                  <div style="font-size: 12px; color: #bfbfbf; margin-top: 2px">
                    {{ formatSpecs(record.specData) }}
                  </div>
                </div>
              </div>
            </template>

            <template v-else-if="column.key === 'price'">
              <div style="font-weight: 600">{{ formatMoney(record.seckillPrice) }}</div>
              <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                原价 {{ formatMoney(record.originalPrice) }}
              </div>
            </template>

            <template v-else-if="column.key === 'stock'">
              <div>已售 {{ record.soldCount || 0 }} / 总量 {{ record.seckillStock || 0 }}</div>
              <a-progress
                :percent="stockPercent(toSeckillSku(record))"
                size="small"
                :show-info="false"
                style="margin-top: 6px"
              />
              <div style="font-size: 12px; color: #8c8c8c">
                剩余 {{ record.remainStock || 0 }}，商品库存 {{ record.productStock ?? '--' }}
              </div>
            </template>

            <template v-else-if="column.key === 'seckillLimit'">
              {{ record.seckillLimit || 1 }}
            </template>

            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" :disabled="!canEditSku()" @click="openSkuModal(toSeckillSku(record))">
                  编辑
                </a-button>
                <a-button
                  type="link"
                  danger
                  size="small"
                  :disabled="!canEditSku()"
                  :loading="actionId === record.id"
                  @click="handleDeleteSku(toSeckillSku(record))"
                >
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="skuModalOpen"
    :title="skuModalMode === 'create' ? '新增秒杀 SKU' : '编辑秒杀 SKU'"
    :confirm-loading="skuSaving"
    width="760px"
    @ok="submitSku"
  >
    <a-form ref="skuFormRef" :model="skuForm" :rules="skuFormRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="商品" name="spuId">
            <a-select
              v-model:value="skuForm.spuId"
              show-search
              :filter-option="false"
              :loading="productLoading"
              placeholder="搜索并选择商品"
              style="width: 100%"
              @search="handleProductSearch"
              @change="handleProductSelect"
            >
              <a-select-option v-for="product in productOptions" :key="product.id" :value="product.id">
                {{ product.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="SKU" name="skuId">
            <a-select
              v-model:value="skuForm.skuId"
              placeholder="请选择 SKU"
              :disabled="!productSkuOptions.length"
              style="width: 100%"
              @change="handleSkuSelect"
            >
              <a-select-option v-for="sku in productSkuOptions" :key="sku.id" :value="sku.id">
                {{ sku.name }} / {{ formatMoney(sku.price) }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="秒杀价" name="seckillPrice">
            <a-input-number v-model:value="skuForm.seckillPrice" :min="0.01" :step="1" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="秒杀库存" name="seckillStock">
            <a-input-number v-model:value="skuForm.seckillStock" :min="1" :step="1" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="每人限购" name="seckillLimit">
            <a-input-number v-model:value="skuForm.seckillLimit" :min="1" :step="1" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="当前商品库存">
            <a-input :value="currentProductSku()?.stock ?? '--'" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-alert
            v-if="currentProductSku()"
            type="info"
            show-icon
            :message="`原价 ${formatMoney(currentProductSku()?.price)}，规格 ${formatSpecs(currentProductSku()?.specData)}`"
          />
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>
