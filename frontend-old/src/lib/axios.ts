import axios, { type InternalAxiosRequestConfig } from 'axios'

import { useAuthStore } from '@/features/auth/store'
import type { AuthResponse } from '@/api/types'

declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    _retry?: boolean
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
})

// Plain client (no interceptors) for the refresh call itself -- routing it through `api`
// would re-trigger the response interceptor below and recurse.
const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

/** De-duplicates concurrent refresh attempts -- multiple requests failing with 401 at once
 * (or React StrictMode double-invoking an effect that calls this) should trigger a single
 * POST /refresh_secure, not one per caller. Exported so useBootstrapAuth shares this same
 * in-flight promise instead of issuing its own separate refresh_secure call. */
export function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post<AuthResponse>('/api/auth/refresh_secure')
      .then(({ data }) => {
        if (!data.accessToken) return null
        useAuthStore.getState().setAuth(data.accessToken, data.user)
        return data.accessToken
      })
      .catch(() => {
        useAuthStore.getState().clearAuth()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig | undefined
    const isAuthEndpoint = originalRequest?.url?.startsWith('/api/auth/')

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true
      const token = await refreshAccessToken()
      if (token) {
        originalRequest.headers.set('Authorization', `Bearer ${token}`)
        return api(originalRequest)
      }
    }

    return Promise.reject(error)
  }
)
