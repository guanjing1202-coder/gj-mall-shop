<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  getOperationLogPage,
  type ApiId,
  type OperationLogRecord,
} from '@/api/operationLog'

const loading = ref(false)
const detailOpen = ref(false)
const logs = ref<OperationLogRecord[]>([])
const currentLog = ref<OperationLogRecord>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive({
  keyword: '',
  adminId: undefined as ApiId | undefined,
  requestMethod: undefined as string | undefined,
  status: undefined as number | undefined,
})

const columns: TableColumnsType<OperationLogRecord> = [
  { title: '操作', dataIndex: 'operation', key: 'operation', width: 260 },
  { title: '管理员', dataIndex: 'username', key: 'admin', width: 170 },
  { title: '请求', dataIndex: 'requestUri', key: 'request', width: 320 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '耗时', dataIndex: 'costTime', key: 'costTime', width: 110 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 110 },
]

onMounted(fetchLogs)

async function fetchLogs() {
  loading.value = true
  try {
    const res = await getOperationLogPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword.trim() || undefined,
      adminId: normalizeId(filters.adminId),
      requestMethod: filters.requestMethod,
      status: filters.status,
    })
    logs.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } catch {
    logs.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchLogs()
}

async function handleReset() {
  filters.keyword = ''
  filters.adminId = undefined
  filters.requestMethod = undefined
  filters.status = undefined
  pagination.current = 1
  await fetchLogs()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchLogs()
}

function handleView(record: OperationLogRecord) {
  currentLog.value = record
  detailOpen.value = true
}

function statusColor(status?: number) {
  return status === 1 ? 'success' : 'error'
}

function methodColor(method?: string) {
  const map: Record<string, string> = {
    POST: 'processing',
    PUT: 'warning',
    PATCH: 'purple',
    DELETE: 'error',
  }
  return method ? map[method] || 'default' : 'default'
}

function normalizeId(value?: ApiId) {
  const text = String(value ?? '').trim()
  return text ? text : undefined
}

function toLog(record: Record<string, any>) {
  return record as OperationLogRecord
}
</script>

<template>
  <a-card title="操作日志" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索管理员/模块/操作/接口"
          style="width: 260px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-input v-model:value="filters.adminId" allow-clear placeholder="管理员 ID" style="width: 140px" />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.requestMethod" allow-clear placeholder="方法" style="width: 120px">
          <a-select-option value="POST">POST</a-select-option>
          <a-select-option value="PUT">PUT</a-select-option>
          <a-select-option value="PATCH">PATCH</a-select-option>
          <a-select-option value="DELETE">DELETE</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="filters.status" allow-clear placeholder="状态" style="width: 120px">
          <a-select-option :value="1">成功</a-select-option>
          <a-select-option :value="0">失败</a-select-option>
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
      :data-source="logs"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1260 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'operation'">
          <div class="log-title">{{ record.operation || '--' }}</div>
          <div class="log-muted">{{ record.module || '--' }}</div>
        </template>
        <template v-else-if="column.key === 'admin'">
          <div>{{ record.username || '--' }}</div>
          <div class="log-muted">ID {{ record.adminId || '--' }}</div>
        </template>
        <template v-else-if="column.key === 'request'">
          <a-tag :color="methodColor(record.requestMethod)">
            {{ record.requestMethod }}
          </a-tag>
          <span class="log-uri">{{ record.requestUri }}</span>
          <div class="log-muted">{{ record.ip || '--' }}</div>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || (record.status === 1 ? '成功' : '失败') }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'costTime'">
          {{ record.costTime ?? 0 }} ms
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleView(toLog(record))">详情</a-button>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal v-model:open="detailOpen" title="操作日志详情" width="780px" :footer="null">
    <a-descriptions v-if="currentLog" :column="2" bordered size="small">
      <a-descriptions-item label="操作">{{ currentLog.operation || '--' }}</a-descriptions-item>
      <a-descriptions-item label="模块">{{ currentLog.module || '--' }}</a-descriptions-item>
      <a-descriptions-item label="管理员">{{ currentLog.username || '--' }}</a-descriptions-item>
      <a-descriptions-item label="管理员ID">{{ currentLog.adminId || '--' }}</a-descriptions-item>
      <a-descriptions-item label="请求方法">{{ currentLog.requestMethod }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="statusColor(currentLog.status)">
          {{ currentLog.statusDesc || (currentLog.status === 1 ? '成功' : '失败') }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="请求地址" :span="2">{{ currentLog.requestUri }}</a-descriptions-item>
      <a-descriptions-item label="请求IP">{{ currentLog.ip || '--' }}</a-descriptions-item>
      <a-descriptions-item label="耗时">{{ currentLog.costTime ?? 0 }} ms</a-descriptions-item>
      <a-descriptions-item label="时间" :span="2">{{ currentLog.createTime || '--' }}</a-descriptions-item>
      <a-descriptions-item label="请求参数" :span="2">
        <pre class="log-pre">{{ currentLog.requestParams || '--' }}</pre>
      </a-descriptions-item>
      <a-descriptions-item label="失败原因" :span="2">
        {{ currentLog.errorMessage || '--' }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<style scoped>
.log-title {
  color: #1f1f1f;
  font-weight: 600;
}

.log-muted {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.log-uri {
  word-break: break-all;
}

.log-pre {
  max-height: 220px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
