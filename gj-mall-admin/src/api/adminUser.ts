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

export interface AdminUserQuery {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface AdminRoleQuery {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface AdminRole {
  id: ApiId
  code: string
  name: string
  description?: string
  status?: number
  createTime?: string
  permissionIds?: ApiId[]
}

export interface AdminRolePayload {
  id?: ApiId
  code: string
  name: string
  description?: string
  status?: number
  permissionIds?: ApiId[]
}

export interface AdminPermission {
  id: ApiId
  parentId?: ApiId
  code: string
  name: string
  type?: number
  path?: string
  icon?: string
  sort?: number
  children?: AdminPermission[]
}

export interface AdminCurrentPermission {
  permissionCodes: string[]
  menus: AdminPermission[]
}

export interface AdminUserRecord {
  id: ApiId
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  status: number
  createTime?: string
  updateTime?: string
  roleIds?: ApiId[]
  roleNames?: string[]
}

export interface AdminUserPayload {
  id?: ApiId
  username: string
  password?: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  status?: number
  roleIds?: ApiId[]
}

export function getAdminUserPage(params: AdminUserQuery) {
  return request.get<ApiResult<PageResult<AdminUserRecord>>>('/api/admin/sys/user/page', { params })
}

export function getAdminUserDetail(id: ApiId) {
  return request.get<ApiResult<AdminUserRecord>>(`/api/admin/sys/user/${id}`)
}

export function createAdminUser(data: AdminUserPayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/sys/user', data)
}

export function updateAdminUser(data: AdminUserPayload) {
  return request.put<ApiResult<void>>('/api/admin/sys/user', data)
}

export function updateAdminUserStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/sys/user/${id}/status`, null, {
    params: { status },
  })
}

export function resetAdminUserPassword(id: ApiId, password: string) {
  return request.put<ApiResult<void>>(`/api/admin/sys/user/${id}/password`, { password })
}

export function deleteAdminUser(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/sys/user/${id}`)
}

export function getAdminRoles() {
  return request.get<ApiResult<AdminRole[]>>('/api/admin/sys/role/list')
}

export function getAdminRolePage(params: AdminRoleQuery) {
  return request.get<ApiResult<PageResult<AdminRole>>>('/api/admin/sys/role/page', { params })
}

export function getAdminRoleDetail(id: ApiId) {
  return request.get<ApiResult<AdminRole>>(`/api/admin/sys/role/${id}`)
}

export function createAdminRole(data: AdminRolePayload) {
  return request.post<ApiResult<ApiId>>('/api/admin/sys/role', data)
}

export function updateAdminRole(data: AdminRolePayload) {
  return request.put<ApiResult<void>>('/api/admin/sys/role', data)
}

export function updateAdminRoleStatus(id: ApiId, status: number) {
  return request.put<ApiResult<void>>(`/api/admin/sys/role/${id}/status`, null, {
    params: { status },
  })
}

export function deleteAdminRole(id: ApiId) {
  return request.delete<ApiResult<void>>(`/api/admin/sys/role/${id}`)
}

export function updateAdminRolePermissions(id: ApiId, permissionIds: ApiId[]) {
  return request.put<ApiResult<void>>(`/api/admin/sys/role/${id}/permissions`, { permissionIds })
}

export function getAdminPermissionTree() {
  return request.get<ApiResult<AdminPermission[]>>('/api/admin/sys/permission/tree')
}

export function getCurrentAdminPermissions() {
  return request.get<ApiResult<AdminCurrentPermission>>('/api/admin/sys/current-permissions')
}
