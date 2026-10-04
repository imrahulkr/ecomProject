import { Link, useNavigate } from 'react-router-dom'

import { Button, buttonVariants } from '@/components/ui/button'
import { logout } from '@/features/auth/api'
import { ChangePasswordForm } from '@/features/auth/ChangePasswordForm'
import { LinkedAccountsSection } from '@/features/auth/LinkedAccountsSection'
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
    <div className="mx-auto flex max-w-md flex-col gap-8">
      <div className="flex flex-col items-center gap-3 text-center">
        <h1 className="text-xl font-semibold">Account</h1>
        <p className="text-sm text-muted-foreground">{user?.email}</p>
        <p className="text-sm text-muted-foreground">Roles: {roles.join(', ') || 'none'}</p>
        <div className="flex flex-wrap justify-center gap-2">
          <Link to="/orders" className={buttonVariants({ variant: 'outline' })}>
            Order history
          </Link>
          <Link to="/addresses" className={buttonVariants({ variant: 'outline' })}>
            Manage addresses
          </Link>
          {roles.includes('ROLE_SELLER') ? (
            <Link to="/seller/products" className={buttonVariants({ variant: 'outline' })}>
              Seller dashboard
            </Link>
          ) : (
            <Link to="/sell" className={buttonVariants({ variant: 'outline' })}>
              Become a seller
            </Link>
          )}
          {roles.includes('ROLE_ADMIN') && (
            <Link to="/admin" className={buttonVariants({ variant: 'outline' })}>
              Admin dashboard
            </Link>
          )}
        </div>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Linked accounts</h2>
        <LinkedAccountsSection />
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Change password</h2>
        <ChangePasswordForm />
      </section>

      <Button onClick={handleLogout} variant="outline" className="self-center">
        Log out
      </Button>
    </div>
  )
}
