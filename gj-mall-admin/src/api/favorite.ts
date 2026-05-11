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

export interface FavoriteQuery {
  keyword?: string
  userId?: ApiId
  spuId?: ApiId
  pageNum?: number
  pageSize?: number
}

export interface FavoriteRecord {
  id: ApiId
  userId: ApiId
  username?: string
  nickname?: string
  phone?: string
  spuId: ApiId
  spuName?: string
  subTitle?: string
  mainImage?: string
  price?: number
  publishStatus?: number
  brandId?: ApiId
  brandName?: string
  categoryId?: ApiId
  categoryName?: string
  createTime?: string
}

export function getFavoritePage(params: FavoriteQuery) {
  return request.get<ApiResult<PageResult<FavoriteRecord>>>('/api/admin/member/favorite/page', { params })
}

export function deleteFavorite(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/member/favorite/${id}`)
}

export function batchDeleteFavorites(ids: ApiId[]) {
  return request.delete<ApiResult<void>>('/api/admin/member/favorite', { data: { ids } })
}
