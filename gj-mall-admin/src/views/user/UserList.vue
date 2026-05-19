<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { uploadImage } from '@/api/file'
import {
  createAdminUser,
  deleteAdminUser,
  getAdminRoles,
  getAdminUserPage,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserStatus,
  type ApiId,
  type AdminRole,
  type AdminUserPayload,
  type AdminUserRecord,
} from '@/api/adminUser'

interface UserFormState {
  id?: ApiId
  username: string
  password: string
  nickname: string
  avatar: string
  email: string
  phone: string
  status: number
  roleIds: ApiId[]
}

const loading = ref(false)
const saving = ref(false)
const actionId = ref<ApiId>()
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const passwordOpen = ref(false)
const passwordSaving = ref(false)
const passwordUser = ref<AdminUserRecord>()
const newPassword = ref('123456')
const formRef = ref<FormInstance>()
const avatarUploading = ref(false)

const keyword = ref('')
const status = ref<number>()
const users = ref<AdminUserRecord[]>([])
const roles = ref<AdminRole[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const userForm = reactive<UserFormState>(createEmptyUserForm())
const formRules: any = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ min: 6, max: 32, message: '密码长度需在 6 到 32 位之间', trigger: 'blur' }],
}

const columns: TableColumnsType<AdminUserRecord> = [
  { title: '账号', dataIndex: 'username', key: 'username', width: 220 },
  { title: '联系方式', dataIndex: 'phone', key: 'contact', width: 220 },
  { title: '角色', dataIndex: 'roleNames', key: 'roleNames' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 260 },
]

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchUsers()])
})

