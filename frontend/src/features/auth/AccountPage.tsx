import { useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { logout } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'

export function AccountPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const roles = useAuthStore((s) => s.roles)

  async function handleLogout() {
    try {
      await logout()
    } finally {
      useAuthStore.getState().clearAuth()
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-3">
      <h1 className="text-xl font-semibold">Account</h1>
      <p className="text-sm text-muted-foreground">{user?.email}</p>
      <p className="text-sm text-muted-foreground">Roles: {roles.join(', ') || 'none'}</p>
      <Button onClick={handleLogout}>Log out</Button>
    </div>
  )
}
