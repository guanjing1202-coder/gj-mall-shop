<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import {
  createCoupon,
  deleteCoupon,
  getCouponPage,
  getCouponUsers,
  issueCoupon,
  updateCoupon,
  updateCouponStatus,
  type ApiId,
  type CouponPayload,
  type CouponRecord,
  type CouponQuery,
  type CouponUserQuery,
  type CouponUserRecord,
} from '@/api/coupon'

interface CouponFormState {
  id?: ApiId
  name: string
  type: number
  discountAmount?: number
  discountRate?: number
  minAmount: number
  totalCount: number
  validityRange?: [string, string]
  status: number
}

const loading = ref(false)
const saving = ref(false)
const actionId = ref<ApiId>()
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const coupons = ref<CouponRecord[]>([])

const issueOpen = ref(false)
const issueSaving = ref(false)
const issueTarget = ref<CouponRecord>()
const issueInput = ref('')

const recordsOpen = ref(false)
const recordsLoading = ref(false)
const recordsTarget = ref<CouponRecord>()
const couponUsers = ref<CouponUserRecord[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const userPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<CouponQuery>({
  keyword: '',
  type: undefined,
  status: undefined,
})

const userFilters = reactive<CouponUserQuery>({
  userKeyword: '',
  status: undefined,
})

const couponForm = reactive<CouponFormState>(createEmptyForm())

const formRules: any = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'change' }],
  validityRange: [{ required: true, type: 'array', message: '请选择有效期', trigger: 'change' }],
}

const columns: TableColumnsType<CouponRecord> = [
  { title: '优惠券', dataIndex: 'name', key: 'name', width: 260 },
  { title: '优惠内容', dataIndex: 'discountAmount', key: 'discount', width: 180 },
  { title: '库存', dataIndex: 'remainCount', key: 'stock', width: 210 },
  { title: '有效期', dataIndex: 'startTime', key: 'validity', width: 260 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 310 },
]

const userColumns: TableColumnsType<CouponUserRecord> = [
  { title: '会员', dataIndex: 'username', key: 'user', width: 220 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '领取时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '使用时间', dataIndex: 'usedAt', key: 'usedAt', width: 180 },
  { title: '关联订单', dataIndex: 'orderId', key: 'orderId', width: 140 },
]

const couponTypes = [
  { label: '满减券', value: 1 },
  { label: '折扣券', value: 2 },
  { label: '新人券', value: 3 },
]

const couponStatuses = [
  { label: '开启', value: 1 },
  { label: '关闭', value: 0 },
]

const couponUserStatuses = [
  { label: '未使用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已过期', value: 2 },
]

onMounted(fetchCoupons)

async function fetchCoupons() {
  loading.value = true
  try {
    const res = await getCouponPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      type: filters.type,
      status: filters.status,
    })
    coupons.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchCoupons()
}

async function handleReset() {
  filters.keyword = ''
  filters.type = undefined
  filters.status = undefined
  pagination.current = 1
  await fetchCoupons()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchCoupons()
}

function createEmptyForm(): CouponFormState {
  return {
    id: undefined,
    name: '',
    type: 1,
    discountAmount: undefined,
    discountRate: undefined,
    minAmount: 0,
    totalCount: 100,
    validityRange: undefined,
    status: 0,
  }
}

function resetForm() {
  Object.assign(couponForm, createEmptyForm())
  formRef.value?.clearValidate()
}

function handleCreate() {
  modalMode.value = 'create'
  resetForm()
  modalOpen.value = true
}

