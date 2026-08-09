import { api } from '@/lib/axios'
import type { PageSellerApplicationDTO, SellerApplicationDTO } from '@/api/types'

export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export function getApplications(status: ApplicationStatus, pageNumber: number) {
  return api
    .get<PageSellerApplicationDTO>('/api/admin/seller-applications', {
      params: { status, pageNumber },
    })
    .then((res) => res.data)
}

export function approveApplication(applicationId: number) {
  return api
    .put<SellerApplicationDTO>(`/api/admin/seller-applications/${applicationId}/approve`)
    .then((res) => res.data)
}

export function rejectApplication(applicationId: number, reason: string) {
  return api
    .put<SellerApplicationDTO>(`/api/admin/seller-applications/${applicationId}/reject`, { reason })
    .then((res) => res.data)
}
