<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="kicker">After Sale</text>
        <text class="title">售后进度</text>
        <text class="subtitle">退款、退货、审核进度集中处理。</text>
      </view>
      <view v-if="isLoggedIn" class="hero-stat">
        <text>{{ total }}</text>
        <text>售后单</text>
      </view>
    </view>

    <scroll-view scroll-x class="tabs">
      <view class="tab-row">
        <view
          v-for="item in statusTabs"
          :key="item.label"
          class="tab"
          :class="{ active: currentStatus === item.value }"
          @tap="selectStatus(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="!isLoggedIn" class="empty-card">
      <text>登录后查看售后</text>
      <button @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view v-if="loading && !afterSales.length" class="empty">加载中...</view>
      <view v-else-if="!afterSales.length" class="empty-card">
        <text>{{ emptyTitle }}</text>
        <text class="empty-hint">{{ emptyHint }}</text>
        <button @tap="goOrders">查看订单</button>
      </view>

      <view v-else class="list">
        <view v-for="item in afterSales" :key="item.id" class="sale-card" @tap="goDetail(item)">
          <view class="card-head">
            <view>
              <text>{{ item.afterSaleNo }}</text>
              <text>{{ item.typeDesc || '售后申请' }} · {{ formatTime(item.createTime) }}</text>
            </view>
            <text :class="statusBadgeClass(item.status)">{{ item.statusDesc || '处理中' }}</text>
          </view>

          <view class="goods-row">
            <view v-if="saleThumbItems(item).length" class="thumb-stack">
              <image
                v-for="(goods, index) in saleThumbItems(item)"
                :key="`${goods.id || goods.skuId || index}`"
                class="thumb"
                :src="normalizeImage(goods.skuImage, `${item.id}-${goods.skuId || index}`)"
                mode="aspectFill"
              />
            </view>
            <view v-else class="thumb-placeholder">售</view>
            <view class="goods-copy">
              <text>{{ primaryGoodsName(item) }}</text>
              <text>{{ primaryGoodsSpec(item) }}</text>
              <text>{{ item.reason || '售后申请' }}</text>
            </view>
          </view>

          <view class="card-main">
            <view>
              <text>订单号</text>
              <text>{{ item.orderNo }}</text>
            </view>
            <view>
              <text>退款金额</text>
              <text>{{ formatPrice(item.amount) }}</text>
            </view>
            <view v-if="item.auditRemark">
              <text>审核备注</text>
              <text>{{ item.auditRemark }}</text>
            </view>
            <view v-if="item.rejectReason">
              <text>拒绝原因</text>
              <text>{{ item.rejectReason }}</text>
            </view>
            <view v-if="item.returnCompany || item.returnNo">
              <text>退货物流</text>
              <text>{{ item.returnCompany || '-' }} {{ item.returnNo || '' }}</text>
            </view>
            <view v-if="refundRecordTitle(item.refundRecord)">
              <text>退款记录</text>
              <text>{{ refundRecordTitle(item.refundRecord) }}</text>
            </view>
            <view v-if="refundRecordMeta(item.refundRecord)">
              <text>退款摘要</text>
              <text>{{ refundRecordMeta(item.refundRecord) }}</text>
            </view>
            <view v-if="refundRecordNotice(item.refundRecord)">
              <text>退款进度</text>
              <text>{{ refundRecordNotice(item.refundRecord) }}</text>
            </view>
          </view>

          <scroll-view v-if="item.images?.length" scroll-x class="proof-scroll">
            <view class="proof-row">
              <image
                v-for="(image, imageIndex) in item.images"
                :key="image + '-' + imageIndex"
                :src="normalizeImage(image, `proof-${item.id}-${imageIndex}`)"
                mode="aspectFill"
                @tap.stop="previewImages(item.images, imageIndex, `proof-${item.id}`)"
              />
            </view>
          </scroll-view>

          <view class="card-foot">
            <text>{{ actionHint(item) }}</text>
            <view class="actions">
              <button @tap.stop="goOrder(item)">查看订单</button>
              <button
                v-if="canSubmitReturnLogistics(item.status)"
                class="primary"
                :disabled="isOperating(item)"
                @tap.stop="openReturnDialog(item)"
              >
                填写物流
              </button>
              <button
                v-if="canCancelAfterSale(item.status)"
                class="ghost"
                :disabled="isOperating(item)"
                @tap.stop="cancelSale(item)"
              >
                取消申请
              </button>
            </view>
          </view>
        </view>
      </view>

      <view v-if="afterSales.length" class="load-state">
        <text>{{ finished ? '已经到底啦' : loading ? '继续加载...' : '上拉加载更多' }}</text>
      </view>
    </view>

    <view v-if="returnDialogOpen" class="dialog-mask" @tap="returnDialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <view>
            <text>填写退货物流</text>
            <text>商家确认收货后进入退款处理。</text>
          </view>
          <button @tap="returnDialogOpen = false">关闭</button>
        </view>
        <view v-if="returnTarget" class="return-summary">
          <text>{{ returnTarget.afterSaleNo }}</text>
          <text>{{ formatPrice(returnTarget.amount) }}</text>
        </view>
        <input v-model="returnForm.returnCompany" class="field" placeholder="物流公司，例如顺丰速运" />
        <input v-model="returnForm.returnNo" class="field" placeholder="退回包裹的物流单号" />
        <button class="save-btn" :disabled="returnSubmitting" @tap="submitReturnInfo">
          {{ returnSubmitting ? '提交中...' : '提交物流' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import {
  cancelAfterSale,
  getAfterSalePage,
  submitAfterSaleReturn,
  type AfterSale,
  type OrderItem,
} from '@/api/order'
import { syncSession } from '@/utils/session'
import {
  canCancelAfterSale,
  canSubmitReturnLogistics,
  refundRecordMeta,
  refundRecordNotice,
  refundRecordTitle,
  statusBadgeClass,
} from '@/utils/after-sale-ui'

const statusTabs = [
  { label: '全部', value: undefined },
  { label: '待审核', value: 0 },
  { label: '待退货', value: 1 },
  { label: '待退款', value: 2 },
  { label: '已完成', value: 4 },
]

const isLoggedIn = ref(false)
const loading = ref(false)
const returnDialogOpen = ref(false)
const returnSubmitting = ref(false)
const afterSales = ref<AfterSale[]>([])
const currentStatus = ref<number | undefined>()
const pageNum = ref(1)
const total = ref(0)
const finished = ref(false)
const operatingId = ref<string | number>('')
const returnTarget = ref<AfterSale>()
const returnForm = reactive({
  returnCompany: '',
  returnNo: '',
})

const activeTab = computed(() => statusTabs.find((item) => item.value === currentStatus.value))
const emptyTitle = computed(() => (currentStatus.value === undefined ? '暂无售后记录' : `暂无${activeTab.value?.label || ''}售后`))
const emptyHint = computed(() => (currentStatus.value === undefined ? '有退款或退货需求时，可从订单详情发起申请。' : '切换其他状态看看，或回到订单详情查看最新进度。'))

onShow(async () => {
  isLoggedIn.value = await syncSession()
  if (!isLoggedIn.value) {
    afterSales.value = []
    total.value = 0
    return
  }
  await refresh()
})

onPullDownRefresh(async () => {
  if (isLoggedIn.value) await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value && !loading.value && isLoggedIn.value) {
    loadAfterSales(false)
  }
})

async function refresh() {
  pageNum.value = 1
  finished.value = false
  await loadAfterSales(true)
}

async function loadAfterSales(reset: boolean) {
  if (!isLoggedIn.value) return
  loading.value = true
  try {
    const res = await getAfterSalePage({
      pageNum: pageNum.value,
      pageSize: 10,
      status: currentStatus.value,
    })
    const page = res.data
    const list = page?.list || []
    afterSales.value = reset ? list : [...afterSales.value, ...list]
    total.value = Number(page?.total || 0)
    finished.value = afterSales.value.length >= total.value || !list.length
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

async function selectStatus(status?: number) {
  currentStatus.value = status
  if (isLoggedIn.value) {
    await refresh()
  }
}

function goOrder(item: AfterSale) {
  uni.navigateTo({ url: `/pages/order/detail?id=${item.orderId}` })
}

function goDetail(item: AfterSale) {
  uni.navigateTo({ url: `/pages/after-sales/detail?id=${item.id}` })
}

function goOrders() {
  uni.navigateTo({ url: '/pages/orders/list' })
}

function goLogin() {
  uni.switchTab({ url: '/pages/user/user' })
}

function isOperating(item: AfterSale) {
  return String(operatingId.value) === String(item.id) || (returnSubmitting.value && String(returnTarget.value?.id) === String(item.id))
}

async function cancelSale(item: AfterSale) {
  const confirmed = await confirmAction('取消售后', '确认取消这个售后申请？订单会恢复到申请前状态。', '取消售后')
  if (!confirmed) return
  operatingId.value = item.id
  try {
    await cancelAfterSale(item.id)
    uni.showToast({ title: '售后已取消', icon: 'success' })
    await refresh()
  } finally {
    operatingId.value = ''
  }
}

function openReturnDialog(item: AfterSale) {
  returnTarget.value = item
  returnForm.returnCompany = item.returnCompany || ''
  returnForm.returnNo = item.returnNo || ''
  returnDialogOpen.value = true
}

async function submitReturnInfo() {
  if (!returnTarget.value) return
  if (!returnForm.returnCompany.trim() || !returnForm.returnNo.trim()) {
    uni.showToast({ title: '请填写物流公司和单号', icon: 'none' })
    return
  }
  returnSubmitting.value = true
  try {
    await submitAfterSaleReturn(returnTarget.value.id, {
      returnCompany: returnForm.returnCompany.trim(),
      returnNo: returnForm.returnNo.trim(),
    })
    uni.showToast({ title: '物流已提交', icon: 'success' })
    returnDialogOpen.value = false
    await refresh()
  } finally {
    returnSubmitting.value = false
  }
}

function confirmAction(title: string, content: string, confirmText: string) {
  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title,
      content,
      confirmText,
      cancelText: '返回',
      confirmColor: '#e5484d',
      success: (res) => resolve(Boolean(res.confirm)),
      fail: () => resolve(false),
    })
  })
}

