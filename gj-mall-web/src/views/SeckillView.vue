<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  addAddress,
  getAddressList,
  type AddressItem,
  type AddressPayload,
} from '@/api/address'
import {
  createSeckillOrder,
  getActiveSeckills,
  getSeckillDetail,
  type SeckillActivity,
  type SeckillSku,
} from '@/api/seckill'
import type { ApiId } from '@/api/product'
import { getOrderByNo } from '@/api/order'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const detailLoading = ref(false)
const addressLoading = ref(false)
const addressSaving = ref(false)
const buyingSkuId = ref<ApiId | ''>('')
const addressDialogOpen = ref(false)
const selectedAddressId = ref<ApiId | ''>('')
const currentActivityId = ref<ApiId | ''>('')
const activities = ref<SeckillActivity[]>([])
const currentActivity = ref<SeckillActivity>()
const addresses = ref<AddressItem[]>([])

const addressForm = reactive<AddressPayload>({
  receiver: '',
  phone: '',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  detail: '',
  postCode: '',
  isDefault: 1,
})

const skus = computed(() => currentActivity.value?.skus || [])
const selectedAddress = computed(() => {
  return addresses.value.find((item) => String(item.id) === String(selectedAddressId.value))
})
const totalRemain = computed(() => skus.value.reduce((sum, item) => sum + Number(item.remainStock || 0), 0))
const totalSold = computed(() => skus.value.reduce((sum, item) => sum + Number(item.soldCount || 0), 0))

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatTime(value?: string) {
  if (!value) return '-'
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) return value.slice(0, 16).replace('T', ' ')
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'seckill') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/520/520`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/520/520`
}

function specText(item: SeckillSku) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : item.skuName || '默认规格'
}

function addressLine(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

function stockPercent(item: SeckillSku) {
  const remain = Number(item.remainStock || 0)
  const sold = Number(item.soldCount || 0)
  const total = Math.max(remain + sold, 1)
  return Math.max(4, Math.min(100, (remain / total) * 100))
}

function discountText(item: SeckillSku) {
  const original = Number(item.originalPrice || 0)
  const seckill = Number(item.seckillPrice || 0)
  if (!original || !seckill || seckill >= original) return '秒杀价'
  return `${(seckill / original * 10).toFixed(1)}折`
}

function canBuy(item: SeckillSku) {
  return Number(item.remainStock || 0) > 0
}

async function loadActivities() {
  loading.value = true
  try {
    const res = await getActiveSeckills()
    activities.value = res.data || []
    const existing = activities.value.find((item) => String(item.id) === String(currentActivityId.value))
    const next = existing || activities.value[0]
    if (next) {
      await selectActivity(next)
    } else {
      currentActivity.value = undefined
      currentActivityId.value = ''
    }
  } finally {
    loading.value = false
  }
}

async function selectActivity(activity: SeckillActivity) {
  currentActivityId.value = activity.id
  detailLoading.value = true
  try {
    const res = await getSeckillDetail(activity.id)
    currentActivity.value = res.data || activity
  } finally {
    detailLoading.value = false
  }
}

async function loadAddresses() {
  if (!auth.isLoggedIn) {
    addresses.value = []
    selectedAddressId.value = ''
    return
  }
  addressLoading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data || []
    const current = addresses.value.find((item) => String(item.id) === String(selectedAddressId.value))
    if (!current && addresses.value.length) {
      selectedAddressId.value = addresses.value.find((item) => item.isDefault === 1)?.id || addresses.value[0].id
    } else if (!addresses.value.length) {
      selectedAddressId.value = ''
    }
  } finally {
    addressLoading.value = false
  }
}

function openAddressDialog() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  Object.assign(addressForm, {
    receiver: '',
    phone: '',
    province: '广东省',
    city: '深圳市',
    district: '南山区',
    detail: '',
    postCode: '',
    isDefault: addresses.value.length ? 0 : 1,
  })
  addressDialogOpen.value = true
}

async function saveAddress() {
  if (!addressForm.receiver.trim() || !addressForm.phone.trim() || !addressForm.detail.trim()) {
    ElMessage.warning('请完整填写收件人、手机号和详细地址')
    return
  }
  addressSaving.value = true
  try {
    const res = await addAddress({
      receiver: addressForm.receiver.trim(),
      phone: addressForm.phone.trim(),
      province: addressForm.province.trim(),
      city: addressForm.city.trim(),
      district: addressForm.district.trim(),
      detail: addressForm.detail.trim(),
      postCode: addressForm.postCode?.trim() || undefined,
      isDefault: addressForm.isDefault,
    })
    selectedAddressId.value = res.data
    addressDialogOpen.value = false
    await loadAddresses()
    ElMessage.success('地址已保存')
  } finally {
    addressSaving.value = false
  }
}

