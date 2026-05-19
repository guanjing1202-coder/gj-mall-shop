import request from '@/utils/request'
import type { ApiId, ApiResult, PageResult } from './product'

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
  return request.get<ApiResult<PageResult<FavoriteItem>>>('/api/user/favorite/page', { params })
}

export function getFavoriteStatus(spuId: ApiId) {
  return request.get<ApiResult<boolean>>(`/api/user/favorite/${spuId}/status`)
}

export function addFavorite(spuId: ApiId) {
  return request.post<ApiResult<ApiId>>(`/api/user/favorite/${spuId}`)
}

export function removeFavorite(spuId: ApiId) {
  return request.delete<ApiResult<void>>(`/api/user/favorite/${spuId}`)
}
