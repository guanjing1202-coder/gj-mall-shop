<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  addAddress,
  deleteAddress,
  getAddressList,
  setDefaultAddress,
  updateAddress,
  type AddressItem,
  type AddressPayload,
} from '@/api/address'
import { useAuthStore } from '@/stores/auth'
import type { ApiId } from '@/api/product'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const deletingId = ref<ApiId | ''>('')
const keyword = ref('')
const dialogOpen = ref(false)
const editingAddressId = ref<ApiId>()
const addresses = ref<AddressItem[]>([])
const addressForm = reactive<AddressPayload>({
  receiver: '',
  phone: '',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  detail: '',
  postCode: '',
  isDefault: 0,
})

const defaultAddress = computed(() => addresses.value.find((item) => Number(item.isDefault) === 1))
const normalAddressCount = computed(() =>
  addresses.value.filter((item) => Number(item.isDefault) !== 1).length,
)
const filteredAddresses = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) {
    return addresses.value
  }
  return addresses.value.filter((item) => {
    const text = `${item.receiver}${item.phone}${addressLine(item)}${item.postCode || ''}`.toLowerCase()
    return text.includes(key)
  })
})

function addressLine(address: AddressItem) {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  const parsed = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(parsed)
}

function resetForm(address?: AddressItem) {
  editingAddressId.value = address?.id
  Object.assign(addressForm, {
    receiver: address?.receiver || '',
    phone: address?.phone || '',
    province: address?.province || '广东省',
    city: address?.city || '深圳市',
    district: address?.district || '南山区',
    detail: address?.detail || '',
    postCode: address?.postCode || '',
    isDefault: address?.isDefault ?? (addresses.value.length ? 0 : 1),
  })
}

function openDialog(address?: AddressItem) {
  resetForm(address)
  dialogOpen.value = true
}

function validateForm() {
  if (!addressForm.receiver.trim()) {
    ElMessage.warning('请填写收件人')
    return false
  }
  if (!addressForm.phone.trim()) {
    ElMessage.warning('请填写手机号')
    return false
  }
  if (addressForm.phone.trim().length < 6) {
    ElMessage.warning('手机号格式不正确')
    return false
  }
  if (!addressForm.province.trim() || !addressForm.city.trim() || !addressForm.district.trim()) {
    ElMessage.warning('请完整填写省市区')
    return false
  }
  if (!addressForm.detail.trim()) {
    ElMessage.warning('请填写详细地址')
    return false
  }
  return true
}

async function loadAddresses() {
  if (!auth.isLoggedIn) {
    addresses.value = []
    auth.openLoginDialog()
    return
  }
  loading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function saveAddress() {
  if (!validateForm()) {
    return
  }
  saving.value = true
  try {
    const payload: AddressPayload = {
      ...addressForm,
      receiver: addressForm.receiver.trim(),
      phone: addressForm.phone.trim(),
      province: addressForm.province.trim(),
      city: addressForm.city.trim(),
      district: addressForm.district.trim(),
      detail: addressForm.detail.trim(),
      postCode: addressForm.postCode?.trim() || undefined,
      isDefault: Number(addressForm.isDefault || 0),
    }
    if (editingAddressId.value) {
      await updateAddress({ ...payload, id: editingAddressId.value })
    } else {
      await addAddress(payload)
    }
    dialogOpen.value = false
    await loadAddresses()
    ElMessage.success('收货地址已保存')
  } finally {
    saving.value = false
  }
}

async function makeDefault(address: AddressItem) {
  if (Number(address.isDefault) === 1) {
    return
  }
  await setDefaultAddress(address.id)
  await loadAddresses()
  ElMessage.success('默认地址已更新')
}

async function removeAddress(address: AddressItem) {
  try {
    await ElMessageBox.confirm(`确认删除「${address.receiver}」的收货地址？`, '删除地址', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deletingId.value = address.id
  try {
    await deleteAddress(address.id)
    await loadAddresses()
    ElMessage.success('地址已删除')
  } finally {
    deletingId.value = ''
  }
}

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      loadAddresses()
    } else {
      addresses.value = []
    }
  },
)

onMounted(loadAddresses)
</script>