async function buy(item: SeckillSku) {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (!canBuy(item)) return
  buyingSkuId.value = item.id
  try {
    const created = await createSeckillOrder(item.id, selectedAddressId.value)
    const detail = await getOrderByNo(created.data)
    ElMessage.success('抢购成功，已生成订单')
    router.push(`/order/${detail.data.id}?pay=1`)
  } finally {
    buyingSkuId.value = ''
    loadActivities()
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadAddresses()
    } else {
      addresses.value = []
      selectedAddressId.value = ''
    }
  },
)

onMounted(async () => {
  await Promise.all([loadActivities(), loadAddresses()])
})
</script>

<template>
  <div class="seckill-page">
    <ShopHeader />

    <main class="seckill-shell">
      <section class="seckill-hero">
        <div>
          <span>Flash Sale</span>
          <h1>限时秒杀</h1>
          <p>实时库存、限购提醒和快捷下单集中在这里，先选地址再开抢。</p>
        </div>
        <div class="hero-actions">
          <button type="button" @click="loadActivities">刷新活动</button>
          <button type="button" @click="router.push('/products')">全部商品</button>
        </div>
      </section>

      <section v-if="loading" v-loading="loading" class="empty-panel" />

      <section v-else-if="!activities.length" class="empty-card">
        <h2>暂无进行中的秒杀</h2>
        <p>可以先去商品列表看看常规在售商品。</p>
        <el-button type="primary" size="large" @click="router.push('/products')">去逛逛</el-button>
      </section>

      <template v-else>
        <section class="activity-rail" aria-label="秒杀活动">
          <button
            v-for="item in activities"
            :key="item.id"
            type="button"
            :class="{ active: String(currentActivityId) === String(item.id) }"
            @click="selectActivity(item)"
          >
            <strong>{{ item.name }}</strong>
            <span>{{ item.statusDesc || '进行中' }}</span>
          </button>
        </section>

        <section class="overview-grid">
          <article class="overview-card main-card">
            <span>Current Event</span>
            <h2>{{ currentActivity?.name || '秒杀活动' }}</h2>
            <p>{{ formatTime(currentActivity?.startTime) }} - {{ formatTime(currentActivity?.endTime) }}</p>
          </article>
          <article class="overview-card">
            <span>Remain</span>
            <strong>{{ totalRemain }}</strong>
            <p>剩余库存</p>
          </article>
          <article class="overview-card">
            <span>Sold</span>
            <strong>{{ totalSold }}</strong>
            <p>已抢数量</p>
          </article>
        </section>

        <section class="address-panel">
          <div class="section-heading">
            <div>
              <span>Address</span>
              <h2>收货地址</h2>
            </div>
            <button type="button" @click="openAddressDialog">新增地址</button>
          </div>

          <div v-if="!auth.isLoggedIn" class="login-card">
            <strong>登录后参与秒杀</strong>
            <span>秒杀订单需要绑定会员和收货地址。</span>
            <el-button type="primary" @click="auth.openLoginDialog()">立即登录</el-button>
          </div>

          <div v-else v-loading="addressLoading" class="address-list">
            <article
              v-for="item in addresses"
              :key="item.id"
              class="address-item"
              :class="{ active: String(selectedAddressId) === String(item.id) }"
              @click="selectedAddressId = item.id"
            >
              <div>
                <strong>{{ item.receiver }}</strong>
                <em v-if="item.isDefault === 1">默认</em>
              </div>
              <span>{{ item.phone }}</span>
              <p>{{ addressLine(item) }}</p>
            </article>
            <button v-if="!addresses.length" class="address-empty" type="button" @click="openAddressDialog">
              <strong>新增收货地址</strong>
              <span>秒杀下单需要先选择地址</span>
            </button>
          </div>

          <div v-if="selectedAddress" class="selected-address">
            <strong>当前收货：{{ selectedAddress.receiver }} {{ selectedAddress.phone }}</strong>
            <span>{{ addressLine(selectedAddress) }}</span>
          </div>
        </section>

        <section v-loading="detailLoading" class="sku-panel">
          <div class="section-heading">
            <div>
              <span>Items</span>
              <h2>秒杀商品</h2>
            </div>
            <strong>{{ skus.length }} 款</strong>
          </div>

          <div v-if="skus.length" class="sku-grid">
            <article v-for="item in skus" :key="item.id" class="sku-card">
              <button class="sku-image" type="button" @click="router.push(`/product/${item.spuId}`)">
                <img
                  :src="normalizeImage(item.image, String(item.skuId))"
                  :alt="item.spuName || item.skuName"
                  @error="handleImageError($event, String(item.skuId))"
                />
                <em>{{ discountText(item) }}</em>
              </button>
              <div class="sku-copy">
                <div class="sku-head">
                  <strong>{{ item.spuName || item.skuName || '秒杀商品' }}</strong>
                  <span>限购 {{ item.seckillLimit || 1 }} 件</span>
                </div>
                <p>{{ specText(item) }}</p>
                <div class="stock-bar">
                  <i :style="{ width: `${stockPercent(item)}%` }" />
                </div>
                <div class="stock-line">
                  <span>剩余 {{ item.remainStock || 0 }}</span>
                  <span>已抢 {{ item.soldCount || 0 }}</span>
                </div>
                <div class="price-row">
                  <div>
                    <strong>{{ formatPrice(item.seckillPrice) }}</strong>
                    <span>{{ formatPrice(item.originalPrice) }}</span>
                  </div>
                  <el-button
                    type="primary"
                    :loading="String(buyingSkuId) === String(item.id)"
                    :disabled="!canBuy(item)"
                    @click="buy(item)"
                  >
                    {{ canBuy(item) ? '立即抢购' : '已抢光' }}
                  </el-button>
                </div>
              </div>
            </article>
          </div>

          <el-empty v-else description="本场活动暂无商品">
            <el-button type="primary" @click="loadActivities">重新加载</el-button>
          </el-empty>
        </section>
      </template>
    </main>

    <el-dialog v-model="addressDialogOpen" title="新增收货地址" width="520px" class="address-dialog">
      <el-form label-position="top" @submit.prevent>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="收件人">
              <el-input v-model="addressForm.receiver" placeholder="姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="addressForm.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="省份">
              <el-input v-model="addressForm.province" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市">
              <el-input v-model="addressForm.city" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县">
              <el-input v-model="addressForm.district" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址">
          <el-input v-model="addressForm.detail" placeholder="街道、小区、门牌号" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="邮编">
              <el-input v-model="addressForm.postCode" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认地址">
              <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="addressSaving" @click="saveAddress">保存地址</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.seckill-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.seckill-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.seckill-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 34px;
  border-radius: 18px;
  background: #111827;
  color: #fff;
}

