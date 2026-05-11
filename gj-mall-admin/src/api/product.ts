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

export interface ProductQueryParams {
  current?: number
  size?: number
  keyword?: string
  brandId?: ApiId
  categoryId?: ApiId
  publishStatus?: number
  newStatus?: number
  recommendStatus?: number
  sort?: string
}

export interface ProductItem {
  id: ApiId
  name: string
  subTitle?: string
  categoryId?: ApiId
  brandId?: ApiId
  mainImage?: string
  price: number
  saleCount?: number
  publishStatus: number
  newStatus?: number
  recommendStatus?: number
  sort?: number
}

export interface BrandItem {
  id: ApiId
  name: string
  logo?: string
  description?: string
  sort?: number
  showStatus?: number
}

export interface BrandPayload {
  id?: ApiId
  name: string
  logo?: string
  description?: string
  sort?: number
  showStatus?: number
}

export interface CategoryTreeItem {
  id: ApiId
  parentId: ApiId
  name: string
  icon?: string
  level?: number
  sort?: number
  showStatus?: number
  children?: CategoryTreeItem[]
}

export interface CategoryPayload {
  id?: ApiId
  parentId: ApiId
  name: string
  icon?: string
  level?: number
  sort?: number
  showStatus?: number
}

export interface ProductSku {
  id: ApiId
  spuId: ApiId
  skuCode?: string
  name: string
  image?: string
  price: number
  stock?: number
  lockedStock?: number
  specData?: Record<string, string>
  saleCount?: number
}

export interface ProductSaveSku {
  id?: ApiId
  skuCode?: string
  name: string
  image?: string
  price: number
  costPrice?: number
  stock: number
  specData?: Record<string, string>
}

export interface ProductSavePayload {
  id?: ApiId
  name: string
  subTitle?: string
  categoryId: ApiId
  brandId?: ApiId
  mainImage?: string
  images?: string[]
  detailHtml?: string
  detailImages?: string[]
  packingList?: string
  afterSale?: string
  publishStatus?: number
  skus: ProductSaveSku[]
}

export interface ProductDetail {
  id: ApiId
  name: string
  subTitle?: string
  categoryId?: ApiId
  categoryName?: string
  brandId?: ApiId
  brandName?: string
  mainImage?: string
  images?: string[]
  price?: number
  saleCount?: number
  publishStatus?: number
  newStatus?: number
  recommendStatus?: number
  sort?: number
  detailHtml?: string
  detailImages?: string[]
  packingList?: string
  afterSale?: string
  skus?: ProductSku[]
}

export interface ProductOperationPayload {
  publishStatus?: number
  newStatus?: number
  recommendStatus?: number
  sort?: number
}

export function getProductPage(params: ProductQueryParams) {
  return request.get<ApiResult<PageResult<ProductItem>>>('/api/admin/product/spu/page', { params })
}

export function getProductDetail(id: ApiId) {
  return request.get<ApiResult<ProductDetail>>(`/api/admin/product/spu/${id}`)
}

export function updateProductPublishStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/product/spu/${id}/publish`, null, {
    params: { status },
  })
}

export function updateProductOperation(id: ApiId, data: ProductOperationPayload) {
  return request.put<ApiResult<void>>(`/api/admin/product/spu/${id}/operation`, data)
}

export function deleteProduct(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/product/spu/${id}`)
}

export function createProduct(data: ProductSavePayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/product/spu', data)
}

export function updateProduct(data: ProductSavePayload) {
  return request.put<ApiResult<void>>('/api/admin/product/spu', data)
}

export function getBrandPage(params: { keyword?: string; pageNum?: number; pageSize?: number }) {
  return request.get<ApiResult<PageResult<BrandItem>>>('/api/admin/product/brand/page', { params })
}

export function createBrand(data: BrandPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/product/brand', data)
}

export function updateBrand(data: BrandPayload) {
  return request.put<ApiResult<void>>('/api/admin/product/brand', data)
}

export function deleteBrand(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/product/brand/${id}`)
}

export function getCategoryTree() {
  return request.get<ApiResult<CategoryTreeItem[]>>('/api/admin/product/category/tree')
}

export function createCategory(data: CategoryPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/product/category', data)
}

export function updateCategory(data: CategoryPayload) {
  return request.put<ApiResult<void>>('/api/admin/product/category', data)
}

export function deleteCategory(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/product/category/${id}`)
}
