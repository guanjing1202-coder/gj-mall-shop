<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">Checkout</text>
      <text class="title">确认订单</text>
      <text class="subtitle">核对商品、地址和优惠券后提交订单。</text>
    </view>

    <view v-if="!isLoggedIn" class="login-card">
      <text>登录后结算</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view class="panel">
        <view class="section-head">
          <text>收货地址</text>
          <button @tap="openAddressForm()">新增</button>
        </view>
        <view v-if="addressLoading" class="empty small">地址加载中...</view>
        <view v-else-if="!addresses.length" class="address-empty" @tap="openAddressForm()">
          <text>新增第一个收货地址</text>
          <text>用于生成订单收货快照</text>
        </view>
        <scroll-view v-else scroll-x class="address-scroll">
          <view class="address-row">
            <view
              v-for="item in addresses"
              :key="item.id"
              class="address-card"
              :class="{ active: String(selectedAddressId) === String(item.id) }"
              @tap="selectedAddressId = item.id"
            >
              <view class="address-top">
                <text>{{ item.receiver }}</text>
                <text v-if="item.isDefault === 1">默认</text>
              </view>
              <text class="phone">{{ item.phone }}</text>
              <text class="address-line">{{ addressLine(item) }}</text>
              <view class="address-actions">
                <text @tap.stop="openAddressForm(item)">编辑</text>
                <text @tap.stop="makeDefault(item)">设默认</text>
                <text @tap.stop="removeAddress(item)">删除</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>商品清单</text>
          <text class="muted">{{ selectedItems.length }} 件</text>
        </view>
        <view v-if="cartLoading" class="empty small">商品加载中...</view>
        <view v-else-if="!selectedItems.length" class="empty small">
          {{ directBuyMode && directBuyInvalidReason ? directBuyInvalidReason : directBuyMode ? '直购商品加载失败' : '购物车暂无已选商品' }}
        </view>
        <view v-else>
          <view v-for="item in selectedItems" :key="item.skuId" class="order-item">
            <image class="thumb" :src="normalizeImage(item.image, String(item.skuId))" mode="aspectFill" />
            <view class="item-copy">
              <text class="item-name">{{ item.spuName || item.skuName || '商品' }}</text>
              <text class="item-spec">{{ specText(item) }}</text>
              <view class="item-foot">
                <text>{{ formatPrice(item.price) }}</text>
                <text>x{{ item.quantity }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>优惠券</text>
          <text v-if="selectedCoupon" class="discount">-{{ formatPrice(couponDiscount) }}</text>
        </view>
        <view v-if="coupons.length" class="coupon-summary">
          <text>{{ usableCouponCount }} 张可用</text>
          <text>{{ unavailableCouponCount }} 张暂不可用</text>
        </view>
        <view v-if="couponPlan" class="coupon-plan" :class="{ active: couponPlan.bestCoupon, pending: !couponPlan.bestCoupon && couponPlan.nextCoupon }">
          <view>
            <text>{{ couponPlan.bestCoupon ? '最优优惠' : couponPlan.nextCoupon ? '凑单提醒' : '优惠状态' }}</text>
            <text>{{ couponPlan.summary }}</text>
            <text v-if="couponPlan.nextHint">{{ couponPlan.nextHint }}</text>
          </view>
          <text v-if="Number(couponPlan.discountAmount || 0) > 0">-{{ formatPrice(couponPlan.discountAmount) }}</text>
        </view>
        <view class="coupon-card" :class="{ active: !selectedCouponId }" @tap="selectedCouponId = ''">
          <text>不使用优惠券</text>
          <text>直接按商品金额结算</text>
        </view>
        <view v-if="couponLoading" class="empty small">优惠券加载中...</view>
        <view v-else-if="!coupons.length" class="empty small">暂无优惠券</view>
        <view
          v-for="item in coupons"
          v-else
          :key="item.id"
          class="coupon-card"
          :class="{
            active: String(selectedCouponId) === String(item.couponId) && isCouponUsable(item),
            disabled: !isCouponUsable(item),
          }"
          @tap="selectCoupon(item)"
        >
          <view>
            <text>{{ item.name }}</text>
            <text>
              {{ isCouponUsable(item) ? `${couponRule(item)} · 预计省 ${formatPrice(couponDiscountValue(item))}` : item.unavailableReason || item.statusDesc || '当前不可用' }}
            </text>
          </view>
          <text class="coupon-value">{{ couponValue(item) }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>订单备注</text>
        </view>
        <textarea v-model="remark" class="remark" maxlength="120" placeholder="选填，给商家的留言" />
      </view>

      <view class="panel">
        <view class="section-head">
          <text>发票信息</text>
          <text class="muted">{{ invoiceTypeLabel(invoiceForm.type) }}</text>
        </view>
        <view class="invoice-tabs">
          <button :class="{ active: invoiceForm.type === 0 }" @tap="invoiceForm.type = 0">不开发票</button>
          <button :class="{ active: invoiceForm.type === 1 }" @tap="invoiceForm.type = 1">个人</button>
          <button :class="{ active: invoiceForm.type === 2 }" @tap="invoiceForm.type = 2">企业</button>
        </view>
        <view v-if="invoiceForm.type" class="invoice-form">
          <input
            v-model="invoiceForm.title"
            class="field"
            maxlength="100"
            :placeholder="invoiceForm.type === 2 ? '企业名称' : '个人姓名 / 发票抬头'"
          />
          <input
            v-if="invoiceForm.type === 2"
            v-model="invoiceForm.taxNo"
            class="field"
            maxlength="32"
            placeholder="纳税人识别号"
          />
          <input v-model="invoiceForm.email" class="field" maxlength="100" placeholder="接收邮箱（选填）" />
          <input v-model="invoiceForm.content" class="field" maxlength="50" placeholder="发票内容，例如：商品明细" />
        </view>
        <text class="invoice-hint">
          {{ invoiceForm.type ? '发票信息会随订单保存，后续由商家按订单开具。' : '本单不需要发票。' }}
        </text>
      </view>

      <view class="summary">
        <view>
          <text>商品金额</text>
          <text>{{ formatPrice(cart.selectedAmount) }}</text>
        </view>
        <view>
          <text>运费</text>
          <text>{{ freightLoading ? '计算中' : formatPrice(freightAmount) }}</text>
        </view>
        <view class="summary-tip freight">
          <text>{{ freightQuote?.summary || '选择地址后计算运费' }}</text>
          <text v-if="freightQuote?.hint">{{ freightQuote.hint }}</text>
        </view>
        <view>
          <text>优惠</text>
          <text>-{{ formatPrice(couponDiscount) }}</text>
        </view>
        <view v-if="couponPlan?.nextHint" class="summary-tip">
          <text>{{ couponPlan.nextHint }}</text>
        </view>
        <view class="total">
          <text>应付</text>
          <text>{{ formatPrice(finalAmount) }}</text>
        </view>
      </view>
    </view>

    <view v-if="isLoggedIn" class="bottom-bar">
      <view>
        <text>合计</text>
        <text>{{ formatPrice(finalAmount) }}</text>
      </view>
      <button :disabled="submitting || !selectedItems.length || !selectedAddressId" @tap="submit">提交订单</button>
    </view>

    <view v-if="addressDialogOpen" class="dialog-mask" @tap="addressDialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <text>{{ editingAddressId ? '编辑地址' : '新增地址' }}</text>
          <button @tap="addressDialogOpen = false">关闭</button>
        </view>
        <input v-model="addressForm.receiver" class="field" placeholder="收件人" />
        <input v-model="addressForm.phone" class="field" placeholder="手机号" />
        <view class="city-grid">
          <input v-model="addressForm.province" class="field" placeholder="省份" />
          <input v-model="addressForm.city" class="field" placeholder="城市" />
          <input v-model="addressForm.district" class="field" placeholder="区县" />
        </view>
        <input v-model="addressForm.detail" class="field" placeholder="详细地址" />
        <input v-model="addressForm.postCode" class="field" placeholder="邮编（选填）" />
        <view class="default-row" @tap="addressForm.isDefault = addressForm.isDefault === 1 ? 0 : 1">
          <view class="select-dot" :class="{ active: addressForm.isDefault === 1 }" />
          <text>设为默认地址</text>
        </view>
        <button class="save-btn" :disabled="addressSaving" @tap="saveAddress">保存地址</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  addAddress,
  deleteAddress,
  getAddressList,
  setDefaultAddress,
  updateAddress,
  type AddressItem,
  type AddressPayload,
} from '@/api/address'
import { getCart, type CartInfo, type CartItem } from '@/api/cart'
import { getCheckoutCouponPlan, type CouponPlan, type MyCoupon } from '@/api/coupon'
import { createOrder, quoteFreight, type FreightQuote } from '@/api/order'
import { getProductDetail, type ApiId } from '@/api/product'
import { requireSession, syncSession } from '@/utils/session'

