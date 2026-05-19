<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  AuditOutlined,
  CommentOutlined,
  BarChartOutlined,
  DashboardOutlined,
  GiftOutlined,
  LogoutOutlined,
  OrderedListOutlined,
  PayCircleOutlined,
  ProjectOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  SendOutlined,
  ShopOutlined,
  ShoppingOutlined,
  TagsOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { logout as logoutApi } from '@/api/auth'
import { getCurrentAdminPermissions } from '@/api/adminUser'

const collapsed = ref<boolean>(false)
const permissionLoaded = ref(false)
const permissionCodes = ref<string[]>(readCachedPermissionCodes())
const currentUser = computed(() => readCurrentUser())
const router = useRouter()
const route = useRoute()

const selectedKeys = computed(() => [String(route.name || '')])
const menuItems = [
  { key: 'Dashboard', label: '仪表盘', permission: 'menu.dashboard', icon: DashboardOutlined },
  { key: 'Report', label: '经营报表', permission: 'menu.dashboard', icon: BarChartOutlined },
  { key: 'Product', label: '商品管理', permission: 'product.spu.manage', icon: ShoppingOutlined },
  { key: 'ProductBrand', label: '品牌管理', permission: 'product.brand.manage', icon: ShopOutlined },
  { key: 'ProductCategory', label: '分类管理', permission: 'product.category.manage', icon: TagsOutlined },
  { key: 'ProductInventory', label: '库存管理', permission: 'product.inventory.manage', icon: ProjectOutlined },
  { key: 'ProductOperation', label: '商品运营', permission: 'product.operation.manage', icon: ToolOutlined },
  { key: 'ProductComment', label: '评价/评论', permission: 'product.comment.manage', icon: CommentOutlined },
  { key: 'Order', label: '订单管理', permission: 'order.manage', icon: OrderedListOutlined },
  { key: 'Logistics', label: '物流/发货', permission: 'logistics.manage', icon: SendOutlined },
  { key: 'Payment', label: '支付退款', permission: 'payment.manage', icon: PayCircleOutlined },
  { key: 'AfterSale', label: '售后/退货', permission: 'after.sale.manage', icon: SafetyCertificateOutlined },
  { key: 'MarketingCoupon', label: '优惠券管理', permission: 'coupon.manage', icon: GiftOutlined },
  { key: 'MarketingSeckill', label: '秒杀活动', permission: 'seckill.manage', icon: AuditOutlined },
  { key: 'Member', label: '会员管理', permission: 'member.manage', icon: TeamOutlined },
  { key: 'MemberFavorite', label: '会员收藏', permission: 'member.favorite.manage', icon: TagsOutlined },
  { key: 'User', label: '后台账号', permission: 'sys.user.manage', icon: UserOutlined },
  { key: 'RolePermission', label: '角色权限', permission: 'sys.role.manage', icon: SafetyCertificateOutlined },
  { key: 'OperationLog', label: '操作日志', permission: 'sys.operation.log.manage', icon: AuditOutlined },
  { key: 'SystemConfig', label: '系统配置', permission: 'sys.config.manage', icon: SettingOutlined },
]

const visibleMenuItems = computed(() => {
  if (!permissionLoaded.value && permissionCodes.value.length === 0) {
    return menuItems
  }
  const granted = new Set(permissionCodes.value)
  return menuItems.filter((item) => granted.has(item.permission))
})

onMounted(() => {
  window.addEventListener('mall-admin-auth-expired', handleAuthExpired)
  window.addEventListener('mall-admin-auth-refreshed', handleAuthRefreshed)
  fetchCurrentPermissions()
})

onBeforeUnmount(() => {
  window.removeEventListener('mall-admin-auth-expired', handleAuthExpired)
  window.removeEventListener('mall-admin-auth-refreshed', handleAuthRefreshed)
})

function handleMenu(name: string) {
  router.push({ name })
}

function readCachedPermissionCodes() {
  try {
    const codes = JSON.parse(localStorage.getItem('admin_permission_codes') || '[]')
    return Array.isArray(codes) ? codes : []
  } catch {
    return []
  }
}

function readCurrentUser() {
  try {
    const user = JSON.parse(localStorage.getItem('admin_user') || '{}')
    return user?.nickname || user?.username || '管理员'
  } catch {
    return '管理员'
  }
}

async function fetchCurrentPermissions() {
  try {
    const res = await getCurrentAdminPermissions()
    permissionCodes.value = res.data.permissionCodes || []
    localStorage.setItem('admin_permission_codes', JSON.stringify(permissionCodes.value))
  } catch {
    permissionCodes.value = []
  } finally {
    permissionLoaded.value = true
  }
}

function clearAdminSession() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_refresh_token')
  localStorage.removeItem('admin_user')
  localStorage.removeItem('admin_permission_codes')
}

function handleAuthExpired() {
  clearAdminSession()
  router.replace({ path: '/login', query: { redirect: route.fullPath } })
}

function handleAuthRefreshed() {
  permissionCodes.value = []
  fetchCurrentPermissions()
}

async function logout() {
  try {
    await logoutApi()
  } catch (e) {
    // 本地退出优先，后端 token 失效时也允许回到登录页。
  }
  clearAdminSession()
  router.push('/login')
}
</script>

<template>
  <a-layout class="admin-layout">
    <a-layout-sider v-model:collapsed="collapsed" collapsible theme="dark" class="admin-sider">
      <div class="brand-block">
        <div class="brand-mark">GJ</div>
        <div v-if="!collapsed" class="brand-copy">
          <strong>GJ Mall</strong>
          <span>Admin Console</span>
        </div>
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys" class="admin-menu">
        <a-menu-item v-for="item in visibleMenuItems" :key="item.key" @click="handleMenu(item.key)">
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="admin-header">
        <div class="header-title">{{ route.meta.title }}</div>
        <div class="header-user">
          <a-avatar :size="32">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <span>{{ currentUser }}</span>
          <a-button type="text" @click="logout">
            <template #icon><LogoutOutlined /></template>
            退出登录
          </a-button>
        </div>
      </a-layout-header>
      <a-layout-content class="admin-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f4f7fb;
}

.admin-sider {
  box-shadow: 8px 0 24px rgba(15, 23, 42, 0.12);
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 16px;
  color: #ffffff;
}

.brand-mark {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #0f766e;
  font-weight: 800;
  letter-spacing: 0;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-copy strong {
  font-size: 15px;
  line-height: 1.2;
  letter-spacing: 0;
}

.brand-copy span {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 11px;
  letter-spacing: 0;
}

.admin-menu {
  border-right: 0;
}

.admin-menu :deep(.ant-menu-item) {
  margin: 4px 10px;
  width: calc(100% - 20px);
  border-radius: 8px;
}

.admin-menu :deep(.ant-menu-item-selected) {
  background: #0f766e;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid #e5ebf3;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.header-title {
  color: #17212b;
  font-size: 16px;
  font-weight: 700;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #526171;
}

.admin-content {
  margin: 16px;
  min-height: calc(100vh - 96px);
}

@media (max-width: 760px) {
  .admin-header {
    padding: 0 14px;
  }

  .header-user span {
    display: none;
  }

  .admin-content {
    margin: 12px;
  }
}
</style>
