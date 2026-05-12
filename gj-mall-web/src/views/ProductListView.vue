<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  getBrandList,
  getCategoryTree,
  getProductPage,
  type ApiId,
  type BrandItem,
  type CategoryItem,
  type ProductItem,
  type ProductQuery,
} from '@/api/product'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const categories = ref<CategoryItem[]>([])
const brands = ref<BrandItem[]>([])
const products = ref<ProductItem[]>([])
const total = ref(0)
const initialized = ref(false)

const query = reactive<ProductQuery>({
  current: 1,
  size: 12,
  keyword: '',
  sort: 'operation',
})

const priceForm = reactive({
  min: '',
  max: '',
})

const sortOptions = [
  { label: '综合', value: 'operation' },
  { label: '销量', value: 'sales' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
]

const categoryList = computed(() => {
  const children = categories.value.flatMap((item) => item.children || [])
  return children.length ? children : categories.value
})

const selectedCategory = computed(() => categoryList.value.find((item) => String(item.id) === String(query.categoryId)))
const selectedBrand = computed(() => brands.value.find((item) => String(item.id) === String(query.brandId)))
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / Number(query.size || 12))))

function numberValue(value?: string | number) {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed : 0
}

function normalizeQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0]
  }
  return value
}

function syncFromRoute() {
  const keyword = normalizeQueryValue(route.query.keyword)
  const categoryId = normalizeQueryValue(route.query.categoryId)
  const brandId = normalizeQueryValue(route.query.brandId)
  const sort = normalizeQueryValue(route.query.sort)
  const page = normalizeQueryValue(route.query.page)
  const newStatus = normalizeQueryValue(route.query.newStatus)
  const recommendStatus = normalizeQueryValue(route.query.recommendStatus)
  const minPrice = normalizeQueryValue(route.query.minPrice)
  const maxPrice = normalizeQueryValue(route.query.maxPrice)

  query.keyword = typeof keyword === 'string' ? keyword : ''
  query.categoryId = categoryId ? String(categoryId) : undefined
  query.brandId = brandId ? String(brandId) : undefined
  query.sort = ['operation', 'sales', 'price_asc', 'price_desc'].includes(String(sort)) ? (sort as ProductQuery['sort']) : 'operation'
  query.current = Math.max(1, Number(page || 1))
  query.newStatus = newStatus === undefined ? undefined : Number(newStatus)
  query.recommendStatus = recommendStatus === undefined ? undefined : Number(recommendStatus)
  query.minPrice = minPrice === undefined || minPrice === '' ? undefined : Number(minPrice)
  query.maxPrice = maxPrice === undefined || maxPrice === '' ? undefined : Number(maxPrice)
  priceForm.min = query.minPrice === undefined ? '' : String(query.minPrice)
  priceForm.max = query.maxPrice === undefined ? '' : String(query.maxPrice)
}

function buildRouteQuery(overrides: Partial<ProductQuery> = {}) {
  const next = {
    ...query,
    ...overrides,
  }
  return {
    keyword: next.keyword?.trim() || undefined,
    categoryId: next.categoryId || undefined,
    brandId: next.brandId || undefined,
    sort: next.sort || 'operation',
    page: next.current && next.current > 1 ? next.current : undefined,
    newStatus: next.newStatus,
    recommendStatus: next.recommendStatus,
    minPrice: next.minPrice,
    maxPrice: next.maxPrice,
  }
}

function pushQuery(overrides: Partial<ProductQuery> = {}) {
  router.push({ path: '/products', query: buildRouteQuery(overrides) })
}

function apiParams() {
  return {
    current: query.current,
    size: query.size,
    keyword: query.keyword?.trim() || undefined,
    categoryId: query.categoryId,
    brandId: query.brandId,
    newStatus: query.newStatus,
    recommendStatus: query.recommendStatus,
    sort: query.sort,
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductPage(apiParams())
    let list = res.data?.list || []
    if (query.minPrice !== undefined || query.maxPrice !== undefined) {
      const min = query.minPrice === undefined ? -Infinity : Number(query.minPrice)
      const max = query.maxPrice === undefined ? Infinity : Number(query.maxPrice)
      list = list.filter((item) => {
        const price = Number(item.price || 0)
        return price >= min && price <= max
      })
      products.value = list
      total.value = list.length
    } else {
      products.value = list
      total.value = numberValue(res.data?.total)
    }
  } finally {
    loading.value = false
  }
}