const emptyCart = (): CartInfo => ({
  items: [],
  totalCount: 0,
  selectedCount: 0,
  selectedAmount: 0,
})

const isLoggedIn = ref(false)
const cartLoading = ref(false)
const addressLoading = ref(false)
const couponLoading = ref(false)
const freightLoading = ref(false)
const submitting = ref(false)
const addressDialogOpen = ref(false)
const addressSaving = ref(false)
const editingAddressId = ref<ApiId>()
const selectedAddressId = ref<ApiId | ''>('')
const selectedCouponId = ref<ApiId | ''>('')
const remark = ref('')
const cart = ref<CartInfo>(emptyCart())
const addresses = ref<AddressItem[]>([])
const coupons = ref<MyCoupon[]>([])
const couponPlan = ref<CouponPlan>()
const freightQuote = ref<FreightQuote>()
const directBuyMode = ref(false)
const directBuySpuId = ref<ApiId>()
const directBuySkuId = ref<ApiId>()
const directBuyQuantity = ref(1)
const directBuyInvalidReason = ref('')

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
const invoiceForm = reactive({
  type: 0,
  title: '',
  taxNo: '',
  email: '',
  content: '商品明细',
})

const selectedItems = computed(() => cart.value.items.filter((item) => item.selected === 1 && !item.invalid))
const selectedCoupon = computed(() => coupons.value.find((item) => String(item.couponId) === String(selectedCouponId.value) && isCouponUsable(item)))
const couponDiscount = computed(() => (selectedCoupon.value ? couponDiscountValue(selectedCoupon.value) : 0))
const freightAmount = computed(() => Number(freightQuote.value?.freightAmount || 0))
const finalAmount = computed(() => Math.max(Number(cart.value.selectedAmount || 0) + freightAmount.value - couponDiscount.value, 0))
const usableCouponCount = computed(() => coupons.value.filter(isCouponUsable).length)
const unavailableCouponCount = computed(() => coupons.value.length - usableCouponCount.value)

