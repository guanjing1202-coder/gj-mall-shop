import request from '@/utils/request'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface SalesReportQuery {
  startDate?: string
  endDate?: string
  granularity?: string
}

export interface SalesReportOverview {
  orderCount: number
  paidOrderCount: number
  refundOrderCount: number
  newMemberCount: number
  grossAmount: number
  paidAmount: number
  refundAmount: number
  netAmount: number
  averageOrderAmount: number
  paidConversionRate: number
  refundRate: number
}

export interface SalesTrendItem {
  date: string
  orderCount: number
  paidOrderCount: number
  paidAmount: number
  netAmount: number
}

export interface ProductRankItem {
  spuId: string | number
  productName: string
  saleQuantity: number
  orderCount: number
  salesAmount: number
  averagePrice: number
}

export interface CategoryRankItem {
  categoryId?: string | number
  categoryName: string
  saleQuantity: number
  orderCount: number
  salesAmount: number
}

export interface RefundTrendItem {
  date: string
  refundCount: number
  refundAmount: number
}

export interface MemberGrowthItem {
  date: string
  newMemberCount: number
}

export interface SalesReport {
  startDate: string
  endDate: string
  granularity: string
  overview: SalesReportOverview
  salesTrend: SalesTrendItem[]
  productRanks: ProductRankItem[]
  categoryRanks: CategoryRankItem[]
  refundTrend: RefundTrendItem[]
  memberGrowth: MemberGrowthItem[]
}

export function getSalesReport(params: SalesReportQuery) {
  return request.get<ApiResult<SalesReport>>('/api/admin/report/sales', { params })
}

export async function downloadSalesReport(params: SalesReportQuery) {
  const blob = await request.get<Blob>('/api/admin/report/sales/export', {
    params,
    responseType: 'blob',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `GJ商城经营报表-${params.startDate || ''}-${params.endDate || ''}.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
