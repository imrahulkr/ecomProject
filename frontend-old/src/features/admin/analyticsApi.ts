import { api } from '@/lib/axios'
import type { AnalyticsResponse } from '@/api/types'

export function getAnalytics() {
  return api.get<AnalyticsResponse>('/api/admin/app/analytics').then((res) => res.data)
}
