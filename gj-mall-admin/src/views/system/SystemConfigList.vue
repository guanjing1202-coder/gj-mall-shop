<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  FolderOpenOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import {
  cleanupUploadStorage,
  createSystemConfig,
  deleteSystemConfig,
  getSystemConfigPage,
  getUploadStorageSummary,
  updateSystemConfig,
  updateSystemConfigStatus,
  type ApiId,
  type SystemConfigPayload,
  type SystemConfigRecord,
  type UploadCleanupResult,
  type UploadStorageSummary,
} from '@/api/systemConfig'

const loading = ref(false)
const modalOpen = ref(false)
const modalLoading = ref(false)
const formRef = ref<FormInstance>()
const editing = ref(false)
const configs = ref<SystemConfigRecord[]>([])
const storageLoading = ref(false)
const cleanupLoading = ref(false)
const storageSummary = ref<UploadStorageSummary>()
const cleanupPreview = ref<UploadCleanupResult>()
const cleanupPreviewOpen = ref(false)
const retainDays = ref(7)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
})

const filters = reactive({
  keyword: '',
  groupCode: '',
  valueType: undefined as string | undefined,
  status: undefined as number | undefined,
})

const formState = reactive<SystemConfigPayload>({
  configKey: '',
  configName: '',
  configValue: '',
  valueType: 'text',
  groupCode: 'basic',
  description: '',
  editable: 1,
  status: 1,
})

const groupOptions = [
  { label: '全部', value: '' },
  { label: '基础', value: 'basic' },
  { label: '商品', value: 'product' },
  { label: '订单', value: 'order' },
  { label: '营销', value: 'marketing' },
  { label: '售后', value: 'after_sale' },
]

const valueTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: 'JSON', value: 'json' },
]

const columns: TableColumnsType<SystemConfigRecord> = [
  { title: '配置项', dataIndex: 'configName', key: 'config', width: 320 },
  { title: '分组', dataIndex: 'groupCode', key: 'group', width: 110 },
  { title: '配置值', dataIndex: 'configValue', key: 'value', width: 280 },
  { title: '类型', dataIndex: 'valueType', key: 'type', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 130 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 160 },
]

const enabledCount = computed(() => configs.value.filter((item) => item.status === 1).length)
const groupCount = computed(() => new Set(configs.value.map((item) => item.groupCode)).size)
const editableCount = computed(() => configs.value.filter((item) => item.editable === 1).length)
const totalCount = computed(() => pagination.total)
const editableGroupOptions = computed(() => groupOptions.filter((option) => option.value))
const cleanupCandidateCount = computed(() => Number(storageSummary.value?.cleanupCandidates || 0))
const totalUploadFiles = computed(() => Number(storageSummary.value?.totalFiles || 0))
const referencedUploadFiles = computed(() => Number(storageSummary.value?.referencedFiles || 0))
const orphanUploadFiles = computed(() => Number(storageSummary.value?.orphanFiles || 0))

onMounted(() => {
  fetchConfigs()
  fetchUploadStorage()
})

async function fetchConfigs() {
  loading.value = true
  try {
    const res = await getSystemConfigPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword.trim() || undefined,
      groupCode: filters.groupCode || undefined,
      valueType: filters.valueType,
      status: filters.status,
    })
    configs.value = res.data.list || []
    pagination.total = Number(res.data.total || 0)
  } catch {
    configs.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchConfigs()
}

async function handleReset() {
  filters.keyword = ''
  filters.groupCode = ''
  filters.valueType = undefined
  filters.status = undefined
  pagination.current = 1
  await fetchConfigs()
}

async function handleGroupChange() {
  pagination.current = 1
  await fetchConfigs()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchConfigs()
}

function openCreate() {
  editing.value = false
  resetForm()
  modalOpen.value = true
}

function openEdit(record: SystemConfigRecord) {
  editing.value = true
  Object.assign(formState, {
    id: record.id,
    configKey: record.configKey,
    configName: record.configName,
    configValue: record.configValue || '',
    valueType: record.valueType || 'text',
    groupCode: record.groupCode || 'basic',
    description: record.description || '',
    editable: record.editable ?? 1,
    status: record.status ?? 1,
  })
  modalOpen.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  modalLoading.value = true
  try {
    if (editing.value) {
      await updateSystemConfig({ ...formState })
      message.success('配置已更新')
    } else {
      await createSystemConfig({ ...formState })
      message.success('配置已新增')
    }
    modalOpen.value = false
    await fetchConfigs()
  } finally {
    modalLoading.value = false
  }
}

