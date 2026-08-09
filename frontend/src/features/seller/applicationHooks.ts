import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { applyAsSeller, getMySellerApplications } from '@/features/seller/applicationApi'
import { getErrorMessage } from '@/lib/errors'
import type { ApplySellerRequest } from '@/api/types'

const MY_APPLICATIONS_KEY = ['seller-applications', 'me']

export function useMySellerApplications() {
  return useQuery({ queryKey: MY_APPLICATIONS_KEY, queryFn: getMySellerApplications })
}

export function useApplyAsSeller() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: ApplySellerRequest) => applyAsSeller(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: MY_APPLICATIONS_KEY })
      toast.success('Application submitted')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not submit application')),
  })
}
