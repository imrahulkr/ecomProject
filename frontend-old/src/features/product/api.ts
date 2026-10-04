import { api } from '@/lib/axios'
import type { ProductDTO, ProductResponse } from '@/api/types'

export interface GetProductsParams {
  keyword?: string
  category?: string
  pageNumber?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export function getProducts(params: GetProductsParams = {}) {
  return api.get<ProductResponse>('/api/public/products', { params }).then((res) => res.data)
}

export function getProductById(productId: number | string) {
  return api.get<ProductDTO>(`/api/public/products/${productId}`).then((res) => res.data)
}
