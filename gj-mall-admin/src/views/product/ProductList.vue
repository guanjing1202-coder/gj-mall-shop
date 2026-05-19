<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { uploadImage } from '@/api/file'
import {
  createProduct,
  deleteProduct,
  getBrandPage,
  getCategoryTree,
  getProductDetail,
  getProductPage,
  updateProduct,
  updateProductPublishStatus,
  type ApiId,
  type BrandItem,
  type CategoryTreeItem,
  type ProductDetail,
  type ProductItem,
  type ProductQueryParams,
  type ProductSavePayload,
} from '@/api/product'

interface CategoryOption {
  title: string
  value: ApiId
  key: ApiId
  children?: CategoryOption[]
}

interface ProductFormSku {
  uid: string
  id?: ApiId
  skuCode: string
  name: string
  image: string
  price?: number
  costPrice?: number
  stock?: number
  specText: string
}

interface ProductFormState {
  id?: ApiId
  name: string
  subTitle: string
  categoryId?: ApiId
  brandId?: ApiId
  mainImage: string
  imagesText: string
  detailHtml: string
  detailImagesText: string
  packingList: string
  afterSale: string
  publishStatus: number
  skus: ProductFormSku[]
}

const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const actionId = ref<ApiId>()
const drawerOpen = ref(false)
const editorOpen = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const productList = ref<ProductItem[]>([])
const brandList = ref<BrandItem[]>([])
const categoryTree = ref<CategoryTreeItem[]>([])
const detail = ref<ProductDetail>()
const formRef = ref<FormInstance>()
const uploadingTarget = ref('')

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
  sort: 'default',
})

const productForm = reactive<ProductFormState>(createEmptyProductForm())

