import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { createCategory, deleteCategory, updateCategory } from '@/features/admin/categoriesApi'
import { getErrorMessage } from '@/lib/errors'

const CATEGORIES_KEY = ['categories']

export function useCreateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { categoryName: string }) => createCategory(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_KEY })
      toast.success('Category created')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not create category')),
  })
}

export function useUpdateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ categoryId, payload }: { categoryId: number; payload: { categoryName: string } }) =>
      updateCategory(categoryId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_KEY })
      toast.success('Category updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not update category')),
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (categoryId: number) => deleteCategory(categoryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_KEY })
      toast.success('Category deleted')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not delete category')),
  })
}
