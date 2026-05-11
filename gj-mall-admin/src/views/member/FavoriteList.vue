<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType, TableProps } from 'ant-design-vue'
import {
  batchDeleteFavorites,
  deleteFavorite,
  getFavoritePage,
  type ApiId,
  type FavoriteQuery,
  type FavoriteRecord,
} from '@/api/favorite'

const loading = ref(false)
const actionId = ref<ApiId>()
const selectedRowKeys = ref<ApiId[]>([])
const favorites = ref<FavoriteRecord[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive<FavoriteQuery>({
  keyword: '',
  userId: undefined,
  spuId: undefined,
})

const columns: TableColumnsType<FavoriteRecord> = [
  { title: '会员', dataIndex: 'userId', key: 'user', width: 230 },
  { title: '收藏商品', dataIndex: 'spuName', key: 'product', width: 360 },
  { title: '品牌/分类', dataIndex: 'brandName', key: 'taxonomy', width: 180 },
  { title: '售价', dataIndex: 'price', key: 'price', width: 120 },
  { title: '商品状态', dataIndex: 'publishStatus', key: 'publishStatus', width: 120 },
  { title: '收藏时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 },
]

const rowSelection = computed<TableProps['rowSelection']>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange(keys) {
    selectedRowKeys.value = keys as ApiId[]
  },
}))

onMounted(fetchFavorites)

async function fetchFavorites() {
  loading.value = true
  try {
    const res = await getFavoritePage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined,
      userId: normalizeId(filters.userId),
      spuId: normalizeId(filters.spuId),
    })
    favorites.value = res.data.list
    pagination.total = Number(res.data.total || 0)
    selectedRowKeys.value = selectedRowKeys.value.filter((id) =>
      favorites.value.some((item) => String(item.id) === String(id)),
    )
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchFavorites()
}

async function handleReset() {
  filters.keyword = ''
  filters.userId = undefined
  filters.spuId = undefined
  pagination.current = 1
  selectedRowKeys.value = []
  await fetchFavorites()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchFavorites()
}

function handleDelete(record: FavoriteRecord) {
  Modal.confirm({
    title: '确认删除这条收藏记录吗？',
    content: `${userText(record)} / ${record.spuName || record.spuId}`,
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteFavorite(record.id)
        message.success('收藏记录已删除')
        selectedRowKeys.value = selectedRowKeys.value.filter((id) => String(id) !== String(record.id))
        if (favorites.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchFavorites()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleBatchDelete() {
  if (!selectedRowKeys.value.length) {
    message.warning('请选择收藏记录')
    return
  }
  Modal.confirm({
    title: `确认删除选中的 ${selectedRowKeys.value.length} 条收藏记录吗？`,
    okButtonProps: { danger: true },
    async onOk() {
      loading.value = true
      try {
        await batchDeleteFavorites(selectedRowKeys.value)
        message.success('已批量删除收藏记录')
        selectedRowKeys.value = []
        await fetchFavorites()
      } finally {
        loading.value = false
      }
    },
  })
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function userText(record: FavoriteRecord) {
  return record.nickname || record.username || `会员 ${record.userId}`
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) {
    return '--'
  }
  return `¥${Number(value).toFixed(2)}`
}

function statusText(status?: number) {
  return status === 1 ? '已上架' : '已下架'
}

function statusColor(status?: number) {
  return status === 1 ? 'success' : 'default'
}

function toFavorite(record: Record<string, any>) {
  return record as FavoriteRecord
}
</script>

<template>
  <a-card title="会员收藏管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索会员/商品"
          style="width: 240px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.userId" allow-clear placeholder="会员 ID" style="width: 160px" />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.spuId" allow-clear placeholder="商品 SPU ID" style="width: 180px" />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button danger :disabled="!selectedRowKeys.length" @click="handleBatchDelete">批量删除</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="favorites"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: 1320 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div style="font-weight: 600">{{ userText(toFavorite(record)) }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            ID {{ record.userId }} / {{ record.phone || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'product'">
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
          <div>{{ record.brandName || '--' }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.categoryName || '--' }}
          </div>
        </template>

        <template v-else-if="column.key === 'price'">
          {{ formatMoney(record.price) }}
        </template>

        <template v-else-if="column.key === 'publishStatus'">
          <a-tag :color="statusColor(record.publishStatus)">
            {{ statusText(record.publishStatus) }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-button
            type="link"
            danger
            size="small"
            :loading="actionId === record.id"
            @click="handleDelete(toFavorite(record))"
          >
            删除
          </a-button>
        </template>
      </template>
    </a-table>
  </a-card>
</template>
