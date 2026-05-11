<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  getBrandPage,
  getCategoryTree,
  getProductPage,
  updateProductOperation,
  type ApiId,
  type BrandItem,
  type CategoryTreeItem,
  type ProductItem,
  type ProductQueryParams,
} from '@/api/product'

interface CategoryOption {
  title: string
  value: ApiId
  key: ApiId
  children?: CategoryOption[]
}

interface OperationFormState {
  publishStatus: number
  newStatus: number
  recommendStatus: number
  sort: number
}

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const productList = ref<ProductItem[]>([])
const brandList = ref<BrandItem[]>([])
const categoryTree = ref<CategoryTreeItem[]>([])
const operationTarget = ref<ProductItem>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<ProductQueryParams>({
  keyword: '',
  brandId: undefined,
  categoryId: undefined,
  publishStatus: undefined,
  newStatus: undefined,
  recommendStatus: undefined,
  sort: 'operation',
})

const operationForm = reactive<OperationFormState>({
  publishStatus: 1,
  newStatus: 0,
  recommendStatus: 0,
  sort: 0,
})

const columns: TableColumnsType<ProductItem> = [
  { title: '商品', dataIndex: 'name', key: 'name', width: 360 },
  { title: '类目 / 品牌', key: 'category', width: 220 },
  { title: '价格 / 销量', key: 'metrics', width: 150 },
  { title: '上架', dataIndex: 'publishStatus', key: 'publishStatus', width: 120 },
  { title: '新品', dataIndex: 'newStatus', key: 'newStatus', width: 120 },
  { title: '推荐', dataIndex: 'recommendStatus', key: 'recommendStatus', width: 120 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 120 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 },
]

const brandMap = computed(() => {
  return brandList.value.reduce<Record<string, string>>((acc, item) => {
    acc[String(item.id)] = item.name
    return acc
  }, {})
})

const categoryMap = computed(() => {
  const map: Record<string, string> = {}
  const walk = (nodes: CategoryTreeItem[]) => {
    nodes.forEach((node) => {
      map[String(node.id)] = node.name
      if (node.children?.length) {
        walk(node.children)
      }
    })
  }
  walk(categoryTree.value)
  return map
})

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

onMounted(async () => {
  await Promise.all([fetchBrands(), fetchCategories()])
  await fetchList()
})

async function fetchBrands() {
  try {
    const res = await getBrandPage({ pageNum: 1, pageSize: 500 })
    brandList.value = res.data.list
  } catch {
    brandList.value = []
  }
}

async function fetchCategories() {
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data
  } catch {
    categoryTree.value = []
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getProductPage({
      ...filters,
      keyword: filters.keyword?.trim() || undefined,
      current: pagination.current,
      size: pagination.pageSize,
    })
    productList.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } catch {
    productList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchList()
}

async function handleReset() {
  filters.keyword = ''
  filters.brandId = undefined
  filters.categoryId = undefined
  filters.publishStatus = undefined
  filters.newStatus = undefined
  filters.recommendStatus = undefined
  filters.sort = 'operation'
  pagination.current = 1
  await fetchList()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchList()
}

function openOperation(record: ProductItem) {
  operationTarget.value = record
  operationForm.publishStatus = record.publishStatus ?? 0
  operationForm.newStatus = record.newStatus ?? 0
  operationForm.recommendStatus = record.recommendStatus ?? 0
  operationForm.sort = record.sort ?? 0
  modalOpen.value = true
}

async function submitOperation() {
  if (!operationTarget.value) {
    return
  }
  if (operationForm.sort < 0) {
    message.warning('排序值不能小于 0')
    return
  }
  saving.value = true
  try {
    await updateProductOperation(operationTarget.value.id, {
      publishStatus: operationForm.publishStatus,
      newStatus: operationForm.newStatus,
      recommendStatus: operationForm.recommendStatus,
      sort: Number(operationForm.sort || 0),
    })
    message.success('运营设置已保存')
    modalOpen.value = false
    await fetchList()
  } catch {
    // Error feedback is already handled by the shared request interceptor.
  } finally {
    saving.value = false
  }
}

function flagColor(value?: number) {
  return value === 1 ? 'success' : 'default'
}

function flagText(value?: number, activeText = '是', inactiveText = '否') {
  return value === 1 ? activeText : inactiveText
}

