import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'

import { buttonVariants } from '@/components/ui/button'
import { verifyEmail } from '@/features/auth/api'
import { getErrorMessage } from '@/lib/errors'

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const { data, error, isPending } = useQuery({
    queryKey: ['verifyEmail', token],
    queryFn: () => verifyEmail(token!),
    enabled: !!token,
    retry: false,
  })

  let title: string
  let message: string

  if (!token) {
    title = 'Invalid link'
    message = 'This verification link is missing its token.'
  } else if (isPending) {
    title = 'Verifying…'
    message = 'Hang on while we verify your account.'
  } else if (error) {
    title = 'Verification failed'
    message = getErrorMessage(error, 'This verification link is invalid or has expired.')
  } else {
    title = 'Email verified'
    message = data?.message ?? 'Your account is verified. You can now log in.'
  }

  return (
    <div className="flex min-h-svh items-center justify-center">
      <div className="flex w-full max-w-sm flex-col gap-3 text-center">
        <h1 className="text-xl font-semibold">{title}</h1>
        <p className="text-sm text-muted-foreground">{message}</p>
        {!isPending && (
          <Link to="/login" className={buttonVariants({ variant: 'outline' })}>
            Back to login
          </Link>
        )}
      </div>
    </div>
  )
}
