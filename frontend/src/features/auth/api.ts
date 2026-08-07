import { api } from '@/lib/axios'
import type { AuthResponse, LoginRequest, SignupRequest } from '@/api/types'

export function login(payload: LoginRequest) {
  return api.post<AuthResponse>('/api/auth/login', payload).then((res) => res.data)
}

export function signup(payload: SignupRequest) {
  return api
    .post<{ message: string }>('/api/auth/signup', payload)
    .then((res) => res.data)
}

export function logout() {
  return api.post('/api/auth/logout_secure')
}

export function refresh() {
  return api.post<AuthResponse>('/api/auth/refresh_secure').then((res) => res.data)
}
