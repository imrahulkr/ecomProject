import { create } from 'zustand'

import { decodeJwt } from '@/lib/jwt'
import type { UserSummary } from '@/api/types'

interface AuthState {
  accessToken: string | null
  user: UserSummary | null
  roles: string[]
  /** True until the initial silent-refresh (session restore) attempt has finished. */
  isBootstrapping: boolean
  setAuth: (accessToken: string, user?: UserSummary | null) => void
  clearAuth: () => void
  finishBootstrap: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  roles: [],
  isBootstrapping: true,
  setAuth: (accessToken, user) =>
    set({
      accessToken,
      user: user ?? null,
      roles: decodeJwt(accessToken)?.roles ?? [],
    }),
  clearAuth: () => set({ accessToken: null, user: null, roles: [] }),
  finishBootstrap: () => set({ isBootstrapping: false }),
}))
