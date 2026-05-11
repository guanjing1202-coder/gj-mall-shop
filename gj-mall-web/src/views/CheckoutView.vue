<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  addAddress,
  deleteAddress as removeAddressApi,
  getAddressList,
  setDefaultAddress,
  updateAddress,
  type AddressItem,
  type AddressPayload,
} from '@/api/address'
import { getAvailableCoupons, type MyCoupon } from '@/api/coupon'
import { createOrder, getOrderByNo } from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { ApiId } from '@/api/product'
import type { CartItem } from '@/api/cart'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const loading = ref(false)
const addressLoading = ref(false)
const couponLoading = ref(false)
const submitting = ref(false)
const addressDialogOpen = ref(false)
const addressSaving = ref(false)
const editingAddressId = ref<ApiId>()
const selectedAddressId = ref<ApiId | ''>('')
const selectedCouponId = ref<ApiId | ''>('')
const remark = ref('')
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
const addresses = ref<AddressItem[]>([])
const coupons = ref<MyCoupon[]>([])

const selectedItems = computed(() => {
  return cart.items.filter((item) => item.selected === 1 && !item.invalid)
})
const selectedAddress = computed(() => {
  return addresses.value.find((item) => String(item.id) === String(selectedAddressId.value))
})
const selectedCoupon = computed(() => {
  return coupons.value.find((item) => String(item.couponId) === String(selectedCouponId.value))
})
const payableAmount = computed(() => Number(cart.selectedAmount || 0))
const freightAmount = computed(() => 0)
const couponDiscount = computed(() => {
  return selectedCoupon.value ? calcCouponDiscount(selectedCoupon.value, payableAmount.value) : 0
})
const finalAmount = computed(() => Math.max(payableAmount.value + freightAmount.value - couponDiscount.value, 0))

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function calcCouponDiscount(coupon: MyCoupon, amount: number) {
  if (amount <= 0) {
    return 0
  }
  if (coupon.type === 2) {
    const rate = Number(coupon.discountRate ?? 1)
    return Math.min(amount, Math.max(0, amount - amount * rate))
  }
  return Math.min(amount, Number(coupon.discountAmount || 0))
}

function formatCouponValue(coupon: MyCoupon) {
  if (coupon.type === 2) {
    const rate = Number(coupon.discountRate ?? 1)
    return `${(rate * 10).toFixed(rate * 10 === Math.trunc(rate * 10) ? 0 : 1)}折`
  }
  return `减 ${formatPrice(coupon.discountAmount)}`
}

function formatCouponLimit(coupon: MyCoupon) {
  const minAmount = Number(coupon.minAmount || 0)
  return minAmount > 0 ? `满 ${formatPrice(minAmount)} 可用` : '无门槛'
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'checkout') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
}

function specText(item: CartItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : item.skuName || '默认规格'
}

function addressLine(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

function resetAddressForm(address?: AddressItem) {
  editingAddressId.value = address?.id
  Object.assign(addressForm, {
    receiver: address?.receiver || '',
    phone: address?.phone || '',
    province: address?.province || '广东省',
    city: address?.city || '深圳市',
    district: address?.district || '南山区',
    detail: address?.detail || '',
    postCode: address?.postCode || '',
    isDefault: address?.isDefault ?? (addresses.value.length ? 0 : 1),
  })
}

function openAddressDialog(address?: AddressItem) {
  resetAddressForm(address)
  addressDialogOpen.value = true
}

async function loadAddresses() {
  addressLoading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data || []
    const current = addresses.value.find((item) => String(item.id) === String(selectedAddressId.value))
    if (!current && addresses.value.length) {
      const defaultAddress = addresses.value.find((item) => item.isDefault === 1)
      selectedAddressId.value = defaultAddress?.id || addresses.value[0].id
    } else if (!addresses.value.length) {
      selectedAddressId.value = ''
    }
  } finally {
    addressLoading.value = false
  }
}

async function loadCoupons() {
  if (!auth.isLoggedIn || payableAmount.value <= 0) {
    coupons.value = []
    selectedCouponId.value = ''
    return
  }
  couponLoading.value = true
  try {
    const res = await getAvailableCoupons(payableAmount.value)
    coupons.value = res.data || []
    const stillAvailable = coupons.value.some((item) => String(item.couponId) === String(selectedCouponId.value))
    if (!stillAvailable) {
      const bestCoupon = [...coupons.value].sort((a, b) => {
        return calcCouponDiscount(b, payableAmount.value) - calcCouponDiscount(a, payableAmount.value)
      })[0]
      selectedCouponId.value = bestCoupon?.couponId || ''
    }
  } finally {
    couponLoading.value = false
  }
}