.seckill-hero span,
.overview-card span,
.section-heading span {
  display: inline-flex;
  margin-bottom: 10px;
  color: #ffd166;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.seckill-hero h1 {
  margin: 0;
  font-size: 46px;
  line-height: 1.1;
}

.seckill-hero p {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.hero-actions button,
.activity-rail button,
.section-heading button,
.address-empty,
.sku-image {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.hero-actions button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  background: #fff;
  color: #111827;
  font-weight: 900;
}

.hero-actions button:first-child {
  background: #ffd166;
}

.empty-panel,
.empty-card,
.overview-card,
.address-panel,
.sku-panel {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.empty-panel {
  min-height: 320px;
}

.empty-card {
  padding: 56px;
  text-align: center;
}

.empty-card h2 {
  margin: 0;
  font-size: 28px;
}

.empty-card p {
  margin: 12px 0 24px;
  color: #6b7280;
}

.activity-rail {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  margin-bottom: 18px;
  padding-bottom: 4px;
}

.activity-rail button {
  display: grid;
  min-width: 200px;
  gap: 8px;
  padding: 16px 18px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
  text-align: left;
}

.activity-rail button.active,
.activity-rail button:hover {
  border-color: #e5484d;
  box-shadow: 0 12px 24px rgba(229, 72, 77, 0.12);
}

.activity-rail strong {
  color: #111827;
}

.activity-rail span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px 170px;
  gap: 16px;
  margin-bottom: 18px;
}

.overview-card {
  padding: 22px;
}

.overview-card span,
.section-heading span {
  color: #2f8f67;
}

.overview-card h2 {
  margin: 0;
  font-size: 26px;
}

.overview-card p {
  margin: 10px 0 0;
  color: #6b7280;
}

.overview-card > strong {
  color: #e5484d;
  font-size: 36px;
}

.address-panel,
.sku-panel {
  margin-top: 18px;
  padding: 22px;
}

.section-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-heading h2 {
  margin: 0;
  font-size: 24px;
}

.section-heading button {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 8px;
  background: #111827;
  color: #fff;
  font-weight: 900;
}

.section-heading > strong {
  color: #6b7280;
}

.login-card {
  display: grid;
  gap: 8px;
  padding: 22px;
  border: 1px dashed rgba(17, 24, 39, 0.18);
  border-radius: 8px;
  background: #fbfcf8;
}

.login-card span {
  color: #6b7280;
}

.login-card :deep(.el-button) {
  width: fit-content;
  margin-top: 10px;
  border-radius: 8px;
  font-weight: 900;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-height: 108px;
}

.address-item,
.address-empty {
  min-height: 132px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
  text-align: left;
}

.address-item {
  cursor: pointer;
}

.address-item.active {
  border-color: #e5484d;
  box-shadow: 0 0 0 2px rgba(229, 72, 77, 0.08);
}

.address-item div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.address-item em {
  padding: 3px 8px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.address-item span,
.address-item p,
.address-empty span {
  display: block;
  margin: 9px 0 0;
  color: #6b7280;
  line-height: 1.55;
}

.address-empty {
  display: grid;
  place-content: center;
  border-style: dashed;
}

.selected-address {
  display: grid;
  gap: 6px;
  margin-top: 14px;
  padding: 14px;
  border-radius: 8px;
  background: #fff7ed;
}

.selected-address span {
  color: #6b7280;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.sku-card {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 16px;
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
}

.sku-image {
  position: relative;
  overflow: hidden;
  padding: 0;
  border-radius: 8px;
  background: #f3f4f6;
}

.sku-image img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.sku-image em {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 5px 9px;
  border-radius: 999px;
  background: #e5484d;
  color: #fff;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.sku-copy {
  min-width: 0;
}

.sku-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.sku-head strong {
  display: -webkit-box;
  overflow: hidden;
  font-size: 18px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.sku-head span {
  flex: 0 0 auto;
  padding: 4px 8px;
  border-radius: 999px;
  background: #fff7ed;
  color: #92400e;
  font-size: 12px;
  font-weight: 900;
}

.sku-copy p {
  overflow: hidden;
  margin: 10px 0 0;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-bar {
  overflow: hidden;
  height: 10px;
  margin-top: 16px;
  border-radius: 999px;
  background: #f3f4f6;
}

.stock-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #e5484d;
}

.stock-line,
.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stock-line {
  margin-top: 8px;
  color: #9ca3af;
  font-size: 13px;
}

.price-row {
  margin-top: 18px;
}

.price-row strong,
.price-row span {
  display: block;
}

.price-row strong {
  color: #e5484d;
  font-size: 24px;
}

.price-row span {
  margin-top: 4px;
  color: #9ca3af;
  font-size: 13px;
  text-decoration: line-through;
}

.price-row :deep(.el-button) {
  flex: 0 0 auto;
  border-radius: 8px;
  font-weight: 900;
}

.price-row :deep(.el-button--primary),
.login-card :deep(.el-button--primary),
.empty-card :deep(.el-button--primary),
.address-dialog :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

:global(.address-dialog.el-dialog) {
  width: min(520px, calc(100vw - 32px));
  border-radius: 14px;
  overflow: hidden;
}

@media (max-width: 1080px) {
  .overview-grid,
  .sku-grid,
  .address-list {
    grid-template-columns: 1fr;
  }

  .sku-card {
    grid-template-columns: 160px minmax(0, 1fr);
  }
}

@media (max-width: 680px) {
  .seckill-shell {
    padding: 28px 16px 52px;
  }

  .seckill-hero,
  .section-heading,
  .sku-head,
  .price-row {
    align-items: start;
    flex-direction: column;
  }

  .seckill-hero {
    padding: 24px;
  }

  .seckill-hero h1 {
    font-size: 34px;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions button {
    flex: 1;
  }

  .sku-card {
    grid-template-columns: 1fr;
  }
}
</style>
