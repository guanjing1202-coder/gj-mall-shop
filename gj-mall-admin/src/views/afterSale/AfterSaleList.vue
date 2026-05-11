<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import {
  approveAfterSale,
  cancelAfterSale,
  createAfterSale,
  getAfterSaleDetail,
  getAfterSalePage,
  receiveAfterSale,
  refundAfterSale,
  rejectAfterSale,
  type ApiId,
  type AfterSaleActionPayload,
  type AfterSaleCreatePayload,
  type AfterSaleItem,
  type AfterSaleQuery,
  type AfterSaleRecord,
} from '@/api/afterSale'

interface CreateFormState {
  orderId?: ApiId
  type: number
  amount?: number
  reason: string
  description: string
  imagesText: string
}

type ActionMode = 'approve' | 'reject' | 'receive' | 'refund' | 'cancel'

const loading = ref(false)
const detailLoading = ref(false)
const actionLoading = ref(false)
const creating = ref(false)
const afterSales = ref<AfterSaleRecord[]>([])
const currentAfterSale = ref<AfterSaleRecord>()
const detailOpen = ref(false)
const createOpen = ref(false)
const actionOpen = ref(false)
const actionMode = ref<ActionMode>('approve')
const actionTarget = ref<AfterSaleRecord>()
const createFormRef = ref<FormInstance>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<AfterSaleQuery>({
  keyword: '',
  userId: undefined,
  type: undefined,
  status: undefined,
})

const createForm = reactive<CreateFormState>(createEmptyForm())
const actionForm = reactive({
  auditRemark: '',
  rejectReason: '',
  returnCompany: '',
  returnNo: '',
})

const createRules: any = {
  orderId: [{ required: true, message: '请输入订单ID', trigger: 'change' }],
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
}

const columns: TableColumnsType<AfterSaleRecord> = [
  { title: '售后单', dataIndex: 'afterSaleNo', key: 'afterSaleNo', width: 280 },
  { title: '订单', dataIndex: 'orderNo', key: 'orderNo', width: 210 },
  { title: '会员', dataIndex: 'userId', key: 'user', width: 180 },
  { title: '类型/金额', dataIndex: 'type', key: 'typeAmount', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '订单状态', dataIndex: 'currentOrderStatus', key: 'orderStatus', width: 130 },
  { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 330 },
]

const itemColumns: TableColumnsType<AfterSaleItem> = [
  { title: '商品', dataIndex: 'skuName', key: 'skuName' },
  { title: '单价', dataIndex: 'price', key: 'price', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90 },
  { title: '小计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
]

const typeOptions = [
  { label: '仅退款', value: 1 },
  { label: '退货退款', value: 2 },
]

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '待退货', value: 1 },
  { label: '待退款', value: 2 },
  { label: '已拒绝', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 },
]

onMounted(fetchAfterSales)

async function fetchAfterSales() {
  loading.value = true
  try {
    const res = await getAfterSalePage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      userId: normalizeId(filters.userId),
      type: filters.type,
      status: filters.status,
    })
    afterSales.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchAfterSales()
}

async function handleReset() {
  filters.keyword = ''
  filters.userId = undefined
  filters.type = undefined
  filters.status = undefined
  pagination.current = 1
  await fetchAfterSales()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchAfterSales()
}

function createEmptyForm(): CreateFormState {
  return {
    orderId: undefined,
    type: 1,
    amount: undefined,
    reason: '',
    description: '',
    imagesText: '',
  }
}

function openCreate() {
  Object.assign(createForm, createEmptyForm())
  createFormRef.value?.clearValidate()
  createOpen.value = true
}