function handleEdit(record: CouponRecord) {
  modalMode.value = 'edit'
  Object.assign(couponForm, {
    id: record.id,
    name: record.name || '',
    type: record.type || 1,
    discountAmount: record.discountAmount,
    discountRate: record.discountRate,
    minAmount: record.minAmount ?? 0,
    totalCount: record.totalCount ?? 1,
    validityRange: record.startTime && record.endTime ? [record.startTime, record.endTime] : undefined,
    status: record.status ?? 0,
  })
  formRef.value?.clearValidate()
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    const payload = buildPayload()
    saving.value = true
    try {
      if (modalMode.value === 'create') {
        await createCoupon(payload)
        message.success('优惠券创建成功')
      } else {
        await updateCoupon(payload)
        message.success('优惠券更新成功')
      }
      modalOpen.value = false
      pagination.current = 1
      await fetchCoupons()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

function buildPayload(): CouponPayload {
  if (!couponForm.validityRange) {
    throw new Error('请选择有效期')
  }
  if (couponForm.type === 2) {
    if (!couponForm.discountRate || couponForm.discountRate <= 0 || couponForm.discountRate >= 1) {
      throw new Error('折扣率需大于0且小于1')
    }
  } else if (!couponForm.discountAmount || couponForm.discountAmount <= 0) {
    throw new Error('优惠金额必须大于0')
  }
  return {
    id: couponForm.id,
    name: couponForm.name.trim(),
    type: couponForm.type,
    discountAmount: couponForm.type === 2 ? undefined : couponForm.discountAmount,
    discountRate: couponForm.type === 2 ? couponForm.discountRate : undefined,
    minAmount: couponForm.minAmount ?? 0,
    totalCount: couponForm.totalCount,
    startTime: couponForm.validityRange[0],
    endTime: couponForm.validityRange[1],
    status: couponForm.status,
  }
}

function handleStatus(record: CouponRecord) {
  const nextStatus = record.status === 1 ? 0 : 1
  const text = nextStatus === 1 ? '开启' : '关闭'
  Modal.confirm({
    title: `确认${text}该优惠券吗？`,
    content: record.name,
    async onOk() {
      actionId.value = record.id
      try {
        await updateCouponStatus(record.id, nextStatus)
        message.success(`${text}成功`)
        await fetchCoupons()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleDelete(record: CouponRecord) {
  Modal.confirm({
    title: '确认删除这个优惠券吗？',
    content: '已有领取记录的优惠券后端会拒绝删除，可改为关闭。',
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteCoupon(record.id)
        message.success('优惠券删除成功')
        if (coupons.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchCoupons()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function openIssue(record: CouponRecord) {
  issueTarget.value = record
  issueInput.value = ''
  issueOpen.value = true
}

async function submitIssue() {
  if (!issueTarget.value) {
    return
  }
  const userIds = parseUserIds(issueInput.value)
  if (!userIds.length) {
    message.error('请输入会员ID')
    return
  }
  issueSaving.value = true
  try {
    const res = await issueCoupon(issueTarget.value.id, userIds)
    message.success(res.data > 0 ? `已发放 ${res.data} 张优惠券` : '所选会员均已领取过该券')
    issueOpen.value = false
    await fetchCoupons()
    if (recordsOpen.value && recordsTarget.value?.id === issueTarget.value.id) {
      await fetchUserRecords()
    }
  } finally {
    issueSaving.value = false
  }
}

function parseUserIds(text: string) {
  return Array.from(new Set(
    text
      .split(/[\s,，;；]+/)
      .map((item) => item.trim())
      .filter((item) => /^\d+$/.test(item) && item !== '0'),
  ))
}

async function openRecords(record: CouponRecord) {
  recordsTarget.value = record
  recordsOpen.value = true
  userFilters.userKeyword = ''
  userFilters.status = undefined
  userPagination.current = 1
  await fetchUserRecords()
}

async function fetchUserRecords() {
  if (!recordsTarget.value) {
    return
  }
  recordsLoading.value = true
  try {
    const res = await getCouponUsers(recordsTarget.value.id, {
      pageNum: userPagination.current,
      pageSize: userPagination.pageSize,
      userKeyword: userFilters.userKeyword?.trim() || undefined,
      status: userFilters.status,
    })
    couponUsers.value = res.data.list
    userPagination.total = Number(res.data.total || 0)
  } finally {
    recordsLoading.value = false
  }
}

async function handleUserSearch() {
  userPagination.current = 1
  await fetchUserRecords()
}

async function handleUserReset() {
  userFilters.userKeyword = ''
  userFilters.status = undefined
  userPagination.current = 1
  await fetchUserRecords()
}

async function handleUserTableChange(page: { current?: number; pageSize?: number }) {
  userPagination.current = page.current || 1
  userPagination.pageSize = page.pageSize || 10
  await fetchUserRecords()
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '¥0.00'
  }
  return `¥${Number(value).toFixed(2)}`
}

function discountText(record: CouponRecord) {
  if (record.type === 2) {
    return record.discountRate ? `${(Number(record.discountRate) * 10).toFixed(1)}折` : '--'
  }
  return `减 ${formatMoney(record.discountAmount)}`
}

function thresholdText(record: CouponRecord) {
  return record.minAmount && record.minAmount > 0 ? `满 ${formatMoney(record.minAmount)} 可用` : '无门槛'
}

function stockPercent(record: CouponRecord) {
  if (!record.totalCount) {
    return 0
  }
  return Math.min(Math.round(((record.receivedCount || 0) / record.totalCount) * 100), 100)
}

function statusColor(status?: number) {
  return status === 1 ? 'success' : 'default'
}

function validityColor(status?: string) {
  if (status === '进行中') {
    return 'processing'
  }
  if (status === '未开始') {
    return 'warning'
  }
  return 'default'
}

function userStatusColor(status?: number) {
  if (status === 0) {
    return 'processing'
  }
  if (status === 1) {
    return 'success'
  }
  return 'default'
}

function userName(record: CouponUserRecord) {
  return record.nickname || record.username || `会员 ${record.userId}`
}

function toCoupon(record: Record<string, any>) {
  return record as CouponRecord
}

function toCouponUser(record: Record<string, any>) {
  return record as CouponUserRecord
}
</script>

<template>
  <a-card title="优惠券管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索优惠券名称"
          style="width: 240px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.type" allow-clear placeholder="类型" style="width: 140px">
          <a-select-option v-for="item in couponTypes" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="状态" style="width: 140px">
          <a-select-option v-for="item in couponStatuses" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="handleCreate">新增优惠券</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="coupons"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1530 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div style="font-weight: 600">{{ record.name }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.typeDesc || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'discount'">
          <div style="font-weight: 600">{{ discountText(toCoupon(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ thresholdText(toCoupon(record)) }}
          </div>
        </template>

        <template v-else-if="column.key === 'stock'">
          <div>{{ record.receivedCount || 0 }} / {{ record.totalCount || 0 }}</div>
          <a-progress
            :percent="stockPercent(toCoupon(record))"
            size="small"
            :show-info="false"
            style="margin-top: 6px"
          />
          <div style="font-size: 12px; color: #8c8c8c">
            已用 {{ record.usedCount || 0 }}，剩余 {{ record.remainCount || 0 }}
          </div>
        </template>

        <template v-else-if="column.key === 'validity'">
          <div>{{ record.startTime || '--' }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin: 4px 0">
            至 {{ record.endTime || '--' }}
          </div>
          <a-tag :color="validityColor(record.validityStatus)">
            {{ record.validityStatus || '--' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || (record.status === 1 ? '开启' : '关闭') }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(toCoupon(record))">编辑</a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleStatus(toCoupon(record))"
            >
              {{ record.status === 1 ? '关闭' : '开启' }}
            </a-button>
            <a-button type="link" size="small" @click="openIssue(toCoupon(record))">发券</a-button>
            <a-button type="link" size="small" @click="openRecords(toCoupon(record))">记录</a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toCoupon(record))"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新增优惠券' : '编辑优惠券'"
    :confirm-loading="saving"
    width="760px"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="couponForm" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="优惠券名称" name="name">
            <a-input v-model:value="couponForm.name" placeholder="请输入名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="优惠券类型" name="type">
            <a-radio-group v-model:value="couponForm.type">
              <a-radio :value="1">满减券</a-radio>
              <a-radio :value="2">折扣券</a-radio>
              <a-radio :value="3">新人券</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col v-if="couponForm.type === 2" :span="12">
          <a-form-item label="折扣率">
            <a-input-number
              v-model:value="couponForm.discountRate"
              :min="0.01"
              :max="0.99"
              :step="0.01"
              style="width: 100%"
              placeholder="0.85 表示 85 折"
            />
          </a-form-item>
        </a-col>
        <a-col v-else :span="12">
          <a-form-item label="优惠金额">
            <a-input-number
              v-model:value="couponForm.discountAmount"
              :min="0.01"
              :step="1"
              style="width: 100%"
              placeholder="请输入优惠金额"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="使用门槛">
            <a-input-number
              v-model:value="couponForm.minAmount"
              :min="0"
              :step="1"
              style="width: 100%"
              placeholder="0 为无门槛"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="发行总量" name="totalCount">
            <a-input-number
              v-model:value="couponForm.totalCount"
              :min="1"
              :step="1"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态">
            <a-radio-group v-model:value="couponForm.status">
              <a-radio :value="1">开启</a-radio>
              <a-radio :value="0">关闭</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="有效期" name="validityRange">
            <a-range-picker
              v-model:value="couponForm.validityRange"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="issueOpen"
    title="发放优惠券"
    :confirm-loading="issueSaving"
    @ok="submitIssue"
  >
    <a-form layout="vertical">
      <a-form-item label="优惠券">
        <a-input :value="issueTarget?.name" disabled />
      </a-form-item>
      <a-form-item label="会员ID">
        <a-textarea
          v-model:value="issueInput"
          :rows="4"
          placeholder="多个会员ID用逗号、空格或换行分隔"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-drawer
    v-model:open="recordsOpen"
    :title="recordsTarget ? `${recordsTarget.name} - 领取记录` : '领取记录'"
    width="920"
    destroy-on-close
  >
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="userFilters.userKeyword"
          allow-clear
          placeholder="搜索会员账号/昵称/手机号"
          style="width: 260px"
          @pressEnter="handleUserSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="userFilters.status" allow-clear placeholder="状态" style="width: 140px">
          <a-select-option v-for="item in couponUserStatuses" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleUserSearch">查询</a-button>
          <a-button @click="handleUserReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      size="small"
      :columns="userColumns"
      :data-source="couponUsers"
      :loading="recordsLoading"
      :pagination="userPagination"
      :scroll="{ x: 900 }"
      @change="handleUserTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div style="font-weight: 600">{{ userName(toCouponUser(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            ID {{ record.userId }} / {{ record.phone || '--' }}
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="userStatusColor(record.status)">
            {{ record.statusDesc || '--' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>
        <template v-else-if="column.key === 'usedAt'">
          {{ record.usedAt || '--' }}
        </template>
        <template v-else-if="column.key === 'orderId'">
          {{ record.orderId || '--' }}
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>
