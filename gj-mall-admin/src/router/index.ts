import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getCurrentAdminPermissions } from '@/api/adminUser'

const routePermissions: Record<string, string> = {
  Dashboard: 'menu.dashboard',
  Report: 'menu.dashboard',
  Product: 'product.spu.manage',
  ProductBrand: 'product.brand.manage',
  ProductCategory: 'product.category.manage',
  ProductInventory: 'product.inventory.manage',
  ProductOperation: 'product.operation.manage',
  ProductComment: 'product.comment.manage',
  Order: 'order.manage',
  Logistics: 'logistics.manage',
  Payment: 'payment.manage',
  AfterSale: 'after.sale.manage',
  MarketingCoupon: 'coupon.manage',
  MarketingSeckill: 'seckill.manage',
  Member: 'member.manage',
  MemberFavorite: 'member.favorite.manage',
  User: 'sys.user.manage',
  RolePermission: 'sys.role.manage',
  OperationLog: 'sys.operation.log.manage',
  SystemConfig: 'sys.config.manage',
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', skipAuth: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '仪表盘', permission: routePermissions.Dashboard },
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/SalesReportView.vue'),
        meta: { title: '经营报表', permission: routePermissions.Report },
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '商品管理', permission: routePermissions.Product },
      },
      {
        path: 'product-brand',
        name: 'ProductBrand',
        component: () => import('@/views/product/BrandList.vue'),
        meta: { title: '品牌管理', permission: routePermissions.ProductBrand },
      },
      {
        path: 'product-category',
        name: 'ProductCategory',
        component: () => import('@/views/product/CategoryList.vue'),
        meta: { title: '分类管理', permission: routePermissions.ProductCategory },
      },
      {
        path: 'product-inventory',
        name: 'ProductInventory',
        component: () => import('@/views/product/InventoryList.vue'),
        meta: { title: '库存管理', permission: routePermissions.ProductInventory },
      },
      {
        path: 'product-operation',
        name: 'ProductOperation',
        component: () => import('@/views/product/ProductOperationList.vue'),
        meta: { title: '商品运营管理', permission: routePermissions.ProductOperation },
      },
      {
        path: 'product-comment',
        name: 'ProductComment',
        component: () => import('@/views/product/CommentList.vue'),
        meta: { title: '评价/评论管理', permission: routePermissions.ProductComment },
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', permission: routePermissions.Order },
      },
      {
        path: 'logistics',
        name: 'Logistics',
        component: () => import('@/views/order/LogisticsList.vue'),
        meta: { title: '物流/发货管理', permission: routePermissions.Logistics },
      },
      {
        path: 'payment',
        name: 'Payment',
        component: () => import('@/views/payment/PaymentList.vue'),
        meta: { title: '支付退款管理', permission: routePermissions.Payment },
      },
      {
        path: 'after-sale',
        name: 'AfterSale',
        component: () => import('@/views/afterSale/AfterSaleList.vue'),
        meta: { title: '售后/退货管理', permission: routePermissions.AfterSale },
      },
      {
        path: 'marketing-coupon',
        name: 'MarketingCoupon',
        component: () => import('@/views/marketing/CouponList.vue'),
        meta: { title: '优惠券管理', permission: routePermissions.MarketingCoupon },
      },
      {
        path: 'marketing-seckill',
        name: 'MarketingSeckill',
        component: () => import('@/views/marketing/SeckillList.vue'),
        meta: { title: '秒杀活动管理', permission: routePermissions.MarketingSeckill },
      },
      {
        path: 'member',
        name: 'Member',
        component: () => import('@/views/member/MemberList.vue'),
        meta: { title: '会员管理', permission: routePermissions.Member },
      },
      {
        path: 'member-favorite',
        name: 'MemberFavorite',
        component: () => import('@/views/member/FavoriteList.vue'),
        meta: { title: '会员收藏管理', permission: routePermissions.MemberFavorite },
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '后台账号', permission: routePermissions.User },
      },
      {
        path: 'role-permission',
        name: 'RolePermission',
        component: () => import('@/views/user/RolePermissionList.vue'),
        meta: { title: '角色权限', permission: routePermissions.RolePermission },
      },
      {
        path: 'operation-log',
        name: 'OperationLog',
        component: () => import('@/views/system/OperationLogList.vue'),
        meta: { title: '操作日志', permission: routePermissions.OperationLog },
      },
      {
        path: 'system-config',
        name: 'SystemConfig',
        component: () => import('@/views/system/SystemConfigList.vue'),
        meta: { title: '系统配置', permission: routePermissions.SystemConfig },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} - GJ Mall Admin`
  }
  const token = localStorage.getItem('admin_token')
  if (!to.meta?.skipAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta?.skipAuth) {
    next()
    return
  }
  const requiredPermission = to.meta?.permission as string | undefined
  if (!requiredPermission) {
    next()
    return
  }
  try {
    let codes = await ensurePermissionCodes()
    if (!codes.includes(requiredPermission)) {
      codes = await ensurePermissionCodes(true)
    }
    if (codes.includes(requiredPermission)) {
      next()
      return
    }
    const fallbackName = Object.keys(routePermissions).find((name) => codes.includes(routePermissions[name]))
    next(fallbackName ? { name: fallbackName } : { path: '/login' })
  } catch {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_refresh_token')
    localStorage.removeItem('admin_user')
    localStorage.removeItem('admin_permission_codes')
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
})

async function ensurePermissionCodes(force = false) {
  const cached = force ? null : localStorage.getItem('admin_permission_codes')
  if (!force && cached) {
    try {
      const codes = JSON.parse(cached)
      if (Array.isArray(codes) && codes.length) {
        return codes as string[]
      }
    } catch {
      localStorage.removeItem('admin_permission_codes')
    }
  }
  const res = await getCurrentAdminPermissions()
  const codes = res.data.permissionCodes || []
  localStorage.setItem('admin_permission_codes', JSON.stringify(codes))
  return codes
}

export default router