function saleThumbItems(item: AfterSale) {
  return (item.items || []).slice(0, 3)
}

function primaryGoodsName(item: AfterSale) {
  return (item.items || [])[0]?.skuName || item.orderNo || '售后商品'
}

function primaryGoodsSpec(item: AfterSale) {
  const goods = (item.items || [])[0]
  if (!goods) return item.typeDesc || '售后申请'
  return specText(goods)
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

function actionHint(item: AfterSale) {
  if (canSubmitReturnLogistics(item.status)) return '请按审核要求寄回商品并填写物流。'
  if (Number(item.status) === 2) return '商家正在处理退款。'
  if (Number(item.status) === 4) return '售后已完成。'
  if ([3, 5].includes(Number(item.status))) return '售后已关闭。'
  return '商家会尽快审核申请。'
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 16).replace('T', ' ')
}

function normalizeImage(url?: string, seed = 'service') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/280/280`
  }
  return url
}

function previewImages(images: string[] = [], index = 0, seed = 'image') {
  const urls = images.map((image, imageIndex) => normalizeImage(image, `${seed}-${imageIndex}`)).filter(Boolean)
  if (!urls.length) return
  uni.previewImage({ urls, current: urls[index] || urls[0] })
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 56rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.sale-card,
.empty-card {
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  padding: 34rpx 28rpx;
  background: #111827;
  color: #fff;
}

.kicker,
.title,
.subtitle,
.hero-stat text,
.goods-copy text {
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
  font-size: 24rpx;
}

.hero-stat {
  min-width: 118rpx;
  padding: 14rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.1);
  text-align: center;
}

.hero-stat text:first-child {
  color: #ffd166;
  font-size: 34rpx;
  font-weight: 900;
}

.hero-stat text:last-child {
  margin-top: 4rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 21rpx;
}

.tabs {
  width: 100%;
  margin: 22rpx 0;
  white-space: nowrap;
}

.tab-row {
  display: flex;
  gap: 12rpx;
}

.tab {
  display: inline-flex;
  height: 62rpx;
  align-items: center;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #6b7280;
  font-size: 25rpx;
  font-weight: 900;
}

.tab.active {
  background: #e5484d;
  color: #fff;
}

.list {
  display: grid;
  gap: 18rpx;
}

.sale-card {
  padding: 22rpx;
}

.card-head,
.card-foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.card-head view {
  min-width: 0;
}

.card-head view text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-head view text:first-child {
  color: #111827;
  font-size: 27rpx;
  font-weight: 900;
}

.card-head view text:last-child {
  margin-top: 6rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.card-head > text {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.card-head > text.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.card-head > text.done {
  background: #eef8f2;
  color: #2f8f67;
}

.card-head > text.closed,
.card-head > text.neutral {
  background: #f3f4f6;
  color: #6b7280;
}

.goods-row {
  display: grid;
  grid-template-columns: 128rpx minmax(0, 1fr);
  gap: 18rpx;
  align-items: center;
  margin-top: 20rpx;
}

.thumb-stack {
  display: flex;
  min-width: 128rpx;
}

.thumb {
  width: 92rpx;
  height: 92rpx;
  flex: 0 0 auto;
  border: 4rpx solid #fff;
  border-radius: 14rpx;
  background: #eef2f7;
}

.thumb + .thumb {
  margin-left: -48rpx;
}

.thumb-placeholder {
  width: 96rpx;
  height: 96rpx;
  border-radius: 14rpx;
  background: #111827;
  color: #ffd166;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 96rpx;
  text-align: center;
}

.goods-copy {
  min-width: 0;
}

.goods-copy text:first-child {
  overflow: hidden;
  color: #111827;
  font-size: 29rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-copy text:nth-child(2),
.goods-copy text:nth-child(3) {
  overflow: hidden;
  margin-top: 7rpx;
  color: #6b7280;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-main {
  display: grid;
  gap: 12rpx;
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.card-main view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.card-main text:first-child,
.card-foot > text {
  color: #6b7280;
  font-size: 23rpx;
}

.card-main text:last-child {
  max-width: 450rpx;
  overflow: hidden;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-main view:nth-child(2) text:last-child {
  color: #e5484d;
}

.proof-scroll {
  width: 100%;
  margin-top: 14rpx;
  white-space: nowrap;
}

.proof-row {
  display: flex;
  gap: 12rpx;
}

.proof-row image {
  width: 124rpx;
  height: 124rpx;
  flex: 0 0 auto;
  border-radius: 12rpx;
  background: #eef2f7;
}

.card-foot {
  align-items: center;
  margin-top: 18rpx;
}

.card-foot > text {
  flex: 1;
  min-width: 0;
  line-height: 1.35;
}

.actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10rpx;
}

.actions button,
.empty-card button,
.dialog-head button,
.save-btn {
  height: 58rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.actions button {
  padding: 0 18rpx;
}

.actions button.primary {
  background: #e5484d;
}

.actions button.ghost,
.dialog-head button {
  background: #f3f4f6;
  color: #374151;
}

.actions button[disabled],
.save-btn[disabled] {
  opacity: 0.65;
}

.empty,
.empty-card {
  padding: 80rpx 24rpx;
  color: #6b7280;
  text-align: center;
}

.empty-card text {
  display: block;
}

.empty-card text:first-child {
  margin-bottom: 12rpx;
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}

.empty-hint {
  margin-bottom: 22rpx;
  color: #6b7280;
  font-size: 24rpx;
  line-height: 1.45;
}

.empty-card button {
  display: inline-block;
  padding: 0 30rpx;
}

.load-state {
  padding: 28rpx 0 10rpx;
  color: #9ca3af;
  font-size: 23rpx;
  text-align: center;
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.dialog-head view {
  min-width: 0;
}

.dialog-head view text {
  display: block;
}

.dialog-head view text:first-child {
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}

.dialog-head view text:last-child {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.dialog-head button {
  flex: 0 0 auto;
}

.return-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.return-summary text {
  overflow: hidden;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.return-summary text:last-child {
  flex: 0 0 auto;
  color: #e5484d;
}

.field {
  width: 100%;
  height: 78rpx;
  margin-top: 14rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  box-sizing: border-box;
  font-size: 26rpx;
}

.save-btn {
  width: 100%;
  height: 82rpx;
  margin-top: 24rpx;
  background: #e5484d;
  line-height: 82rpx;
}
</style>