async function saveAddress() {
  if (!addressForm.receiver.trim() || !addressForm.phone.trim() || !addressForm.detail.trim()) {
    ElMessage.warning('请完整填写收件人、手机号和详细地址')
    return
  }
  addressSaving.value = true
  try {
    const payload: AddressPayload = {
      ...addressForm,
      receiver: addressForm.receiver.trim(),
      phone: addressForm.phone.trim(),
      province: addressForm.province.trim(),
      city: addressForm.city.trim(),
      district: addressForm.district.trim(),
      detail: addressForm.detail.trim(),
      postCode: addressForm.postCode?.trim() || undefined,
    }
    if (editingAddressId.value) {
      await updateAddress({ ...payload, id: editingAddressId.value })
      selectedAddressId.value = editingAddressId.value
    } else {
      const res = await addAddress(payload)
      selectedAddressId.value = res.data
    }
    addressDialogOpen.value = false
    await loadAddresses()
    ElMessage.success('地址已保存')
  } finally {
    addressSaving.value = false
  }
}

async function removeAddress(address: AddressItem) {
  await ElMessageBox.confirm('确认删除这个收货地址？', '删除地址', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await removeAddressApi(address.id)
  if (String(selectedAddressId.value) === String(address.id)) {
    selectedAddressId.value = ''
  }
  await loadAddresses()
}

async function makeDefault(address: AddressItem) {
  await setDefaultAddress(address.id)
  selectedAddressId.value = address.id
  await loadAddresses()
  ElMessage.success('默认地址已更新')
}

async function initialize() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    await Promise.all([cart.fetchCart(), loadAddresses()])
    await loadCoupons()
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  if (!selectedItems.value.length) {
    ElMessage.warning('请先选择要结算的商品')
    router.push('/cart')
    return
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const created = await createOrder({
      addressId: selectedAddressId.value,
      remark: remark.value.trim() || undefined,
      couponId: selectedCoupon.value?.couponId,
    })
    await cart.fetchCart()
    const detail = await getOrderByNo(created.data)
    ElMessage.success('订单已提交')
    router.push(`/order/${detail.data.id}?pay=1`)
  } finally {
    submitting.value = false
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      initialize()
    } else {
      coupons.value = []
      selectedCouponId.value = ''
    }
  },
)

watch(payableAmount, () => {
  loadCoupons()
})

onMounted(initialize)
</script>

