<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElImageViewer, ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import { uploadImage } from '@/api/file'
import {
  cancelAfterSale,
  cancelOrder,
  createAfterSale,
  createOrderComment,
  getOrderAfterSales,
  getOrderComments,
  getOrderDetail,
  getOrderLogistics,
  receiveOrder,
  submitAfterSaleReturn,
  type AfterSale,
  type OrderComment,
  type OrderDetail,
  type OrderItem,
  type OrderLogistics,
} from '@/api/order'
import { createPay, type PayChannel } from '@/api/pay'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const loading = ref(false)
const actionLoading = ref<'pay' | 'cancel' | 'receive' | ''>('')
const order = ref<OrderDetail>()
const logistics = ref<OrderLogistics>()
const comments = ref<OrderComment[]>([])
const afterSales = ref<AfterSale[]>([])
const payChannel = ref<PayChannel>('mock')
const previewImages = ref<string[]>([])
const previewIndex = ref(0)
const imageUploading = ref<ImageFormKind | ''>('')
const commentDialogOpen = ref(false)
const commenting = ref(false)
const commentTarget = ref<OrderItem>()
const commentForm = ref({
  score: 5,
  content: '',
  imageInput: '',
  images: [] as string[],
})
const afterSaleDialogOpen = ref(false)
const afterSaleSubmitting = ref(false)
const afterSaleOperatingId = ref<string | number>('')
const returnDialogOpen = ref(false)
const returnSubmitting = ref(false)
const returnTarget = ref<AfterSale>()
const afterSaleForm = ref({
  type: 2,
  reason: '',
  description: '',
  imageInput: '',
  images: [] as string[],
})
const returnForm = ref({
  returnCompany: '',
  returnNo: '',
})
const MAX_FORM_IMAGES = 6

const canPay = computed(() => Number(order.value?.status) === 0)
const canCancel = computed(() => Number(order.value?.status) === 0)
const canReceive = computed(() => Number(order.value?.status) === 2)
const canComment = computed(() => Number(order.value?.status) === 3)
const latestAfterSale = computed(() => afterSales.value[0])
const hasActiveAfterSale = computed(() => {
  const status = Number(latestAfterSale.value?.status)
  return [0, 1, 2].includes(status)
})
const canApplyAfterSale = computed(() => {
  const status = Number(order.value?.status)
  return [1, 2, 3].includes(status) && !hasActiveAfterSale.value
})
const commentMap = computed(() => {
  const map = new Map<string, OrderComment>()
  comments.value.forEach((item) => map.set(String(item.orderItemId), item))
  return map
})
const activeStep = computed(() => {
  const status = Number(order.value?.status)
  if (status === 0) return 0
  if (status === 1) return 1
  if (status === 2) return 2
  if (status === 3) return 3
  return 0
})
const logisticsTraces = computed(() => logistics.value?.traces || [])
const commentPreviewImages = computed(() =>
  commentForm.value.images.map((image, index) => normalizeImage(image, `comment-form-${index}`)),
)
const afterSalePreviewImages = computed(() =>
  afterSaleForm.value.images.map((image, index) => normalizeImage(image, `after-sale-form-${index}`)),
)

function statusClass(status?: number) {
  const code = Number(status)
  if (code === 0) return 'pending'
  if (code === 1 || code === 2) return 'processing'
  if (code === 3) return 'done'
  if (code === 4 || code === 6) return 'closed'
  return 'neutral'
}

function afterSaleStatusClass(status?: number) {
  const code = Number(status)
  if (code === 0 || code === 1 || code === 2) return 'processing'
  if (code === 4) return 'done'
  if (code === 3 || code === 5) return 'closed'
  return 'neutral'
}

