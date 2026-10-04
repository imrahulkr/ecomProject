import { api } from '@/lib/axios'
import type { AddressDTO } from '@/api/types'

export function getAddresses() {
  return api.get<AddressDTO[]>('/api/users/addresses').then((res) => res.data)
}

export function createAddress(payload: AddressDTO) {
  return api.post<AddressDTO>('/api/addresses', payload).then((res) => res.data)
}

export function updateAddress(addressId: number, payload: AddressDTO) {
  return api.put<AddressDTO>(`/api/addresses/${addressId}`, payload).then((res) => res.data)
}

export function deleteAddress(addressId: number) {
  return api.delete<string>(`/api/addresses/${addressId}`).then((res) => res.data)
}
