import { api } from '@/lib/axios'
import type { ApplySellerRequest, SellerApplicationDTO } from '@/api/types'

export function applyAsSeller(payload: ApplySellerRequest) {
  return api.post<SellerApplicationDTO>('/api/seller-applications', payload).then((res) => res.data)
}

export function getMySellerApplications() {
  return api.get<SellerApplicationDTO[]>('/api/seller-applications/me').then((res) => res.data)
}
