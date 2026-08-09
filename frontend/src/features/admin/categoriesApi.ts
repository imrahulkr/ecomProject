import { api } from '@/lib/axios'
import type { CategoryDTO } from '@/api/types'

export function createCategory(payload: { categoryName: string }) {
  return api.post<CategoryDTO>('/api/admin/categories', payload).then((res) => res.data)
}

export function updateCategory(categoryId: number, payload: { categoryName: string }) {
  return api
    .put<CategoryDTO>(`/api/admin/categories/${categoryId}`, payload)
    .then((res) => res.data)
}

export function deleteCategory(categoryId: number) {
  return api.delete<CategoryDTO>(`/api/admin/categories/${categoryId}`).then((res) => res.data)
}
