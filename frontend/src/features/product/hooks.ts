import { useQuery } from '@tanstack/react-query'

import { getProductById, getProducts, type GetProductsParams } from '@/features/product/api'

export function useProducts(params: GetProductsParams) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => getProducts(params),
  })
}

export function useProduct(productId: number | string | undefined) {
  return useQuery({
    queryKey: ['product', productId],
    queryFn: () => getProductById(productId!),
    enabled: productId !== undefined,
  })
}
