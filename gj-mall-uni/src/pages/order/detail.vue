<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中...</view>

    <view v-else-if="order">
      <view class="hero">
        <text class="kicker">Order</text>
        <text class="title">{{ order.statusDesc || '订单详情' }}</text>
        <text class="subtitle">订单号 {{ order.orderNo }}</text>
      </view>

      <view class="panel action-panel">
        <view>
          <text>应付金额</text>
          <text>{{ formatPrice(order.payAmount) }}</text>
        </view>
        <view class="action-buttons">
          <button v-if="canPay" :disabled="actionLoading" @tap="pay">去支付</button>
          <button v-else-if="canReceive" :disabled="actionLoading" @tap="receive">确认收货</button>
          <button v-if="canApplyAfterSale" class="dark" @tap="openAfterSale">申请售后</button>
          <button v-if="!canPay && !canReceive && !canApplyAfterSale" @tap="goHome">继续逛逛</button>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>订单进度</text>
        </view>
        <view class="timeline">
          <view v-for="item in timeline" :key="item.label" :class="{ active: item.active }">
            <view />
            <text>{{ item.label }}</text>
            <text>{{ item.time || '-' }}</text>
          </view>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>物流轨迹</text>
          <text class="muted">{{ logistics?.deliveryCompany || logistics?.statusDesc || '履约中' }}</text>
        </view>
        <view v-if="logisticsLoading" class="empty small">物流加载中...</view>
        <view v-else>
          <view v-if="logistics?.currentAction || logistics?.nextHint" class="logistics-hint">
            <text>{{ logistics?.currentAction || logistics?.statusDesc || '履约中' }}</text>
            <text>{{ logistics?.nextHint || '请关注订单状态变化。' }}</text>
          </view>
          <view v-if="logistics?.deliveryNo" class="delivery-card">
            <view>
              <text>物流公司</text>
              <text>{{ logistics.deliveryCompany || '-' }}</text>
            </view>
            <view>
              <text>物流单号</text>
              <text>{{ logistics.deliveryNo }}</text>
            </view>
            <view v-if="logistics.deliveryRemark">
              <text>发货备注</text>
              <text>{{ logistics.deliveryRemark }}</text>
            </view>
          </view>
          <view v-if="logisticsTraces.length" class="logistics-timeline">
            <view v-for="item in logisticsTraces" :key="item.title" :class="{ active: item.active }">
              <view />
              <view>
                <text>{{ item.title }}</text>
                <text>{{ item.description || '-' }}</text>
              </view>
              <text>{{ formatTime(item.time) || '-' }}</text>
            </view>
          </view>
          <view v-else class="service-empty">
            <text>暂无物流轨迹</text>
            <text>支付完成后商家会更新发货信息。</text>
          </view>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>商品明细</text>
          <text class="muted">{{ order.items?.length || 0 }} 件</text>
        </view>
        <view v-for="item in order.items || []" :key="item.id" class="order-item">
          <image class="thumb" :src="normalizeImage(item.skuImage, String(item.skuId))" mode="aspectFill" />
          <view class="item-copy">
            <text class="item-name">{{ item.skuName || '商品' }}</text>
            <text class="item-spec">{{ specText(item) }}</text>
            <view class="item-foot">
              <text>{{ formatPrice(item.totalAmount) }}</text>
              <text>x{{ item.quantity }}</text>
            </view>
            <view class="comment-row">
              <text v-if="itemComment(item)">{{ itemComment(item)?.statusDesc || '已评价' }}</text>
              <button v-else-if="canComment" @tap="openComment(item)">评价</button>
            </view>
            <view v-if="itemComment(item)" class="order-comment-preview">
              <view>
                <text>我的评价</text>
                <text>{{ itemComment(item)?.score || 5 }} 星</text>
              </view>
              <text>{{ itemComment(item)?.content }}</text>
              <scroll-view v-if="itemComment(item)?.images?.length" scroll-x class="comment-preview-scroll">
                <view class="comment-preview-row">
                  <image
                    v-for="(image, imageIndex) in itemComment(item)?.images"
                    :key="image + '-' + imageIndex"
                    :src="normalizeImage(image, `order-comment-${item.id}-${imageIndex}`)"
                    mode="aspectFill"
                    @tap="previewImages(itemComment(item)?.images, imageIndex, `order-comment-${item.id}`)"
                  />
                </view>
              </scroll-view>
              <text v-if="itemComment(item)?.replyContent" class="merchant-reply">商家回复：{{ itemComment(item)?.replyContent }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>收货信息</text>
        </view>
        <view class="info-row">
          <text>收件人</text>
          <text>{{ order.receiver?.receiver || '-' }}</text>
        </view>
        <view class="info-row">
          <text>手机号</text>
          <text>{{ order.receiver?.phone || '-' }}</text>
        </view>
        <view class="info-row">
          <text>地址</text>
          <text>{{ receiverLine }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>发票信息</text>
          <text class="muted">{{ invoiceTypeLabel(order.invoice?.type) }}</text>
        </view>
        <template v-if="order.invoice">
          <view class="info-row">
            <text>发票抬头</text>
            <text>{{ order.invoice.title || '-' }}</text>
          </view>
          <view v-if="order.invoice.type === 2" class="info-row">
            <text>税号</text>
            <text>{{ order.invoice.taxNo || '-' }}</text>
          </view>
          <view class="info-row">
            <text>接收邮箱</text>
            <text>{{ order.invoice.email || '-' }}</text>
          </view>
          <view class="info-row">
            <text>发票内容</text>
            <text>{{ order.invoice.content || '商品明细' }}</text>
          </view>
        </template>
        <view v-else class="service-empty">
          <text>本单未申请发票</text>
          <text>如需补开，可联系商家处理。</text>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>金额明细</text>
        </view>
        <view class="info-row">
          <text>商品金额</text>
          <text>{{ formatPrice(order.totalAmount) }}</text>
        </view>
        <view class="info-row">
          <text>运费</text>
          <text>{{ formatPrice(order.freightAmount) }}</text>
        </view>
        <view class="info-row">
          <text>优惠</text>
          <text>-{{ formatPrice(order.couponAmount) }}</text>
        </view>
        <view class="info-row total">
          <text>实付</text>
          <text>{{ formatPrice(order.payAmount) }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="section-head">
          <text>售后服务</text>
          <button v-if="canApplyAfterSale" class="head-action" @tap="openAfterSale">申请</button>
        </view>
        <view v-if="afterSaleLoading" class="empty small">售后记录加载中...</view>
        <view v-else-if="!afterSales.length" class="service-empty">
          <text>暂无售后记录</text>
          <text>{{ afterSaleUnavailableReason || '已付款、待收货或已完成订单可申请售后。' }}</text>
        </view>
        <view v-else class="service-list">
          <view v-for="item in afterSales" :key="item.id" class="service-card">
            <view class="service-head">
              <view>
                <text>{{ item.typeDesc || '售后申请' }}</text>
                <text>{{ item.afterSaleNo }}</text>
              </view>
              <text>{{ item.statusDesc || '处理中' }}</text>
            </view>
            <view class="service-row">
              <text>退款金额</text>
              <text>{{ formatPrice(item.amount) }}</text>
            </view>
            <view class="service-row">
              <text>申请原因</text>
              <text>{{ item.reason || '-' }}</text>
            </view>
            <view v-if="item.auditRemark" class="service-row">
              <text>审核备注</text>
              <text>{{ item.auditRemark }}</text>
            </view>
            <view v-if="item.rejectReason" class="service-row">
              <text>拒绝原因</text>
              <text>{{ item.rejectReason }}</text>
            </view>
            <view v-if="item.returnCompany || item.returnNo" class="service-row">
              <text>退货物流</text>
              <text>{{ item.returnCompany || '-' }} {{ item.returnNo || '' }}</text>
            </view>
            <scroll-view v-if="item.images?.length" scroll-x class="service-images">
              <view class="service-image-row">
                <image
                  v-for="(image, imageIndex) in item.images"
                  :key="image + '-' + imageIndex"
                  :src="normalizeImage(image, `after-sale-${item.id}-${imageIndex}`)"
                  mode="aspectFill"
                  @tap="previewImages(item.images, imageIndex, `after-sale-${item.id}`)"
                />
              </view>
            </scroll-view>
            <view v-if="canCancelAfterSale(item.status)" class="service-actions">
              <button v-if="canSubmitReturnLogistics(item.status)" :disabled="serviceActionLoading === item.id" @tap="openReturnDialog(item)">
                填写物流
              </button>
              <button class="ghost" :disabled="serviceActionLoading === item.id" @tap="cancelSale(item)">取消售后</button>
            </view>
          </view>
        </view>
      </view>

      <view v-if="canCancel" class="cancel-wrap">
        <button :disabled="actionLoading" @tap="cancel">取消订单</button>
      </view>
    </view>

    <view v-else class="empty">
      <text>订单不存在</text>
      <button @tap="goHome">返回首页</button>
    </view>

    <view v-if="afterSaleDialogOpen" class="dialog-mask" @tap="afterSaleDialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <text>申请售后</text>
          <button @tap="afterSaleDialogOpen = false">关闭</button>
        </view>
        <view class="type-row">
          <view :class="{ active: afterSaleForm.type === 1 }" @tap="setAfterSaleType(1)">仅退款</view>
          <view
            :class="{ active: afterSaleForm.type === 2, disabled: afterSaleEligibility?.returnRefundAllowed === false }"
            @tap="setAfterSaleType(2)"
          >
            退货退款
          </view>
        </view>
        <input v-model="afterSaleForm.reason" class="field" placeholder="售后原因，例如商品破损、发错货" />
        <textarea v-model="afterSaleForm.description" class="textarea" maxlength="500" placeholder="问题描述（选填）" />
        <view class="image-field">
          <view class="image-field-head">
            <text>凭证图片</text>
            <text>{{ afterSaleForm.images.length }}/{{ MAX_FORM_IMAGES }}</text>
          </view>
          <view class="image-input-row">
            <input v-model="afterSaleForm.imageInput" class="field image-input" placeholder="粘贴图片 URL" />
            <button @tap="addFormImages('afterSale')">添加</button>
          </view>
          <button class="upload-image-btn" :disabled="imageUploading === 'afterSale'" @tap="chooseAndUploadImages('afterSale')">
            {{ imageUploading === 'afterSale' ? '上传中...' : '选择图片上传' }}
          </button>
          <view v-if="afterSaleForm.images.length" class="form-image-grid">
            <view v-for="(image, index) in afterSaleForm.images" :key="image + '-' + index" class="form-image-card">
              <image :src="normalizeImage(image, `after-sale-form-${index}`)" mode="aspectFill" @tap="previewFormImages('afterSale', index)" />
              <button @tap.stop="removeFormImage('afterSale', index)">移除</button>
            </view>
          </view>
        </view>
        <button class="save-btn" :disabled="afterSaleSubmitting" @tap="submitAfterSale">提交申请</button>
      </view>
    </view>

    <view v-if="returnDialogOpen" class="dialog-mask" @tap="returnDialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <text>退货物流</text>
          <button @tap="returnDialogOpen = false">关闭</button>
        </view>
        <input v-model="returnForm.returnCompany" class="field" placeholder="物流公司，例如顺丰速运" />
        <input v-model="returnForm.returnNo" class="field" placeholder="物流单号" />
        <button class="save-btn" :disabled="returnSubmitting" @tap="submitReturnInfo">提交物流</button>
      </view>
    </view>

    <view v-if="commentDialogOpen" class="dialog-mask" @tap="commentDialogOpen = false">
      <view class="dialog" @tap.stop>
        <view class="dialog-head">
          <text>评价商品</text>
          <button @tap="commentDialogOpen = false">关闭</button>
        </view>
        <view class="score-row">
          <view
            v-for="score in [1, 2, 3, 4, 5]"
            :key="score"
            :class="{ active: commentForm.score >= score }"
            @tap="commentForm.score = score"
          >
            ★
          </view>
        </view>
        <textarea v-model="commentForm.content" class="textarea" maxlength="500" placeholder="说说商品体验、包装、物流或使用感受" />
        <view class="image-field">
          <view class="image-field-head">
            <text>评价图片</text>
            <text>{{ commentForm.images.length }}/{{ MAX_FORM_IMAGES }}</text>
          </view>
          <view class="image-input-row">
            <input v-model="commentForm.imageInput" class="field image-input" placeholder="粘贴图片 URL" />
            <button @tap="addFormImages('comment')">添加</button>
          </view>
          <button class="upload-image-btn" :disabled="imageUploading === 'comment'" @tap="chooseAndUploadImages('comment')">
            {{ imageUploading === 'comment' ? '上传中...' : '选择图片上传' }}
          </button>
          <view v-if="commentForm.images.length" class="form-image-grid">
            <view v-for="(image, index) in commentForm.images" :key="image + '-' + index" class="form-image-card">
              <image :src="normalizeImage(image, `comment-form-${index}`)" mode="aspectFill" @tap="previewFormImages('comment', index)" />
              <button @tap.stop="removeFormImage('comment', index)">移除</button>
            </view>
          </view>
        </view>
        <button class="save-btn" :disabled="commentSubmitting" @tap="submitComment">提交评价</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { uploadImage } from '@/api/file'
import {
  cancelAfterSale,
  cancelOrder,
  createAfterSale,
  createOrderComment,
  getAfterSaleEligibility,
  getOrderByNo,
  getOrderComments,
  getOrderDetail,
  getOrderAfterSales,
  getOrderLogistics,
  receiveOrder,
  submitAfterSaleReturn,
  type AfterSale,
  type AfterSaleEligibility,
  type OrderComment,
  type OrderDetail,
  type OrderLogistics,
  type OrderItem,
} from '@/api/order'
import { canCancelAfterSale, canSubmitReturnLogistics } from '@/utils/after-sale-ui'

const loading = ref(false)
const actionLoading = ref(false)
const afterSaleLoading = ref(false)
const logisticsLoading = ref(false)
const afterSaleSubmitting = ref(false)
const returnSubmitting = ref(false)
const commentSubmitting = ref(false)
const imageUploading = ref<ImageFormKind | ''>('')
const afterSaleDialogOpen = ref(false)
const returnDialogOpen = ref(false)
const commentDialogOpen = ref(false)
const serviceActionLoading = ref<string | number>('')
const order = ref<OrderDetail>()
const logistics = ref<OrderLogistics>()
const afterSales = ref<AfterSale[]>([])
const afterSaleEligibility = ref<AfterSaleEligibility>()
const comments = ref<OrderComment[]>([])
const returnTarget = ref<AfterSale>()
const commentTarget = ref<OrderItem>()

const afterSaleForm = reactive({
  type: 2,
  reason: '',
  description: '',
  imageInput: '',
  images: [] as string[],
})

const returnForm = reactive({
  returnCompany: '',
  returnNo: '',
})

const commentForm = reactive({
  score: 5,
  content: '',
  imageInput: '',
  images: [] as string[],
})
const MAX_FORM_IMAGES = 6

const canPay = computed(() => Number(order.value?.status) === 0)
const canCancel = computed(() => Number(order.value?.status) === 0)
const canReceive = computed(() => Number(order.value?.status) === 2)
const canComment = computed(() => Number(order.value?.status) === 3)
const hasActiveAfterSale = computed(() => afterSales.value.some((item) => canCancelAfterSale(item.status)))
const canApplyAfterSale = computed(() => {
  const status = Number(order.value?.status)
  return [1, 2, 3].includes(status) && !hasActiveAfterSale.value && afterSaleEligibility.value?.available !== false
})
const afterSaleUnavailableReason = computed(() => {
  if (hasActiveAfterSale.value) return '当前订单已有处理中售后单。'
  if (afterSaleEligibility.value?.available === false) {
    return afterSaleEligibility.value.unavailableReason || '当前订单暂不能申请售后。'
  }
  return ''
})
const commentMap = computed(() => {
  const map = new Map<string, OrderComment>()
  comments.value.forEach((item) => map.set(String(item.orderItemId), item))
  return map
})

const receiverLine = computed(() => {
  const receiver = order.value?.receiver
  if (!receiver) return '-'
  return `${receiver.province || ''}${receiver.city || ''}${receiver.district || ''}${receiver.detail || ''}`
})

const timeline = computed(() => {
  const status = Number(order.value?.status ?? 0)
  return [
    { label: '提交订单', time: formatTime(order.value?.createTime), active: true },
    { label: '完成支付', time: formatTime(order.value?.payTime), active: status >= 1 && status !== 4 && status !== 6 },
    { label: '商家发货', time: formatTime(order.value?.deliveryTime), active: status >= 2 && status !== 4 && status !== 6 },
    { label: '交易完成', time: formatTime(order.value?.receiveTime), active: status >= 3 && status !== 4 && status !== 6 },
  ]
})

const logisticsTraces = computed(() => logistics.value?.traces || [])

onLoad((options: any) => {
  if (options?.orderNo) {
    loadByNo(decodeURIComponent(options.orderNo))
  } else if (options?.id) {
    loadById(options.id)
  }
})

async function loadByNo(orderNo: string) {
  loading.value = true
  try {
    const res = await getOrderByNo(orderNo)
    order.value = res.data
    if (res.data?.id) {
      await Promise.all([loadAfterSales(res.data.id), loadAfterSaleEligibility(res.data.id), loadComments(res.data.id), loadLogistics(res.data.id)])
    }
  } finally {
    loading.value = false
  }
}

async function loadById(id: string | number) {
  loading.value = true
  try {
    const res = await getOrderDetail(id)
    order.value = res.data
    if (res.data?.id) {
      await Promise.all([loadAfterSales(res.data.id), loadAfterSaleEligibility(res.data.id), loadComments(res.data.id), loadLogistics(res.data.id)])
    }
  } finally {
    loading.value = false
  }
}

async function loadAfterSales(orderId: string | number) {
  afterSaleLoading.value = true
  try {
    const res = await getOrderAfterSales(orderId)
    afterSales.value = res.data || []
  } finally {
    afterSaleLoading.value = false
  }
}

async function loadAfterSaleEligibility(orderId: string | number) {
  try {
    const res = await getAfterSaleEligibility(orderId)
    afterSaleEligibility.value = res.data
  } catch {
    afterSaleEligibility.value = undefined
  }
}

async function loadComments(orderId: string | number) {
  const res = await getOrderComments(orderId)
  comments.value = res.data || []
}

async function loadLogistics(orderId: string | number) {
  logisticsLoading.value = true
  try {
    const res = await getOrderLogistics(orderId)
    logistics.value = res.data
  } finally {
    logisticsLoading.value = false
  }
}

async function refresh() {
  if (order.value?.id) {
    await loadById(order.value.id)
  }
}

async function pay() {
  if (!order.value) return
  uni.navigateTo({ url: `/pages/pay/pay?id=${order.value.id}` })
}

async function cancel() {
  if (!order.value) return
  actionLoading.value = true
  try {
    await cancelOrder(order.value.id)
    uni.showToast({ title: '订单已取消', icon: 'success' })
    await refresh()
  } finally {
    actionLoading.value = false
  }
}

async function receive() {
  if (!order.value) return
  actionLoading.value = true
  try {
    await receiveOrder(order.value.id)
    uni.showToast({ title: '已确认收货', icon: 'success' })
    await refresh()
  } finally {
    actionLoading.value = false
  }
}

function openAfterSale() {
  if (afterSaleEligibility.value?.available === false) {
    uni.showToast({ title: afterSaleEligibility.value.unavailableReason || '当前订单暂不能申请售后', icon: 'none' })
    return
  }
  afterSaleForm.type = Number(order.value?.status) === 1 || afterSaleEligibility.value?.returnRefundAllowed === false ? 1 : 2
  afterSaleForm.reason = ''
  afterSaleForm.description = ''
  afterSaleForm.imageInput = ''
  afterSaleForm.images = []
  afterSaleDialogOpen.value = true
}

function setAfterSaleType(type: number) {
  if (type === 2 && afterSaleEligibility.value?.returnRefundAllowed === false) {
    uni.showToast({ title: '待发货订单仅支持仅退款', icon: 'none' })
    return
  }
  afterSaleForm.type = type
}

async function submitAfterSale() {
  if (!order.value) return
  if (!afterSaleForm.reason.trim()) {
    uni.showToast({ title: '请填写售后原因', icon: 'none' })
    return
  }
  afterSaleSubmitting.value = true
  try {
    await createAfterSale(order.value.id, {
      type: afterSaleForm.type,
      reason: afterSaleForm.reason.trim(),
      description: afterSaleForm.description.trim() || undefined,
      images: collectFormImages('afterSale'),
    })
    uni.showToast({ title: '申请已提交', icon: 'success' })
    afterSaleDialogOpen.value = false
    await refresh()
  } finally {
    afterSaleSubmitting.value = false
  }
}

async function cancelSale(item: AfterSale) {
  serviceActionLoading.value = item.id
  try {
    await cancelAfterSale(item.id)
    uni.showToast({ title: '售后已取消', icon: 'success' })
    await refresh()
  } finally {
    serviceActionLoading.value = ''
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

function itemComment(item: OrderItem) {
  return commentMap.value.get(String(item.id))
}

function openComment(item: OrderItem) {
  commentTarget.value = item
  commentForm.score = 5
  commentForm.content = ''
  commentForm.imageInput = ''
  commentForm.images = []
  commentDialogOpen.value = true
}

async function submitComment() {
  if (!order.value || !commentTarget.value) return
  if (!commentForm.content.trim()) {
    uni.showToast({ title: '请填写评价内容', icon: 'none' })
    return
  }
  commentSubmitting.value = true
  try {
    const images = collectFormImages('comment')
    await createOrderComment(order.value.id, {
      orderItemId: commentTarget.value.id,
      score: commentForm.score,
      content: commentForm.content.trim(),
      images,
    })
    uni.showToast({ title: '评价已提交', icon: 'success' })
    commentDialogOpen.value = false
    await refresh()
  } finally {
    commentSubmitting.value = false
  }
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

function formatPrice(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return ''
  return String(value).slice(0, 16).replace('T', ' ')
}

function invoiceTypeLabel(type?: number) {
  if (type === 1) return '个人发票'
  if (type === 2) return '企业发票'
  return '不开发票'
}

function normalizeImage(url?: string, seed = 'order') {
  if (!url || url.indexOf('x.com/') >= 0) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}

function previewImages(images: string[] = [], index = 0, seed = 'image') {
  const urls = images.map((image, imageIndex) => normalizeImage(image, `${seed}-${imageIndex}`)).filter(Boolean)
  if (!urls.length) return
  uni.previewImage({ urls, current: urls[index] || urls[0] })
}

type ImageFormKind = 'comment' | 'afterSale'

function splitImageInput(value?: string) {
  return String(value || '')
    .split(/[\n,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function imageForm(kind: ImageFormKind) {
  return kind === 'comment' ? commentForm : afterSaleForm
}

function imageScene(kind: ImageFormKind) {
  return kind === 'comment' ? 'comment' : 'after-sale'
}

async function chooseAndUploadImages(kind: ImageFormKind) {
  const form = imageForm(kind)
  const remaining = MAX_FORM_IMAGES - form.images.length
  if (remaining <= 0) {
    uni.showToast({ title: `最多添加 ${MAX_FORM_IMAGES} 张`, icon: 'none' })
    return
  }
  try {
    const chooseRes = await uni.chooseImage({
      count: remaining,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
    })
    const paths = chooseRes.tempFilePaths || []
    if (!paths.length) return
    imageUploading.value = kind
    for (const path of paths) {
      if (form.images.length >= MAX_FORM_IMAGES) break
      const uploaded = await uploadImage(path, imageScene(kind))
      if (uploaded.url && !form.images.includes(uploaded.url)) {
        form.images.push(uploaded.url)
      }
    }
    uni.showToast({ title: '图片已上传', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: '图片上传失败', icon: 'none' })
  } finally {
    imageUploading.value = ''
  }
}

function addFormImages(kind: ImageFormKind) {
  const form = imageForm(kind)
  const additions = splitImageInput(form.imageInput)
  if (!additions.length) {
    uni.showToast({ title: '请先粘贴图片 URL', icon: 'none' })
    return
  }
  const merged: string[] = []
  ;[...form.images, ...additions].forEach((image) => {
    if (!merged.includes(image) && merged.length < MAX_FORM_IMAGES) {
      merged.push(image)
    }
  })
  form.images = merged
  form.imageInput = ''
  if (merged.length >= MAX_FORM_IMAGES) {
    uni.showToast({ title: `最多添加 ${MAX_FORM_IMAGES} 张`, icon: 'none' })
  }
}

function removeFormImage(kind: ImageFormKind, index: number) {
  imageForm(kind).images.splice(index, 1)
}

function collectFormImages(kind: ImageFormKind) {
  const form = imageForm(kind)
  const merged: string[] = []
  ;[...form.images, ...splitImageInput(form.imageInput)].forEach((image) => {
    if (!merged.includes(image) && merged.length < MAX_FORM_IMAGES) {
      merged.push(image)
    }
  })
  return merged
}

function previewFormImages(kind: ImageFormKind, index: number) {
  const images = imageForm(kind).images
  previewImages(images, index, `${kind}-form`)
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
.panel {
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
.subtitle {
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

.panel {
  margin-top: 22rpx;
  padding: 24rpx;
}

.action-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.action-panel view text {
  display: block;
}

.action-panel view text:first-child {
  color: #6b7280;
  font-size: 24rpx;
}

.action-panel view text:last-child {
  color: #e5484d;
  font-size: 40rpx;
  font-weight: 900;
}

.action-buttons {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 12rpx;
}

.action-buttons button {
  min-width: 172rpx;
  height: 64rpx;
  margin: 0;
  font-size: 24rpx;
  line-height: 64rpx;
}

.action-buttons button.dark {
  background: #111827;
}

.action-panel button,
.cancel-wrap button,
.empty button,
.section-head .head-action,
.service-actions button,
.dialog-head button,
.save-btn {
  border-radius: 999rpx;
  background: #e5484d;
  color: #fff;
  font-weight: 900;
}

.section-head,
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-head {
  margin-bottom: 18rpx;
}

.section-head text:first-child {
  font-size: 30rpx;
  font-weight: 900;
}

.section-head .head-action {
  height: 54rpx;
  margin: 0;
  padding: 0 22rpx;
  background: #111827;
  font-size: 23rpx;
  line-height: 54rpx;
}

.muted {
  color: #9ca3af;
  font-size: 24rpx;
}

.timeline {
  display: grid;
  gap: 18rpx;
}

.timeline > view {
  display: grid;
  grid-template-columns: 32rpx minmax(0, 1fr) 220rpx;
  gap: 14rpx;
  align-items: center;
  color: #9ca3af;
}

.timeline > view > view {
  width: 24rpx;
  height: 24rpx;
  border: 4rpx solid #d1d5db;
  border-radius: 999rpx;
}

.timeline > view.active {
  color: #111827;
  font-weight: 900;
}

.timeline > view.active > view {
  border-color: #e5484d;
  background: #e5484d;
}

.timeline text:last-child {
  color: #9ca3af;
  font-size: 22rpx;
  text-align: right;
}

.delivery-card {
  display: grid;
  gap: 12rpx;
  margin-bottom: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.logistics-hint {
  display: grid;
  gap: 8rpx;
  margin-bottom: 18rpx;
  padding: 18rpx;
  border: 1rpx solid #bfdbfe;
  border-radius: 14rpx;
  background: #eff6ff;
}

.logistics-hint text:first-child {
  color: #1d4ed8;
  font-size: 27rpx;
  font-weight: 900;
}

.logistics-hint text:last-child {
  color: #475569;
  font-size: 23rpx;
  line-height: 1.4;
}

.delivery-card view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.delivery-card text:first-child {
  color: #6b7280;
  font-size: 23rpx;
}

.delivery-card text:last-child {
  overflow: hidden;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logistics-timeline {
  display: grid;
  gap: 20rpx;
}

.logistics-timeline > view {
  display: grid;
  grid-template-columns: 32rpx minmax(0, 1fr) 156rpx;
  gap: 14rpx;
  align-items: flex-start;
  color: #9ca3af;
}

.logistics-timeline > view > view:first-child {
  width: 22rpx;
  height: 22rpx;
  margin-top: 6rpx;
  border: 4rpx solid #d1d5db;
  border-radius: 999rpx;
}

.logistics-timeline > view.active > view:first-child {
  border-color: #e5484d;
  background: #e5484d;
}

.logistics-timeline > view > view:nth-child(2) {
  min-width: 0;
}

.logistics-timeline > view > view:nth-child(2) text {
  display: block;
}

.logistics-timeline > view > view:nth-child(2) text:first-child {
  color: #111827;
  font-size: 26rpx;
  font-weight: 900;
}

.logistics-timeline > view > view:nth-child(2) text:last-child {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.35;
}

.logistics-timeline > view > text {
  color: #9ca3af;
  font-size: 21rpx;
  text-align: right;
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

.comment-row {
  display: flex;
  justify-content: flex-end;
  min-height: 50rpx;
  margin-top: 12rpx;
}

.comment-row text {
  color: #2f8f67;
  font-size: 23rpx;
  font-weight: 900;
}

.comment-row button {
  height: 50rpx;
  margin: 0;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: #111827;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 50rpx;
}

.order-comment-preview {
  display: grid;
  gap: 10rpx;
  margin-top: 12rpx;
  padding: 16rpx;
  border-radius: 14rpx;
  background: #fbfcf8;
}

.order-comment-preview > view:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-comment-preview > view:first-child text:first-child {
  color: #2f8f67;
  font-size: 23rpx;
  font-weight: 900;
}

.order-comment-preview > view:first-child text:last-child {
  color: #f59e0b;
  font-size: 23rpx;
  font-weight: 900;
}

.order-comment-preview > text {
  color: #374151;
  font-size: 24rpx;
  line-height: 1.45;
}

.comment-preview-scroll {
  width: 100%;
  white-space: nowrap;
}

.comment-preview-row {
  display: flex;
  gap: 12rpx;
}

.comment-preview-row image {
  width: 112rpx;
  height: 112rpx;
  flex: 0 0 auto;
  border-radius: 12rpx;
  background: #eef2f7;
}

.order-comment-preview .merchant-reply {
  padding-top: 10rpx;
  border-top: 1rpx solid #eef0f3;
  color: #6b7280;
}

.info-row {
  gap: 24rpx;
  padding: 14rpx 0;
  border-top: 1rpx solid #eef0f3;
  color: #6b7280;
  font-size: 25rpx;
}

.info-row:first-of-type {
  border-top: 0;
}

.info-row text:last-child {
  max-width: 500rpx;
  color: #111827;
  font-weight: 900;
  text-align: right;
}

.info-row.total text:last-child {
  color: #e5484d;
  font-size: 34rpx;
}

.service-empty {
  padding: 28rpx;
  border: 2rpx dashed #d1d5db;
  border-radius: 14rpx;
  background: #fbfcf8;
}

.service-empty text {
  display: block;
}

.service-empty text:first-child {
  color: #111827;
  font-size: 28rpx;
  font-weight: 900;
}

.service-empty text:last-child {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
}

.service-list {
  display: grid;
  gap: 16rpx;
}

.service-card {
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f7f8f5;
}

.service-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.service-head view {
  min-width: 0;
}

.service-head view text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-head view text:first-child {
  color: #111827;
  font-size: 29rpx;
  font-weight: 900;
}

.service-head view text:last-child {
  margin-top: 6rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.service-head > text {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 22rpx;
  font-weight: 900;
}

.service-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 10rpx 0;
  color: #6b7280;
  font-size: 24rpx;
}

.service-row text:last-child {
  max-width: 460rpx;
  overflow: hidden;
  color: #111827;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-images {
  width: 100%;
  margin-top: 12rpx;
  white-space: nowrap;
}

.service-image-row {
  display: flex;
  gap: 12rpx;
}

.service-image-row image {
  width: 128rpx;
  height: 128rpx;
  flex: 0 0 auto;
  border-radius: 12rpx;
  background: #eef2f7;
}

.service-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 14rpx;
}

.service-actions button {
  height: 56rpx;
  margin: 0;
  padding: 0 20rpx;
  font-size: 23rpx;
  line-height: 56rpx;
}

.service-actions button.ghost {
  background: #111827;
}

.cancel-wrap {
  padding: 28rpx 0;
}

.cancel-wrap button {
  width: 100%;
  background: #111827;
}

.empty {
  padding: 120rpx 30rpx;
  color: #6b7280;
  text-align: center;
}

.empty.small {
  padding: 32rpx 0;
}

.empty text {
  display: block;
  margin-bottom: 24rpx;
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

.type-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
}

.type-row view {
  height: 70rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 70rpx;
  text-align: center;
}

.type-row view.active {
  background: #111827;
  color: #fff;
}

.type-row view.disabled {
  opacity: 0.45;
}

.score-row {
  display: flex;
  justify-content: center;
  gap: 10rpx;
  margin-bottom: 14rpx;
}

.score-row view {
  width: 66rpx;
  height: 66rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #d1d5db;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 66rpx;
  text-align: center;
}

.score-row view.active {
  background: #fff7ed;
  color: #f59e0b;
}

.field,
.textarea {
  width: 100%;
  margin-top: 14rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
  box-sizing: border-box;
  font-size: 26rpx;
}

.field {
  height: 78rpx;
}

.textarea {
  min-height: 170rpx;
  padding-top: 18rpx;
}

.small-textarea {
  min-height: 118rpx;
}

.image-field {
  margin-top: 16rpx;
}

.image-field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.image-field-head text:first-child {
  color: #111827;
  font-size: 26rpx;
  font-weight: 900;
}

.image-field-head text:last-child {
  color: #9ca3af;
  font-size: 23rpx;
  font-weight: 800;
}

.image-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 122rpx;
  gap: 12rpx;
}

.image-input-row .image-input {
  margin-top: 0;
}

.image-input-row button {
  height: 78rpx;
  margin: 0;
  border-radius: 14rpx;
  background: #111827;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.upload-image-btn {
  width: 100%;
  height: 72rpx;
  margin: 12rpx 0 0;
  border-radius: 14rpx;
  background: #eef2f7;
  color: #111827;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 72rpx;
}

.upload-image-btn[disabled] {
  opacity: 0.7;
}

.form-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 14rpx;
}

.form-image-card {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 14rpx;
  background: #eef2f7;
}

.form-image-card image {
  width: 100%;
  height: 100%;
}

.form-image-card button {
  position: absolute;
  right: 8rpx;
  bottom: 8rpx;
  height: 42rpx;
  margin: 0;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(17, 24, 39, 0.86);
  color: #fff;
  font-size: 21rpx;
  font-weight: 900;
  line-height: 42rpx;
}

.save-btn {
  width: 100%;
  height: 82rpx;
  margin-top: 24rpx;
  line-height: 82rpx;
}
</style>
