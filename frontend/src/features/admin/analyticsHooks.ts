import { useQuery } from '@tanstack/react-query'

import { getAnalytics } from '@/features/admin/analyticsApi'

export function useAnalytics() {
  return useQuery({ queryKey: ['admin-analytics'], queryFn: getAnalytics })
}
