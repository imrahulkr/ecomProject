import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import {
  createSellerProduct,
  deleteSellerProduct,
  getSellerProducts,
  updateSellerProduct,
  uploadSellerProductImage,
  type GetSellerProductsParams,
} from '@/features/seller/productsApi'
import { getErrorMessage } from '@/lib/errors'
import type { ProductDTO } from '@/api/types'

const SELLER_PRODUCTS_KEY = ['seller-products']

export function useSellerProducts(params: GetSellerProductsParams) {
  return useQuery({
    queryKey: [...SELLER_PRODUCTS_KEY, params],
    queryFn: () => getSellerProducts(params),
  })
}

export function useCreateSellerProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ categoryId, payload }: { categoryId: number; payload: ProductDTO }) =>
      createSellerProduct(categoryId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCTS_KEY })
      toast.success('Product created')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not create product')),
  })
}

export function useUpdateSellerProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, payload }: { productId: number; payload: ProductDTO }) =>
      updateSellerProduct(productId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCTS_KEY })
      toast.success('Product updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not update product')),
  })
}

export function useDeleteSellerProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (productId: number) => deleteSellerProduct(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCTS_KEY })
      toast.success('Product deleted')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not delete product')),
  })
}

export function useUploadSellerProductImage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, file }: { productId: number; file: File }) =>
      uploadSellerProductImage(productId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCTS_KEY })
      toast.success('Image uploaded')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not upload image')),
  })
}
