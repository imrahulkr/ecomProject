import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import {
  changePassword,
  forgotPassword,
  getLinkedAccounts,
  resetPassword,
  unlinkProvider,
} from '@/features/auth/api'
import { getErrorMessage } from '@/lib/errors'
import { useAuthStore } from '@/features/auth/store'
import type { ForgotPasswordRequestDTO, PasswordChangeRequestDTO, ResetPasswordRequestDTO } from '@/api/types'

const LINKED_ACCOUNTS_KEY = ['linkedAccounts']

export function useLinkedAccounts() {
  const accessToken = useAuthStore((s) => s.accessToken)
  return useQuery({
    queryKey: LINKED_ACCOUNTS_KEY,
    queryFn: getLinkedAccounts,
    enabled: !!accessToken,
  })
}

export function useUnlinkProvider() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (provider: string) => unlinkProvider(provider),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LINKED_ACCOUNTS_KEY })
      toast.success('Account unlinked')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not unlink account')),
  })
}

export function useChangePassword() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: PasswordChangeRequestDTO) => changePassword(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LINKED_ACCOUNTS_KEY })
      toast.success('Password changed successfully')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Could not change password')),
  })
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (payload: ForgotPasswordRequestDTO) => forgotPassword(payload),
    onError: (err) => toast.error(getErrorMessage(err, 'Could not send reset link')),
  })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: (payload: ResetPasswordRequestDTO) => resetPassword(payload),
    onError: (err) => toast.error(getErrorMessage(err, 'Could not reset password')),
  })
}
