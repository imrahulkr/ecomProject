import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { getUsers, grantRole, revokeRole } from '@/features/admin/usersApi'
import { getErrorMessage } from '@/lib/errors'

const ADMIN_USERS_KEY = ['admin-users']

export function useAdminUsers(pageNumber: number) {
  return useQuery({
    queryKey: [...ADMIN_USERS_KEY, pageNumber],
    queryFn: () => getUsers(pageNumber),
  })
}

export function useGrantRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: string }) => grantRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_USERS_KEY })
      toast.success('Role granted')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not grant role')),
  })
}

export function useRevokeRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: string }) => revokeRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_USERS_KEY })
      toast.success('Role revoked')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not revoke role')),
  })
}