function formatPrice(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

function formatTime(value?: string) {
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
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function normalizeImage(url?: string, seed = 'order') {
  if (!url || url.includes('x.com/')) {
    return `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
  }
  return url
}

function handleImageError(event: Event, seed: string) {
  const target = event.target as HTMLImageElement
  target.src = `https://picsum.photos/seed/${encodeURIComponent(seed)}/360/360`
}

function specText(item: OrderItem) {
  const values = item.specData ? Object.values(item.specData).filter(Boolean) : []
  return values.length ? values.join(' / ') : '默认规格'
}

type ImageFormKind = 'comment' | 'afterSale'

function splitImageInput(value?: string) {
  return String(value || '')
    .split(/[\n,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function imageForm(kind: ImageFormKind) {
  return kind === 'comment' ? commentForm.value : afterSaleForm.value
}

function imageScene(kind: ImageFormKind) {
  return kind === 'comment' ? 'comment' : 'after-sale'
}

async function uploadFormImage(kind: ImageFormKind, options: UploadRequestOptions) {
  const form = imageForm(kind)
  if (form.images.length >= MAX_FORM_IMAGES) {
    ElMessage.info(`最多添加 ${MAX_FORM_IMAGES} 张图片`)
    options.onError(new Error('image limit exceeded') as any)
    return
  }
  imageUploading.value = kind
  try {
    const res = await uploadImage(options.file as File, imageScene(kind))
    if (res.data?.url && !form.images.includes(res.data.url)) {
      form.images.push(res.data.url)
    }
    ElMessage.success('图片已上传')
    options.onSuccess(res.data)
  } catch (error) {
    options.onError(error as any)
  } finally {
    imageUploading.value = ''
  }
}

function uploadCommentImage(options: UploadRequestOptions) {
  return uploadFormImage('comment', options)
}

function uploadAfterSaleImage(options: UploadRequestOptions) {
  return uploadFormImage('afterSale', options)
}

function addFormImages(kind: ImageFormKind) {
  const form = imageForm(kind)
  const additions = splitImageInput(form.imageInput)
  if (!additions.length) {
    ElMessage.warning('请先粘贴图片 URL')
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
  if (additions.length && merged.length >= MAX_FORM_IMAGES) {
    ElMessage.info(`最多添加 ${MAX_FORM_IMAGES} 张图片`)
  }
}

function removeFormImage(kind: ImageFormKind, index: number) {
  imageForm(kind).images.splice(index, 1)
}

function previewImageList(images: string[] = [], index = 0, seed = 'image') {
  const urls = images.map((image, imageIndex) => normalizeImage(image, `${seed}-${imageIndex}`)).filter(Boolean)
  if (!urls.length) {
    return
  }
  previewImages.value = urls
  previewIndex.value = index
}

function closePreview() {
  previewImages.value = []
  previewIndex.value = 0
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

function receiverLine(detail?: OrderDetail) {
  const receiver = detail?.receiver
  if (!receiver) {
    return '-'
  }
  return `${receiver.province || ''}${receiver.city || ''}${receiver.district || ''}${receiver.detail || ''}`
}

function invoiceTypeLabel(type?: number) {
  if (type === 1) return '个人发票'
  if (type === 2) return '企业发票'
  return '不开发票'
}

async function loadOrder() {
  if (!auth.isLoggedIn) {
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id as string)
    order.value = res.data
    if (res.data?.id) {
      const [commentRes, afterSaleRes, logisticsRes] = await Promise.all([
        getOrderComments(res.data.id),
        getOrderAfterSales(res.data.id),
        getOrderLogistics(res.data.id),
      ])
      comments.value = commentRes.data || []
      afterSales.value = afterSaleRes.data || []
      logistics.value = logisticsRes.data
    }
    await cart.fetchCart()
  } finally {
    loading.value = false
  }
}

function itemComment(item: OrderItem) {
  return commentMap.value.get(String(item.id))
}

function openComment(item: OrderItem) {
  commentTarget.value = item
  commentForm.value = {
    score: 5,
    content: '',
    imageInput: '',
    images: [],
  }
  commentDialogOpen.value = true
}

async function submitComment() {
  if (!order.value || !commentTarget.value) {
    return
  }
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  commenting.value = true
  try {
    const images = collectFormImages('comment')
    await createOrderComment(order.value.id, {
      orderItemId: commentTarget.value.id,
      score: commentForm.value.score,
      content: commentForm.value.content.trim(),
      images,
    })
    ElMessage.success('评价已提交，等待审核')
    commentDialogOpen.value = false
    await loadOrder()
  } finally {
    commenting.value = false
  }
}

function openAfterSale() {
  afterSaleForm.value = {
    type: Number(order.value?.status) === 1 ? 1 : 2,
    reason: '',
    description: '',
    imageInput: '',
    images: [],
  }
  afterSaleDialogOpen.value = true
}

async function submitAfterSale() {
  if (!order.value) {
    return
  }
  if (!afterSaleForm.value.reason.trim()) {
    ElMessage.warning('请填写售后原因')
    return
  }
  afterSaleSubmitting.value = true
  try {
    const images = collectFormImages('afterSale')
    await createAfterSale(order.value.id, {
      type: afterSaleForm.value.type,
      reason: afterSaleForm.value.reason.trim(),
      description: afterSaleForm.value.description.trim() || undefined,
      images,
    })
    ElMessage.success('售后申请已提交')
    afterSaleDialogOpen.value = false
    await loadOrder()
  } finally {
    afterSaleSubmitting.value = false
  }
}

async function cancelCurrentAfterSale(item: AfterSale) {
  try {
    await ElMessageBox.confirm('确认取消这个售后申请？取消后订单会恢复到申请前状态。', '取消售后', {
      confirmButtonText: '取消售后',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  afterSaleOperatingId.value = item.id
  try {
    await cancelAfterSale(item.id)
    ElMessage.success('售后申请已取消')
    await loadOrder()
  } finally {
    afterSaleOperatingId.value = ''
  }
}

function openReturnDialog(item: AfterSale) {
  returnTarget.value = item
  returnForm.value = {
    returnCompany: item.returnCompany || '',
    returnNo: item.returnNo || '',
  }
  returnDialogOpen.value = true
}

async function submitReturnInfo() {
  if (!returnTarget.value) {
    return
  }
  if (!returnForm.value.returnCompany.trim() || !returnForm.value.returnNo.trim()) {
    ElMessage.warning('请填写退货物流公司和单号')
    return
  }
  returnSubmitting.value = true
  try {
    await submitAfterSaleReturn(returnTarget.value.id, {
      returnCompany: returnForm.value.returnCompany.trim(),
      returnNo: returnForm.value.returnNo.trim(),
    })
    ElMessage.success('退货物流已提交，等待商家确认收货')
    returnDialogOpen.value = false
    await loadOrder()
  } finally {
    returnSubmitting.value = false
  }
}

async function payOrder() {
  if (!order.value) {
    return
  }
  actionLoading.value = 'pay'
  try {
    const res = await createPay({ orderId: order.value.id, channel: payChannel.value })
    router.replace({
      path: '/pay/result',
      query: {
        status: res.data.paid ? 'success' : 'pending',
        orderId: String(order.value.id),
        orderNo: order.value.orderNo,
        payNo: res.data.payNo || '',
      },
    })
  } catch (error: any) {
    router.replace({
      path: '/pay/result',
      query: {
        status: 'fail',
        orderId: String(order.value.id),
        orderNo: order.value.orderNo,
        message: error?.message || '支付失败，请稍后再试',
      },
    })
  } finally {
    actionLoading.value = ''
  }
}

async function cancelCurrentOrder() {
  if (!order.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认取消这个订单？库存会自动释放。', '取消订单', {
      confirmButtonText: '取消订单',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }
  actionLoading.value = 'cancel'
  try {
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await loadOrder()
  } finally {
    actionLoading.value = ''
  }
}

async function receiveCurrentOrder() {
  if (!order.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认已经收到商品？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '返回',
      type: 'info',
    })
  } catch {
    return
  }
  actionLoading.value = 'receive'
  try {
    await receiveOrder(order.value.id)
    ElMessage.success('已确认收货')
    await loadOrder()
  } finally {
    actionLoading.value = ''
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadOrder()
    }
  },
)

onMounted(loadOrder)
</script>

<template>
  <div class="order-detail-page">
    <ShopHeader />

    <main class="order-shell">
      <section class="order-hero">
        <div>
          <span>Order</span>
          <h1>订单详情</h1>
          <p v-if="order">订单号 {{ order.orderNo }}</p>
          <p v-else>查看订单支付、物流和商品明细。</p>
        </div>
        <button class="back-link" type="button" @click="router.push('/orders')">返回订单列表</button>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后查看订单</h2>
        <p>订单属于会员账户资产，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else v-loading="loading" class="detail-layout">
        <div v-if="order" class="detail-main">
          <section class="detail-block status-block">
            <div class="status-heading">
              <div>
                <span>Current Status</span>
                <h2>{{ order.statusDesc || '订单状态' }}</h2>
              </div>
              <em :class="statusClass(order.status)">{{ order.statusDesc || '-' }}</em>
            </div>
            <el-steps :active="activeStep" finish-status="success" align-center>
              <el-step title="提交订单" :description="formatTime(order.createTime)" />
              <el-step title="完成支付" :description="formatTime(order.payTime)" />
              <el-step title="商家发货" :description="formatTime(order.deliveryTime)" />
              <el-step title="交易完成" :description="formatTime(order.receiveTime)" />
            </el-steps>
          </section>

          <section class="detail-block">
            <div class="block-heading">
              <div>
                <span>Items</span>
                <h2>商品明细</h2>
              </div>
              <strong>{{ order.items?.length || 0 }} 件</strong>
            </div>

            <article v-for="item in order.items || []" :key="item.id" class="order-item">
              <img
                :src="normalizeImage(item.skuImage, String(item.skuId))"
                :alt="item.skuName"
                @error="handleImageError($event, String(item.skuId))"
              />
              <div class="item-copy">
                <strong>{{ item.skuName || '商品' }}</strong>
                <span>{{ specText(item) }}</span>
              </div>
              <div class="item-qty">x{{ item.quantity }}</div>
              <div class="item-price">
                <strong>{{ formatPrice(item.totalAmount) }}</strong>
                <em v-if="itemComment(item)">{{ itemComment(item)?.statusDesc || '已评价' }}</em>
                <button v-else-if="canComment" type="button" @click="openComment(item)">评价</button>
              </div>
              <div v-if="itemComment(item)" class="order-comment-preview">
                <div>
                  <span>我的评价</span>
                  <el-rate :model-value="itemComment(item)?.score || 5" disabled size="small" />
                </div>
                <p>{{ itemComment(item)?.content }}</p>
                <div v-if="itemComment(item)?.images?.length" class="comment-preview-images">
                  <button
                    v-for="(image, index) in itemComment(item)?.images"
                    :key="`${item.id}-${image}`"
                    type="button"
                    @click="previewImageList(itemComment(item)?.images, index, `order-comment-${item.id}`)"
                  >
                    <img
                      :src="normalizeImage(image, `order-comment-${item.id}-${index}`)"
                      alt="评价图片"
                      @error="handleImageError($event, `order-comment-${item.id}-${index}`)"
                    />
                  </button>
                </div>
                <p v-if="itemComment(item)?.replyContent" class="merchant-comment-reply">
                  商家回复：{{ itemComment(item)?.replyContent }}
                </p>
              </div>
            </article>
          </section>

          <section class="info-grid">
            <article class="detail-block">
              <span>Receiver</span>
              <h2>收货信息</h2>
              <dl>
                <div>
                  <dt>收件人</dt>
                  <dd>{{ order.receiver?.receiver || '-' }}</dd>
                </div>
                <div>
                  <dt>手机号</dt>
                  <dd>{{ order.receiver?.phone || '-' }}</dd>
                </div>
                <div>
                  <dt>地址</dt>
                  <dd>{{ receiverLine(order) }}</dd>
                </div>
              </dl>
            </article>

            <article class="detail-block">
              <span>Invoice</span>
              <h2>发票信息</h2>
              <dl>
                <div>
                  <dt>类型</dt>
                  <dd>{{ invoiceTypeLabel(order.invoice?.type) }}</dd>
                </div>
                <template v-if="order.invoice">
                  <div>
                    <dt>抬头</dt>
                    <dd>{{ order.invoice.title || '-' }}</dd>
                  </div>
                  <div v-if="order.invoice.type === 2">
                    <dt>税号</dt>
                    <dd>{{ order.invoice.taxNo || '-' }}</dd>
                  </div>
                  <div>
                    <dt>邮箱</dt>
                    <dd>{{ order.invoice.email || '-' }}</dd>
                  </div>
                  <div>
                    <dt>内容</dt>
                    <dd>{{ order.invoice.content || '商品明细' }}</dd>
                  </div>
                </template>
              </dl>
            </article>

            <article class="detail-block">
              <span>Delivery</span>
              <h2>物流信息</h2>
              <dl>
                <div>
                  <dt>物流公司</dt>
                  <dd>{{ logistics?.deliveryCompany || order.deliveryCompany || '-' }}</dd>
                </div>
                <div>
                  <dt>物流单号</dt>
                  <dd>{{ logistics?.deliveryNo || order.deliveryNo || '-' }}</dd>
                </div>
                <div>
                  <dt>备注</dt>
                  <dd>{{ logistics?.deliveryRemark || order.deliveryRemark || order.remark || '-' }}</dd>
                </div>
              </dl>
            </article>
          </section>

          <section class="detail-block logistics-block">
            <div class="block-heading">
              <div>
                <span>Tracking</span>
                <h2>物流轨迹</h2>
              </div>
              <strong>{{ logistics?.statusDesc || order.statusDesc || '-' }}</strong>
            </div>

            <div v-if="logistics?.currentAction || logistics?.nextHint" class="tracking-hint">
              <strong>{{ logistics?.currentAction || logistics?.statusDesc || order.statusDesc || '履约中' }}</strong>
              <span>{{ logistics?.nextHint || '请关注订单状态变化。' }}</span>
            </div>

            <div v-if="logisticsTraces.length" class="tracking-list">
              <article v-for="trace in logisticsTraces" :key="`${trace.title}-${trace.time}`" :class="{ active: trace.active }">
                <i />
                <div>
                  <strong>{{ trace.title }}</strong>
                  <span>{{ trace.description || '-' }}</span>
                </div>
                <time>{{ formatTime(trace.time) }}</time>
              </article>
            </div>
            <div v-else class="tracking-empty">
              <strong>暂无物流轨迹</strong>
              <span>订单支付后，商家会在发货时更新物流公司和单号。</span>
            </div>
          </section>

          <section class="detail-block after-sale-block">
            <div class="block-heading">
              <div>
                <span>Service</span>
                <h2>售后服务</h2>
              </div>
              <button v-if="canApplyAfterSale" class="plain-action" type="button" @click="openAfterSale">申请售后</button>
            </div>

            <div v-if="afterSales.length" class="after-sale-list">
              <article v-for="item in afterSales" :key="item.id" class="after-sale-card">
                <div class="after-sale-card__main">
                  <div>
                    <strong>{{ item.typeDesc || '售后申请' }}</strong>
                    <span>{{ item.afterSaleNo }} · {{ formatTime(item.createTime) }}</span>
                  </div>
                  <em :class="afterSaleStatusClass(item.status)">{{ item.statusDesc || '-' }}</em>
                </div>
                <dl>
                  <div>
                    <dt>退款金额</dt>
                    <dd>{{ formatPrice(item.amount) }}</dd>
                  </div>
                  <div>
                    <dt>申请原因</dt>
                    <dd>{{ item.reason || '-' }}</dd>
                  </div>
                  <div v-if="item.auditRemark">
                    <dt>审核备注</dt>
                    <dd>{{ item.auditRemark }}</dd>
                  </div>
                  <div v-if="item.rejectReason">
                    <dt>拒绝原因</dt>
                    <dd>{{ item.rejectReason }}</dd>
                  </div>
                  <div v-if="item.returnCompany || item.returnNo">
                    <dt>退货物流</dt>
                    <dd>{{ item.returnCompany || '-' }} {{ item.returnNo || '' }}</dd>
                  </div>
                </dl>
                <div v-if="item.images?.length" class="after-sale-images">
                  <button
                    v-for="(image, index) in item.images"
                    :key="`${item.id}-${image}`"
                    type="button"
                    @click="previewImageList(item.images, index, `after-sale-${item.id}`)"
                  >
                    <img
                      :src="normalizeImage(image, `after-sale-${item.id}-${index}`)"
                      alt="售后凭证"
                      @error="handleImageError($event, `after-sale-${item.id}-${index}`)"
                    />
                  </button>
                </div>
                <div v-if="[0, 1, 2].includes(Number(item.status))" class="after-sale-actions">
                  <el-button
                    v-if="Number(item.status) === 1"
                    size="small"
                    type="primary"
                    @click="openReturnDialog(item)"
                  >
                    填写退货物流
                  </el-button>
                  <el-button
                    size="small"
                    :loading="String(afterSaleOperatingId) === String(item.id)"
                    @click="cancelCurrentAfterSale(item)"
                  >
                    取消申请
                  </el-button>
                </div>
              </article>
            </div>

            <div v-else class="after-sale-empty">
              <strong>暂无售后记录</strong>
              <span>已付款、待收货或已完成订单可以在这里申请售后。</span>
            </div>
          </section>
        </div>

        <aside v-if="order" class="pay-panel">
          <span>Payment</span>
          <h2>{{ formatPrice(order.payAmount) }}</h2>
          <dl>
            <div>
              <dt>商品金额</dt>
              <dd>{{ formatPrice(order.totalAmount) }}</dd>
            </div>
            <div>
              <dt>运费</dt>
              <dd>{{ formatPrice(order.freightAmount) }}</dd>
            </div>
            <div>
              <dt>优惠</dt>
              <dd>- {{ formatPrice(order.couponAmount) }}</dd>
            </div>
            <div>
              <dt>下单时间</dt>
              <dd>{{ formatTime(order.createTime) }}</dd>
            </div>
          </dl>

          <div v-if="canPay" class="channel-box">
            <strong>支付方式</strong>
            <el-radio-group v-model="payChannel">
              <el-radio-button label="mock">模拟支付</el-radio-button>
            </el-radio-group>
          </div>

          <div class="action-stack">
            <el-button
              v-if="canPay"
              size="large"
              type="primary"
              :loading="actionLoading === 'pay'"
              @click="payOrder"
            >
              立即支付
            </el-button>
            <el-button
              v-if="canCancel"
              size="large"
              :loading="actionLoading === 'cancel'"
              @click="cancelCurrentOrder"
            >
              取消订单
            </el-button>
            <el-button
              v-if="canReceive"
              size="large"
              type="primary"
              :loading="actionLoading === 'receive'"
              @click="receiveCurrentOrder"
            >
              确认收货
            </el-button>
            <el-button v-if="canApplyAfterSale" size="large" @click="openAfterSale">申请售后</el-button>
            <el-button size="large" @click="router.push('/')">继续逛逛</el-button>
          </div>
        </aside>

        <el-empty v-if="!loading && !order" description="订单不存在">
          <el-button type="primary" @click="router.push('/orders')">返回订单列表</el-button>
        </el-empty>
      </section>
    </main>

    <el-dialog v-model="commentDialogOpen" title="评价商品" width="520px" class="comment-dialog" append-to-body>
      <div v-if="commentTarget" class="comment-target">
        <img
          :src="normalizeImage(commentTarget.skuImage, String(commentTarget.skuId))"
          :alt="commentTarget.skuName"
        />
        <div>
          <strong>{{ commentTarget.skuName || '商品' }}</strong>
          <span>{{ specText(commentTarget) }}</span>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="评分">
          <el-rate v-model="commentForm.score" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说商品体验、包装、物流或使用感受"
          />
        </el-form-item>
        <el-form-item label="评价图片">
          <div class="image-url-field">
            <el-input
              v-model="commentForm.imageInput"
              placeholder="粘贴图片 URL，可一次粘贴多条"
              @keyup.enter="addFormImages('comment')"
            />
            <el-button @click="addFormImages('comment')">添加</el-button>
          </div>
          <el-upload
            class="image-upload-action"
            accept="image/*"
            :show-file-list="false"
            :http-request="uploadCommentImage"
          >
            <el-button :loading="imageUploading === 'comment'">选择图片上传</el-button>
          </el-upload>
          <div v-if="commentForm.images.length" class="form-image-grid">
            <figure v-for="(image, index) in commentForm.images" :key="image">
              <img
                :src="commentPreviewImages[index]"
                alt="评价图片"
                @error="handleImageError($event, `comment-form-${index}`)"
              />
              <button type="button" @click="removeFormImage('comment', index)">移除</button>
            </figure>
          </div>
          <p class="image-field-hint">最多 {{ MAX_FORM_IMAGES }} 张，提交前会自动带上输入框中尚未添加的链接。</p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="commentDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="commenting" @click="submitComment">提交评价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="afterSaleDialogOpen" title="申请售后" width="560px" class="service-dialog" append-to-body>
      <div v-if="order" class="service-summary">
        <strong>{{ order.orderNo }}</strong>
        <span>预计退款 {{ formatPrice(order.payAmount) }}，当前版本支持整单售后。</span>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="售后类型">
          <el-segmented
            v-model="afterSaleForm.type"
            :options="[
              { label: '仅退款', value: 1 },
              { label: '退货退款', value: 2 },
            ]"
          />
        </el-form-item>
        <el-form-item label="售后原因">
          <el-input v-model="afterSaleForm.reason" maxlength="120" show-word-limit placeholder="例如：商品破损、发错货、不想要了" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="afterSaleForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="补充说明商品问题、包装情况或你的诉求"
          />
        </el-form-item>
        <el-form-item label="凭证图片">
          <div class="image-url-field">
            <el-input
              v-model="afterSaleForm.imageInput"
              placeholder="粘贴图片 URL，可一次粘贴多条"
              @keyup.enter="addFormImages('afterSale')"
            />
            <el-button @click="addFormImages('afterSale')">添加</el-button>
          </div>
          <el-upload
            class="image-upload-action"
            accept="image/*"
            :show-file-list="false"
            :http-request="uploadAfterSaleImage"
          >
            <el-button :loading="imageUploading === 'afterSale'">选择图片上传</el-button>
          </el-upload>
          <div v-if="afterSaleForm.images.length" class="form-image-grid">
            <figure v-for="(image, index) in afterSaleForm.images" :key="image">
              <img
                :src="afterSalePreviewImages[index]"
                alt="售后凭证"
                @error="handleImageError($event, `after-sale-form-${index}`)"
              />
              <button type="button" @click="removeFormImage('afterSale', index)">移除</button>
            </figure>
          </div>
          <p class="image-field-hint">商品破损、错发等问题可以上传凭证图，帮助商家更快审核。</p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="afterSaleDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="afterSaleSubmitting" @click="submitAfterSale">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialogOpen" title="填写退货物流" width="480px" class="service-dialog" append-to-body>
      <div v-if="returnTarget" class="service-summary">
        <strong>{{ returnTarget.afterSaleNo }}</strong>
        <span>提交后售后单将进入待退款状态。</span>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="物流公司">
          <el-input v-model="returnForm.returnCompany" placeholder="例如：顺丰速运" />
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="returnForm.returnNo" placeholder="填写退回包裹的物流单号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="returnDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="returnSubmitting" @click="submitReturnInfo">提交物流</el-button>
      </template>
    </el-dialog>

    <el-image-viewer
      v-if="previewImages.length"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="closePreview"
    />
  </div>
</template>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f7f8f5;
  color: #111827;
}

.order-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.order-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.order-hero span,
.detail-block > span,
.block-heading span,
.status-heading span,
.pay-panel > span {
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.order-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.order-hero p {
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

.plain-action {
  min-height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: #111827;
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
}

.login-needed,
.detail-block,
.pay-panel {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
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

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
  min-height: 420px;
}

.detail-main {
  display: grid;
  gap: 18px;
}

.detail-block {
  padding: 24px;
}

.status-heading,
.block-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.status-heading h2,
.block-heading h2,
.detail-block h2 {
  margin: 9px 0 0;
  font-size: 24px;
}

.status-heading em {
  min-width: 72px;
  padding: 7px 12px;
  border-radius: 8px;
  font-style: normal;
  font-weight: 900;
  text-align: center;
}

.status-heading em.pending {
  background: #fff4dc;
  color: #b7791f;
}

.status-heading em.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.status-heading em.done {
  background: #eef8f2;
  color: #2f8f67;
}

.status-heading em.closed {
  background: #f3f4f6;
  color: #6b7280;
}

.after-sale-card__main em.processing {
  background: #e8f3ff;
  color: #2563eb;
}

.after-sale-card__main em.done {
  background: #eef8f2;
  color: #2f8f67;
}

.after-sale-card__main em.closed {
  background: #f3f4f6;
  color: #6b7280;
}

.status-block :deep(.el-step__title) {
  font-weight: 900;
}

.order-item {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) 50px 116px;
  gap: 14px;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.order-item img {
  width: 78px;
  height: 78px;
  border-radius: 8px;
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

.item-price strong,
.item-price em,
.item-price button {
  display: block;
}

.item-price em {
  margin-top: 8px;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
}

.item-price button {
  margin-top: 8px;
  margin-left: auto;
  padding: 5px 10px;
  border: 0;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
}

.order-comment-preview {
  grid-column: 2 / -1;
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #fbfcf8;
}

.order-comment-preview > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-comment-preview span {
  color: #2f8f67;
  font-size: 12px;
  font-weight: 900;
}

.order-comment-preview p {
  margin: 0;
  color: #374151;
  font-size: 13px;
  line-height: 1.55;
}

.merchant-comment-reply {
  padding-top: 8px;
  border-top: 1px solid rgba(17, 24, 39, 0.06);
  color: #6b7280 !important;
}

.comment-preview-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-preview-images button {
  width: 64px;
  height: 64px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 8px;
  background: #eef2f7;
  cursor: pointer;
}

.comment-preview-images img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-target {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  margin-bottom: 18px;
  padding: 12px;
  border-radius: 14px;
  background: #f7f8f5;
}

.comment-target img {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  object-fit: cover;
}

.comment-target strong,
.comment-target span {
  display: block;
}

.comment-target strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-target span {
  margin-top: 7px;
  color: #6b7280;
  font-size: 13px;
}

.image-url-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 78px;
  gap: 10px;
  width: 100%;
}

.image-url-field :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.image-upload-action {
  width: 100%;
  margin-top: 10px;
}

.image-upload-action :deep(.el-upload),
.image-upload-action :deep(.el-button) {
  width: 100%;
}

.image-upload-action :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.form-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  margin-top: 12px;
}

.form-image-grid figure {
  position: relative;
  min-width: 0;
  margin: 0;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 8px;
  background: #f3f4f6;
}

.form-image-grid img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-image-grid button {
  position: absolute;
  right: 6px;
  bottom: 6px;
  min-height: 26px;
  padding: 0 8px;
  border: 0;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.86);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
}

.image-field-hint {
  width: 100%;
  margin: 8px 0 0;
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.5;
}

:global(.comment-dialog.el-dialog) {
  width: min(520px, calc(100vw - 32px));
  border-radius: 14px;
  overflow: hidden;
}

:global(.comment-dialog .el-dialog__title) {
  font-weight: 900;
}

:global(.comment-dialog .el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.after-sale-block {
  overflow: hidden;
}

.logistics-block .block-heading strong {
  min-width: 72px;
  padding: 7px 12px;
  border-radius: 8px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 13px;
  text-align: center;
}

.tracking-list {
  display: grid;
  gap: 16px;
}

.tracking-hint {
  display: grid;
  gap: 6px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
}

.tracking-hint strong {
  color: #1d4ed8;
  font-size: 15px;
}

.tracking-hint span {
  color: #475569;
  font-size: 13px;
  line-height: 1.55;
}

.tracking-list article {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 156px;
  gap: 14px;
  align-items: start;
}

.tracking-list i {
  width: 14px;
  height: 14px;
  margin-top: 6px;
  border: 3px solid #d1d5db;
  border-radius: 999px;
}

.tracking-list article.active i {
  border-color: #e5484d;
  background: #e5484d;
}

.tracking-list strong,
.tracking-list span {
  display: block;
}

.tracking-list strong {
  color: #111827;
}

.tracking-list span {
  margin-top: 6px;
  color: #6b7280;
  line-height: 1.55;
}

.tracking-list time {
  color: #9ca3af;
  font-size: 13px;
  text-align: right;
}

.tracking-empty {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px dashed rgba(17, 24, 39, 0.16);
  border-radius: 8px;
  background: #fbfcf8;
}

.tracking-empty span {
  color: #6b7280;
  font-size: 13px;
}

.after-sale-list {
  display: grid;
  gap: 12px;
}

.after-sale-card {
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: linear-gradient(180deg, #fff 0%, #fbfcf8 100%);
}

.after-sale-card__main {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
}

.after-sale-card__main strong,
.after-sale-card__main span {
  display: block;
}

.after-sale-card__main span {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.after-sale-card__main em {
  min-width: 72px;
  padding: 6px 10px;
  border-radius: 8px;
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
  text-align: center;
}

.after-sale-card dl {
  margin: 14px 0 0;
}

.after-sale-card dl > div {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 12px;
  padding: 9px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.06);
}

.after-sale-card dt,
.after-sale-card dd {
  margin: 0;
}

.after-sale-card dt {
  color: #6b7280;
}

.after-sale-card dd {
  color: #111827;
  font-weight: 800;
}

.after-sale-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.after-sale-images button {
  width: 78px;
  height: 78px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 8px;
  background: #eef2f7;
  cursor: pointer;
}

.after-sale-images img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.after-sale-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.after-sale-actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 900;
}

.after-sale-actions :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.after-sale-empty {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px dashed rgba(17, 24, 39, 0.16);
  border-radius: 8px;
  background: #fbfcf8;
}

.after-sale-empty span {
  color: #6b7280;
  font-size: 13px;
}

.service-summary {
  display: grid;
  gap: 7px;
  margin-bottom: 18px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f8f5;
}

.service-summary strong {
  color: #111827;
}

.service-summary span {
  color: #6b7280;
  font-size: 13px;
}

:global(.service-dialog.el-dialog) {
  width: min(560px, calc(100vw - 32px));
  border-radius: 14px;
  overflow: hidden;
}

:global(.service-dialog .el-dialog__title) {
  font-weight: 900;
}

:global(.service-dialog .el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.detail-block dl,
.detail-block dd,
.pay-panel dl,
.pay-panel dd {
  margin: 0;
}

.detail-block dl {
  margin-top: 18px;
}

.detail-block dl > div,
.pay-panel dl > div {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.detail-block dt,
.pay-panel dt {
  color: #6b7280;
}

.detail-block dd,
.pay-panel dd {
  max-width: 68%;
  color: #111827;
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
  color: #e5484d;
  font-size: 30px;
}

.channel-box {
  margin-top: 18px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f8f5;
}

.channel-box strong {
  display: block;
  margin-bottom: 12px;
}

.action-stack {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.action-stack :deep(.el-button) {
  width: 100%;
  height: 46px;
  margin-left: 0;
  border-radius: 8px;
  font-weight: 900;
}

.action-stack :deep(.el-button--primary),
.login-needed :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
}

@media (max-width: 1040px) {
  .detail-layout,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .pay-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .order-shell {
    padding: 28px 16px 52px;
  }

  .order-hero,
  .status-heading,
  .block-heading,
  .after-sale-card__main {
    align-items: start;
    flex-direction: column;
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

  .detail-block dd,
  .pay-panel dd {
    max-width: 60%;
  }

  .after-sale-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .tracking-list article {
    grid-template-columns: 24px minmax(0, 1fr);
  }

  .tracking-list time {
    grid-column: 2;
    text-align: left;
  }

  .after-sale-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
