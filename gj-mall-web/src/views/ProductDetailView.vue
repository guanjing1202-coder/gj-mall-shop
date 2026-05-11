<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { getProductDetail, type ProductDetail, type ProductSku } from '@/api/product'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const loading = ref(false)
const adding = ref(false)
const quantity = ref(1)
const product = ref<ProductDetail>()
const selectedSkuId = ref<string | number>()
const activeImage = ref('')

const currentSku = computed(() => {
  return product.value?.skus?.find((sku) => sku.id === selectedSkuId.value)
})

const gallery = computed(() => {
  const detail = product.value
  if (!detail) {
    return []
  }
  const images = [detail.mainImage, ...(detail.images || []), ...(detail.detailImages || [])]
  return Array.from(new Set(images.filter(Boolean))) as string[]
})

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
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/720/720`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/720/720`
}

function skuLabel(sku: ProductSku) {
  const specs = sku.specData ? Object.values(sku.specData).filter(Boolean).join(' / ') : ''
  return specs || sku.name || sku.skuCode || '默认规格'
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id as string)
    product.value = res.data
    selectedSkuId.value = res.data.skus?.[0]?.id
    activeImage.value = normalizeImage(res.data.mainImage || res.data.images?.[0], String(res.data.id))
  } finally {
    loading.value = false
  }
}

function selectSku(sku: ProductSku) {
  selectedSkuId.value = sku.id
  quantity.value = 1
  if (sku.image) {
    activeImage.value = normalizeImage(sku.image, String(sku.id))
  }
}

async function addToCart(redirectToCart = false) {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  const sku = currentSku.value
  if (!sku) {
    ElMessage.warning('请选择商品规格')
    return
  }
  adding.value = true
  try {
    await cart.add(sku.id, quantity.value)
    if (redirectToCart) {
      router.push('/cart')
    }
  } finally {
    adding.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="detail-page">
    <ShopHeader />

    <main v-loading="loading">
      <section v-if="product" class="detail-shell">
        <div class="breadcrumb">
          <button type="button" @click="router.push('/')">首页</button>
          <span>/</span>
          <span>{{ product.categoryName || '商品详情' }}</span>
          <span>/</span>
          <strong>{{ product.name }}</strong>
        </div>

        <section class="product-hero">
          <div class="gallery-panel">
            <div class="main-image">
              <img
                :src="normalizeImage(activeImage || product.mainImage, String(product.id))"
                :alt="product.name"
                @error="handleImageError($event, String(product.id))"
              />
            </div>
            <div class="thumb-row">
              <button
                v-for="image in gallery.slice(0, 6)"
                :key="image"
                class="thumb"
                :class="{ active: normalizeImage(image, String(product?.id)) === activeImage }"
                type="button"
                @click="activeImage = normalizeImage(image, String(product?.id))"
              >
                <img
                  :src="normalizeImage(image, String(product.id))"
                  :alt="product.name"
                  @error="handleImageError($event, String(product.id))"
                />
              </button>
            </div>
          </div>

          <div class="buy-panel">
            <div class="tag-row">
              <span v-if="product.newStatus === 1">新品</span>
              <span v-if="product.recommendStatus === 1" class="hot">推荐</span>
              <span>{{ product.brandName || '精选品牌' }}</span>
            </div>

            <h1>{{ product.name }}</h1>
            <p>{{ product.subTitle }}</p>

            <div class="price-box">
              <strong>{{ formatPrice(currentSku?.price || product.price) }}</strong>
              <span>已售 {{ formatSales(product.saleCount) }}</span>
            </div>

            <div v-if="product.skus?.length" class="sku-section">
              <div class="section-title">选择规格</div>
              <div class="sku-grid">
                <button
                  v-for="sku in product.skus"
                  :key="sku.id"
                  class="sku-chip"
                  :class="{ active: selectedSkuId === sku.id }"
                  type="button"
                  @click="selectSku(sku)"
                >
                  <strong>{{ skuLabel(sku) }}</strong>
                  <small>库存 {{ sku.stock ?? 0 }}</small>
                </button>
              </div>
            </div>

            <div class="quantity-section">
              <div class="section-title">购买数量</div>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="Math.max(1, currentSku?.stock || 1)"
                controls-position="right"
              />
            </div>

            <div class="service-row">
              <span>正品保障</span>
              <span>极速发货</span>
              <span>售后无忧</span>
            </div>

            <div class="buy-actions">
              <el-button size="large" type="primary" :loading="adding" @click="addToCart(true)">立即购买</el-button>
              <el-button size="large" :loading="adding" @click="addToCart(false)">加入购物车</el-button>
            </div>
          </div>
        </section>

        <section class="detail-content">
          <div class="detail-main">
            <div class="section-heading">
              <span>Details</span>
              <h2>商品详情</h2>
            </div>
            <div v-if="product.detailHtml" class="rich-detail" v-html="product.detailHtml" />
            <div v-else class="detail-fallback">
              <p>{{ product.subTitle || '这件商品暂未配置图文详情，基础信息已经接入真实商品接口。' }}</p>
            </div>

            <div v-if="product.detailImages?.length" class="detail-images">
              <img
                v-for="image in product.detailImages"
                :key="image"
                :src="normalizeImage(image, String(product.id))"
                :alt="product.name"
                @error="handleImageError($event, String(product.id))"
              />
            </div>
          </div>

          <aside class="promise-panel">
            <div class="section-heading compact">
              <span>Service</span>
              <h2>购买保障</h2>
            </div>
            <dl>
              <div>
                <dt>包装清单</dt>
                <dd>{{ product.packingList || '商品主件、说明资料、官方包装' }}</dd>
              </div>
              <div>
                <dt>售后服务</dt>
                <dd>{{ product.afterSale || '支持平台售后流程，订单、退款、退货可在商城系统内闭环处理。' }}</dd>
              </div>
              <div>
                <dt>库存状态</dt>
                <dd>{{ currentSku?.stock ?? 0 }} 件可售</dd>
              </div>
            </dl>
          </aside>
        </section>
      </section>

      <el-empty v-else-if="!loading" description="商品不存在">
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </el-empty>
    </main>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

main {
  min-height: calc(100vh - 72px);
}

.detail-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px;
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 22px;
  color: #6b7280;
  font-size: 14px;
}

.breadcrumb button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #e5484d;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
}