<template>
  <div class="address-page">
    <ShopHeader />

    <main class="address-shell">
      <section class="address-hero">
        <div>
          <span>Address Book</span>
          <h1>收货地址</h1>
          <p>维护常用收货信息，结算、秒杀和售后寄回时都可以直接复用。</p>
        </div>
        <div class="hero-actions">
          <button class="plain-link" type="button" @click="router.push('/profile')">个人中心</button>
          <el-button type="primary" size="large" @click="openDialog()">新增地址</el-button>
        </div>
      </section>

      <section v-if="!auth.isLoggedIn" class="login-needed">
        <h2>登录后管理收货地址</h2>
        <p>地址会绑定到会员账户，请先登录。</p>
        <el-button type="primary" size="large" @click="auth.openLoginDialog()">立即登录</el-button>
      </section>

      <section v-else class="address-layout">
        <aside class="address-summary">
          <span>Overview</span>
          <h2>地址概览</h2>
          <dl>
            <div>
              <dt>全部地址</dt>
              <dd>{{ addresses.length }}</dd>
            </div>
            <div>
              <dt>普通地址</dt>
              <dd>{{ normalAddressCount }}</dd>
            </div>
          </dl>
          <div v-if="defaultAddress" class="default-card">
            <strong>默认地址</strong>
            <p>{{ defaultAddress.receiver }} {{ defaultAddress.phone }}</p>
            <small>{{ addressLine(defaultAddress) }}</small>
          </div>
          <div v-else class="default-card empty">
            <strong>还没有默认地址</strong>
            <p>新增地址时可以直接设为默认。</p>
          </div>
          <el-button type="primary" size="large" @click="openDialog()">新增地址</el-button>
          <el-button size="large" @click="router.push('/checkout')">去结算页</el-button>
        </aside>

        <section class="address-main">
          <div class="toolbar">
            <div>
              <span>Saved Address</span>
              <h2>{{ filteredAddresses.length }} 条地址</h2>
            </div>
            <el-input v-model="keyword" clearable placeholder="搜索姓名、电话、地址" />
          </div>

          <div v-loading="loading" class="address-grid">
            <article v-for="address in filteredAddresses" :key="address.id" class="address-card">
              <div class="card-top">
                <div>
                  <strong>{{ address.receiver }}</strong>
                  <span>{{ address.phone }}</span>
                </div>
                <em v-if="Number(address.isDefault) === 1">默认</em>
              </div>
              <p>{{ addressLine(address) }}</p>
              <small>邮编 {{ address.postCode || '-' }} · 更新于 {{ formatTime(address.updateTime) }}</small>
              <div class="card-actions">
                <el-button @click="openDialog(address)">编辑</el-button>
                <el-button v-if="Number(address.isDefault) !== 1" @click="makeDefault(address)">设为默认</el-button>
                <el-button :loading="String(deletingId) === String(address.id)" @click="removeAddress(address)">
                  删除
                </el-button>
              </div>
            </article>

            <button v-if="!loading && !addresses.length" class="empty-address" type="button" @click="openDialog()">
              <strong>新增第一个收货地址</strong>
              <span>下单前会自动带入默认地址</span>
            </button>

            <el-empty v-if="!loading && addresses.length && !filteredAddresses.length" description="没有匹配的地址">
              <el-button @click="keyword = ''">清空搜索</el-button>
            </el-empty>
          </div>
        </section>
      </section>
    </main>

    <el-dialog
      v-model="dialogOpen"
      :title="editingAddressId ? '编辑收货地址' : '新增收货地址'"
      width="520px"
      class="address-dialog"
      append-to-body
    >
      <el-form label-position="top" @submit.prevent>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="收件人">
              <el-input v-model="addressForm.receiver" placeholder="姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="addressForm.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="省份">
              <el-input v-model="addressForm.province" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市">
              <el-input v-model="addressForm.city" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县">
              <el-input v-model="addressForm.district" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址">
          <el-input v-model="addressForm.detail" placeholder="街道、小区、门牌号" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="邮编">
              <el-input v-model="addressForm.postCode" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认地址">
              <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAddress">保存地址</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.address-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 4%, rgba(47, 143, 103, 0.12), transparent 25%),
    linear-gradient(180deg, #fbfcf8 0%, #f4f7f2 100%);
  color: #111827;
}

