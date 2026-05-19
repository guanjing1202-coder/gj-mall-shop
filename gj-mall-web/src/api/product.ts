import request from '@/utils/request'

export type ApiId = string | number

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string | number
}

export interface PageResult<T> {
  total: string | number
  pageNum: string | number
  pageSize: string | number
  list: T[]
}

export interface ProductQuery {
  current?: number
  size?: number
  keyword?: string
  categoryId?: ApiId
  brandId?: ApiId
  newStatus?: number
  recommendStatus?: number
  minPrice?: number
  maxPrice?: number
  sort?: 'default' | 'operation' | 'sales' | 'price_asc' | 'price_desc'
}

export interface ProductItem {
  id: ApiId
  name: string
  subTitle?: string
  categoryId?: ApiId
  brandId?: ApiId
  mainImage?: string
  price?: number
  saleCount?: number
  publishStatus?: number
  newStatus?: number
  recommendStatus?: number
  sort?: number
}

export interface ProductSku {
  id: ApiId
  spuId: ApiId
  skuCode?: string
  name?: string
  image?: string
  price?: number
  stock?: number
  lockedStock?: number
  specData?: Record<string, string>
  saleCount?: number
}

export interface ProductDetail extends ProductItem {
  categoryName?: string
  brandName?: string
  images?: string[]
  detailHtml?: string
  detailImages?: string[]
  packingList?: string
  afterSale?: string
  skus?: ProductSku[]
}

export interface ProductComment {
  id: ApiId
  userId?: ApiId
  spuId: ApiId
  skuId?: ApiId
  skuName?: string
  specData?: Record<string, string>
  score: number
  content: string
  images?: string[]
  replyContent?: string
  replyTime?: string
  createTime?: string
}

export interface ProductCommentSummary {
  total: string | number
  averageScore: number
  goodCount: string | number
  goodRate: number
  imageCount: string | number
}

export interface BrandItem {
  id: ApiId
  name: string
  logo?: string
  description?: string
  sort?: number
  showStatus?: number
}

export interface CategoryItem {
  id: ApiId
  parentId?: ApiId
  name: string
  icon?: string
  level?: number
  sort?: number
  showStatus?: number
  children?: CategoryItem[]
}

export function getProductPage(params: ProductQuery) {
  return request.get<ApiResult<PageResult<ProductItem>>>('/api/product/spu/page', { params })
}

export function getProductDetail(id: ApiId) {
  return request.get<ApiResult<ProductDetail>>(`/api/product/spu/${id}`)
}

export function getProductComments(
  spuId: ApiId,
  params: { current?: number; size?: number; hasImage?: boolean },
) {
  return request.get<ApiResult<PageResult<ProductComment>>>(`/api/product/comment/spu/${spuId}/page`, { params })
}

export function getProductCommentSummary(spuId: ApiId) {
  return request.get<ApiResult<ProductCommentSummary>>(`/api/product/comment/spu/${spuId}/summary`)
}

export function getCategoryTree() {
  return request.get<ApiResult<CategoryItem[]>>('/api/product/category/tree')
}

export function getBrandList() {
  return request.get<ApiResult<BrandItem[]>>('/api/product/brand')
}
