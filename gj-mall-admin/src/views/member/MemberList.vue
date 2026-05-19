<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  deleteMember,
  getMemberDetail,
  getMemberPage,
  updateMemberStatus,
  type ApiId,
  type MemberAddress,
  type MemberBrowseHistory,
  type MemberQuery,
  type MemberRecord,
} from '@/api/member'

const loading = ref(false)
const detailLoading = ref(false)
const actionId = ref<ApiId>()
const detailOpen = ref(false)
const currentMember = ref<MemberRecord>()
const members = ref<MemberRecord[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<MemberQuery>({
  keyword: '',
  status: undefined,
})

const columns: TableColumnsType<MemberRecord> = [
  { title: '会员', dataIndex: 'username', key: 'username', width: 260 },
  { title: '联系方式', dataIndex: 'phone', key: 'contact', width: 220 },
  { title: '消费概览', dataIndex: 'paidAmount', key: 'summary', width: 190 },
  { title: '地址数', dataIndex: 'addressCount', key: 'addressCount', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '最近登录', dataIndex: 'lastLoginAt', key: 'lastLoginAt', width: 190 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 220 },
]

const addressColumns: TableColumnsType<MemberAddress> = [
  { title: '收货人', dataIndex: 'receiver', key: 'receiver', width: 160 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 150 },
  { title: '地区', dataIndex: 'province', key: 'region', width: 220 },
  { title: '详细地址', dataIndex: 'detail', key: 'detail' },
  { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 80 },
]

const historyColumns: TableColumnsType<MemberBrowseHistory> = [
  { title: '商品', dataIndex: 'spuName', key: 'product', width: 360 },
  { title: '品牌/分类', dataIndex: 'brandName', key: 'taxonomy', width: 180 },
  { title: '价格', dataIndex: 'price', key: 'price', width: 120 },
  { title: '销量', dataIndex: 'saleCount', key: 'saleCount', width: 100 },
  { title: '状态', dataIndex: 'publishStatus', key: 'publishStatus', width: 100 },
  { title: '浏览时间', dataIndex: 'browseTime', key: 'browseTime', width: 190 },
]

onMounted(fetchMembers)

async function fetchMembers() {
  loading.value = true
  try {
    const res = await getMemberPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      status: filters.status,
    })
    members.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchMembers()
}

async function handleReset() {
  filters.keyword = ''
  filters.status = undefined
  pagination.current = 1
  await fetchMembers()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchMembers()
}