async function submitCreate() {
  try {
    await createFormRef.value?.validate()
    const payload: AfterSaleCreatePayload = {
      orderId: requireId(createForm.orderId, '请输入正确的订单ID'),
      type: createForm.type,
      amount: createForm.amount,
      reason: normalizeOptional(createForm.reason),
      description: normalizeOptional(createForm.description),
      images: splitLines(createForm.imagesText),
    }
    creating.value = true
    try {
      await createAfterSale(payload)
      message.success('售后单创建成功')
      createOpen.value = false
      pagination.current = 1
      await fetchAfterSales()
    } finally {
      creating.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

async function handleView(record: AfterSaleRecord) {
  detailOpen.value = true
  detailLoading.value = true
  currentAfterSale.value = record
  try {
    const res = await getAfterSaleDetail(record.id)
    currentAfterSale.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function openAction(mode: ActionMode, record: AfterSaleRecord) {
  actionMode.value = mode
  actionTarget.value = record
  actionForm.auditRemark = ''
  actionForm.rejectReason = ''
  actionForm.returnCompany = record.returnCompany || ''
  actionForm.returnNo = record.returnNo || ''
  actionOpen.value = true
}

async function submitAction() {
  if (!actionTarget.value) {
    return
  }
  const payload: AfterSaleActionPayload = {
    auditRemark: normalizeOptional(actionForm.auditRemark),
    rejectReason: normalizeOptional(actionForm.rejectReason),
    returnCompany: normalizeOptional(actionForm.returnCompany),
    returnNo: normalizeOptional(actionForm.returnNo),
  }
  if (actionMode.value === 'reject' && !payload.rejectReason) {
    message.error('请输入拒绝原因')
    return
  }
  actionLoading.value = true
  try {
    const id = actionTarget.value.id
    if (actionMode.value === 'approve') {
      await approveAfterSale(id, payload)
      message.success('审核通过')
    } else if (actionMode.value === 'reject') {
      await rejectAfterSale(id, payload)
      message.success('已拒绝')
    } else if (actionMode.value === 'receive') {
      await receiveAfterSale(id, payload)
      message.success('已确认收货')
    } else if (actionMode.value === 'refund') {
      await refundAfterSale(id, payload)
      message.success('退款完成')
    } else {
      await cancelAfterSale(id, payload)
      message.success('售后单已取消')
    }
    actionOpen.value = false
    await fetchAfterSales()
    await refreshCurrent(id)
  } finally {
    actionLoading.value = false
  }
}

async function refreshCurrent(id: ApiId) {
  if (currentAfterSale.value?.id !== id) {
    return
  }
  const res = await getAfterSaleDetail(id)
  currentAfterSale.value = res.data
}

function statusColor(status?: number) {
  if (status === 0 || status === 1 || status === 2) {
    return 'processing'
  }
  if (status === 3) {
    return 'error'
  }
  if (status === 4) {
    return 'success'
  }
  return 'default'
}

function orderStatusColor(status?: number) {
  if (status === 5) {
    return 'warning'
  }
  if (status === 6) {
    return 'default'
  }
  return 'processing'
}

function actionTitle() {
  const map: Record<ActionMode, string> = {
    approve: '审核通过',
    reject: '审核拒绝',
    receive: '确认退货收货',
    refund: '确认退款',
    cancel: '取消售后单',
  }
  return map[actionMode.value]
}

function canApprove(record: AfterSaleRecord) {
  return record.status === 0
}

function canReject(record: AfterSaleRecord) {
  return record.status === 0
}

function canReceive(record: AfterSaleRecord) {
  return record.status === 1
}

function canRefund(record: AfterSaleRecord) {
  return record.status === 2
}

function canCancel(record: AfterSaleRecord) {
  return record.status === 0 || record.status === 1 || record.status === 2
}

function userText(record?: AfterSaleRecord) {
  if (!record) {
    return '--'
  }
  return record.nickname || record.username || `会员 ${record.userId}`
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '--'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatSpecs(value?: string | Record<string, string>) {
  if (!value) {
    return '--'
  }
  const specs = typeof value === 'string' ? parseSpecString(value) : value
  const items = Object.entries(specs).map(([key, item]) => `${key}: ${item}`)
  return items.length ? items.join(' / ') : '--'
}

function parseSpecString(value: string) {
  try {
    return JSON.parse(value) as Record<string, string>
  } catch {
    return {}
  }
}

function splitLines(value: string) {
  return value
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function requireId(value: ApiId | undefined, errorMessage: string) {
  const id = normalizeId(value)
  if (!id || !/^\d+$/.test(String(id))) {
    throw new Error(errorMessage)
  }
  return id
}

function normalizeOptional(value: string) {
  const text = value.trim()
  return text ? text : undefined
}

function toAfterSale(record: Record<string, any>) {
  return record as AfterSaleRecord
}

function toAfterSaleItem(record: Record<string, any>) {
  return record as AfterSaleItem
}
</script>

<template>
  <a-card title="售后/退货管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索售后单号/订单号/原因"
          style="width: 260px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.userId" allow-clear placeholder="会员 ID" style="width: 140px" />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.type" allow-clear placeholder="类型" style="width: 140px">
          <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="售后状态" style="width: 140px">
          <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="openCreate">新建售后单</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="afterSales"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1520 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'afterSaleNo'">
          <div style="font-weight: 600">{{ record.afterSaleNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.reason || '暂无原因' }}
          </div>
        </template>

        <template v-else-if="column.key === 'orderNo'">
          <div>{{ record.orderNo }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            订单 ID {{ record.orderId }}
          </div>
        </template>

        <template v-else-if="column.key === 'user'">
          <div>{{ userText(toAfterSale(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            ID {{ record.userId }} / {{ record.phone || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'typeAmount'">
          <div>{{ record.typeDesc || '--' }}</div>
          <div style="font-weight: 600; margin-top: 4px">{{ formatMoney(record.amount) }}</div>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'orderStatus'">
          <a-tag :color="orderStatusColor(record.currentOrderStatus)">
            {{ record.currentOrderStatusDesc || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toAfterSale(record))">查看</a-button>
            <a-button v-if="canApprove(toAfterSale(record))" type="link" size="small" @click="openAction('approve', toAfterSale(record))">
              通过
            </a-button>
            <a-button v-if="canReject(toAfterSale(record))" type="link" danger size="small" @click="openAction('reject', toAfterSale(record))">
              拒绝
            </a-button>
            <a-button v-if="canReceive(toAfterSale(record))" type="link" size="small" @click="openAction('receive', toAfterSale(record))">
              收货
            </a-button>
            <a-button v-if="canRefund(toAfterSale(record))" type="link" danger size="small" @click="openAction('refund', toAfterSale(record))">
              退款
            </a-button>
            <a-button v-if="canCancel(toAfterSale(record))" type="link" size="small" @click="openAction('cancel', toAfterSale(record))">
              取消
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="createOpen"
    title="新建售后单"
    :confirm-loading="creating"
    width="680px"
    @ok="submitCreate"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="订单 ID" name="orderId">
            <a-input v-model:value="createForm.orderId" placeholder="请输入订单ID" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="售后类型" name="type">
            <a-radio-group v-model:value="createForm.type">
              <a-radio :value="1">仅退款</a-radio>
              <a-radio :value="2">退货退款</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="售后金额">
            <a-input-number v-model:value="createForm.amount" :min="0.01" style="width: 100%" placeholder="留空默认订单实付" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="售后原因">
            <a-input v-model:value="createForm.reason" placeholder="例如：商品破损" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="问题描述">
            <a-textarea v-model:value="createForm.description" :rows="4" placeholder="请输入问题描述" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="凭证图片 URL">
            <a-textarea v-model:value="createForm.imagesText" :rows="3" placeholder="每行一个图片 URL" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-drawer v-model:open="detailOpen" title="售后详情" width="900" destroy-on-close>
    <a-spin :spinning="detailLoading">
      <template v-if="currentAfterSale">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="售后单号" :span="2">{{ currentAfterSale.afterSaleNo }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentAfterSale.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="会员">{{ userText(currentAfterSale) }}</a-descriptions-item>
          <a-descriptions-item label="售后类型">{{ currentAfterSale.typeDesc || '--' }}</a-descriptions-item>
          <a-descriptions-item label="售后金额">{{ formatMoney(currentAfterSale.amount) }}</a-descriptions-item>
          <a-descriptions-item label="售后状态">
            <a-tag :color="statusColor(currentAfterSale.status)">
              {{ currentAfterSale.statusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="订单状态">
            <a-tag :color="orderStatusColor(currentAfterSale.currentOrderStatus)">
              {{ currentAfterSale.currentOrderStatusDesc || '--' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="申请原因">{{ currentAfterSale.reason || '--' }}</a-descriptions-item>
          <a-descriptions-item label="原订单状态">{{ currentAfterSale.orderStatusSnapshotDesc || '--' }}</a-descriptions-item>
          <a-descriptions-item label="审核备注">{{ currentAfterSale.auditRemark || '--' }}</a-descriptions-item>
          <a-descriptions-item label="拒绝原因">{{ currentAfterSale.rejectReason || '--' }}</a-descriptions-item>
          <a-descriptions-item label="退货物流">{{ [currentAfterSale.returnCompany, currentAfterSale.returnNo].filter(Boolean).join(' / ') || '--' }}</a-descriptions-item>
          <a-descriptions-item label="退款流水ID">{{ currentAfterSale.refundPaymentId || '--' }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ currentAfterSale.createTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="退款时间">{{ currentAfterSale.refundTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="问题描述" :span="2">{{ currentAfterSale.description || '--' }}</a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">订单商品</h4>
          <a-table
            row-key="id"
            size="small"
            :columns="itemColumns"
            :data-source="currentAfterSale.items || []"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuName'">
                <div style="display: flex; gap: 12px; align-items: center">
                  <img
                    v-if="record.skuImage"
                    :src="record.skuImage"
                    alt="商品图"
                    style="width: 48px; height: 48px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0"
                  />
                  <div>
                    <div style="font-weight: 600">{{ record.skuName }}</div>
                    <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                      {{ formatSpecs(toAfterSaleItem(record).specData) }}
                    </div>
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'price'">
                {{ formatMoney(record.price) }}
              </template>
              <template v-else-if="column.key === 'totalAmount'">
                {{ formatMoney(record.totalAmount) }}
              </template>
            </template>
          </a-table>
        </div>

        <div v-if="currentAfterSale.images?.length" style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">售后凭证</h4>
          <a-space wrap>
            <a v-for="image in currentAfterSale.images" :key="image" :href="image" target="_blank">
              {{ image }}
            </a>
          </a-space>
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: flex-end">
          <a-space>
            <a-button v-if="canApprove(currentAfterSale)" @click="openAction('approve', currentAfterSale)">审核通过</a-button>
            <a-button v-if="canReject(currentAfterSale)" danger @click="openAction('reject', currentAfterSale)">拒绝</a-button>
            <a-button v-if="canReceive(currentAfterSale)" @click="openAction('receive', currentAfterSale)">确认收货</a-button>
            <a-button v-if="canRefund(currentAfterSale)" danger @click="openAction('refund', currentAfterSale)">确认退款</a-button>
            <a-button v-if="canCancel(currentAfterSale)" @click="openAction('cancel', currentAfterSale)">取消售后</a-button>
          </a-space>
        </div>
      </template>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="actionOpen"
    :title="actionTitle()"
    :confirm-loading="actionLoading"
    @ok="submitAction"
  >
    <a-form layout="vertical">
      <a-form-item label="售后单">
        <a-input :value="actionTarget?.afterSaleNo" disabled />
      </a-form-item>
      <a-form-item v-if="actionMode === 'reject'" label="拒绝原因">
        <a-textarea v-model:value="actionForm.rejectReason" :rows="3" placeholder="请输入拒绝原因" />
      </a-form-item>
      <a-row v-if="actionMode === 'receive'" :gutter="12">
        <a-col :span="12">
          <a-form-item label="退货物流公司">
            <a-input v-model:value="actionForm.returnCompany" placeholder="例如：顺丰" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="退货物流单号">
            <a-input v-model:value="actionForm.returnNo" placeholder="请输入物流单号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="操作备注">
        <a-textarea v-model:value="actionForm.auditRemark" :rows="4" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
