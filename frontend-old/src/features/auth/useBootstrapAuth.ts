import { useEffect } from 'react'

import { refreshAccessToken } from '@/lib/axios'
import { useAuthStore } from '@/features/auth/store'

/** Restores a session on page load from the httpOnly refresh cookie, if present. Runs once.
 *
 * Goes through the shared, de-duplicated `refreshAccessToken()` (the same one the 401-retry
 * interceptor uses) rather than calling POST /refresh_secure directly -- React StrictMode
 * double-invokes this effect in dev, and without sharing the in-flight promise, two concurrent
 * refresh_secure calls with the same still-unrotated cookie would race the backend's reuse
 * detection and revoke the whole session. */
export function useBootstrapAuth() {
  useEffect(() => {
    let cancelled = false

    refreshAccessToken().finally(() => {
      if (!cancelled) useAuthStore.getState().finishBootstrap()
    })

    return () => {
      cancelled = true
    }
  }, [])
}