async function handleView(record: MemberRecord) {
  detailOpen.value = true
  detailLoading.value = true
  currentMember.value = record
  try {
    const res = await getMemberDetail(record.id)
    currentMember.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function handleStatus(record: MemberRecord) {
  const nextStatus = record.status === 1 ? 0 : 1
  const text = nextStatus === 1 ? '启用' : '禁用'
  Modal.confirm({
    title: `确认${text}该会员吗？`,
    content: `账号：${displayName(record)}`,
    async onOk() {
      actionId.value = record.id
      try {
        await updateMemberStatus(record.id, nextStatus)
        message.success(`${text}成功`)
        await fetchMembers()
        syncCurrentMember(record.id)
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleDelete(record: MemberRecord) {
  Modal.confirm({
    title: '确认删除这个会员吗？',
    content: `账号：${displayName(record)}`,
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteMember(record.id)
        message.success('会员删除成功')
        if (members.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchMembers()
        if (currentMember.value?.id === record.id) {
          detailOpen.value = false
          currentMember.value = undefined
        }
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function syncCurrentMember(id: ApiId) {
  if (currentMember.value?.id !== id) {
    return
  }
  const latest = members.value.find((item) => item.id === id)
  if (latest) {
    currentMember.value = {
      ...currentMember.value,
      ...latest,
      addresses: currentMember.value.addresses,
    }
  }
}

function displayName(record?: MemberRecord) {
  if (!record) {
    return '--'
  }
  return record.nickname || record.username || `ID ${record.id}`
}

function genderText(gender?: number) {
  const map: Record<number, string> = {
    0: '未知',
    1: '男',
    2: '女',
  }
  return gender === undefined || gender === null ? '--' : map[gender] || '未知'
}

function statusText(status?: number) {
  return status === 1 ? '启用' : '禁用'
}

function statusColor(status?: number) {
  return status === 1 ? 'success' : 'default'
}

function productStatusText(status?: number) {
  return status === 1 ? '已上架' : '已下架'
}

function productStatusColor(status?: number) {
  return status === 1 ? 'success' : 'default'
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '¥0.00'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatRegion(address?: MemberAddress) {
  if (!address) {
    return '--'
  }
  return [address.province, address.city, address.district].filter(Boolean).join(' ') || '--'
}

function formatAddress(address?: MemberAddress) {
  if (!address) {
    return '--'
  }
  return [address.province, address.city, address.district, address.detail]
    .filter(Boolean)
    .join(' ') || '--'
}

function toMember(record: Record<string, any>) {
  return record as MemberRecord
}

function toAddress(record: Record<string, any>) {
  return record as MemberAddress
}

function toHistory(record: Record<string, any>) {
  return record as MemberBrowseHistory
}
</script>

<template>
  <a-card title="会员管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索账号/昵称/手机号"
          style="width: 260px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="状态" style="width: 140px">
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="members"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1480 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <div style="display: flex; gap: 12px; align-items: center">
            <a-avatar :src="record.avatar" :size="40">
              {{ displayName(toMember(record)).slice(0, 1) }}
            </a-avatar>
            <div>
              <div style="font-weight: 600">{{ record.username }}</div>
              <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                {{ record.nickname || '暂无昵称' }}
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="column.key === 'contact'">
          <div>{{ record.phone || '--' }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.email || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'summary'">
          <div style="font-weight: 600">{{ formatMoney(record.paidAmount) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.orderCount || 0 }} 笔订单
          </div>
        </template>

        <template v-else-if="column.key === 'addressCount'">
          {{ record.addressCount || 0 }}
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ statusText(record.status) }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'lastLoginAt'">
          <div>{{ record.lastLoginAt || '--' }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.lastLoginIp || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleView(toMember(record))">查看</a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleStatus(toMember(record))"
            >
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toMember(record))"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-drawer v-model:open="detailOpen" title="会员详情" width="860" destroy-on-close>
    <a-spin :spinning="detailLoading">
      <template v-if="currentMember">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="账号">{{ currentMember.username }}</a-descriptions-item>
          <a-descriptions-item label="昵称">{{ currentMember.nickname || '--' }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ currentMember.phone || '--' }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ currentMember.email || '--' }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ genderText(currentMember.gender) }}</a-descriptions-item>
          <a-descriptions-item label="生日">{{ currentMember.birthday || '--' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(currentMember.status)">
              {{ statusText(currentMember.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="注册时间">{{ currentMember.createTime || '--' }}</a-descriptions-item>
          <a-descriptions-item label="最近登录">{{ currentMember.lastLoginAt || '--' }}</a-descriptions-item>
          <a-descriptions-item label="登录 IP">{{ currentMember.lastLoginIp || '--' }}</a-descriptions-item>
          <a-descriptions-item label="订单数">{{ currentMember.orderCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="累计实付">{{ formatMoney(currentMember.paidAmount) }}</a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 12px">收货地址</h4>
          <a-table
            row-key="id"
            size="small"
            :columns="addressColumns"
            :data-source="currentMember.addresses || []"
            :pagination="false"
            :scroll="{ x: 760 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'region'">
                {{ formatRegion(toAddress(record)) }}
              </template>
              <template v-else-if="column.key === 'detail'">
                {{ formatAddress(toAddress(record)) }}
              </template>
              <template v-else-if="column.key === 'isDefault'">
                <a-tag v-if="record.isDefault === 1" color="processing">默认</a-tag>
                <span v-else>--</span>
              </template>
            </template>
          </a-table>
        </div>

        <div style="margin-top: 20px">
          <div style="display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 12px">
            <h4 style="margin: 0">最近浏览足迹</h4>
            <a-tag color="processing">共 {{ currentMember.browseHistoryTotal || 0 }} 条</a-tag>
          </div>
          <a-table
            row-key="spuId"
            size="small"
            :columns="historyColumns"
            :data-source="currentMember.browseHistories || []"
            :pagination="false"
            :scroll="{ x: 1050 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'product'">
                <div style="display: flex; gap: 12px; align-items: center">
                  <img
                    v-if="record.mainImage"
                    :src="record.mainImage"
                    alt="商品图"
                    style="width: 48px; height: 48px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0"
                  />
                  <div
                    v-else
                    style="width: 48px; height: 48px; border-radius: 6px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; color: #999"
                  >
                    无图
                  </div>
                  <div>
                    <div style="font-weight: 600">{{ record.spuName || '商品已删除' }}</div>
                    <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                      {{ record.subTitle || '暂无副标题' }}
                    </div>
                    <div style="font-size: 12px; color: #bfbfbf; margin-top: 2px">SPU {{ record.spuId }}</div>
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'taxonomy'">
                <div>{{ toHistory(record).brandName || '--' }}</div>
                <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                  {{ toHistory(record).categoryName || '--' }}
                </div>
              </template>
              <template v-else-if="column.key === 'price'">
                {{ formatMoney(record.price) }}
              </template>
              <template v-else-if="column.key === 'saleCount'">
                {{ record.saleCount || 0 }}
              </template>
              <template v-else-if="column.key === 'publishStatus'">
                <a-tag :color="productStatusColor(record.publishStatus)">
                  {{ productStatusText(record.publishStatus) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'browseTime'">
                {{ record.browseTime || '--' }}
              </template>
            </template>
          </a-table>
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: flex-end">
          <a-space>
            <a-button
              :loading="actionId === currentMember.id"
              @click="handleStatus(currentMember)"
            >
              {{ currentMember.status === 1 ? '禁用会员' : '启用会员' }}
            </a-button>
            <a-button
              danger
              :loading="actionId === currentMember.id"
              @click="handleDelete(currentMember)"
            >
              删除会员
            </a-button>
          </a-space>
        </div>
      </template>
    </a-spin>
  </a-drawer>
</template>
