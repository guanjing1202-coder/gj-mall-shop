import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/AboutView.vue'),
    meta: { title: '关于' },
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetailView.vue'),
    meta: { title: '商品详情' },
  },
  {
    path: '/products',
    name: 'ProductList',
    component: () => import('@/views/ProductListView.vue'),
    meta: { title: '商品列表' },
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人中心' },
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('@/views/CheckoutView.vue'),
    meta: { title: '确认订单' },
  },
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('@/views/OrderListView.vue'),
    meta: { title: '我的订单' },
  },
  {
    path: '/coupons',
    name: 'CouponList',
    component: () => import('@/views/CouponListView.vue'),
    meta: { title: '我的优惠券' },
  },
  {
    path: '/favorites',
    name: 'FavoriteList',
    component: () => import('@/views/FavoriteListView.vue'),
    meta: { title: '我的收藏' },
  },
  {
    path: '/history',
    name: 'HistoryList',
    component: () => import('@/views/HistoryListView.vue'),
    meta: { title: '浏览足迹' },
  },
  {
    path: '/messages',
    name: 'MessageList',
    component: () => import('@/views/MessageListView.vue'),
    meta: { title: '消息中心' },
  },
  {
    path: '/after-sales',
    name: 'AfterSaleList',
    component: () => import('@/views/AfterSaleListView.vue'),
    meta: { title: '我的售后' },
  },
  {
    path: '/addresses',
    name: 'AddressList',
    component: () => import('@/views/AddressListView.vue'),
    meta: { title: '收货地址' },
  },
  {
    path: '/coupon-center',
    name: 'CouponCenter',
    component: () => import('@/views/CouponCenterView.vue'),
    meta: { title: '领券中心' },
  },
  {
    path: '/seckill',
    name: 'Seckill',
    component: () => import('@/views/SeckillView.vue'),
    meta: { title: '限时秒杀' },
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情' },
  },
  {
    path: '/pay/result',
    name: 'PayResult',
    component: () => import('@/views/PayResultView.vue'),
    meta: { title: '支付结果' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} - GJ Mall`
  }
  next()
})

export default router
