import { useSearchParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/features/auth/store'
import { useAdminUsers, useGrantRole, useRevokeRole } from '@/features/admin/usersHooks'
import type { AdminUserSummaryDTO } from '@/api/types'

const ALL_ROLES = ['ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN']

function UserRow({ user }: { user: AdminUserSummaryDTO }) {
  const currentUserId = useAuthStore((s) => s.user?.id)
  const grantMutation = useGrantRole()
  const revokeMutation = useRevokeRole()
  const isSelf = currentUserId != null && String(user.userId) === currentUserId

  return (
    <div className="flex flex-wrap items-center gap-4 rounded-xl border border-border p-4">
      <div className="flex flex-1 flex-col gap-1 text-sm">
        <span className="font-medium">{user.name ?? user.username}</span>
        <span className="text-muted-foreground">{user.email}</span>
        {!user.enabled && <span className="text-xs text-destructive">Not enabled</span>}
      </div>
      <div className="flex flex-wrap gap-2">
        {ALL_ROLES.map((role) => {
          const hasRole = user.roles?.includes(role)
          const disableRevoke = role === 'ROLE_ADMIN' && isSelf
          return (
            <Button
              key={role}
              size="sm"
              variant={hasRole ? 'default' : 'outline'}
              disabled={
                (hasRole ? revokeMutation.isPending : grantMutation.isPending) ||
                (hasRole && disableRevoke)
              }
              title={hasRole && disableRevoke ? 'You cannot revoke your own admin role' : undefined}
              onClick={() => {
                if (!user.userId) return
                if (hasRole) {
                  revokeMutation.mutate({ userId: user.userId, role })
                } else {
                  grantMutation.mutate({ userId: user.userId, role })
                }
              }}
            >
              {role.replace('ROLE_', '')}
            </Button>
          )
        })}
      </div>
    </div>
  )
}

export function AdminUsersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const { data, isPending, isError } = useAdminUsers(pageNumber)

  function goToPage(next: number) {
    const params = new URLSearchParams(searchParams)
    params.set('pageNumber', String(next))
    setSearchParams(params)
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Users</h1>

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Couldn't load users. Please try again.</p>}

      {data && (data.content?.length ?? 0) === 0 && (
        <p className="text-sm text-muted-foreground">No users found.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {data.content!.map((user) => (
              <UserRow key={user.userId} user={user} />
            ))}
          </div>

          <div className="flex items-center justify-center gap-3">
            <Button variant="outline" disabled={pageNumber === 0} onClick={() => goToPage(pageNumber - 1)}>
              Previous
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {pageNumber + 1} of {Math.max(data.totalPages ?? 1, 1)}
            </span>
            <Button variant="outline" disabled={data.last} onClick={() => goToPage(pageNumber + 1)}>
              Next
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
