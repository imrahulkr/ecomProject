import { api } from '@/lib/axios'
import type { CategoryResponse } from '@/api/types'

export interface GetCategoriesParams {
  pageNumber?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export function getCategories(params: GetCategoriesParams = {}) {
  return api.get<CategoryResponse>('/api/public/categories', { params }).then((res) => res.data)
}