.breadcrumb strong {
  color: #111827;
}

.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 440px;
  gap: 30px;
  align-items: start;
}

.gallery-panel,
.buy-panel,
.detail-main,
.promise-panel {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.gallery-panel {
  padding: 18px;
}

.main-image {
  overflow: hidden;
  border-radius: 8px;
  background: #f3f4f6;
}

.main-image img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.thumb-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.thumb,
.sku-chip {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.thumb {
  overflow: hidden;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 8px;
  background: #f3f4f6;
}

.thumb.active,
.thumb:hover {
  border-color: #e5484d;
}

.thumb img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.buy-panel {
  padding: 28px;
}

.tag-row,
.service-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span,
.service-row span {
  min-height: 28px;
  padding: 5px 10px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-weight: 900;
}

.tag-row span.hot {
  background: #fff0f0;
  color: #e5484d;
}

.buy-panel h1 {
  margin: 18px 0 0;
  font-size: 34px;
  line-height: 1.18;
  letter-spacing: 0;
}

.buy-panel p {
  margin: 14px 0 0;
  color: #4b5563;
  font-size: 16px;
  line-height: 1.75;
}

.price-box {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  padding: 18px;
  border-radius: 8px;
  background: #111827;
  color: #fff;
}

.price-box strong {
  color: #ffd166;
  font-size: 32px;
}

.price-box span {
  color: rgba(255, 255, 255, 0.76);
}

.sku-section {
  margin-top: 24px;
}

.quantity-section {
  margin-top: 22px;
}

.section-title {
  margin-bottom: 12px;
  color: #111827;
  font-weight: 900;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sku-chip {
  min-height: 68px;
  padding: 12px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 8px;
  background: #fff;
  color: #111827;
  text-align: left;
}

.sku-chip.active,
.sku-chip:hover {
  border-color: #e5484d;
  box-shadow: 0 0 0 2px rgba(229, 72, 77, 0.08);
}

.sku-chip strong,
.sku-chip small {
  display: block;
}

.sku-chip small {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
}

.service-row {
  margin-top: 22px;
}

.buy-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 24px;
}

.buy-actions :deep(.el-button) {
  height: 46px;
  border-radius: 8px;
  font-weight: 900;
}

.buy-actions :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.detail-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  margin-top: 28px;
  align-items: start;
}

.detail-main,
.promise-panel {
  padding: 24px;
}

.section-heading {
  margin-bottom: 18px;
}

.section-heading span {
  display: inline-flex;
  margin-bottom: 10px;
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.section-heading h2 {
  margin: 0;
  font-size: 26px;
}

.section-heading.compact h2 {
  font-size: 22px;
}

.rich-detail,
.detail-fallback {
  color: #374151;
  line-height: 1.9;
}

.rich-detail :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.detail-images {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.detail-images img {
  width: 100%;
  border-radius: 8px;
}

.promise-panel {
  position: sticky;
  top: 96px;
}

.promise-panel dl,
.promise-panel dd {
  margin: 0;
}

.promise-panel div + div {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.promise-panel dt {
  color: #111827;
  font-weight: 900;
}

.promise-panel dd {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .product-hero,
  .detail-content {
    grid-template-columns: 1fr;
  }

  .promise-panel {
    position: static;
  }
}

@media (max-width: 680px) {
  .detail-shell {
    padding: 18px 16px 32px;
  }

  .buy-panel {
    padding: 20px;
  }

  .buy-panel h1 {
    font-size: 28px;
  }

  .thumb-row,
  .sku-grid,
  .buy-actions {
    grid-template-columns: 1fr;
  }
}
</style>
