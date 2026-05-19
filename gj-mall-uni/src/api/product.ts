import { request } from '@/utils/request'

export type ApiId = string | number

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

export interface SearchHotWord {
  keyword: string
  type: 'keyword' | 'product' | 'brand' | 'category' | string
  label?: string
  targetId?: ApiId
  heat?: number
}

export interface SearchSuggestItem {
  keyword: string
  type: 'keyword' | 'product' | 'sku' | 'brand' | 'category' | string
  label?: string
  targetId?: ApiId
  image?: string
}

export function getProductPage(params: ProductQuery) {
  return request<PageResult<ProductItem>>({ url: '/api/product/spu/page', method: 'GET', data: params })
}

export function getProductDetail(id: ApiId) {
  return request<ProductDetail>({ url: `/api/product/spu/${id}`, method: 'GET' })
}

export function getProductComments(spuId: ApiId, params: { current?: number; size?: number; hasImage?: boolean }) {
  return request<PageResult<ProductComment>>({
    url: `/api/product/comment/spu/${spuId}/page`,
    method: 'GET',
    data: params,
  })
}

export function getProductCommentSummary(spuId: ApiId) {
  return request<ProductCommentSummary>({ url: `/api/product/comment/spu/${spuId}/summary`, method: 'GET' })
}

export function getCategoryTree() {
  return request<CategoryItem[]>({ url: '/api/product/category/tree', method: 'GET' })
}

export function getBrandList() {
  return request<BrandItem[]>({ url: '/api/product/brand', method: 'GET' })
}

export function getSearchHotWords(limit = 12) {
  return request<SearchHotWord[]>({ url: '/api/product/search/hot', method: 'GET', data: { limit } })
}

export function getSearchSuggestions(keyword: string, limit = 10) {
  return request<SearchSuggestItem[]>({
    url: '/api/product/search/suggest',
    method: 'GET',
    data: { keyword, limit },
  })
}