async function handleStatusChange(record: SystemConfigRecord, checked: boolean) {
  try {
    await updateSystemConfigStatus(record.id, checked ? 1 : 0)
    record.status = checked ? 1 : 0
    message.success(checked ? '已启用' : '已停用')
  } catch {
    await fetchConfigs()
  }
}

function handleDelete(record: SystemConfigRecord) {
  Modal.confirm({
    title: '删除配置',
    content: `确认删除「${record.configName}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteSystemConfig(record.id)
      message.success('配置已删除')
      await fetchConfigs()
    },
  })
}

function resetForm() {
  Object.assign(formState, {
    id: undefined,
    configKey: '',
    configName: '',
    configValue: '',
    valueType: 'text',
    groupCode: 'basic',
    description: '',
    editable: 1,
    status: 1,
  })
  formRef.value?.clearValidate()
}

function valueClass(valueType?: string) {
  return `value-pill value-${valueType || 'text'}`
}

function displayValue(record: SystemConfigRecord) {
  const value = String(record.configValue ?? '').trim()
  return value || '--'
}

function handleSwitchChange(record: SystemConfigRecord, checked: unknown) {
  handleStatusChange(record, Boolean(checked))
}

function toRecord(record: Record<string, any>) {
  return record as SystemConfigRecord
}

async function fetchUploadStorage() {
  storageLoading.value = true
  try {
    const res = await getUploadStorageSummary(retainDays.value)
    storageSummary.value = res.data
  } finally {
    storageLoading.value = false
  }
}

async function previewCleanup() {
  cleanupLoading.value = true
  try {
    const res = await cleanupUploadStorage(retainDays.value, true)
    cleanupPreview.value = res.data
    cleanupPreviewOpen.value = true
  } finally {
    cleanupLoading.value = false
  }
}

function confirmCleanup() {
  const candidates = Number(cleanupPreview.value?.candidateFiles || storageSummary.value?.cleanupCandidates || 0)
  Modal.confirm({
    title: '清理未引用上传文件',
    content: `将删除 ${candidates} 个超过 ${retainDays.value} 天且未被数据库引用的上传文件。此操作不可恢复，确认继续吗？`,
    okText: '确认清理',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      cleanupLoading.value = true
      try {
        const res = await cleanupUploadStorage(retainDays.value, false)
        message.success(`已清理 ${res.data.deletedFiles || 0} 个文件，释放 ${formatSize(res.data.deletedSize || 0)}`)
        cleanupPreviewOpen.value = false
        await fetchUploadStorage()
      } finally {
        cleanupLoading.value = false
      }
    },
  })
}

function formatSize(value?: number) {
  const bytes = Number(value || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}
</script>

<template>
  <div class="config-page">
    <section class="config-heading">
      <div>
        <div class="page-kicker">System Settings</div>
        <h1>系统配置</h1>
      </div>
      <a-button type="primary" size="large" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新增配置
      </a-button>
    </section>

    <section class="metric-grid">
      <div class="metric-item accent-teal">
        <span>配置总数</span>
        <strong>{{ totalCount }}</strong>
      </div>
      <div class="metric-item accent-amber">
        <span>当前页启用</span>
        <strong>{{ enabledCount }}</strong>
      </div>
      <div class="metric-item accent-blue">
        <span>当前页分组</span>
        <strong>{{ groupCount }}</strong>
      </div>
      <div class="metric-item accent-rose">
        <span>可编辑项</span>
        <strong>{{ editableCount }}</strong>
      </div>
    </section>

    <section class="storage-panel">
      <div class="storage-head">
        <div>
          <div class="page-kicker">Upload Storage</div>
          <h2>上传文件体检</h2>
          <p>{{ storageSummary?.rootPath || '本地上传目录' }}</p>
        </div>
        <a-space>
          <a-input-number v-model:value="retainDays" :min="0" :max="3650" addon-after="天" />
          <a-button :loading="storageLoading" @click="fetchUploadStorage">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button :loading="cleanupLoading" @click="previewCleanup">
            <template #icon><FolderOpenOutlined /></template>
            预览清理
          </a-button>
        </a-space>
      </div>
      <div class="storage-metrics" :class="{ loading: storageLoading }">
        <div>
          <span>上传文件</span>
          <strong>{{ totalUploadFiles }}</strong>
          <small>{{ formatSize(storageSummary?.totalSize) }}</small>
        </div>
        <div>
          <span>数据库引用</span>
          <strong>{{ referencedUploadFiles }}</strong>
          <small>{{ storageSummary?.referenceCount || 0 }} 条 URL</small>
        </div>
        <div>
          <span>未引用文件</span>
          <strong>{{ orphanUploadFiles }}</strong>
          <small>{{ formatSize(storageSummary?.orphanSize) }}</small>
        </div>
        <div class="danger">
          <span>可清理候选</span>
          <strong>{{ cleanupCandidateCount }}</strong>
          <small>{{ formatSize(storageSummary?.cleanupCandidateSize) }}</small>
        </div>
      </div>
      <a-alert
        v-if="cleanupCandidateCount > 0"
        type="warning"
        show-icon
        class="storage-alert"
        :message="`发现 ${cleanupCandidateCount} 个超过 ${retainDays} 天且未被引用的上传文件`"
        description="建议先预览清理清单，确认没有人工保留需求后再执行删除。"
      />
    </section>

    <section class="config-panel">
      <div class="group-strip">
        <a-segmented v-model:value="filters.groupCode" :options="groupOptions" @change="handleGroupChange" />
      </div>

      <div class="filter-row">
        <a-input
          v-model:value="filters.keyword"
          allow-clear
          class="search-input"
          placeholder="搜索配置键 / 名称 / 说明"
          @pressEnter="handleSearch"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select v-model:value="filters.valueType" allow-clear placeholder="类型" class="compact-select">
          <a-select-option v-for="item in valueTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="filters.status" allow-clear placeholder="状态" class="compact-select">
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">停用</a-select-option>
        </a-select>
        <a-space class="toolbar-actions">
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="configs"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1280 }"
        class="config-table"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'config'">
            <div class="config-name">{{ record.configName }}</div>
            <div class="config-key">{{ record.configKey }}</div>
            <div v-if="record.description" class="config-desc">{{ record.description }}</div>
          </template>
          <template v-else-if="column.key === 'group'">
            <a-tag class="soft-tag">{{ record.groupName || record.groupCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'value'">
            <code :class="valueClass(record.valueType)">{{ displayValue(toRecord(record)) }}</code>
          </template>
          <template v-else-if="column.key === 'type'">
            <span class="type-text">{{ record.valueTypeDesc || record.valueType }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :disabled="record.editable === 0"
              checked-children="启"
              un-checked-children="停"
              @change="handleSwitchChange(toRecord(record), $event)"
            />
          </template>
          <template v-else-if="column.key === 'updateTime'">
            <span class="muted-text">{{ record.updateTime || '--' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" :disabled="record.editable === 0" @click="openEdit(toRecord(record))">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button type="link" size="small" danger :disabled="record.editable === 0" @click="handleDelete(toRecord(record))">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="modalOpen"
      :title="editing ? '编辑系统配置' : '新增系统配置'"
      width="760px"
    >
      <a-form ref="formRef" :model="formState" layout="vertical" class="config-form">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item name="configName" label="配置名称" :rules="[{ required: true, message: '请输入配置名称' }]">
              <a-input v-model:value="formState.configName" placeholder="例如：商城名称" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="configKey" label="配置键" :rules="[{ required: true, message: '请输入配置键' }]">
              <a-input v-model:value="formState.configKey" placeholder="例如：mall.site.name" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="groupCode" label="分组">
              <a-select v-model:value="formState.groupCode">
                <a-select-option v-for="item in editableGroupOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="valueType" label="类型">
              <a-select v-model:value="formState.valueType">
                <a-select-option v-for="item in valueTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="configValue" label="配置值">
              <a-textarea v-model:value="formState.configValue" :rows="4" placeholder="按所选类型填写配置值" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="description" label="说明">
              <a-textarea v-model:value="formState.description" :rows="3" placeholder="可选" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="status" label="状态">
              <a-radio-group v-model:value="formState.status" button-style="solid">
                <a-radio-button :value="1">启用</a-radio-button>
                <a-radio-button :value="0">停用</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item name="editable" label="编辑权限">
              <a-radio-group v-model:value="formState.editable" button-style="solid">
                <a-radio-button :value="1">允许编辑</a-radio-button>
                <a-radio-button :value="0">锁定</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-button @click="modalOpen = false">取消</a-button>
        <a-button type="primary" :loading="modalLoading" @click="handleSubmit">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
      </template>
    </a-modal>

    <a-modal
      v-model:open="cleanupPreviewOpen"
      title="未引用文件清理预览"
      width="860px"
    >
      <a-alert
        type="info"
        show-icon
        class="storage-alert"
        :message="`候选 ${cleanupPreview?.candidateFiles || 0} 个文件，可释放 ${formatSize(cleanupPreview?.candidateSize)}`"
        description="预览模式不会删除文件；执行清理时只处理超过保留天数且数据库未引用的本地上传文件。"
      />
      <a-list
        class="orphan-list"
        :data-source="cleanupPreview?.files || []"
        :pagination="(cleanupPreview?.files?.length || 0) > 8 ? { pageSize: 8 } : false"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <WarningOutlined class="orphan-icon" />
              </template>
              <template #title>
                <code>{{ item.relativePath }}</code>
              </template>
              <template #description>
                {{ item.lastModified || '--' }} · {{ formatSize(item.size) }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
      <template #footer>
        <a-button @click="cleanupPreviewOpen = false">关闭</a-button>
        <a-button danger type="primary" :disabled="!cleanupPreview?.candidateFiles" :loading="cleanupLoading" @click="confirmCleanup">
          确认清理
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<style scoped>
.config-page {
  min-height: calc(100vh - 104px);
  padding: 4px;
  color: #1f2933;
}

.config-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 22px 24px;
  border: 1px solid #e5ebf3;
  border-left: 4px solid #0f766e;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(20, 31, 43, 0.06);
}

.page-kicker {
  margin-bottom: 4px;
  color: #5f6f7f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.config-heading h1 {
  margin: 0;
  color: #17212b;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-item {
  min-height: 88px;
  padding: 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(20, 31, 43, 0.04);
}

.metric-item span {
  display: block;
  color: #657384;
  font-size: 13px;
}

.metric-item strong {
  display: block;
  margin-top: 10px;
  color: #17212b;
  font-size: 26px;
  line-height: 1;
}

.accent-teal {
  border-top: 3px solid #0f766e;
}

.accent-amber {
  border-top: 3px solid #b7791f;
}

.accent-blue {
  border-top: 3px solid #2563eb;
}

.accent-rose {
  border-top: 3px solid #be365d;
}

.storage-panel {
  margin-bottom: 16px;
  padding: 18px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(20, 31, 43, 0.06);
}

.storage-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.storage-head h2 {
  margin: 0;
  color: #17212b;
  font-size: 20px;
}

.storage-head p {
  margin: 6px 0 0;
  color: #657384;
  font-size: 13px;
}

.storage-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.storage-metrics > div {
  padding: 14px;
  border: 1px solid #edf1f6;
  border-radius: 8px;
  background: #f8fafc;
}

.storage-metrics span,
.storage-metrics small {
  display: block;
  color: #657384;
  font-size: 12px;
}

.storage-metrics strong {
  display: block;
  margin: 8px 0 4px;
  color: #17212b;
  font-size: 24px;
  line-height: 1;
}

.storage-metrics .danger {
  border-color: #f7d9df;
  background: #fff7f8;
}

.storage-metrics .danger strong {
  color: #be365d;
}

.storage-alert {
  margin-top: 14px;
}

.orphan-list {
  margin-top: 14px;
}

.orphan-icon {
  color: #be365d;
  font-size: 18px;
}

.config-panel {
  padding: 18px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(20, 31, 43, 0.06);
}

.group-strip {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  overflow-x: auto;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.search-input {
  width: min(360px, 100%);
}

.compact-select {
  width: 128px;
}

.toolbar-actions {
  margin-left: auto;
}

.config-table :deep(.ant-table-thead > tr > th) {
  background: #f7f9fc;
  color: #526171;
  font-weight: 700;
}

.config-name {
  color: #17212b;
  font-weight: 700;
}

.config-key {
  margin-top: 4px;
  color: #0f766e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.config-desc {
  margin-top: 6px;
  color: #7b8794;
  font-size: 12px;
}

.soft-tag {
  border: 0;
  border-radius: 999px;
  background: #edf7f4;
  color: #0f766e;
  font-weight: 600;
}

.value-pill {
  display: inline-block;
  max-width: 240px;
  padding: 5px 8px;
  overflow: hidden;
  border-radius: 6px;
  background: #f4f6f8;
  color: #2d3748;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.value-number {
  background: #fff7e6;
  color: #8a5a12;
}

.value-boolean {
  background: #eaf7f1;
  color: #0f766e;
}

.value-json {
  background: #f0f4ff;
  color: #2850a7;
}

.type-text,
.muted-text {
  color: #657384;
}

.config-form :deep(.ant-form-item-label > label) {
  color: #526171;
  font-weight: 600;
}

@media (max-width: 900px) {
  .config-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .storage-head {
    flex-direction: column;
  }

  .storage-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar-actions {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 560px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .storage-metrics {
    grid-template-columns: 1fr;
  }

  .compact-select,
  .search-input {
    width: 100%;
  }
}
</style>