const formRules: any = {
  name: [{ required: true, message: '请输入商品名', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const columns: TableColumnsType<ProductItem> = [
  { title: '商品', dataIndex: 'name', key: 'name', width: 280 },
  { title: '分类', dataIndex: 'categoryId', key: 'categoryId', width: 150 },
  { title: '品牌', dataIndex: 'brandId', key: 'brandId', width: 150 },
  { title: '价格', dataIndex: 'price', key: 'price', width: 120 },
  { title: '销量', dataIndex: 'saleCount', key: 'saleCount', width: 100 },
  { title: '状态', dataIndex: 'publishStatus', key: 'publishStatus', width: 120 },
  { title: '操作', key: 'action', fixed: 'right', width: 220 },
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
  const res = await getBrandPage({ pageNum: 1, pageSize: 200 })
  brandList.value = res.data.list
}

async function fetchCategories() {
  const res = await getCategoryTree()
  categoryTree.value = res.data
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getProductPage({
      ...filters,
      current: pagination.current,
      size: pagination.pageSize,
    })
    productList.value = res.data.list
    pagination.total = Number(res.data.total || 0)
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
  filters.sort = 'default'
  pagination.current = 1
  await fetchList()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchList()
}

async function handleView(record: ProductItem) {
  drawerOpen.value = true
  detailLoading.value = true
  detail.value = undefined
  try {
    const res = await getProductDetail(record.id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function handleCreate() {
  editorMode.value = 'create'
  editorOpen.value = true
  resetProductForm()
}

async function handleEdit(record: ProductItem) {
  editorMode.value = 'edit'
  editorOpen.value = true
  saving.value = false
  resetProductForm()
  detailLoading.value = true
  try {
    const res = await getProductDetail(record.id)
    fillProductForm(res.data)
  } finally {
    detailLoading.value = false
  }
}

function handlePublish(record: ProductItem) {
  const nextStatus = record.publishStatus === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '上架' : '下架'
  Modal.confirm({
    title: `确认${actionText}这个商品吗？`,
    async onOk() {
      actionId.value = record.id
      try {
        await updateProductPublishStatus(record.id, nextStatus)
        message.success(`${actionText}成功`)
        await fetchList()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleDelete(record: ProductItem) {
  Modal.confirm({
    title: '确认删除这个商品吗？',
    content: '删除后商品会从列表中移除，SKU 也会一起删除。',
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteProduct(record.id)
        message.success('删除成功')
        if (productList.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchList()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleCloseEditor() {
  if (saving.value) {
    return
  }
  editorOpen.value = false
}

function resetProductForm() {
  Object.assign(productForm, createEmptyProductForm())
  formRef.value?.clearValidate()
}

function createEmptyProductForm(): ProductFormState {
  return {
    id: undefined,
    name: '',
    subTitle: '',
    categoryId: undefined,
    brandId: undefined,
    mainImage: '',
    imagesText: '',
    detailHtml: '',
    detailImagesText: '',
    packingList: '',
    afterSale: '',
    publishStatus: 0,
    skus: [createEmptySku()],
  }
}

function createEmptySku(): ProductFormSku {
  return {
    uid: `sku_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    skuCode: '',
    name: '',
    image: '',
    price: undefined,
    costPrice: undefined,
    stock: 0,
    specText: '',
  }
}

function addSku() {
  productForm.skus.push(createEmptySku())
}

function removeSku(index: number) {
  if (productForm.skus.length === 1) {
    message.warning('至少保留一个 SKU')
    return
  }
  productForm.skus.splice(index, 1)
}

function fillProductForm(data: ProductDetail) {
  productForm.id = data.id
  productForm.name = data.name || ''
  productForm.subTitle = data.subTitle || ''
  productForm.categoryId = data.categoryId
  productForm.brandId = data.brandId
  productForm.mainImage = data.mainImage || ''
  productForm.imagesText = joinLines(data.images)
  productForm.detailHtml = data.detailHtml || ''
  productForm.detailImagesText = joinLines(data.detailImages)
  productForm.packingList = data.packingList || ''
  productForm.afterSale = data.afterSale || ''
  productForm.publishStatus = data.publishStatus ?? 0
  productForm.skus = (data.skus?.length ? data.skus : []).map((sku) => ({
    uid: `sku_${sku.id ?? Math.random().toString(36).slice(2, 8)}`,
    id: sku.id,
    skuCode: sku.skuCode || '',
    name: sku.name || '',
    image: sku.image || '',
    price: sku.price ?? undefined,
    costPrice: undefined,
    stock: sku.stock ?? 0,
    specText: specMapToText(sku.specData),
  }))
  if (!productForm.skus.length) {
    productForm.skus = [createEmptySku()]
  }
}

async function submitProduct() {
  try {
    await formRef.value?.validate()
    validateSkus()

    const payload = buildPayload()
    saving.value = true
    try {
      if (editorMode.value === 'create') {
        await createProduct(payload)
        message.success('商品创建成功')
      } else {
        await updateProduct(payload)
        message.success('商品更新成功')
      }
      editorOpen.value = false
      pagination.current = 1
      await fetchList()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

function validateSkus() {
  if (!productForm.skus.length) {
    throw new Error('至少需要一个 SKU')
  }
  for (const [index, sku] of productForm.skus.entries()) {
    if (!sku.name.trim()) {
      throw new Error(`第 ${index + 1} 个 SKU 缺少名称`)
    }
    if (sku.price === null || sku.price === undefined) {
      throw new Error(`第 ${index + 1} 个 SKU 缺少售价`)
    }
    if (sku.stock === null || sku.stock === undefined) {
      throw new Error(`第 ${index + 1} 个 SKU 缺少库存`)
    }
  }
}

function buildPayload(): ProductSavePayload {
  return {
    id: productForm.id,
    name: productForm.name.trim(),
    subTitle: normalizeOptional(productForm.subTitle),
    categoryId: productForm.categoryId as ApiId,
    brandId: productForm.brandId || undefined,
    mainImage: normalizeOptional(productForm.mainImage),
    images: splitLines(productForm.imagesText),
    detailHtml: normalizeOptional(productForm.detailHtml),
    detailImages: splitLines(productForm.detailImagesText),
    packingList: normalizeOptional(productForm.packingList),
    afterSale: normalizeOptional(productForm.afterSale),
    publishStatus: productForm.publishStatus,
    skus: productForm.skus.map((sku) => ({
      id: sku.id,
      skuCode: normalizeOptional(sku.skuCode),
      name: sku.name.trim(),
      image: normalizeOptional(sku.image),
      price: Number(sku.price),
      costPrice: sku.costPrice === null || sku.costPrice === undefined ? undefined : Number(sku.costPrice),
      stock: Number(sku.stock),
      specData: parseSpecText(sku.specText),
    })),
  }
}

function splitLines(value: string) {
  return value
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function appendImageLine(field: 'imagesText' | 'detailImagesText', url: string) {
  const list = splitLines(productForm[field])
  if (!list.includes(url)) {
    list.push(url)
    productForm[field] = list.join('\n')
  }
}

function removeImageLine(field: 'imagesText' | 'detailImagesText', url: string) {
  productForm[field] = splitLines(productForm[field])
    .filter((item) => item !== url)
    .join('\n')
}

function joinLines(value?: string[]) {
  return value?.join('\n') || ''
}

function normalizeOptional(value: string) {
  const text = value.trim()
  return text ? text : undefined
}

function parseSpecText(value: string) {
  const map: Record<string, string> = {}
  value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .forEach((line) => {
      const [rawKey, ...rest] = line.split(':')
      const key = rawKey?.trim()
      const itemValue = rest.join(':').trim()
      if (key && itemValue) {
        map[key] = itemValue
      }
    })
  return Object.keys(map).length ? map : undefined
}

function specMapToText(specs?: Record<string, string>) {
  if (!specs) {
    return ''
  }
  return Object.entries(specs)
    .map(([key, value]) => `${key}: ${value}`)
    .join('\n')
}

function formatPrice(price?: number) {
  if (price === undefined || price === null) {
    return '--'
  }
  return `¥${Number(price).toFixed(2)}`
}

function formatSpecs(specs?: Record<string, string>) {
  if (!specs) {
    return '--'
  }
  const items = Object.entries(specs).map(([key, value]) => `${key}: ${value}`)
  return items.length ? items.join(' / ') : '--'
}

function toProduct(record: Record<string, any>) {
  return record as ProductItem
}

const galleryImages = computed(() => splitLines(productForm.imagesText))
const detailImages = computed(() => splitLines(productForm.detailImagesText))

async function uploadToForm(options: any, target: string, scene: string, onUrl: (url: string) => void) {
  uploadingTarget.value = target
  try {
    const res = await uploadImage(options.file as File, scene)
    const url = res.data?.url
    if (!url) {
      throw new Error('上传结果缺少图片地址')
    }
    onUrl(url)
    message.success('图片已上传')
    options.onSuccess?.(res.data)
  } catch (error) {
    message.error('图片上传失败')
    options.onError?.(error)
  } finally {
    uploadingTarget.value = ''
  }
}

function uploadMainImage(options: any) {
  return uploadToForm(options, 'main', 'product-main', (url) => {
    productForm.mainImage = url
  })
}

function uploadGalleryImage(options: any) {
  return uploadToForm(options, 'gallery', 'product-gallery', (url) => appendImageLine('imagesText', url))
}

function uploadDetailImage(options: any) {
  return uploadToForm(options, 'detail', 'product-detail', (url) => appendImageLine('detailImagesText', url))
}

function uploadSkuImage(sku: ProductFormSku, options: any) {
  return uploadToForm(options, `sku:${sku.uid}`, 'product-sku', (url) => {
    sku.image = url
  })
}
</script>

<template>
  <a-card title="商品管理" :bordered="false">
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
        <a-select
          v-model:value="filters.brandId"
          allow-clear
          placeholder="品牌"
          style="width: 180px"
        >
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
        <a-select
          v-model:value="filters.publishStatus"
          allow-clear
          placeholder="状态"
          style="width: 140px"
        >
          <a-select-option :value="1">已上架</a-select-option>
          <a-select-option :value="0">已下架</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.sort" style="width: 160px">
          <a-select-option value="default">默认排序</a-select-option>
          <a-select-option value="sales">销量优先</a-select-option>
          <a-select-option value="price_asc">价格从低到高</a-select-option>
          <a-select-option value="price_desc">价格从高到低</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="handleCreate">新增商品</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="productList"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1200 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div style="display: flex; gap: 12px; align-items: center">
            <img
              v-if="record.mainImage"
              :src="record.mainImage"
              alt="商品图"
              style="width: 52px; height: 52px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0"
            />
            <div
              v-else
              style="width: 52px; height: 52px; border-radius: 6px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; color: #999"
            >
              无图
            </div>
            <div>
              <div style="font-weight: 600; color: #1f1f1f">{{ record.name }}</div>
              <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                {{ record.subTitle || '暂无副标题' }}
              </div>
              <div style="font-size: 12px; color: #bfbfbf; margin-top: 2px">
                ID {{ record.id }}
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="column.key === 'categoryId'">
          {{ categoryMap[record.categoryId] || '--' }}
        </template>

        <template v-else-if="column.key === 'brandId'">
          {{ brandMap[record.brandId] || '--' }}
        </template>

        <template v-else-if="column.key === 'price'">
          {{ formatPrice(record.price) }}
        </template>

        <template v-else-if="column.key === 'saleCount'">
          {{ record.saleCount ?? 0 }}
        </template>

        <template v-else-if="column.key === 'publishStatus'">
          <a-tag :color="record.publishStatus === 1 ? 'success' : 'default'">
            {{ record.publishStatus === 1 ? '已上架' : '已下架' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toProduct(record))">查看</a-button>
            <a-button type="link" size="small" @click="handleEdit(toProduct(record))">编辑</a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handlePublish(toProduct(record))"
            >
              {{ record.publishStatus === 1 ? '下架' : '上架' }}
            </a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toProduct(record))"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-drawer v-model:open="drawerOpen" title="商品详情" width="720" destroy-on-close>
    <a-spin :spinning="detailLoading">
      <template v-if="detail">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="商品名" :span="2">{{ detail.name }}</a-descriptions-item>
          <a-descriptions-item label="副标题" :span="2">{{ detail.subTitle || '--' }}</a-descriptions-item>
          <a-descriptions-item label="分类">{{ detail.categoryName || '--' }}</a-descriptions-item>
          <a-descriptions-item label="品牌">{{ detail.brandName || '--' }}</a-descriptions-item>
          <a-descriptions-item label="价格">{{ formatPrice(detail.price) }}</a-descriptions-item>
          <a-descriptions-item label="销量">{{ detail.saleCount ?? 0 }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ detail.publishStatus === 1 ? '已上架' : '已下架' }}
          </a-descriptions-item>
          <a-descriptions-item label="主图">
            <a :href="detail.mainImage" target="_blank">{{ detail.mainImage || '--' }}</a>
          </a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">SKU 列表</h4>
          <a-table row-key="id" size="small" :pagination="false" :data-source="detail.skus || []">
            <a-table-column title="SKU 名称" data-index="name" key="name" />
            <a-table-column title="价格" key="price">
              <template #default="{ record }">{{ formatPrice(record.price) }}</template>
            </a-table-column>
            <a-table-column title="库存" data-index="stock" key="stock" />
            <a-table-column title="锁定库存" data-index="lockedStock" key="lockedStock" />
            <a-table-column title="规格" key="specData">
              <template #default="{ record }">{{ formatSpecs(record.specData) }}</template>
            </a-table-column>
          </a-table>
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">商品图册</h4>
          <a-space wrap>
            <img
              v-for="image in detail.images || []"
              :key="image"
              :src="image"
              alt="商品图册"
              style="width: 88px; height: 88px; object-fit: cover; border-radius: 6px; border: 1px solid #f0f0f0"
            />
            <span v-if="!detail.images?.length" style="color: #999">暂无图册</span>
          </a-space>
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">图文详情</h4>
          <div
            v-if="detail.detailHtml"
            v-html="detail.detailHtml"
            style="padding: 12px; border: 1px solid #f0f0f0; border-radius: 8px; overflow: auto"
          />
          <div v-else style="color: #999">暂无详情内容</div>
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">包装清单</h4>
          <div style="white-space: pre-wrap; color: #595959">{{ detail.packingList || '--' }}</div>
        </div>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">售后说明</h4>
          <div style="white-space: pre-wrap; color: #595959">{{ detail.afterSale || '--' }}</div>
        </div>
      </template>
    </a-spin>
  </a-drawer>

  <a-drawer
    v-model:open="editorOpen"
    :title="editorMode === 'create' ? '新增商品' : '编辑商品'"
    width="960"
    destroy-on-close
    @close="handleCloseEditor"
  >
    <a-spin :spinning="detailLoading || saving">
      <a-form
        ref="formRef"
        :model="productForm"
        layout="vertical"
        :rules="formRules"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="商品名" name="name">
              <a-input v-model:value="productForm.name" placeholder="请输入商品名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="副标题">
              <a-input v-model:value="productForm.subTitle" placeholder="请输入副标题" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类" name="categoryId">
              <a-tree-select
                v-model:value="productForm.categoryId"
                tree-default-expand-all
                :tree-data="categoryOptions"
                placeholder="请选择分类"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="品牌">
              <a-select v-model:value="productForm.brandId" allow-clear placeholder="请选择品牌">
                <a-select-option v-for="brand in brandList" :key="brand.id" :value="brand.id">
                  {{ brand.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="主图">
              <div class="image-input-block">
                <div class="single-image-row">
                  <img v-if="productForm.mainImage" :src="productForm.mainImage" alt="主图预览" class="image-preview" />
                  <div v-else class="image-empty">主图</div>
                  <div class="image-input-main">
                    <a-input v-model:value="productForm.mainImage" placeholder="https://example.com/main.jpg" />
                    <a-upload
                      accept="image/png,image/jpeg,image/webp,image/gif"
                      :show-upload-list="false"
                      :custom-request="uploadMainImage"
                    >
                      <a-button :loading="uploadingTarget === 'main'">上传主图</a-button>
                    </a-upload>
                  </div>
                </div>
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上架状态">
              <a-radio-group v-model:value="productForm.publishStatus">
                <a-radio :value="0">先下架</a-radio>
                <a-radio :value="1">直接上架</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="商品图册">
              <div class="multi-image-toolbar">
                <span>每行一个图片 URL，也可以直接上传追加</span>
                <a-upload
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  :show-upload-list="false"
                  :custom-request="uploadGalleryImage"
                >
                  <a-button :loading="uploadingTarget === 'gallery'">上传图册图片</a-button>
                </a-upload>
              </div>
              <a-textarea
                v-model:value="productForm.imagesText"
                :rows="4"
                placeholder="每行一个图片 URL"
              />
              <div v-if="galleryImages.length" class="image-chip-grid">
                <div v-for="image in galleryImages" :key="image" class="image-chip">
                  <img :src="image" alt="商品图册预览" />
                  <button type="button" @click="removeImageLine('imagesText', image)">移除</button>
                </div>
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="详情图片">
              <div class="multi-image-toolbar">
                <span>详情页图片会按顺序展示</span>
                <a-upload
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  :show-upload-list="false"
                  :custom-request="uploadDetailImage"
                >
                  <a-button :loading="uploadingTarget === 'detail'">上传详情图片</a-button>
                </a-upload>
              </div>
              <a-textarea
                v-model:value="productForm.detailImagesText"
                :rows="4"
                placeholder="每行一个详情图片 URL"
              />
              <div v-if="detailImages.length" class="image-chip-grid">
                <div v-for="image in detailImages" :key="image" class="image-chip">
                  <img :src="image" alt="详情图片预览" />
                  <button type="button" @click="removeImageLine('detailImagesText', image)">移除</button>
                </div>
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="图文详情 HTML">
              <a-textarea
                v-model:value="productForm.detailHtml"
                :rows="8"
                placeholder="<p>这里填写商品详情 HTML</p>"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="包装清单">
              <a-textarea
                v-model:value="productForm.packingList"
                :rows="5"
                placeholder="例如：手机*1&#10;充电器*1"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="售后说明">
              <a-textarea
                v-model:value="productForm.afterSale"
                :rows="5"
                placeholder="例如：7 天无理由退货"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <div style="display: flex; justify-content: space-between; align-items: center; margin: 8px 0 12px">
          <h3 style="margin: 0">SKU 列表</h3>
          <a-button type="dashed" @click="addSku">新增 SKU</a-button>
        </div>

        <a-alert
          type="info"
          show-icon
          style="margin-bottom: 16px"
          message="规格格式"
          description="在规格文本中按“键: 值”填写，每行一组，例如：颜色: 黑色"
        />

        <div
          v-for="(sku, index) in productForm.skus"
          :key="sku.uid"
          style="margin-bottom: 16px; padding: 16px; border: 1px solid #f0f0f0; border-radius: 8px"
        >
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
            <strong>SKU {{ index + 1 }}</strong>
            <a-button danger type="link" @click="removeSku(index)">删除</a-button>
          </div>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="`SKU 名称 ${index + 1}`" required>
                <a-input v-model:value="sku.name" placeholder="例如：黑色 12+256G" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="SKU 编码">
                <a-input v-model:value="sku.skuCode" placeholder="可选" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="`售价 ${index + 1}`" required>
                <a-input-number v-model:value="sku.price" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="成本价">
                <a-input-number v-model:value="sku.costPrice" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="`库存 ${index + 1}`" required>
                <a-input-number v-model:value="sku.stock" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="SKU 图片">
                <div class="single-image-row single-image-row--compact">
                  <img v-if="sku.image" :src="sku.image" alt="SKU 图片预览" class="image-preview" />
                  <div v-else class="image-empty">SKU</div>
                  <div class="image-input-main">
                    <a-input v-model:value="sku.image" placeholder="可选" />
                    <a-upload
                      accept="image/png,image/jpeg,image/webp,image/gif"
                      :show-upload-list="false"
                      :custom-request="(options: any) => uploadSkuImage(sku, options)"
                    >
                      <a-button :loading="uploadingTarget === `sku:${sku.uid}`">上传 SKU 图</a-button>
                    </a-upload>
                  </div>
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="规格文本">
                <a-textarea
                  v-model:value="sku.specText"
                  :rows="3"
                  placeholder="颜色: 黑色&#10;容量: 256G"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </a-form>
    </a-spin>

    <template #footer>
      <a-space>
        <a-button @click="handleCloseEditor">取消</a-button>
        <a-button type="primary" :loading="saving" @click="submitProduct">保存商品</a-button>
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped>
.image-input-block {
  width: 100%;
}

.single-image-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.single-image-row--compact {
  grid-template-columns: 72px minmax(0, 1fr);
}

.image-preview,
.image-empty {
  width: 92px;
  height: 92px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.single-image-row--compact .image-preview,
.single-image-row--compact .image-empty {
  width: 72px;
  height: 72px;
}

.image-preview {
  object-fit: cover;
}

.image-empty {
  display: grid;
  place-items: center;
  background: #fafafa;
  color: #8c8c8c;
  font-size: 12px;
}

.image-input-main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.multi-image-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #8c8c8c;
}

.image-chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.image-chip {
  position: relative;
  overflow: hidden;
  width: 84px;
  height: 84px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
}

.image-chip img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-chip button {
  position: absolute;
  right: 4px;
  bottom: 4px;
  border: 0;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.68);
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}
</style>
