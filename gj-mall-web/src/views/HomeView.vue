<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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

const router = useRouter()

const loading = ref(false)
const products = ref<ProductItem[]>([])
const categories = ref<CategoryItem[]>([])
const brands = ref<BrandItem[]>([])
const total = ref(0)

const query = reactive<ProductQuery>({
  current: 1,
  size: 12,
  sort: 'operation',
})

const sortOptions = [
  { label: '综合推荐', value: 'operation' },
  { label: '销量优先', value: 'sales' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' },
]

const categoryTabs = computed(() => {
  const children = categories.value.flatMap((item) => item.children || [])
  return children.length ? children : categories.value
})

const heroProduct = computed(() => products.value[0])
const heroSideProducts = computed(() => products.value.slice(1, 4))
const recommendedProducts = computed(() => products.value.slice(0, 4))
const brandStrip = computed(() => brands.value.slice(0, 8))

function toNumber(value?: string | number) {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed : 0
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

function normalizeImage(url?: string, seed = 'gjmall') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/640/640`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/640/640`
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductPage({
      current: query.current,
      size: query.size,
      keyword: query.keyword?.trim() || undefined,
      categoryId: query.categoryId,
      brandId: query.brandId,
      sort: query.sort,
    })
    products.value = res.data.list || []
    total.value = toNumber(res.data.total)
  } finally {
    loading.value = false
  }
}

async function loadFilters() {
  const [categoryRes, brandRes] = await Promise.all([getCategoryTree(), getBrandList()])
  categories.value = categoryRes.data || []
  brands.value = brandRes.data || []
}

function selectCategory(id?: ApiId) {
  query.categoryId = id
  query.current = 1
  loadProducts()
}

function selectBrand(id?: ApiId) {
  query.brandId = id
  query.current = 1
  loadProducts()
}

function resetFilters() {
  query.keyword = ''
  query.categoryId = undefined
  query.brandId = undefined
  query.current = 1
  query.sort = 'operation'
  loadProducts()
}

function goProduct(product: ProductItem) {
  router.push({ name: 'ProductDetail', params: { id: product.id } })
}

onMounted(async () => {
  await Promise.all([loadFilters(), loadProducts()])
})
</script>

<template>
  <div class="shop-page">
    <ShopHeader />

    <main>
      <section class="commerce-hero">
        <div class="hero-copy">
          <div class="hero-kicker">精选好物 · 今日上新</div>
          <h1>把值得买的商品摆到最前面</h1>
          <p>真实接口驱动的商品浏览入口，支持分类、品牌、搜索和排序。</p>

          <div class="hero-search">
            <el-input
              v-model="query.keyword"
              clearable
              size="large"
              placeholder="搜索手机、耳机、护肤、运动鞋"
              @keyup.enter="loadProducts"
              @clear="loadProducts"
            />
            <el-button size="large" type="primary" @click="loadProducts">搜索</el-button>
          </div>

          <div class="hero-metrics">
            <span><strong>{{ total }}</strong> 件在售</span>
            <span><strong>{{ brands.length }}</strong> 个品牌</span>
            <span><strong>{{ categoryTabs.length }}</strong> 个类目</span>
          </div>
        </div>

        <div class="hero-showcase" v-if="heroProduct">
          <button class="hero-feature" type="button" @click="goProduct(heroProduct)">
            <img
              :src="normalizeImage(heroProduct.mainImage, String(heroProduct.id))"
              :alt="heroProduct.name"
              @error="handleImageError($event, String(heroProduct.id))"
            />
            <span class="hero-feature__info">
              <small>本周焦点</small>
              <strong>{{ heroProduct.name }}</strong>
              <em>{{ formatPrice(heroProduct.price) }}</em>
            </span>
          </button>

          <div class="hero-mini-list">
            <button
              v-for="item in heroSideProducts"
              :key="item.id"
              class="hero-mini"
              type="button"
              @click="goProduct(item)"
            >
              <img
                :src="normalizeImage(item.mainImage, String(item.id))"
                :alt="item.name"
                @error="handleImageError($event, String(item.id))"
              />
              <span>{{ item.name }}</span>
            </button>
          </div>
        </div>
      </section>

      <section class="content-band">
        <div class="section-heading">
          <div>
            <span>Category</span>
            <h2>按场景快速挑选</h2>
          </div>
          <button class="text-link" type="button" @click="resetFilters">重置筛选</button>
        </div>

        <div class="category-rail">
          <button
            class="category-chip"
            :class="{ active: !query.categoryId }"
            type="button"
            @click="selectCategory(undefined)"
          >
            全部
          </button>
          <button
            v-for="item in categoryTabs"
            :key="item.id"
            class="category-chip"
            :class="{ active: query.categoryId === item.id }"
            type="button"
            @click="selectCategory(item.id)"
          >
            {{ item.name }}
          </button>
        </div>
      </section>

      <section class="content-band product-layout">
        <aside class="brand-panel">
          <div class="section-heading compact">
            <div>
              <span>Brands</span>
              <h2>品牌馆</h2>
            </div>
          </div>

          <button
            class="brand-row"
            :class="{ active: !query.brandId }"
            type="button"
            @click="selectBrand(undefined)"
          >
            <span class="brand-logo text-logo">ALL</span>
            <strong>全部品牌</strong>
          </button>
          <button
            v-for="brand in brandStrip"
            :key="brand.id"
            class="brand-row"
            :class="{ active: query.brandId === brand.id }"
            type="button"
            @click="selectBrand(brand.id)"
          >
            <img
              class="brand-logo"
              :src="normalizeImage(brand.logo, String(brand.id))"
              :alt="brand.name"
              @error="handleImageError($event, String(brand.id))"
            />
            <strong>{{ brand.name }}</strong>
          </button>
        </aside>

        <div class="product-main">
          <div class="toolbar">
            <div>
              <span>Products</span>
              <h2>精选商品</h2>
            </div>
            <el-segmented v-model="query.sort" :options="sortOptions" @change="loadProducts" />
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

            <el-empty v-if="!loading && !products.length" description="没有找到匹配商品" />
          </div>
        </div>
      </section>

      <section class="content-band recommend-band" v-if="recommendedProducts.length">
        <div class="section-heading">
          <div>
            <span>Selection</span>
            <h2>值得优先看的几件</h2>
          </div>
        </div>

        <div class="recommend-list">
          <button
            v-for="product in recommendedProducts"
            :key="product.id"
            class="recommend-item"
            type="button"
            @click="goProduct(product)"
          >
            <img
              :src="normalizeImage(product.mainImage, String(product.id))"
              :alt="product.name"
              @error="handleImageError($event, String(product.id))"
            />
            <span>
              <strong>{{ product.name }}</strong>
              <small>{{ formatPrice(product.price) }}</small>
            </span>
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.shop-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 4%, rgba(47, 143, 103, 0.14), transparent 28%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 48%, #f8faf7 100%);
  color: #111827;
}