function formatPrice(price?: number) {
  if (price === undefined || price === null) {
    return '--'
  }
  return `¥${Number(price).toFixed(2)}`
}

function toProduct(record: Record<string, any>) {
  return record as ProductItem
}
</script>

<template>
  <a-card title="商品运营管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索商品名"
          style="width: 220px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.brandId" allow-clear placeholder="品牌" style="width: 170px">
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
          style="width: 210px"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.publishStatus" allow-clear placeholder="上架" style="width: 120px">
          <a-select-option :value="1">已上架</a-select-option>
          <a-select-option :value="0">已下架</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.newStatus" allow-clear placeholder="新品" style="width: 120px">
          <a-select-option :value="1">新品</a-select-option>
          <a-select-option :value="0">非新品</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.recommendStatus" allow-clear placeholder="推荐" style="width: 120px">
          <a-select-option :value="1">推荐</a-select-option>
          <a-select-option :value="0">不推荐</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.sort" style="width: 150px">
          <a-select-option value="operation">运营排序</a-select-option>
          <a-select-option value="sales">销量优先</a-select-option>
          <a-select-option value="price_asc">价格从低到高</a-select-option>
          <a-select-option value="price_desc">价格从高到低</a-select-option>
          <a-select-option value="default">默认排序</a-select-option>
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
      :data-source="productList"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1320 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="operation-product">
            <img v-if="record.mainImage" :src="record.mainImage" alt="商品图" class="operation-thumb" />
            <div v-else class="operation-thumb operation-thumb-empty">无图</div>
            <div class="operation-product-text">
              <div class="operation-title">{{ record.name }}</div>
              <div class="operation-muted">{{ record.subTitle || '暂无副标题' }}</div>
              <div class="operation-id">ID {{ record.id }}</div>
            </div>
          </div>
        </template>

        <template v-else-if="column.key === 'category'">
          <div>{{ categoryMap[String(record.categoryId)] || '--' }}</div>
          <div class="operation-muted">{{ brandMap[String(record.brandId)] || '--' }}</div>
        </template>

        <template v-else-if="column.key === 'metrics'">
          <div>{{ formatPrice(record.price) }}</div>
          <div class="operation-muted">销量 {{ record.saleCount ?? 0 }}</div>
        </template>

        <template v-else-if="column.key === 'publishStatus'">
          <a-tag :color="flagColor(record.publishStatus)">
            {{ flagText(record.publishStatus, '已上架', '已下架') }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'newStatus'">
          <a-tag :color="flagColor(record.newStatus)">
            {{ flagText(record.newStatus, '新品', '普通') }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'recommendStatus'">
          <a-tag :color="flagColor(record.recommendStatus)">
            {{ flagText(record.recommendStatus, '推荐', '未推荐') }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'sort'">
          {{ record.sort ?? 0 }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openOperation(toProduct(record))">设置</a-button>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="modalOpen"
    title="运营设置"
    :confirm-loading="saving"
    @ok="submitOperation"
  >
    <a-form layout="vertical">
      <a-form-item label="商品">
        <div class="operation-modal-title">{{ operationTarget?.name || '--' }}</div>
        <div class="operation-muted">ID {{ operationTarget?.id || '--' }}</div>
      </a-form-item>
      <a-form-item label="上架状态">
        <a-radio-group v-model:value="operationForm.publishStatus">
          <a-radio-button :value="1">已上架</a-radio-button>
          <a-radio-button :value="0">已下架</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="新品标记">
        <a-radio-group v-model:value="operationForm.newStatus">
          <a-radio-button :value="1">新品</a-radio-button>
          <a-radio-button :value="0">普通</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="推荐标记">
        <a-radio-group v-model:value="operationForm.recommendStatus">
          <a-radio-button :value="1">推荐</a-radio-button>
          <a-radio-button :value="0">不推荐</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="运营排序">
        <a-input-number v-model:value="operationForm.sort" :min="0" :precision="0" style="width: 180px" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.operation-product {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.operation-thumb {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  object-fit: cover;
}

.operation-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}

.operation-product-text {
  min-width: 0;
}

.operation-title {
  color: #1f1f1f;
  font-weight: 600;
}

.operation-modal-title {
  color: #1f1f1f;
  font-weight: 600;
}

.operation-muted {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.operation-id {
  margin-top: 3px;
  color: #bfbfbf;
  font-size: 12px;
  word-break: break-all;
}
</style>