<template>
  <div class="checkout-page">
    <ShopHeader />

    <main class="checkout-shell">
      <section class="checkout-hero">
        <div>
          <span>Checkout</span>
          <h1>确认订单</h1>
          <p>核对收货地址、商品明细和应付金额后提交订单。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/cart')">返回购物车</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后结算</h2>
        <p>订单会绑定到当前会员账户，请先登录再继续。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else v-loading="loading" class="checkout-layout">
        <div class="checkout-main">
          <section class="checkout-block">
            <div class="block-heading">
              <div>
                <span>Address</span>
                <h2>收货地址</h2>
              </div>
              <button class="plain-action" type="button" @click="openAddressDialog()">新增地址</button>
            </div>

            <div v-loading="addressLoading" class="address-grid">
              <article
                v-for="address in addresses"
                :key="address.id"
                class="address-item"
                :class="{ active: String(selectedAddressId) === String(address.id) }"
                @click="selectedAddressId = address.id"
              >
                <div class="address-top">
                  <el-radio
                    :model-value="selectedAddressId"
                    :label="address.id"
                    @change="selectedAddressId = address.id"
                  >
                    {{ address.receiver }}
                  </el-radio>
                  <em v-if="address.isDefault === 1">默认</em>
                </div>
                <strong>{{ address.phone }}</strong>
                <p>{{ addressLine(address) }}</p>
                <div class="address-actions">
                  <button type="button" @click.stop="openAddressDialog(address)">编辑</button>
                  <button v-if="address.isDefault !== 1" type="button" @click.stop="makeDefault(address)">
                    设为默认
                  </button>
                  <button type="button" @click.stop="removeAddress(address)">删除</button>
                </div>
              </article>

              <button v-if="!addresses.length" class="address-empty" type="button" @click="openAddressDialog()">
                <strong>新增第一个收货地址</strong>
                <span>用于生成订单收货快照</span>
              </button>
            </div>
          </section>

          <section class="checkout-block">
            <div class="block-heading">
              <div>
                <span>Items</span>
                <h2>商品清单</h2>
              </div>
              <strong>{{ selectedItems.length }} 件</strong>
            </div>

            <article v-for="item in selectedItems" :key="item.skuId" class="order-item">
              <img
                :src="normalizeImage(item.image, String(item.skuId))"
                :alt="item.spuName || item.skuName"
                @error="handleImageError($event, String(item.skuId))"
              />
              <div class="item-copy">
                <strong>{{ item.spuName || item.skuName || '商品' }}</strong>
                <span>{{ specText(item) }}</span>
              </div>
              <div class="item-qty">x{{ item.quantity }}</div>
              <div class="item-price">{{ formatPrice(item.totalAmount) }}</div>
            </article>

            <el-empty v-if="!selectedItems.length" description="没有已选商品">
              <el-button type="primary" @click="router.push('/cart')">返回购物车选择</el-button>
            </el-empty>
          </section>

          <section class="checkout-block">
            <div class="block-heading">
              <div>
                <span>Coupon</span>
                <h2>优惠券</h2>
              </div>
              <strong v-if="selectedCoupon">已优惠 {{ formatPrice(couponDiscount) }}</strong>
            </div>

            <div v-loading="couponLoading" class="coupon-grid">
              <article
                class="coupon-item no-coupon"
                :class="{ active: !selectedCouponId }"
                @click="selectedCouponId = ''"
              >
                <el-radio :model-value="selectedCouponId" label="" @change="selectedCouponId = ''">
                  不使用优惠券
                </el-radio>
                <p>保留本次可用优惠，直接按商品金额结算。</p>
              </article>

              <article
                v-for="coupon in coupons"
                :key="coupon.id"
                class="coupon-item"
                :class="{ active: String(selectedCouponId) === String(coupon.couponId) }"
                @click="selectedCouponId = coupon.couponId"
              >
                <div class="coupon-top">
                  <el-radio
                    :model-value="selectedCouponId"
                    :label="coupon.couponId"
                    @change="selectedCouponId = coupon.couponId"
                  >
                    {{ coupon.name }}
                  </el-radio>
                  <em>{{ coupon.typeDesc || '优惠券' }}</em>
                </div>
                <strong>{{ formatCouponValue(coupon) }}</strong>
                <p>{{ formatCouponLimit(coupon) }} · 有效期至 {{ formatDate(coupon.endTime) }}</p>
              </article>

              <el-empty v-if="!couponLoading && !coupons.length" description="暂无可用优惠券" />
            </div>
          </section>

          <section class="checkout-block">
            <div class="block-heading">
              <div>
                <span>Remark</span>
                <h2>订单备注</h2>
              </div>
            </div>
            <el-input
              v-model="remark"
              type="textarea"
              :rows="4"
              maxlength="120"
              show-word-limit
              placeholder="选填，给商家的留言"
            />
          </section>
        </div>

        <aside class="pay-panel">
          <span>Payment</span>
          <h2>应付金额</h2>
          <dl>
            <div>
              <dt>商品金额</dt>
              <dd>{{ formatPrice(payableAmount) }}</dd>
            </div>
            <div>
              <dt>运费</dt>
              <dd>{{ formatPrice(freightAmount) }}</dd>
            </div>
            <div>
              <dt>优惠</dt>
              <dd>- {{ formatPrice(couponDiscount) }}</dd>
            </div>
            <div class="total-row">
              <dt>合计</dt>
              <dd>{{ formatPrice(finalAmount) }}</dd>
            </div>
          </dl>
          <div v-if="selectedCoupon" class="selected-coupon">
            <strong>{{ selectedCoupon.name }}</strong>
            <p>{{ formatCouponValue(selectedCoupon) }}，{{ formatCouponLimit(selectedCoupon) }}</p>
          </div>
          <div v-if="selectedAddress" class="selected-address">
            <strong>{{ selectedAddress.receiver }} {{ selectedAddress.phone }}</strong>
            <p>{{ addressLine(selectedAddress) }}</p>
          </div>
          <el-button
            size="large"
            type="primary"
            :loading="submitting"
            :disabled="!selectedItems.length || !selectedAddressId"
            @click="submitOrder"
          >
            提交订单
          </el-button>
        </aside>
      </section>
    </main>

    <el-dialog
      v-model="addressDialogOpen"
      :title="editingAddressId ? '编辑收货地址' : '新增收货地址'"
      width="520px"
      class="address-dialog"
    >
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
.checkout-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 5%, rgba(47, 143, 103, 0.12), transparent 26%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.checkout-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.checkout-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.checkout-hero span,
.block-heading span,
.pay-panel > span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.checkout-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.checkout-hero p {
  margin: 12px 0 0;
  color: #6b7280;
}

