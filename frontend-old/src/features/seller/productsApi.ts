import { api } from '@/lib/axios'
import type { ProductDTO, ProductResponse } from '@/api/types'

export interface GetSellerProductsParams {
  pageNumber?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export function getSellerProducts(params: GetSellerProductsParams = {}) {
  return api.get<ProductResponse>('/api/seller/products', { params }).then((res) => res.data)
}

export function createSellerProduct(categoryId: number, payload: ProductDTO) {
  return api
    .post<ProductDTO>(`/api/seller/categories/${categoryId}/products`, payload)
    .then((res) => res.data)
}

export function updateSellerProduct(productId: number, payload: ProductDTO) {
  return api.put<ProductDTO>(`/api/seller/products/${productId}`, payload).then((res) => res.data)
}

export function deleteSellerProduct(productId: number) {
  return api.delete<ProductDTO>(`/api/seller/products/${productId}`).then((res) => res.data)
}

export function uploadSellerProductImage(productId: number, file: File) {
  const formData = new FormData()
  formData.append('Image', file)
  return api
    .put<ProductDTO>(`/api/seller/products/${productId}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data)
}
