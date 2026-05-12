import { request } from '@/utils/request'
import type { ApiId, PageResult } from './product'

export interface FavoriteItem {
  id: ApiId
  spuId: ApiId
  spuName?: string
  subTitle?: string
  mainImage?: string
  price?: number
  saleCount?: number
  publishStatus?: number
  brandId?: ApiId
  brandName?: string
  categoryId?: ApiId
  categoryName?: string
  createTime?: string
}

export interface FavoriteQuery {
  current?: number
  size?: number
}

export function getFavoritePage(params: FavoriteQuery) {
  return request<PageResult<FavoriteItem>>({ url: '/api/user/favorite/page', method: 'GET', data: params })
}

export function getFavoriteStatus(spuId: ApiId) {
  return request<boolean>({ url: `/api/user/favorite/${spuId}/status`, method: 'GET' })
}

export function addFavorite(spuId: ApiId) {
  return request<ApiId>({ url: `/api/user/favorite/${spuId}`, method: 'POST' })
}

export function removeFavorite(spuId: ApiId) {
  return request<void>({ url: `/api/user/favorite/${spuId}`, method: 'DELETE' })
}