.back-link,
.plain-action,
.address-actions button {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.back-link,
.plain-action {
  color: #e5484d;
  font-weight: 900;
}

.login-needed,
.checkout-block,
.pay-panel {
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.06);
}

.login-needed {
  padding: 44px;
}

.login-needed h2 {
  margin: 0;
  font-size: 28px;
}

.login-needed p {
  margin: 12px 0 24px;
  color: #6b7280;
}

.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

.checkout-main {
  display: grid;
  gap: 18px;
}

.checkout-block {
  padding: 24px;
}

.block-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.block-heading h2 {
  margin: 9px 0 0;
  font-size: 24px;
}

.block-heading > strong {
  color: #6b7280;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-height: 120px;
}

.address-item,
.coupon-item,
.address-empty {
  min-height: 148px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.07);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.address-item {
  cursor: pointer;
}

.coupon-item {
  min-height: 132px;
  cursor: pointer;
}

.address-item.active,
.coupon-item.active {
  border-color: #e5484d;
  box-shadow: 0 0 0 2px rgba(229, 72, 77, 0.08), 0 14px 28px rgba(15, 23, 42, 0.07);
  transform: translateY(-1px);
}

.address-top,
.coupon-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.address-top em,
.coupon-top em {
  padding: 3px 8px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.address-item strong,
.coupon-item strong {
  display: block;
  margin-top: 8px;
  font-size: 16px;
}

.coupon-item strong {
  color: #e5484d;
  font-size: 24px;
}

.coupon-item.no-coupon strong {
  color: #111827;
  font-size: 16px;
}

.address-item p,
.coupon-item p {
  margin: 8px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.address-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.address-actions button {
  color: #6b7280;
  font-size: 13px;
  font-weight: 900;
}

.address-actions button:hover {
  color: #e5484d;
}

.address-empty {
  display: grid;
  place-content: center;
  border-style: dashed;
  color: #111827;
  cursor: pointer;
  font: inherit;
}

.address-empty span {
  display: block;
  margin-top: 8px;
  color: #6b7280;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-height: 132px;
}

.order-item {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) 48px 110px;
  gap: 14px;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.order-item img {
  width: 76px;
  height: 76px;
  border-radius: 14px;
  object-fit: cover;
  background: #f3f4f6;
}

.item-copy {
  min-width: 0;
}

.item-copy strong,
.item-copy span {
  display: block;
}

.item-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-copy span {
  margin-top: 7px;
  color: #6b7280;
  font-size: 13px;
}

.item-qty {
  color: #6b7280;
  font-weight: 900;
}

.item-price {
  color: #e5484d;
  font-weight: 900;
  text-align: right;
}

.pay-panel {
  position: sticky;
  top: 96px;
  padding: 24px;
}

.pay-panel h2 {
  margin: 10px 0 20px;
  font-size: 26px;
}

.pay-panel dl,
.pay-panel dd {
  margin: 0;
}

.pay-panel dl > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.pay-panel dt {
  color: #6b7280;
}

.pay-panel dd {
  font-weight: 900;
}

.pay-panel .total-row dd {
  color: #e5484d;
  font-size: 25px;
}

.selected-address,
.selected-coupon {
  margin-top: 18px;
  padding: 14px;
  border-radius: 14px;
  background: #f7f8f5;
}

.selected-coupon {
  background: #fff7ed;
}

.selected-address strong,
.selected-address p,
.selected-coupon strong,
.selected-coupon p {
  display: block;
  margin: 0;
}

.selected-address p,
.selected-coupon p {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.6;
}

.pay-panel :deep(.el-button) {
  width: 100%;
  height: 46px;
  margin-top: 20px;
  border-radius: 12px;
  font-weight: 900;
}

.pay-panel :deep(.el-button--primary),
.login-needed :deep(.el-button--primary),
.address-dialog :deep(.el-button--primary) {
  background: linear-gradient(135deg, #e5484d, #c92432);
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(229, 72, 77, 0.22);
}

.address-dialog :deep(.el-dialog) {
  border-radius: 8px;
}

@media (max-width: 1040px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }

  .pay-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .checkout-shell {
    padding: 28px 16px 52px;
  }

  .checkout-hero,
  .block-heading {
    align-items: start;
    flex-direction: column;
  }

  .address-grid {
    grid-template-columns: 1fr;
  }

  .coupon-grid {
    grid-template-columns: 1fr;
  }

  .order-item {
    grid-template-columns: 70px minmax(0, 1fr);
  }

  .order-item img {
    width: 70px;
    height: 70px;
  }

  .item-qty,
  .item-price {
    grid-column: 2;
    text-align: left;
  }
}
</style>