watch([selectedAddressId, () => cart.value.selectedAmount], () => {
  loadFreight()
})

onLoad((options: any) => {
  if (options?.skuId) {
    directBuyMode.value = true
    directBuySpuId.value = options.spuId
    directBuySkuId.value = options.skuId
    directBuyQuantity.value = Math.max(1, Number(options.quantity || 1))
  }
})

onShow(async () => {
  isLoggedIn.value = await syncSession()
  if (!isLoggedIn.value) {
    cart.value = emptyCart()
    addresses.value = []
    coupons.value = []
    couponPlan.value = undefined
    return
  }
  await Promise.all([loadCheckoutItems(), loadAddresses()])
  await Promise.all([loadCoupons(), loadFreight()])
})

async function loadCheckoutItems() {
  if (directBuyMode.value) {
    await loadDirectBuyItem()
    return
  }
  await loadCart()
}

async function loadDirectBuyItem() {
  if (!directBuySpuId.value || !directBuySkuId.value) return
  directBuyInvalidReason.value = ''
  cartLoading.value = true
  try {
    const detail = await getProductDetail(directBuySpuId.value)
    const sku = detail.data.skus?.find((candidate) => String(candidate.id) === String(directBuySkuId.value))
    const requestedQuantity = directBuyQuantity.value
    const quantity = Math.min(requestedQuantity, Math.max(1, Number(sku?.stock || 1)))
    const price = Number(sku?.price || detail.data.price || 0)
    const matchedItem: CartItem | undefined = sku
      ? {
          skuId: sku.id,
          spuId: detail.data.id,
          spuName: detail.data.name,
          skuName: sku.name || sku.skuCode,
          image: sku.image || detail.data.mainImage,
          price,
          stock: Number(sku.stock || 0),
          publishStatus: detail.data.publishStatus,
          invalid: detail.data.publishStatus !== 1 || Number(sku.stock || 0) <= 0 || requestedQuantity > Number(sku.stock || 0),
          stockEnough: Number(sku.stock || 0) >= requestedQuantity,
          invalidReason:
            detail.data.publishStatus !== 1
              ? '商品已下架'
              : Number(sku.stock || 0) <= 0
                ? '商品暂时无库存'
                : requestedQuantity > Number(sku.stock || 0)
                  ? `库存仅剩 ${sku.stock || 0} 件，请重新选择数量`
                  : undefined,
          specData: sku.specData,
          quantity,
          selected: 1,
          totalAmount: price * quantity,
        }
      : undefined
    cart.value = matchedItem
      ? {
          items: [matchedItem],
          totalCount: matchedItem.quantity,
          selectedCount: matchedItem.quantity,
          selectedAmount: Number(matchedItem.price || 0) * matchedItem.quantity,
        }
      : emptyCart()
    directBuyInvalidReason.value = matchedItem?.invalidReason || ''
  } finally {
    cartLoading.value = false
  }
}

