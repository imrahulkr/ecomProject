import { Navigate, Outlet } from 'react-router-dom'

import { useAuthStore } from '@/features/auth/store'

/** Nest inside <ProtectedRoute> -- this only checks role, not whether the user is logged in. */
export function RoleRoute({ allowedRoles }: { allowedRoles: string[] }) {
  const roles = useAuthStore((s) => s.roles)
  const hasAccess = roles.some((role) => allowedRoles.includes(role))

  if (!hasAccess) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
