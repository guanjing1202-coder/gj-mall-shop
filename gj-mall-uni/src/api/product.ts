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
  return request<PageResult<ProductItem>>({ url: '/api/product/spu/page', method: 'GET', data: params })
}

export function getProductDetail(id: ApiId) {
  return request<ProductDetail>({ url: `/api/product/spu/${id}`, method: 'GET' })
}

export function getCategoryTree() {
  return request<CategoryItem[]>({ url: '/api/product/category/tree', method: 'GET' })
}

export function getBrandList() {
  return request<BrandItem[]>({ url: '/api/product/brand', method: 'GET' })
}
