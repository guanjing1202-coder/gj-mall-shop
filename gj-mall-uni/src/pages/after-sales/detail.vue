<template>
  <view class="page">
    <view v-if="loading && !afterSale" class="empty">加载中...</view>

    <view v-else-if="!afterSale" class="empty-card">
      <text>未找到售后单</text>
      <button @tap="goList">返回列表</button>
    </view>

    <view v-else>
      <view class="hero">
        <view>
          <text class="kicker">After Sale</text>
          <text class="title">{{ afterSale.afterSaleNo }}</text>
          <text class="subtitle">{{ afterSale.typeDesc || '售后申请' }} · {{ formatTime(afterSale.createTime) }}</text>
        </view>
        <text :class="statusBadgeClass(afterSale.status)">{{ afterSale.statusDesc || '处理中' }}</text>
      </view>

      <view class="amount-card">
        <text>退款金额</text>
        <text>{{ formatPrice(afterSale.amount) }}</text>
        <view class="amount-actions">
          <button v-if="canSubmitReturnLogistics(afterSale.status)" class="primary" :disabled="operating" @tap="openReturnDialog">
            填写物流
          </button>
          <button v-if="canCancelAfterSale(afterSale.status)" :disabled="operating" @tap="cancelSale">
            取消申请
          </button>
          <button @tap="goOrder">查看订单</button>
        </view>
      </view>

      <view class="section">
        <text class="section-title">处理进度</text>
        <view class="timeline">
          <view
            v-for="row in afterSale.timeline || []"
            :key="row.title"
            class="timeline-row"
            :class="[{ active: row.active }, row.tone ? `tone-${row.tone}` : '']"
          >
            <view class="dot" />
            <view>
              <text>{{ row.title }}</text>
              <text>{{ row.description }}</text>
              <text>{{ formatTime(row.time) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">售后信息</text>
        <view class="kv">
          <view><text>订单号</text><text>{{ afterSale.orderNo }}</text></view>
          <view><text>申请原因</text><text>{{ afterSale.reason || '-' }}</text></view>
          <view><text>问题描述</text><text>{{ afterSale.description || '-' }}</text></view>
          <view><text>审核备注</text><text>{{ afterSale.auditRemark || '-' }}</text></view>
          <view><text>拒绝原因</text><text>{{ afterSale.rejectReason || '-' }}</text></view>
          <view><text>退货物流</text><text>{{ [afterSale.returnCompany, afterSale.returnNo].filter(Boolean).join(' / ') || '-' }}</text></view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">退款记录</text>
        <view v-if="afterSale.refundRecord" class="refund-card">
          <text>{{ refundRecordTitle(afterSale.refundRecord) }}</text>
          <text>{{ refundRecordMeta(afterSale.refundRecord) }}</text>
          <text v-if="refundRecordNotice(afterSale.refundRecord)">{{ refundRecordNotice(afterSale.refundRecord) }}</text>
          <text>支付流水：{{ afterSale.refundRecord.payNo || '-' }}</text>
        </view>
        <view v-else class="empty small">暂无退款记录</view>
      </view>

      <view class="section">
        <text class="section-title">售后商品</text>
        <view class="goods-list">
          <view v-for="goods in afterSale.items || []" :key="goods.id" class="goods-row">
            <image :src="normalizeImage(goods.skuImage, `${goods.skuId}`)" mode="aspectFill" />
            <view>
              <text>{{ goods.skuName }}</text>
              <text>{{ specText(goods) }}</text>
            </view>
            <text>x{{ goods.quantity }}</text>
          </view>
        </view>
      </view>

      <scroll-view v-if="afterSale.images?.length" scroll-x class="proof-scroll">
        <view class="proof-row">
          <image
            v-for="(image, index) in afterSale.images"
            :key="image + index"
            :src="normalizeImage(image, `after-sale-proof-${index}`)"
            mode="aspectFill"
            @tap="previewImages(afterSale.images, index)"
          />
        </view>
      </scroll-view>
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
import { reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import {
  cancelAfterSale,
  getAfterSaleDetail,
  submitAfterSaleReturn,
  type AfterSale,
  type OrderItem,
} from '@/api/order'
import {
  canCancelAfterSale,
  canSubmitReturnLogistics,
  refundRecordNotice,
  refundRecordMeta,
  refundRecordTitle,
  statusBadgeClass,
} from '@/utils/after-sale-ui'

const id = ref<string | number>('')
const loading = ref(false)
const operating = ref(false)
const returnDialogOpen = ref(false)
const returnSubmitting = ref(false)
const afterSale = ref<AfterSale>()
const returnForm = reactive({
  returnCompany: '',
  returnNo: '',
})

onLoad((query) => {
  id.value = String(query?.id || '')
  loadDetail()
})

onPullDownRefresh(async () => {
  await loadDetail()
  uni.stopPullDownRefresh()
})

async function loadDetail() {
  if (!id.value) return
  loading.value = true
  try {
    const res = await getAfterSaleDetail(id.value)
    afterSale.value = res.data
  } finally {
    loading.value = false
  }
}

function openReturnDialog() {
  if (!afterSale.value) return
  returnForm.returnCompany = afterSale.value.returnCompany || ''
  returnForm.returnNo = afterSale.value.returnNo || ''
  returnDialogOpen.value = true
}

async function submitReturnInfo() {
  if (!afterSale.value) return
  if (!returnForm.returnCompany.trim() || !returnForm.returnNo.trim()) {
    uni.showToast({ title: '请填写物流公司和单号', icon: 'none' })
    return
  }
  returnSubmitting.value = true
  try {
    await submitAfterSaleReturn(afterSale.value.id, {
      returnCompany: returnForm.returnCompany.trim(),
      returnNo: returnForm.returnNo.trim(),
    })
    uni.showToast({ title: '物流已提交', icon: 'success' })
    returnDialogOpen.value = false
    await loadDetail()
  } finally {
    returnSubmitting.value = false
  }
}

async function cancelSale() {
  if (!afterSale.value) return
  const confirmed = await confirmAction('取消售后', '确认取消这个售后申请？订单会恢复到申请前状态。', '取消售后')
  if (!confirmed) return
  operating.value = true
  try {
    await cancelAfterSale(afterSale.value.id)
    uni.showToast({ title: '售后已取消', icon: 'success' })
    await loadDetail()
  } finally {
    operating.value = false
  }
}

function goOrder() {
  if (!afterSale.value) return
  uni.navigateTo({ url: `/pages/order/detail?id=${afterSale.value.orderId}` })
}

function goList() {
  uni.navigateTo({ url: '/pages/after-sales/list' })
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
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

function previewImages(images: string[] = [], index = 0) {
  const urls = images.map((image, imageIndex) => normalizeImage(image, `after-sale-proof-${imageIndex}`)).filter(Boolean)
  if (!urls.length) return
  uni.previewImage({ urls, current: urls[index] || urls[0] })
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
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 56rpx;
  background: #f7f8f5;
  color: #111827;
}

.hero,
.amount-card,
.section,
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
.amount-card > text,
.section-title,
.timeline-row text,
.kv text,
.refund-card text,
.goods-row text {
  display: block;
}

.kicker {
  color: #ffd166;
  font-size: 23rpx;
  font-weight: 900;
}

.title {
  margin-top: 10rpx;
  font-size: 36rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
}

.hero > text {
  flex: 0 0 auto;
  padding: 7rpx 14rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.hero > text.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.hero > text.done {
  background: #eef8f2;
  color: #2f8f67;
}

.hero > text.closed,
.hero > text.neutral {
  background: #f3f4f6;
  color: #6b7280;
}

.amount-card,
.section {
  margin-top: 18rpx;
  padding: 24rpx;
}

.amount-card > text:first-child {
  color: #6b7280;
  font-size: 24rpx;
}

.amount-card > text:nth-child(2) {
  margin-top: 8rpx;
  color: #e5484d;
  font-size: 44rpx;
  font-weight: 900;
}

.amount-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}

.amount-actions button,
.dialog-head button,
.save-btn,
.empty-card button {
  height: 58rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.amount-actions button {
  padding: 0 20rpx;
}

.amount-actions button.primary,
.save-btn {
  background: #e5484d;
}

.amount-actions button[disabled],
.save-btn[disabled] {
  opacity: 0.65;
}

.section-title {
  margin-bottom: 18rpx;
  font-size: 30rpx;
  font-weight: 900;
}

.timeline {
  display: grid;
}

.timeline-row {
  position: relative;
  display: grid;
  grid-template-columns: 22rpx minmax(0, 1fr);
  gap: 18rpx;
  padding-bottom: 22rpx;
}

.timeline-row::before {
  position: absolute;
  top: 22rpx;
  bottom: 0;
  left: 10rpx;
  width: 2rpx;
  background: #e5e7eb;
  content: '';
}

.timeline-row:last-child {
  padding-bottom: 0;
}

.timeline-row:last-child::before {
  display: none;
}

.dot {
  width: 22rpx;
  height: 22rpx;
  border: 6rpx solid #e5e7eb;
  border-radius: 999rpx;
  background: #fff;
  box-sizing: border-box;
}

.timeline-row.active .dot {
  border-color: #2f8f67;
}

.timeline-row.tone-danger.active .dot {
  border-color: #e5484d;
}

.timeline-row.tone-warning.active .dot {
  border-color: #f59e0b;
}

.timeline-row text:first-child {
  color: #111827;
  font-size: 26rpx;
  font-weight: 900;
}

.timeline-row text:nth-child(2),
.timeline-row text:nth-child(3) {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.kv {
  display: grid;
  gap: 14rpx;
}

.kv view {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.kv text:first-child {
  flex: 0 0 140rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.kv text:last-child {
  min-width: 0;
  color: #111827;
  font-size: 25rpx;
  font-weight: 900;
  text-align: right;
  word-break: break-all;
}

.refund-card {
  display: grid;
  gap: 8rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.refund-card text:first-child {
  color: #111827;
  font-size: 26rpx;
  font-weight: 900;
}

.refund-card text:nth-child(2),
.refund-card text:nth-child(3) {
  color: #6b7280;
  font-size: 23rpx;
}

.goods-list {
  display: grid;
  gap: 14rpx;
}

.goods-row {
  display: grid;
  grid-template-columns: 92rpx minmax(0, 1fr) auto;
  gap: 16rpx;
  align-items: center;
}

.goods-row image {
  width: 92rpx;
  height: 92rpx;
  border-radius: 14rpx;
  background: #eef2f7;
}

.goods-row view text:first-child {
  overflow: hidden;
  color: #111827;
  font-size: 27rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-row view text:last-child {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.goods-row > text {
  color: #6b7280;
  font-size: 24rpx;
  font-weight: 900;
}

.proof-scroll {
  width: 100%;
  margin-top: 18rpx;
  white-space: nowrap;
}

.proof-row {
  display: flex;
  gap: 12rpx;
}

.proof-row image {
  width: 132rpx;
  height: 132rpx;
  flex: 0 0 auto;
  border-radius: 14rpx;
  background: #eef2f7;
}

.empty,
.empty-card {
  padding: 80rpx 24rpx;
  color: #6b7280;
  text-align: center;
}

.empty.small {
  padding: 24rpx 0;
}

.empty-card text {
  display: block;
  margin-bottom: 22rpx;
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}

.empty-card button {
  display: inline-block;
  padding: 0 30rpx;
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

.dialog-head text {
  display: block;
}

.dialog-head text:first-child {
  color: #111827;
  font-size: 32rpx;
  font-weight: 900;
}

.dialog-head text:last-child {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.dialog-head button {
  flex: 0 0 auto;
  background: #f3f4f6;
  color: #374151;
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
  line-height: 82rpx;
}
</style>
