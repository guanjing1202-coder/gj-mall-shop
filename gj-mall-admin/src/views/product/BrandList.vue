<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { uploadImage } from '@/api/file'
import {
  createBrand,
  deleteBrand,
  getBrandPage,
  updateBrand,
  type ApiId,
  type BrandItem,
  type BrandPayload,
} from '@/api/product'

interface BrandFormState {
  id?: ApiId
  name: string
  logo: string
  description: string
  sort: number
  showStatus: number
}

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const actionId = ref<ApiId>()
const formRef = ref<FormInstance>()
const logoUploading = ref(false)

const keyword = ref('')
const brandList = ref<BrandItem[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const brandForm = reactive<BrandFormState>(createEmptyBrandForm())
const formRules: any = {
  name: [{ required: true, message: '请输入品牌名', trigger: 'blur' }],
}

const columns: TableColumnsType<BrandItem> = [
  { title: '品牌名', dataIndex: 'name', key: 'name', width: 220 },
  { title: 'Logo', dataIndex: 'logo', key: 'logo', width: 240 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 100 },
  { title: '状态', dataIndex: 'showStatus', key: 'showStatus', width: 100 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

onMounted(fetchList)

async function fetchList() {
  loading.value = true
  try {
    const res = await getBrandPage({
      keyword: keyword.value.trim() || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    brandList.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchList()
}

async function handleReset() {
  keyword.value = ''
  pagination.current = 1
  await fetchList()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchList()
}

function createEmptyBrandForm(): BrandFormState {
  return {
    id: undefined,
    name: '',
    logo: '',
    description: '',
    sort: 0,
    showStatus: 1,
  }
}

function resetForm() {
  Object.assign(brandForm, createEmptyBrandForm())
  formRef.value?.clearValidate()
}

function handleCreate() {
  modalMode.value = 'create'
  resetForm()
  modalOpen.value = true
}

function handleEdit(record: BrandItem) {
  modalMode.value = 'edit'
  Object.assign(brandForm, {
    id: record.id,
    name: record.name || '',
    logo: record.logo || '',
    description: record.description || '',
    sort: record.sort ?? 0,
    showStatus: record.showStatus ?? 1,
  })
  formRef.value?.clearValidate()
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    const payload: BrandPayload = {
      id: brandForm.id,
      name: brandForm.name.trim(),
      logo: normalizeOptional(brandForm.logo),
      description: normalizeOptional(brandForm.description),
      sort: brandForm.sort,
      showStatus: brandForm.showStatus,
    }
    saving.value = true
    try {
      if (modalMode.value === 'create') {
        await createBrand(payload)
        message.success('品牌创建成功')
      } else {
        await updateBrand(payload)
        message.success('品牌更新成功')
      }
      modalOpen.value = false
      pagination.current = 1
      await fetchList()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

function handleDelete(record: BrandItem) {
  Modal.confirm({
    title: '确认删除这个品牌吗？',
    async onOk() {
      actionId.value = record.id
      try {
        await deleteBrand(record.id)
        message.success('品牌删除成功')
        if (brandList.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchList()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function normalizeOptional(value: string) {
  const text = value.trim()
  return text ? text : undefined
}

function toBrand(record: Record<string, any>) {
  return record as BrandItem
}

async function uploadLogo(options: any) {
  logoUploading.value = true
  try {
    const res = await uploadImage(options.file as File, 'brand-logo')
    const url = res.data?.url
    if (!url) {
      throw new Error('上传结果缺少图片地址')
    }
    brandForm.logo = url
    message.success('Logo 已上传')
    options.onSuccess?.(res.data)
  } catch (error) {
    message.error('Logo 上传失败')
    options.onError?.(error)
  } finally {
    logoUploading.value = false
  }
}
</script>

<template>
  <a-card title="品牌管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="keyword"
          allow-clear
          placeholder="搜索品牌名"
          style="width: 220px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="handleCreate">新增品牌</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="brandList"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1000 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div style="font-weight: 600">{{ record.name }}</div>
        </template>
        <template v-else-if="column.key === 'logo'">
          <div v-if="record.logo" class="logo-cell">
            <img :src="record.logo" alt="品牌 Logo" />
            <a :href="record.logo" target="_blank">查看</a>
          </div>
          <span v-else>--</span>
        </template>
        <template v-else-if="column.key === 'description'">
          {{ record.description || '--' }}
        </template>
        <template v-else-if="column.key === 'showStatus'">
          <a-tag :color="record.showStatus === 1 ? 'success' : 'default'">
            {{ record.showStatus === 1 ? '显示' : '隐藏' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(toBrand(record))">编辑</a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toBrand(record))"
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
    :title="modalMode === 'create' ? '新增品牌' : '编辑品牌'"
    :confirm-loading="saving"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="brandForm" layout="vertical" :rules="formRules">
      <a-form-item label="品牌名" name="name">
        <a-input v-model:value="brandForm.name" placeholder="请输入品牌名" />
      </a-form-item>
      <a-form-item label="Logo">
        <div class="logo-editor">
          <img v-if="brandForm.logo" :src="brandForm.logo" alt="品牌 Logo 预览" />
          <div v-else class="logo-empty">Logo</div>
          <div class="logo-editor-main">
            <a-input v-model:value="brandForm.logo" placeholder="https://example.com/logo.png" />
            <a-upload
              accept="image/png,image/jpeg,image/webp,image/gif"
              :show-upload-list="false"
              :custom-request="uploadLogo"
            >
              <a-button :loading="logoUploading">上传 Logo</a-button>
            </a-upload>
          </div>
        </div>
      </a-form-item>
      <a-form-item label="品牌描述">
        <a-textarea v-model:value="brandForm.description" :rows="4" placeholder="请输入品牌描述" />
      </a-form-item>
      <a-form-item label="排序">
        <a-input-number v-model:value="brandForm.sort" :min="0" style="width: 100%" />
      </a-form-item>
      <a-form-item label="显示状态">
        <a-radio-group v-model:value="brandForm.showStatus">
          <a-radio :value="1">显示</a-radio>
          <a-radio :value="0">隐藏</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.logo-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-cell img,
.logo-editor img,
.logo-empty {
  width: 56px;
  height: 56px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.logo-cell img,
.logo-editor img {
  object-fit: cover;
}

.logo-editor {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.logo-empty {
  display: grid;
  place-items: center;
  background: #fafafa;
  color: #8c8c8c;
  font-size: 12px;
}

.logo-editor-main {
  display: grid;
  gap: 8px;
  min-width: 0;
}
</style>
