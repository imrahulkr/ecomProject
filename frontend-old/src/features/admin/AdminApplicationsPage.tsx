import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useAdminApplications,
  useApproveApplication,
  useRejectApplication,
} from '@/features/admin/applicationsHooks'
import type { ApplicationStatus } from '@/features/admin/applicationsApi'
import type { SellerApplicationDTO } from '@/api/types'

const STATUS_TABS: ApplicationStatus[] = ['PENDING', 'APPROVED', 'REJECTED']

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  APPROVED: 'default',
  PENDING: 'secondary',
  REJECTED: 'destructive',
}

function ApplicationRow({ application }: { application: SellerApplicationDTO }) {
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const approveMutation = useApproveApplication()
  const rejectMutation = useRejectApplication()

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-border p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="text-sm">
          <p className="font-medium">{application.businessName}</p>
          <p className="text-muted-foreground">User #{application.userId}</p>
        </div>
        <Badge variant={STATUS_VARIANT[application.status ?? ''] ?? 'outline'}>
          {application.status}
        </Badge>
      </div>

      {application.businessDescription && (
        <p className="text-sm text-muted-foreground">{application.businessDescription}</p>
      )}

      {application.status === 'REJECTED' && application.rejectionReason && (
        <p className="text-sm text-destructive">Reason: {application.rejectionReason}</p>
      )}

      {application.status === 'PENDING' && (
        <div className="flex flex-col gap-2">
          <div className="flex gap-2">
            <Button
              size="sm"
              disabled={approveMutation.isPending}
              onClick={() => application.id && approveMutation.mutate(application.id)}
            >
              Approve
            </Button>
            <Button
              size="sm"
              variant="outline"
              className="text-destructive"
              disabled={rejectMutation.isPending}
              onClick={() => setRejecting((v) => !v)}
            >
              Reject
            </Button>
          </div>
          {rejecting && (
            <div className="flex flex-wrap items-end gap-2">
              <div className="flex flex-1 flex-col gap-1">
                <label className="text-xs text-muted-foreground">Rejection reason</label>
                <Input
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  aria-label="Rejection reason"
                  className="h-8"
                />
              </div>
              <Button
                size="sm"
                variant="outline"
                className="text-destructive"
                disabled={!reason.trim() || rejectMutation.isPending}
                onClick={() =>
                  application.id &&
                  rejectMutation.mutate(
                    { applicationId: application.id, reason },
                    { onSuccess: () => setRejecting(false) }
                  )
                }
              >
                Confirm reject
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export function AdminApplicationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = (searchParams.get('status') as ApplicationStatus | null) ?? 'PENDING'
  const pageNumber = Number(searchParams.get('pageNumber') ?? '0')
  const { data, isPending, isError } = useAdminApplications(status, pageNumber)

  function setStatus(next: ApplicationStatus) {
    const params = new URLSearchParams(searchParams)
    params.set('status', next)
    params.set('pageNumber', '0')
    setSearchParams(params)
  }

  function goToPage(next: number) {
    const params = new URLSearchParams(searchParams)
    params.set('pageNumber', String(next))
    setSearchParams(params)
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Seller applications</h1>

      <div className="flex gap-2">
        {STATUS_TABS.map((tab) => (
          <Button
            key={tab}
            size="sm"
            variant={status === tab ? 'default' : 'outline'}
            onClick={() => setStatus(tab)}
          >
            {tab}
          </Button>
        ))}
      </div>

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Couldn't load applications. Please try again.</p>
      )}

      {data && (data.content?.length ?? 0) === 0 && (
        <p className="text-sm text-muted-foreground">No {status.toLowerCase()} applications.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {data.content!.map((application) => (
              <ApplicationRow key={application.id} application={application} />
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