main {
  overflow: hidden;
}

.commerce-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr);
  gap: 42px;
  max-width: 1240px;
  margin: 0 auto;
  padding: 64px 24px 38px;
}

.commerce-hero::before {
  position: absolute;
  right: 18px;
  bottom: 6px;
  left: 18px;
  z-index: 0;
  height: 220px;
  border-radius: 32px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.2), rgba(47, 143, 103, 0.08));
  content: '';
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 1;
  align-self: center;
  min-width: 0;
}

.hero-kicker,
.section-heading span,
.toolbar span {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
  color: #2f8f67;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-copy h1 {
  max-width: 620px;
  margin: 0;
  color: #111827;
  font-size: 56px;
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 540px;
  margin: 18px 0 0;
  color: #4b5563;
  font-size: 17px;
  line-height: 1.8;
}

.hero-search {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  max-width: 560px;
  margin-top: 30px;
  padding: 8px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.hero-search :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 12px;
  box-shadow: none;
}

.hero-search :deep(.el-button) {
  min-width: 92px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e5484d, #c92432);
  border-color: transparent;
  box-shadow: 0 12px 22px rgba(229, 72, 77, 0.22);
  font-weight: 900;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.hero-metrics span {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  min-height: 36px;
  padding: 8px 12px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  color: #4b5563;
}

.hero-metrics strong {
  color: #111827;
  font-size: 20px;
}

.hero-showcase {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 176px;
  gap: 16px;
  min-width: 0;
}

.hero-feature,
.hero-mini,
.product-card,
.brand-row,
.recommend-item,
.category-chip,
.text-link {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.hero-feature {
  position: relative;
  min-height: 520px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 32px 70px rgba(17, 24, 39, 0.16);
}

.hero-feature img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-feature::after {
  position: absolute;
  inset: auto 0 0 0;
  height: 44%;
  background: linear-gradient(180deg, rgba(17, 24, 39, 0), rgba(17, 24, 39, 0.78));
  content: '';
}

.hero-feature__info {
  position: absolute;
  right: 22px;
  bottom: 22px;
  left: 22px;
  z-index: 1;
  color: #fff;
  text-align: left;
}

.hero-feature__info small,
.hero-feature__info strong,
.hero-feature__info em {
  display: block;
}

.hero-feature__info small {
  font-size: 13px;
  font-weight: 800;
}

.hero-feature__info strong {
  margin-top: 6px;
  font-size: 28px;
  line-height: 1.2;
}

.hero-feature__info em {
  margin-top: 10px;
  color: #ffd166;
  font-size: 22px;
  font-style: normal;
  font-weight: 900;
}

.hero-mini-list {
  display: grid;
  gap: 16px;
}

.hero-mini {
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(17, 24, 39, 0.05);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-mini:hover {
  transform: translateY(-3px);
  box-shadow: 0 24px 44px rgba(17, 24, 39, 0.12);
}

.hero-mini img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.hero-mini span {
  display: -webkit-box;
  min-height: 48px;
  padding: 10px 12px;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.35;
  text-align: left;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.content-band {
  max-width: 1240px;
  margin: 0 auto;
  padding: 34px 24px;
}

.section-heading,
.toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.section-heading.compact {
  margin-bottom: 16px;
}

.section-heading h2,
.toolbar h2 {
  margin: 0;
  color: #111827;
  font-size: 26px;
  line-height: 1.2;
}

.text-link {
  padding: 0;
  background: transparent;
  color: #e5484d;
  font-weight: 800;
}

.category-rail {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 6px;
}

.category-chip {
  flex: 0 0 auto;
  min-width: 80px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(17, 24, 39, 0.07);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #374151;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
  font-weight: 800;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.category-chip.active,
.category-chip:hover {
  border-color: transparent;
  background: linear-gradient(135deg, #111827, #2f8f67);
  color: #fff;
  box-shadow: 0 12px 24px rgba(17, 24, 39, 0.14);
  transform: translateY(-1px);
}

.product-layout {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.brand-panel {
  position: sticky;
  top: 96px;
  padding: 18px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 22px 46px rgba(15, 23, 42, 0.07);
}

.brand-row {
  display: grid;
  grid-template-columns: 42px 1fr;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 56px;
  padding: 8px;
  border-radius: 14px;
  background: transparent;
  color: #111827;
  text-align: left;
  transition: background 0.18s ease, transform 0.18s ease;
}

.brand-row + .brand-row {
  margin-top: 6px;
}

.brand-row.active,
.brand-row:hover {
  background: #f1f5f9;
  transform: translateX(2px);
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  object-fit: cover;
  background: #eef2f7;
}

.text-logo {
  display: grid;
  place-items: center;
  color: #fff;
  background: #2f8f67;
  font-size: 12px;
  font-weight: 900;
}

.product-main {
  min-width: 0;
}

.toolbar {
  padding: 18px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
}

.toolbar :deep(.el-segmented) {
  border-radius: 999px;
  background: #f3f4f6;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  min-height: 320px;
}

.product-card {
  position: relative;
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  color: #111827;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  border-color: rgba(229, 72, 77, 0.2);
  box-shadow: 0 24px 48px rgba(17, 24, 39, 0.12);
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
  border-radius: 15px;
  background: #f3f4f6;
}

.product-image img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  transition: transform 0.28s ease;
}

.product-card:hover .product-image img {
  transform: scale(1.045);
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

.recommend-band {
  padding-bottom: 64px;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.recommend-item {
  display: grid;
  grid-template-columns: 78px 1fr;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.recommend-item:hover {
  border-color: rgba(229, 72, 77, 0.42);
  box-shadow: 0 18px 34px rgba(17, 24, 39, 0.1);
  transform: translateY(-2px);
}

.recommend-item img {
  width: 78px;
  height: 78px;
  border-radius: 14px;
  object-fit: cover;
}

.recommend-item strong,
.recommend-item small {
  display: block;
}

.recommend-item strong {
  display: -webkit-box;
  overflow: hidden;
  font-weight: 900;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.recommend-item small {
  margin-top: 8px;
  color: #e5484d;
  font-weight: 900;
}

@media (max-width: 1080px) {
  .commerce-hero,
  .product-layout {
    grid-template-columns: 1fr;
  }

  .hero-showcase {
    grid-template-columns: 1fr;
  }

  .hero-mini-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .brand-panel {
    position: static;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recommend-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .commerce-hero {
    padding: 34px 16px 20px;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .hero-search {
    grid-template-columns: 1fr;
  }

  .hero-feature {
    min-height: 360px;
  }

  .hero-mini-list,
  .product-grid,
  .recommend-list {
    grid-template-columns: 1fr;
  }

  .content-band {
    padding: 26px 16px;
  }

  .section-heading,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar :deep(.el-segmented) {
    overflow-x: auto;
  }
}
</style>
