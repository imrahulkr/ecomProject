import { useQuery } from '@tanstack/react-query'

import { getCategories } from '@/features/category/api'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => getCategories({ pageSize: 100 }),
  })
}