.address-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 24px 72px;
}

.address-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.address-hero span,
.address-summary > span,
.toolbar span {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(47, 143, 103, 0.1);
  color: #2f8f67;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.address-hero h1 {
  margin: 12px 0 0;
  font-size: 42px;
  line-height: 1.12;
}

.address-hero p {
  margin: 12px 0 0;
  color: #6b7280;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.plain-link {
  border: 0;
  background: transparent;
  color: #e5484d;
  cursor: pointer;
  font: inherit;
  font-weight: 900;
}

.login-needed,
.address-summary,
.toolbar,
.address-card,
.empty-address {
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.login-needed {
  padding: 44px;
}

.login-needed h2 {
  margin: 0;
  font-size: 28px;
}

.login-needed p {
  margin: 12px 0 24px;
  color: #6b7280;
}

.address-layout {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.address-summary {
  position: sticky;
  top: 96px;
  padding: 24px;
}

.address-summary h2 {
  margin: 10px 0 20px;
  font-size: 26px;
}

.address-summary dl,
.address-summary dd {
  margin: 0;
}

.address-summary dl > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.address-summary dt {
  color: #6b7280;
}

.address-summary dd {
  font-size: 22px;
  font-weight: 900;
}

.default-card {
  margin: 16px 0;
  padding: 16px;
  border-radius: 8px;
  background: #f7f8f5;
}

.default-card.empty {
  background: #fff7ed;
}

.default-card strong,
.default-card p,
.default-card small {
  display: block;
}

.default-card p {
  margin: 10px 0 0;
  color: #111827;
  font-weight: 800;
}

.default-card small {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.6;
}

.address-summary :deep(.el-button) {
  width: 100%;
  height: 46px;
  margin: 0 0 10px;
  border-radius: 8px;
  font-weight: 900;
}

.toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
}

.toolbar h2 {
  margin: 9px 0 0;
  font-size: 26px;
}

.toolbar :deep(.el-input) {
  width: 300px;
}

.toolbar :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  min-height: 320px;
}

.address-card {
  min-height: 210px;
  padding: 20px;
}

.card-top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.card-top strong,
.card-top span {
  display: block;
}

.card-top strong {
  font-size: 22px;
}

.card-top span {
  margin-top: 8px;
  color: #4b5563;
  font-weight: 800;
}

.card-top em {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: 999px;
  background: #eef8f2;
  color: #2f8f67;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.address-card p {
  margin: 18px 0 0;
  color: #111827;
  line-height: 1.7;
}

.address-card small {
  display: block;
  margin-top: 10px;
  color: #6b7280;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.card-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 900;
}

.empty-address {
  display: grid;
  min-height: 210px;
  place-content: center;
  border-style: dashed;
  cursor: pointer;
  font: inherit;
  text-align: center;
}

.empty-address strong,
.empty-address span {
  display: block;
}

.empty-address strong {
  font-size: 20px;
}

.empty-address span {
  margin-top: 10px;
  color: #6b7280;
}

.address-hero :deep(.el-button--primary),
.address-summary :deep(.el-button--primary),
.login-needed :deep(.el-button--primary),
.address-dialog :deep(.el-button--primary) {
  background: #e5484d;
  border-color: #e5484d;
  box-shadow: 0 12px 24px rgba(229, 72, 77, 0.2);
}

:global(.address-dialog.el-dialog) {
  width: min(520px, calc(100vw - 32px));
  border-radius: 8px;
  overflow: hidden;
}

@media (max-width: 980px) {
  .address-layout {
    grid-template-columns: 1fr;
  }

  .address-summary {
    position: static;
  }
}

@media (max-width: 760px) {
  .address-shell {
    padding: 28px 16px 52px;
  }

  .address-hero,
  .toolbar {
    align-items: start;
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    justify-content: space-between;
  }

  .toolbar :deep(.el-input) {
    width: 100%;
  }

  .address-grid {
    grid-template-columns: 1fr;
  }
}
</style>
