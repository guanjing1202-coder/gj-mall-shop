<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import {
  createCategory,
  deleteCategory,
  getCategoryTree,
  updateCategory,
  type ApiId,
  type CategoryPayload,
  type CategoryTreeItem,
} from '@/api/product'

interface CategoryOption {
  title: string
  value: ApiId
  key: ApiId
  children?: CategoryOption[]
}

interface CategoryFormState {
  id?: ApiId
  parentId: ApiId
  name: string
  icon: string
  sort: number
  showStatus: number
}

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const actionId = ref<ApiId>()
const modalTitle = ref('新增分类')
const formRef = ref<FormInstance>()
const categoryTree = ref<CategoryTreeItem[]>([])

const categoryForm = reactive<CategoryFormState>(createEmptyCategoryForm())
const formRules: any = {
  parentId: [{ required: true, message: '请选择父分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入分类名', trigger: 'blur' }],
}

const columns: TableColumnsType<CategoryTreeItem> = [
  { title: '分类名', dataIndex: 'name', key: 'name', width: 260 },
  { title: '层级', dataIndex: 'level', key: 'level', width: 100 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 240 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 100 },
  { title: '状态', dataIndex: 'showStatus', key: 'showStatus', width: 100 },
  { title: '操作', key: 'action', width: 220 },
]

const categoryOptions = computed<CategoryOption[]>(() => {
  const options: CategoryOption[] = [{ title: '顶级分类', value: 0, key: 0 }]
  const toOption = (nodes: CategoryTreeItem[]): CategoryOption[] =>
    nodes.map((node) => ({
      title: node.name,
      value: node.id,
      key: node.id,
      children: node.children?.length ? toOption(node.children) : undefined,
    }))
  return options.concat(toOption(categoryTree.value))
})

onMounted(fetchTree)

async function fetchTree() {
  loading.value = true
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data
  } finally {
    loading.value = false
  }
}

function createEmptyCategoryForm(): CategoryFormState {
  return {
    id: undefined,
    parentId: 0,
    name: '',
    icon: '',
    sort: 0,
    showStatus: 1,
  }
}

function resetForm() {
  Object.assign(categoryForm, createEmptyCategoryForm())
  formRef.value?.clearValidate()
}

function handleCreateRoot() {
  modalMode.value = 'create'
  modalTitle.value = '新增顶级分类'
  resetForm()
  categoryForm.parentId = 0
  modalOpen.value = true
}

function handleCreateChild(record: CategoryTreeItem) {
  modalMode.value = 'create'
  modalTitle.value = `新增子分类 - ${record.name}`
  resetForm()
  categoryForm.parentId = record.id
  modalOpen.value = true
}

function handleEdit(record: CategoryTreeItem) {
  modalMode.value = 'edit'
  modalTitle.value = `编辑分类 - ${record.name}`
  Object.assign(categoryForm, {
    id: record.id,
    parentId: record.parentId ?? 0,
    name: record.name || '',
    icon: record.icon || '',
    sort: record.sort ?? 0,
    showStatus: record.showStatus ?? 1,
  })
  formRef.value?.clearValidate()
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    const payload: CategoryPayload = {
      id: categoryForm.id,
      parentId: categoryForm.parentId,
      name: categoryForm.name.trim(),
      icon: normalizeOptional(categoryForm.icon),
      sort: categoryForm.sort,
      showStatus: categoryForm.showStatus,
    }
    saving.value = true
    try {
      if (modalMode.value === 'create') {
        await createCategory(payload)
        message.success('分类创建成功')
      } else {
        await updateCategory(payload)
        message.success('分类更新成功')
      }
      modalOpen.value = false
      await fetchTree()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

function handleDelete(record: CategoryTreeItem) {
  Modal.confirm({
    title: '确认删除这个分类吗？',
    content: '如果还有子分类，后端会拦截删除。',
    async onOk() {
      actionId.value = record.id
      try {
        await deleteCategory(record.id)
        message.success('分类删除成功')
        await fetchTree()
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

function toCategory(record: Record<string, any>) {
  return record as CategoryTreeItem
}
</script>

<template>
  <a-card title="分类管理" :bordered="false">
    <div style="margin-bottom: 16px">
      <a-button type="primary" ghost @click="handleCreateRoot">新增顶级分类</a-button>
    </div>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="categoryTree"
      :loading="loading"
      :pagination="false"
      :default-expand-all-rows="true"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div style="font-weight: 600">{{ record.name }}</div>
        </template>
        <template v-else-if="column.key === 'level'">
          {{ record.level || '--' }}
        </template>
        <template v-else-if="column.key === 'icon'">
          {{ record.icon || '--' }}
        </template>
        <template v-else-if="column.key === 'showStatus'">
          <a-tag :color="record.showStatus === 1 ? 'success' : 'default'">
            {{ record.showStatus === 1 ? '显示' : '隐藏' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleCreateChild(toCategory(record))">新增子类</a-button>
            <a-button type="link" size="small" @click="handleEdit(toCategory(record))">编辑</a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toCategory(record))"
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
    :title="modalTitle"
    :confirm-loading="saving"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="categoryForm" layout="vertical" :rules="formRules">
      <a-form-item label="父分类" name="parentId">
        <a-tree-select
          v-model:value="categoryForm.parentId"
          tree-default-expand-all
          :tree-data="categoryOptions"
          placeholder="请选择父分类"
        />
      </a-form-item>
      <a-form-item label="分类名" name="name">
        <a-input v-model:value="categoryForm.name" placeholder="请输入分类名" />
      </a-form-item>
      <a-form-item label="图标">
        <a-input v-model:value="categoryForm.icon" placeholder="可选，填写图标名或 URL" />
      </a-form-item>
      <a-form-item label="排序">
        <a-input-number v-model:value="categoryForm.sort" :min="0" style="width: 100%" />
      </a-form-item>
      <a-form-item label="显示状态">
        <a-radio-group v-model:value="categoryForm.showStatus">
          <a-radio :value="1">显示</a-radio>
          <a-radio :value="0">隐藏</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </a-modal>
</template>
