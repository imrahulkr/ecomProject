import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { buttonVariants } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/features/auth/store'
import { useApplyAsSeller, useMySellerApplications } from '@/features/seller/applicationHooks'
import { SellerApplyForm } from '@/features/seller/SellerApplyForm'

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  APPROVED: 'default',
  PENDING: 'secondary',
  REJECTED: 'destructive',
}

export function SellerApplyPage() {
  const roles = useAuthStore((s) => s.roles)
  const isSeller = roles.includes('ROLE_SELLER')
  const { data: applications, isPending } = useMySellerApplications()
  const applyMutation = useApplyAsSeller()

  const hasPendingApplication = applications?.some((a) => a.status === 'PENDING')

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6">
      <h1 className="text-xl font-semibold">Become a seller</h1>

      {isSeller && (
        <div className="flex flex-col gap-2 rounded-xl border border-border p-4 text-sm">
          <p>You're already an approved seller.</p>
          <Link to="/seller/products" className={buttonVariants({ variant: 'outline' })}>
            Go to seller dashboard
          </Link>
        </div>
      )}

      {isPending && (
        <div className="flex flex-col gap-3">
          <Skeleton className="h-16 w-full" />
        </div>
      )}

      {!isPending && applications && applications.length > 0 && (
        <div className="flex flex-col gap-2">
          <h2 className="text-sm font-semibold">Your applications</h2>
          {applications.map((application) => (
            <div
              key={application.id}
              className="flex flex-col gap-1 rounded-xl border border-border p-3 text-sm"
            >
              <div className="flex items-center justify-between">
                <span className="font-medium">{application.businessName}</span>
                <Badge variant={STATUS_VARIANT[application.status ?? ''] ?? 'outline'}>
                  {application.status}
                </Badge>
              </div>
              {application.businessDescription && (
                <p className="text-muted-foreground">{application.businessDescription}</p>
              )}
              {application.status === 'REJECTED' && application.rejectionReason && (
                <p className="text-destructive">Reason: {application.rejectionReason}</p>
              )}
            </div>
          ))}
        </div>
      )}

      {!isSeller && !isPending && !hasPendingApplication && (
        <SellerApplyForm
          onSubmit={(values) => applyMutation.mutate(values)}
          isSubmitting={applyMutation.isPending}
        />
      )}

      {!isSeller && !isPending && hasPendingApplication && (
        <p className="text-sm text-muted-foreground">
          Your application is pending review. We'll let you know once it's decided.
        </p>
      )}
    </div>
  )
}
