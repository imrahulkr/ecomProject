import { api } from '@/lib/axios'
import type {
  AuthResponse,
  ForgotPasswordRequestDTO,
  LinkedAccounts,
  LoginRequest,
  PasswordChangeRequestDTO,
  ResetPasswordRequestDTO,
  SignupRequest,
} from '@/api/types'

// Hand-built Map response from AuthController, not a generated OpenAPI schema.
export interface ResetTokenValidation {
  valid: boolean
  reason?: string
}

export function login(payload: LoginRequest) {
  return api.post<AuthResponse>('/api/auth/login', payload).then((res) => res.data)
}

export function signup(payload: SignupRequest) {
  return api
    .post<{ message: string }>('/api/auth/signup', payload)
    .then((res) => res.data)
}

export function verifyEmail(token: string) {
  return api
    .get<{ message: string }>('/api/auth/verif-yemail', { params: { token } })
    .then((res) => res.data)
}

export function logout() {
  return api.post('/api/auth/logout_secure')
}

export function refresh() {
  return api.post<AuthResponse>('/api/auth/refresh_secure').then((res) => res.data)
}

export function getLinkedAccounts() {
  return api.get<LinkedAccounts>('/api/account/linked-accounts').then((res) => res.data)
}

export function unlinkProvider(provider: string) {
  return api.delete(`/api/account/link/${provider}`)
}

export function changePassword(payload: PasswordChangeRequestDTO) {
  return api.post<{ message: string }>('/api/auth/change-password', payload).then((res) => res.data)
}

export function forgotPassword(payload: ForgotPasswordRequestDTO) {
  return api.post<{ message: string }>('/api/auth/forgot-password', payload).then((res) => res.data)
}

export function validateResetToken(token: string) {
  return api
    .get<ResetTokenValidation>('/api/auth/reset-password/validate', { params: { token } })
    .then((res) => res.data)
}

export function resetPassword(payload: ResetPasswordRequestDTO) {
  return api.post<{ message: string }>('/api/auth/reset-password', payload).then((res) => res.data)
}
