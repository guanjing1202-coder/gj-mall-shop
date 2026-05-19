import request from '@/utils/request'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface DashboardOverview {
  productTotal: number
  productOnSale: number
  brandTotal: number
  categoryTotal: number
  userTotal: number
  todayNewUsers: number
  orderTotal: number
  todayOrderCount: number
  pendingPayCount: number
  pendingDeliveryCount: number
  pendingReceiveCount: number
  completedCount: number
  canceledCount: number
  paidAmountTotal: number
  todayPaidAmount: number
}

export interface DashboardTrendItem {
  date: string
  orderCount: number
  paidAmount: number
}

export interface DashboardHotProductItem {
  id: string | number
  name: string
  mainImage?: string
  price?: number
  saleCount?: number
}

export interface DashboardLowStockItem {
  skuId: string | number
  spuId?: string | number
  spuName?: string
  skuName?: string
  skuCode?: string
  stock?: number
  warnStock?: number
  alertGap?: number
}

export interface DashboardLatestOrderItem {
  id: string | number
  orderNo: string
  userId: string | number
  payAmount?: number
  status?: number
  statusDesc?: string
  createTime?: string
}

export interface DashboardBusiness {
  productTotal: number
  productOnSale: number
  lowStockSkuCount: number
  emptyStockSkuCount: number
  brandTotal: number
  categoryTotal: number
  userTotal: number
  todayNewUsers: number
  orderTotal: number
  todayOrderCount: number
  pendingPayCount: number
  pendingDeliveryCount: number
  pendingReceiveCount: number
  completedCount: number
  canceledCount: number
  refundingCount: number
  refundedCount: number
  paidAmountTotal: number
  todayPaidAmount: number
  pendingDeliveryAmount: number
  paidConversionRate: number
  averageOrderAmount: number
  refundAmountTotal: number
  todayRefundAmount: number
  refundRate: number
  pendingAfterSaleCount: number
  pendingCommentCount: number
  activeCouponCount: number
  activeSeckillCount: number
  orderTrend: DashboardTrendItem[]
  hotProducts: DashboardHotProductItem[]
  lowStockSkus: DashboardLowStockItem[]
  latestOrders: DashboardLatestOrderItem[]
}

export function getDashboardOverview() {
  return request.get<ApiResult<DashboardOverview>>('/api/admin/dashboard/overview')
}

export function getDashboardBusiness() {
  return request.get<ApiResult<DashboardBusiness>>('/api/admin/dashboard/business')
}