async function fetchRoles() {
  const res = await getAdminRoles()
  roles.value = res.data
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getAdminUserPage({
      keyword: keyword.value.trim() || undefined,
      status: status.value,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    users.value = res.data.list
    pagination.total = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await fetchUsers()
}

async function handleReset() {
  keyword.value = ''
  status.value = undefined
  pagination.current = 1
  await fetchUsers()
}

async function handleTableChange(page: { current?: number; pageSize?: number }) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  await fetchUsers()
}

function createEmptyUserForm(): UserFormState {
  return {
    id: undefined,
    username: '',
    password: '',
    nickname: '',
    avatar: '',
    email: '',
    phone: '',
    status: 1,
    roleIds: [],
  }
}

function resetForm() {
  Object.assign(userForm, createEmptyUserForm())
  formRef.value?.clearValidate()
}

function handleCreate() {
  modalMode.value = 'create'
  resetForm()
  userForm.password = '123456'
  modalOpen.value = true
}

function handleEdit(record: AdminUserRecord) {
  modalMode.value = 'edit'
  Object.assign(userForm, {
    id: record.id,
    username: record.username || '',
    password: '',
    nickname: record.nickname || '',
    avatar: record.avatar || '',
    email: record.email || '',
    phone: record.phone || '',
    status: record.status ?? 1,
    roleIds: record.roleIds || [],
  })
  formRef.value?.clearValidate()
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    if (modalMode.value === 'create' && !userForm.password.trim()) {
      message.error('新增用户需要设置密码')
      return
    }
    const payload: AdminUserPayload = {
      id: userForm.id,
      username: userForm.username.trim(),
      password: normalizeOptional(userForm.password),
      nickname: normalizeOptional(userForm.nickname),
      avatar: normalizeOptional(userForm.avatar),
      email: normalizeOptional(userForm.email),
      phone: normalizeOptional(userForm.phone),
      status: userForm.status,
      roleIds: userForm.roleIds,
    }
    saving.value = true
    try {
      if (modalMode.value === 'create') {
        await createAdminUser(payload)
        message.success('后台用户创建成功')
      } else {
        await updateAdminUser(payload)
        message.success('后台用户更新成功')
      }
      modalOpen.value = false
      pagination.current = 1
      await fetchUsers()
    } finally {
      saving.value = false
    }
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

function handleStatus(record: AdminUserRecord) {
  const nextStatus = record.status === 1 ? 0 : 1
  const text = nextStatus === 1 ? '启用' : '禁用'
  Modal.confirm({
    title: `确认${text}该后台用户吗？`,
    content: `账号：${record.username}`,
    async onOk() {
      actionId.value = record.id
      try {
        await updateAdminUserStatus(record.id, nextStatus)
        message.success(`${text}成功`)
        await fetchUsers()
      } finally {
        actionId.value = undefined
      }
    },
  })
}

function openPasswordModal(record: AdminUserRecord) {
  passwordUser.value = record
  newPassword.value = '123456'
  passwordOpen.value = true
}

async function submitPassword() {
  if (!passwordUser.value) {
    return
  }
  const password = newPassword.value.trim()
  if (password.length < 6 || password.length > 32) {
    message.error('密码长度需在 6 到 32 位之间')
    return
  }
  passwordSaving.value = true
  try {
    await resetAdminUserPassword(passwordUser.value.id, password)
    message.success('密码已重置')
    passwordOpen.value = false
  } finally {
    passwordSaving.value = false
  }
}

function handleDelete(record: AdminUserRecord) {
  Modal.confirm({
    title: '确认删除这个后台用户吗？',
    content: `账号：${record.username}`,
    okButtonProps: { danger: true },
    async onOk() {
      actionId.value = record.id
      try {
        await deleteAdminUser(record.id)
        message.success('后台用户删除成功')
        if (users.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        await fetchUsers()
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

function roleText(record: AdminUserRecord) {
  return record.roleNames?.length ? record.roleNames.join(' / ') : '--'
}

function toAdminUser(record: Record<string, any>) {
  return record as AdminUserRecord
}

async function uploadAvatar(options: any) {
  avatarUploading.value = true
  try {
    const res = await uploadImage(options.file as File, 'admin-avatar')
    const url = res.data?.url
    if (!url) {
      throw new Error('上传结果缺少图片地址')
    }
    userForm.avatar = url
    message.success('头像已上传')
    options.onSuccess?.(res.data)
  } catch (error) {
    message.error('头像上传失败')
    options.onError?.(error)
  } finally {
    avatarUploading.value = false
  }
}
</script>

<template>
  <a-card title="后台用户管理" :bordered="false">
    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 12px">
      <a-form-item>
        <a-input
          v-model:value="keyword"
          allow-clear
          placeholder="搜索用户名/昵称/手机号"
          style="width: 260px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-select v-model:value="status" allow-clear placeholder="状态" style="width: 140px">
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" ghost @click="handleCreate">新增用户</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="users"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1150 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <div style="font-weight: 600">{{ record.username }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.nickname || '暂无昵称' }}
          </div>
        </template>
        <template v-else-if="column.key === 'contact'">
          <div>{{ record.phone || '--' }}</div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
            {{ record.email || '--' }}
          </div>
        </template>
        <template v-else-if="column.key === 'roleNames'">
          {{ roleText(toAdminUser(record)) }}
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
            <a-button type="link" size="small" @click="handleEdit(toAdminUser(record))">编辑</a-button>
            <a-button
              type="link"
              size="small"
              :loading="actionId === record.id"
              @click="handleStatus(toAdminUser(record))"
            >
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="link" size="small" @click="openPasswordModal(toAdminUser(record))">
              重置密码
            </a-button>
            <a-button
              type="link"
              danger
              size="small"
              :loading="actionId === record.id"
              @click="handleDelete(toAdminUser(record))"
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
    :title="modalMode === 'create' ? '新增后台用户' : '编辑后台用户'"
    :confirm-loading="saving"
    width="680px"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="userForm" layout="vertical" :rules="formRules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="用户名" name="username">
            <a-input v-model:value="userForm.username" placeholder="请输入用户名" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="modalMode === 'create' ? '初始密码' : '新密码（留空不修改）'" name="password">
            <a-input-password v-model:value="userForm.password" placeholder="请输入密码" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="昵称">
            <a-input v-model:value="userForm.nickname" placeholder="请输入昵称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号">
            <a-input v-model:value="userForm.phone" placeholder="请输入手机号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱">
            <a-input v-model:value="userForm.email" placeholder="请输入邮箱" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="头像">
            <div class="avatar-field">
              <a-avatar :src="userForm.avatar" :size="56">{{ userForm.nickname?.slice(0, 1) || 'U' }}</a-avatar>
              <div class="avatar-field-main">
                <a-input v-model:value="userForm.avatar" placeholder="可选，也可上传图片" />
                <a-upload
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  :show-upload-list="false"
                  :custom-request="uploadAvatar"
                >
                  <a-button :loading="avatarUploading">上传头像</a-button>
                </a-upload>
              </div>
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态">
            <a-radio-group v-model:value="userForm.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">禁用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="角色">
            <a-select v-model:value="userForm.roleIds" mode="multiple" allow-clear placeholder="请选择角色">
              <a-select-option v-for="role in roles" :key="role.id" :value="role.id">
                {{ role.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="passwordOpen"
    title="重置密码"
    :confirm-loading="passwordSaving"
    @ok="submitPassword"
  >
    <a-form layout="vertical">
      <a-form-item label="账号">
        <a-input :value="passwordUser?.username" disabled />
      </a-form-item>
      <a-form-item label="新密码">
        <a-input-password v-model:value="newPassword" placeholder="请输入新密码" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.avatar-field {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.avatar-field-main {
  display: grid;
  gap: 8px;
  min-width: 0;
}
</style>