async function loadFilters() {
  const [categoryRes, brandRes] = await Promise.all([getCategoryTree(), getBrandList()])
  categories.value = categoryRes.data || []
  brands.value = brandRes.data || []
}

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}

function formatSales(value?: number) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`
  }
  return String(count)
}

function normalizeImage(url?: string, seed = 'products') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/520/520`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/520/520`
}

function selectCategory(id?: ApiId) {
  pushQuery({ categoryId: id, current: 1 })
}

function selectBrand(id?: ApiId) {
  pushQuery({ brandId: id, current: 1 })
}

function selectFlag(type: 'new' | 'recommend') {
  pushQuery({
    newStatus: type === 'new' ? (query.newStatus === 1 ? undefined : 1) : query.newStatus,
    recommendStatus: type === 'recommend' ? (query.recommendStatus === 1 ? undefined : 1) : query.recommendStatus,
    current: 1,
  })
}

function search() {
  pushQuery({ current: 1 })
}

function changeSort() {
  pushQuery({ current: 1 })
}

function changePage(page: number) {
  pushQuery({ current: page })
}

function applyPrice() {
  const min = priceForm.min.trim() ? Number(priceForm.min) : undefined
  const max = priceForm.max.trim() ? Number(priceForm.max) : undefined
  pushQuery({
    minPrice: Number.isFinite(min) ? min : undefined,
    maxPrice: Number.isFinite(max) ? max : undefined,
    current: 1,
  })
}

function resetFilters() {
  router.push({ path: '/products', query: { sort: 'operation' } })
}

function removeCategory() {
  pushQuery({ categoryId: undefined, current: 1 })
}

function removeBrand() {
  pushQuery({ brandId: undefined, current: 1 })
}

function goProduct(product: ProductItem) {
  router.push({ name: 'ProductDetail', params: { id: product.id } })
}

watch(
  () => route.fullPath,
  async () => {
    if (!initialized.value) {
      return
    }
    syncFromRoute()
    await loadProducts()
  },
)

onMounted(async () => {
  syncFromRoute()
  await Promise.all([loadFilters(), loadProducts()])
  initialized.value = true
})
</script>

<template>
  <div class="products-page">
    <ShopHeader />

    <main class="products-shell">
      <section class="products-hero">
        <div>
          <span>Products</span>
          <h1>商品发现</h1>
          <p>按关键词、类目、品牌、价格和排序筛出更接近需求的商品。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/')">回到首页</button>
      </section>

      <section class="search-panel">
        <div class="search-line">
          <el-input
            v-model="query.keyword"
            size="large"
            clearable
            placeholder="搜索手机、耳机、护肤、运动鞋"
            @keyup.enter="search"
            @clear="search"
          />
          <el-button size="large" type="primary" @click="search">搜索</el-button>
        </div>

        <div class="active-tags">
          <button v-if="selectedCategory" type="button" @click="removeCategory">
            类目：{{ selectedCategory.name }} ×
          </button>
          <button v-if="selectedBrand" type="button" @click="removeBrand">品牌：{{ selectedBrand.name }} ×</button>
          <button v-if="query.newStatus === 1" type="button" @click="selectFlag('new')">新品 ×</button>
          <button v-if="query.recommendStatus === 1" type="button" @click="selectFlag('recommend')">推荐 ×</button>
          <button v-if="query.minPrice !== undefined || query.maxPrice !== undefined" type="button" @click="priceForm.min = ''; priceForm.max = ''; applyPrice()">
            价格：{{ query.minPrice ?? 0 }}-{{ query.maxPrice ?? '不限' }} ×
          </button>
          <span v-if="!selectedCategory && !selectedBrand && query.newStatus !== 1 && query.recommendStatus !== 1 && query.minPrice === undefined && query.maxPrice === undefined">
            当前展示全部在售商品
          </span>
        </div>
      </section>

      <section class="products-layout">
        <aside class="filter-panel">
          <div class="filter-section">
            <div class="filter-heading">
              <span>Category</span>
              <strong>商品类目</strong>
            </div>
            <div class="filter-list">
              <button :class="{ active: !query.categoryId }" type="button" @click="selectCategory(undefined)">全部类目</button>
              <button
                v-for="item in categoryList"
                :key="item.id"
                :class="{ active: String(query.categoryId) === String(item.id) }"
                type="button"
                @click="selectCategory(item.id)"
              >
                {{ item.name }}
              </button>
            </div>
          </div>

          <div class="filter-section">
            <div class="filter-heading">
              <span>Brand</span>
              <strong>品牌</strong>
            </div>
            <div class="brand-filter">
              <button :class="{ active: !query.brandId }" type="button" @click="selectBrand(undefined)">
                <span>ALL</span>
                <strong>全部品牌</strong>
              </button>
              <button
                v-for="brand in brands"
                :key="brand.id"
                :class="{ active: String(query.brandId) === String(brand.id) }"
                type="button"
                @click="selectBrand(brand.id)"
              >
                <img
                  :src="normalizeImage(brand.logo, String(brand.id))"
                  :alt="brand.name"
                  @error="handleImageError($event, String(brand.id))"
                />
                <strong>{{ brand.name }}</strong>
              </button>
            </div>
          </div>

          <div class="filter-section">
            <div class="filter-heading">
              <span>Price</span>
              <strong>价格区间</strong>
            </div>
            <div class="price-filter">
              <el-input v-model="priceForm.min" placeholder="最低价" />
              <el-input v-model="priceForm.max" placeholder="最高价" />
              <el-button type="primary" @click="applyPrice">应用</el-button>
            </div>
          </div>
        </aside>

        <section class="result-panel">
          <div class="result-toolbar">
            <div>
              <span>Result</span>
              <h2>{{ total }} 件商品</h2>
            </div>
            <div class="toolbar-actions">
              <button type="button" :class="{ active: query.newStatus === 1 }" @click="selectFlag('new')">新品</button>
              <button type="button" :class="{ active: query.recommendStatus === 1 }" @click="selectFlag('recommend')">推荐</button>
              <el-segmented v-model="query.sort" :options="sortOptions" @change="changeSort" />
            </div>
          </div>

          <div v-loading="loading" class="product-grid">
            <button
              v-for="product in products"
              :key="product.id"
              class="product-card"
              type="button"
              @click="goProduct(product)"
            >
              <span class="badge-row">
                <em v-if="product.newStatus === 1">新品</em>
                <em v-if="product.recommendStatus === 1" class="hot">推荐</em>
              </span>
              <span class="product-image">
                <img
                  :src="normalizeImage(product.mainImage, String(product.id))"
                  :alt="product.name"
                  @error="handleImageError($event, String(product.id))"
                />
              </span>
              <span class="product-info">
                <strong>{{ product.name }}</strong>
                <small>{{ product.subTitle || '精选商城在售商品' }}</small>
                <span class="price-line">
                  <b>{{ formatPrice(product.price) }}</b>
                  <i>已售 {{ formatSales(product.saleCount) }}</i>
                </span>
              </span>
            </button>

            <el-empty v-if="!loading && !products.length" description="没有找到匹配商品">
              <el-button type="primary" @click="resetFilters">重置筛选</el-button>
            </el-empty>
          </div>

          <div v-if="total > Number(query.size || 12) && pageCount > 1" class="pagination-row">
            <el-pagination
              :current-page="Number(query.current || 1)"
              :page-size="Number(query.size || 12)"
              :total="total"
              layout="prev, pager, next"
              @current-change="changePage"
            />
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.products-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 0%, rgba(47, 143, 103, 0.12), transparent 26%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.products-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.products-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.products-hero span,
.filter-heading span,
.result-toolbar span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.products-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.products-hero p {
  margin: 12px 0 0;
  color: #6b7280;
}

.back-link {
  border: 0;
  background: transparent;
  color: #e5484d;
  cursor: pointer;
  font: inherit;
  font-weight: 900;
}

.search-panel,
.filter-panel,
.result-toolbar {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.search-panel {
  margin-bottom: 24px;
  padding: 18px;
}

.search-line {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.search-line :deep(.el-input__wrapper),
.search-line :deep(.el-button) {
  min-height: 46px;
  border-radius: 8px;
  font-weight: 900;
}

.search-line :deep(.el-button--primary),
.price-filter :deep(.el-button--primary),
.product-grid :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.active-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.active-tags button,
.active-tags span {
  min-height: 30px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: #f3f4f6;
  color: #4b5563;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
}

.active-tags span {
  display: inline-flex;
  align-items: center;
  cursor: default;
}

.products-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.filter-panel {
  position: sticky;
  top: 96px;
  padding: 20px;
}

.filter-section + .filter-section {
  margin-top: 24px;
}

.filter-heading {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.filter-heading strong {
  font-size: 19px;
}

.filter-list,
.brand-filter {
  display: grid;
  gap: 8px;
}

.filter-list button,
.brand-filter button,
.toolbar-actions button,
.product-card {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.filter-list button {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  color: #4b5563;
  text-align: left;
  font-weight: 800;
}

.filter-list button.active,
.filter-list button:hover {
  background: #111827;
  color: #fff;
}

.brand-filter {
  max-height: 330px;
  overflow-y: auto;
}

.brand-filter button {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 6px;
  border-radius: 8px;
  color: #111827;
  text-align: left;
}

.brand-filter button.active,
.brand-filter button:hover {
  background: #f3f4f6;
}

.brand-filter img,
.brand-filter span {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #2f8f67;
  object-fit: cover;
}

.brand-filter span {
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}

.brand-filter strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-filter {
  display: grid;
  gap: 10px;
}

.price-filter :deep(.el-input__wrapper),
.price-filter :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.result-panel {
  min-width: 0;
}

.result-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
}

.result-toolbar h2 {
  margin: 9px 0 0;
  font-size: 26px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.toolbar-actions button {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #4b5563;
  font-weight: 900;
}

.toolbar-actions button.active,
.toolbar-actions button:hover {
  background: #111827;
  color: #fff;
}

.toolbar-actions :deep(.el-segmented) {
  border-radius: 999px;
  background: #f3f4f6;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  min-height: 360px;
}

.product-card {
  position: relative;
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
  color: #111827;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.product-card:hover {
  border-color: rgba(229, 72, 77, 0.24);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
  transform: translateY(-3px);
}

.badge-row {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 1;
  display: flex;
  gap: 6px;
}

.badge-row em {
  padding: 4px 8px;
  border-radius: 999px;
  background: #2f8f67;
  color: #fff;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.badge-row em.hot {
  background: #e5484d;
}

.product-image {
  display: block;
  overflow: hidden;
  border-radius: 8px;
  background: #f3f4f6;
}

.product-image img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  transition: transform 0.26s ease;
}

.product-card:hover .product-image img {
  transform: scale(1.04);
}

.product-info {
  display: grid;
  gap: 8px;
}

.product-info strong {
  display: -webkit-box;
  min-height: 44px;
  overflow: hidden;
  font-size: 17px;
  line-height: 1.3;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-info small {
  display: -webkit-box;
  min-height: 40px;
  overflow: hidden;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.price-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.price-line b {
  color: #e5484d;
  font-size: 20px;
}

.price-line i {
  color: #6b7280;
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 24px;
}

@media (max-width: 1040px) {
  .products-layout {
    grid-template-columns: 1fr;
  }

  .filter-panel {
    position: static;
  }

  .brand-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: none;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .products-shell {
    padding: 28px 16px 52px;
  }

  .products-hero,
  .result-toolbar {
    align-items: start;
    flex-direction: column;
  }

  .search-line,
  .product-grid,
  .brand-filter {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