async function loadCart() {
  cartLoading.value = true
  try {
    const res = await getCart()
    cart.value = res.data || emptyCart()
  } finally {
    cartLoading.value = false
  }
}

async function loadAddresses() {
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

async function loadCoupons() {
  couponLoading.value = true
  try {
    const res = await getCheckoutCouponPlan(Number(cart.value.selectedAmount || 0))
    couponPlan.value = res.data
    coupons.value = res.data?.candidates || []
    const available = coupons.value.some((item) => String(item.couponId) === String(selectedCouponId.value) && isCouponUsable(item))
    if (!available) {
      selectedCouponId.value = res.data?.bestCoupon?.couponId || ''
    }
  } finally {
    couponLoading.value = false
  }
}

async function loadFreight() {
  if (!selectedAddressId.value || Number(cart.value.selectedAmount || 0) <= 0) {
    freightQuote.value = undefined
    return
  }
  freightLoading.value = true
  try {
    const res = await quoteFreight({
      addressId: selectedAddressId.value,
      orderAmount: Number(cart.value.selectedAmount || 0),
    })
    freightQuote.value = res.data
  } catch {
    freightQuote.value = undefined
  } finally {
    freightLoading.value = false
  }
}

function openAddressForm(address?: AddressItem) {
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
  addressDialogOpen.value = true
}

async function saveAddress() {
  if (!addressForm.receiver.trim() || !addressForm.phone.trim() || !addressForm.detail.trim()) {
    uni.showToast({ title: '请完整填写地址', icon: 'none' })
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
    uni.showToast({ title: '地址已保存', icon: 'success' })
  } finally {
    addressSaving.value = false
  }
}

async function makeDefault(address: AddressItem) {
  await setDefaultAddress(address.id)
  selectedAddressId.value = address.id
  await loadAddresses()
  await loadFreight()
}

async function removeAddress(address: AddressItem) {
  await deleteAddress(address.id)
  if (String(selectedAddressId.value) === String(address.id)) selectedAddressId.value = ''
  await loadAddresses()
  await loadFreight()
}

async function submit() {
  if (!(await requireSession('请先登录后结算'))) {
    isLoggedIn.value = false
    return
  }
  if (!selectedItems.value.length) {
    uni.showToast({ title: '请选择结算商品', icon: 'none' })
    return
  }
  if (!selectedAddressId.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const invoiceInfo = buildInvoiceInfo()
    if (invoiceInfo === false) {
      return
    }
    const res = await createOrder({
      addressId: selectedAddressId.value,
      remark: remark.value.trim() || undefined,
      couponId: selectedCoupon.value?.couponId,
      invoiceInfo,
      items: directBuyMode.value
        ? selectedItems.value.map((item) => ({ skuId: item.skuId, quantity: item.quantity }))
        : undefined,
    })
    uni.redirectTo({ url: `/pages/pay/pay?orderNo=${encodeURIComponent(res.data)}` })
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function addressLine(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

function specText(item: CartItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : item.skuName || '默认规格'
}

function couponValue(item: MyCoupon) {
  if (item.type === 2 && item.discountRate) return `${Number(item.discountRate).toFixed(1)}折`
  return `¥${Number(item.discountAmount || 0).toFixed(0)}`
}

function invoiceTypeLabel(type?: number) {
  if (type === 1) return '个人'
  if (type === 2) return '企业'
  return '不开发票'
}

function buildInvoiceInfo() {
  if (!invoiceForm.type) return undefined
  const title = invoiceForm.title.trim()
  const taxNo = invoiceForm.taxNo.trim()
  if (!title) {
    uni.showToast({ title: '请填写发票抬头', icon: 'none' })
    return false
  }
  if (invoiceForm.type === 2 && !taxNo) {
    uni.showToast({ title: '请填写企业税号', icon: 'none' })
    return false
  }
  return {
    type: invoiceForm.type,
    title,
    taxNo: invoiceForm.type === 2 ? taxNo : undefined,
    email: invoiceForm.email.trim() || undefined,
    content: invoiceForm.content.trim() || '商品明细',
  }
}

function couponRule(item: MyCoupon) {
  const minAmount = Number(item.minAmount || 0)
  return minAmount > 0 ? `满 ${minAmount.toFixed(0)} 元可用` : '无门槛可用'
}

function isCouponUsable(item: MyCoupon) {
  return item.usable !== false && Number(item.status ?? 0) === 0
}

function couponDiscountValue(item: MyCoupon) {
  return Number(item.discountEstimate ?? calcCouponDiscount(item, Number(cart.value.selectedAmount || 0)))
}

function selectCoupon(item: MyCoupon) {
  if (!isCouponUsable(item)) {
    uni.showToast({ title: item.unavailableReason || '这张券当前不可用', icon: 'none' })
    return
  }
  selectedCouponId.value = item.couponId || ''
}

function calcCouponDiscount(coupon: MyCoupon, amount: number) {
  if (amount <= 0) return 0
  if (coupon.type === 2) {
    const rate = Number(coupon.discountRate ?? 1)
    return Math.min(amount, Math.max(0, amount - amount * rate))
  }
  return Math.min(amount, Number(coupon.discountAmount || 0))
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function normalizeImage(url?: string, seed = 'checkout') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 150rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.panel,
.summary,
.login-card {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  padding: 36rpx 30rpx;
  background: #111827;
  color: #fff;
}

.kicker,
.title,
.subtitle,
.login-card text {
  display: block;
}

.kicker {
  color: #ffd166;
  font-size: 23rpx;
  font-weight: 900;
}

.title {
  margin-top: 10rpx;
  font-size: 44rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 25rpx;
}

.login-card,
.panel,
.summary {
  margin-top: 22rpx;
  padding: 24rpx;
}

.login-card {
  text-align: center;
}

.login-card text {
  margin-bottom: 22rpx;
  font-size: 32rpx;
  font-weight: 900;
}

.login-card button,
.section-head button,
.bottom-bar button,
.save-btn,
.dialog-head button {
  border-radius: 999rpx;
  font-weight: 900;
}

.login-card button,
.save-btn,
.bottom-bar button {
  background: #e5484d;
  color: #fff;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.section-head text:first-child {
  font-size: 30rpx;
  font-weight: 900;
}

.section-head button {
  height: 56rpx;
  margin: 0;
  padding: 0 20rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  line-height: 56rpx;
}

.muted {
  color: #9ca3af;
  font-size: 24rpx;
}

.discount {
  color: #e5484d;
  font-size: 26rpx;
  font-weight: 900;
}

.address-scroll {
  width: 100%;
  white-space: nowrap;
}

.address-row {
  display: flex;
  gap: 16rpx;
}

.address-card {
  display: inline-flex;
  width: 520rpx;
  min-height: 190rpx;
  flex-direction: column;
  padding: 20rpx;
  border: 2rpx solid #eef0f3;
  border-radius: 14rpx;
  white-space: normal;
}

.address-card.active {
  border-color: #e5484d;
  background: #fff7f7;
}

.address-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.address-top text:first-child {
  font-size: 29rpx;
  font-weight: 900;
}

.address-top text:last-child {
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 21rpx;
  font-weight: 900;
}

.phone,
.address-line {
  display: block;
}

.phone {
  margin-top: 10rpx;
  color: #111827;
  font-weight: 800;
}

.address-line {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
  line-height: 1.45;
}

.address-actions {
  display: flex;
  gap: 22rpx;
  margin-top: 14rpx;
  color: #e5484d;
  font-size: 23rpx;
  font-weight: 900;
}

.address-empty {
  padding: 36rpx 20rpx;
  border: 2rpx dashed #d1d5db;
  border-radius: 14rpx;
  text-align: center;
}

.address-empty text {
  display: block;
}

.address-empty text:first-child {
  font-size: 30rpx;
  font-weight: 900;
}

.address-empty text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
}

.order-item {
  display: grid;
  grid-template-columns: 138rpx minmax(0, 1fr);
  gap: 16rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;
}

.order-item:first-of-type {
  border-top: 0;
}

.thumb {
  width: 138rpx;
  height: 138rpx;
  border-radius: 12rpx;
  background: #eef2f7;
}

.item-copy {
  min-width: 0;
}

.item-name,
.item-spec {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
}

.item-name {
  color: #111827;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 1.35;
  -webkit-line-clamp: 2;
}

.item-spec {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  -webkit-line-clamp: 1;
}

.item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}

.item-foot text:first-child {
  color: #e5484d;
  font-size: 29rpx;
  font-weight: 900;
}

.item-foot text:last-child {
  color: #6b7280;
  font-size: 24rpx;
  font-weight: 800;
}

.coupon-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 14rpx;
  padding: 20rpx;
  border: 2rpx solid #eef0f3;
  border-radius: 14rpx;
}

.coupon-card.active {
  border-color: #e5484d;
  background: #fff7f7;
}

.coupon-card.disabled {
  background: #f8fafc;
  opacity: 0.72;
}

.coupon-card.disabled .coupon-value {
  color: #9ca3af;
}

.coupon-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
  font-weight: 800;
}

.coupon-plan {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 14rpx;
  padding: 20rpx;
  border: 2rpx solid #eef0f3;
  border-radius: 14rpx;
  background: #f8fafc;
}

.coupon-plan.active {
  border-color: rgba(229, 72, 77, 0.32);
  background: #fff7f7;
}

.coupon-plan.pending {
  border-color: rgba(47, 143, 103, 0.32);
  background: #f4fbf7;
}

.coupon-plan view {
  min-width: 0;
}

.coupon-plan text {
  display: block;
}

.coupon-plan view text:first-child {
  color: #e5484d;
  font-size: 22rpx;
  font-weight: 900;
}

.coupon-plan view text:nth-child(2) {
  margin-top: 8rpx;
  color: #111827;
  font-size: 26rpx;
  font-weight: 900;
  line-height: 1.45;
}

.coupon-plan view text:nth-child(3) {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.45;
}

.coupon-plan > text {
  flex: 0 0 auto;
  color: #e5484d;
  font-size: 32rpx;
  font-weight: 900;
}

.coupon-card text {
  display: block;
}

.coupon-card text:first-child {
  font-size: 27rpx;
  font-weight: 900;
}

.coupon-card text:nth-child(2) {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.coupon-value {
  color: #e5484d;
  font-size: 34rpx;
  font-weight: 900;
}

.remark {
  width: 100%;
  min-height: 160rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  box-sizing: border-box;
  font-size: 26rpx;
}

.invoice-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  padding: 8rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
}

.invoice-tabs button {
  height: 62rpx;
  margin: 0;
  border-radius: 999rpx;
  background: transparent;
  color: #6b7280;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 62rpx;
}

.invoice-tabs button.active {
  background: #111827;
  color: #fff;
}

.invoice-form {
  display: grid;
  gap: 12rpx;
  margin-top: 16rpx;
}

.invoice-hint {
  display: block;
  margin-top: 14rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.55;
}

.summary view,
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary view {
  padding: 12rpx 0;
  color: #6b7280;
  font-size: 25rpx;
}

.summary .summary-tip {
  display: block;
  padding: 14rpx 0;
  border-top: 1rpx solid #eef0f3;
}

.summary .summary-tip text {
  display: block;
  color: #2f8f67;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 1.45;
}

.summary .total {
  color: #111827;
  font-weight: 900;
}

.summary .total text:last-child {
  color: #e5484d;
  font-size: 36rpx;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  gap: 20rpx;
  padding: 18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 28rpx rgba(15, 23, 42, 0.1);
}

.bottom-bar view {
  flex: 1;
}

.bottom-bar text {
  display: block;
}

.bottom-bar text:first-child {
  color: #6b7280;
  font-size: 22rpx;
}

.bottom-bar text:last-child {
  color: #e5484d;
  font-size: 36rpx;
  font-weight: 900;
}

.bottom-bar button {
  width: 220rpx;
  height: 78rpx;
  margin: 0;
  line-height: 78rpx;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-end;
  background: rgba(17, 24, 39, 0.46);
}

.dialog {
  width: 100%;
  padding: 26rpx;
  border-radius: 24rpx 24rpx 0 0;
  background: #fff;
  box-sizing: border-box;
}

.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.dialog-head text {
  font-size: 32rpx;
  font-weight: 900;
}

.dialog-head button {
  margin: 0;
  background: #f3f4f6;
  color: #374151;
  font-size: 23rpx;
}

.field {
  height: 78rpx;
  margin-top: 14rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  font-size: 26rpx;
}

.city-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.default-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
  color: #374151;
  font-size: 25rpx;
  font-weight: 900;
}

.select-dot {
  width: 34rpx;
  height: 34rpx;
  border: 3rpx solid #d1d5db;
  border-radius: 999rpx;
}

.select-dot.active {
  border-color: #e5484d;
  background: #e5484d;
  box-shadow: inset 0 0 0 8rpx #fff;
}

.save-btn {
  width: 100%;
  height: 82rpx;
  margin-top: 24rpx;
  line-height: 82rpx;
}

.empty {
  padding: 80rpx 0;
  color: #6b7280;
  text-align: center;
}

.empty.small {
  padding: 32rpx 0;
}
</style>
