import { api } from '@/lib/axios'
import type { AdminUserSummaryDTO, PageAdminUserSummaryDTO } from '@/api/types'

export function getUsers(pageNumber: number) {
  return api
    .get<PageAdminUserSummaryDTO>('/api/admin/users', { params: { pageNumber } })
    .then((res) => res.data)
}

export function grantRole(userId: number, role: string) {
  return api
    .put<AdminUserSummaryDTO>(`/api/admin/users/${userId}/roles/${role}`)
    .then((res) => res.data)
}

export function revokeRole(userId: number, role: string) {
  return api
    .delete<AdminUserSummaryDTO>(`/api/admin/users/${userId}/roles/${role}`)
    .then((res) => res.data)
}
