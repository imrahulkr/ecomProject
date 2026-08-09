import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import {
  approveApplication,
  getApplications,
  rejectApplication,
  type ApplicationStatus,
} from '@/features/admin/applicationsApi'
import { getErrorMessage } from '@/lib/errors'

const ADMIN_APPLICATIONS_KEY = ['admin-seller-applications']

export function useAdminApplications(status: ApplicationStatus, pageNumber: number) {
  return useQuery({
    queryKey: [...ADMIN_APPLICATIONS_KEY, status, pageNumber],
    queryFn: () => getApplications(status, pageNumber),
  })
}

export function useApproveApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (applicationId: number) => approveApplication(applicationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_APPLICATIONS_KEY })
      toast.success('Application approved')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not approve application')),
  })
}

export function useRejectApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ applicationId, reason }: { applicationId: number; reason: string }) =>
      rejectApplication(applicationId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_APPLICATIONS_KEY })
      toast.success('Application rejected')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not reject application')),
  })
}
