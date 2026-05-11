<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import {
  createAdminRole,
  deleteAdminRole,
  getAdminPermissionTree,
  getAdminRoleDetail,
  getAdminRolePage,
  updateAdminRole,
  updateAdminRolePermissions,
  updateAdminRoleStatus,
  type AdminPermission,
  type AdminRole,
  type AdminRolePayload,
  type ApiId,
} from '@/api/adminUser'

type ModalMode = 'create' | 'edit' | 'permission'

interface RoleFormState {
  id?: ApiId
  code: string
  name: string
  description: string
  status: number
}

interface PermissionTreeNode {
  key: ApiId
  title: string
  children?: PermissionTreeNode[]
}

const loading = ref(false)
const saving = ref(false)
const actionId = ref<ApiId>()
const modalOpen = ref(false)
const modalMode = ref<ModalMode>('create')
const formRef = ref<FormInstance>()

const keyword = ref('')
const status = ref<number>()
const roles = ref<AdminRole[]>([])
const permissions = ref<AdminPermission[]>([])
const checkedPermissionKeys = ref<ApiId[]>([])
const expandedPermissionKeys = ref<ApiId[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const roleForm = reactive<RoleFormState>(createEmptyRoleForm())
const formRules: any = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

const columns: TableColumnsType<AdminRole> = [
  { title: '角色', dataIndex: 'name', key: 'name', width: 260 },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '权限数量', dataIndex: 'permissionIds', key: 'permissionCount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 280 },
]

const permissionTreeData = computed(() => permissions.value.map(toTreeNode))
const modalTitle = computed(() => {
  if (modalMode.value === 'create') {
    return '新增角色'
  }
  return modalMode.value === 'permission' ? '角色授权' : '编辑角色'
})
const roleFieldsDisabled = computed(() => modalMode.value === 'permission')

onMounted(async () => {
  await Promise.all([fetchPermissions(), fetchRoles()])
})

async function fetchPermissions() {
  try {
    const res = await getAdminPermissionTree()
    permissions.value = res.data || []
    expandedPermissionKeys.value = collectPermissionIds(permissions.value)
  } catch {
    permissions.value = []
    expandedPermissionKeys.value = []
  }
}

async function fetchRoles() {
  loading.value = true
  try {
    const res = await getAdminRolePage({
      keyword: keyword.value.trim() || undefined,
      status: status.value,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    roles.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } catch {
    roles.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchRoles()
}

async function handleReset() {
  keyword.value = ''
  status.value = undefined
  pagination.current = 1
  await fetchRoles()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchRoles()
}

function createEmptyRoleForm(): RoleFormState {
  return {
    id: undefined,
    code: '',
    name: '',
    description: '',
    status: 1,
  }
}

function resetForm() {
  Object.assign(roleForm, createEmptyRoleForm())
  checkedPermissionKeys.value = []
  formRef.value?.clearValidate()
}

function handleCreate() {
  modalMode.value = 'create'
  resetForm()
  modalOpen.value = true
}

async function handleEdit(record: AdminRole) {
  await openRoleModal('edit', record)
}

async function handlePermission(record: AdminRole) {
  await openRoleModal('permission', record)
}

async function openRoleModal(mode: ModalMode, record: AdminRole) {
  modalMode.value = mode
  resetForm()
  modalOpen.value = true
  saving.value = true
  try {
    const res = await getAdminRoleDetail(record.id)
    fillRoleForm(res.data)
  } catch {
    modalOpen.value = false
  } finally {
    saving.value = false
  }
}

function fillRoleForm(record: AdminRole) {
  Object.assign(roleForm, {
    id: record.id,
    code: record.code || '',
    name: record.name || '',
    description: record.description || '',
    status: record.status ?? 1,
  })
  checkedPermissionKeys.value = [...(record.permissionIds || [])]
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    const payload: AdminRolePayload = {
      id: roleForm.id,
      code: roleForm.code.trim(),
      name: roleForm.name.trim(),
      description: normalizeOptional(roleForm.description),
      status: roleForm.status,
      permissionIds: checkedPermissionKeys.value,
    }
    saving.value = true
    try {
      if (modalMode.value === 'create') {
        await createAdminRole(payload)
        message.success('角色创建成功')
      } else if (modalMode.value === 'permission' && roleForm.id !== undefined) {
        await updateAdminRolePermissions(roleForm.id, checkedPermissionKeys.value)
        message.success('角色权限已更新')
      } else {
        await updateAdminRole(payload)
        message.success('角色更新成功')
      }
      modalOpen.value = false
      await fetchRoles()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

function handleStatus(record: AdminRole) {
  const nextStatus = record.status === 1 ? 0 : 1
  const text = nextStatus === 1 ? '启用' : '禁用'
  Modal.confirm({
    title: `确认${text}该角色吗？`,
    content: `${record.name}（${record.code}）`,
    async onOk() {
      actionId.value = record.id
      try {
        await updateAdminRoleStatus(record.id, nextStatus)
        message.success(`${text}成功`)
        await fetchRoles()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handleDelete(record: AdminRole) {
  Modal.confirm({
    title: '确认删除这个角色吗？',
    content: `${record.name}（${record.code}）`,
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteAdminRole(record.id)
        message.success('角色删除成功')
        if (roles.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchRoles()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function handlePermissionCheck(keys: ApiId[] | { checked: ApiId[] }) {
  checkedPermissionKeys.value = Array.isArray(keys) ? keys : keys.checked
}

function handlePermissionExpand(keys: ApiId[]) {
  expandedPermissionKeys.value = keys
}

function normalizeOptional(value: string) {
  const text = value.trim()
  return text ? text : undefined
}

function toTreeNode(permission: AdminPermission): PermissionTreeNode {
  return {
    key: permission.id,
    title: `${permission.name}（${permission.code}）`,
    children: permission.children?.map(toTreeNode),
  }
}

function collectPermissionIds(list: AdminPermission[]): ApiId[] {
  return list.flatMap((item) => [item.id, ...collectPermissionIds(item.children || [])])
}

function permissionCount(record: AdminRole) {
  return record.permissionIds?.length || 0
}

function toRole(record: Record<string, any>) {
  return record as AdminRole
}
</script>

<template>
  <a-card title="角色权限管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="keyword"
          allow-clear
          placeholder="搜索角色编码/名称"
          style="width: 240px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="status" allow-clear placeholder="状态" style="width: 130px">
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="handleCreate">新增角色</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="roles"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1220 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="role-name">{{ record.name }}</div>
          <div class="role-muted">{{ record.code }}</div>
        </template>
        <template v-else-if="column.key === 'description'">
          {{ record.description || '--' }}
        </template>
        <template v-else-if="column.key === 'permissionCount'">
          {{ permissionCount(toRole(record)) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime || '--' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(toRole(record))">编辑</a-button>
            <a-button type="link" size="small" @click="handlePermission(toRole(record))">授权</a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleStatus(toRole(record))"
            >
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toRole(record))"
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
    width="760px"
    destroy-on-close
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="roleForm" layout="vertical" :rules="formRules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="角色编码" name="code">
            <a-input v-model:value="roleForm.code" :disabled="roleFieldsDisabled" placeholder="如 ROLE_OPERATOR" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="角色名称" name="name">
            <a-input v-model:value="roleForm.name" :disabled="roleFieldsDisabled" placeholder="请输入角色名称" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="角色说明">
            <a-textarea
              v-model:value="roleForm.description"
              :disabled="roleFieldsDisabled"
              :rows="3"
              :maxlength="255"
              show-count
              placeholder="请输入角色说明"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态">
            <a-radio-group v-model:value="roleForm.status" :disabled="roleFieldsDisabled">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">禁用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="权限范围">
        <div class="permission-tree-wrap">
          <a-tree
            checkable
            :tree-data="permissionTreeData"
            :checked-keys="checkedPermissionKeys"
            :expanded-keys="expandedPermissionKeys"
            @check="handlePermissionCheck"
            @expand="handlePermissionExpand"
          />
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.role-name {
  color: #1f1f1f;
  font-weight: 600;
}

.role-muted {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.permission-tree-wrap {
  max-height: 360px;
  overflow: auto;
  padding: 8px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fff;
}
</style>
