import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'

import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useForgotPassword } from '@/features/auth/hooks'

const forgotPasswordSchema = z.object({
  email: z.string().email('Enter a valid email address'),
})

type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>

export function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false)
  const forgotPassword = useForgotPassword()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  function onSubmit(values: ForgotPasswordValues) {
    forgotPassword.mutate(values, { onSuccess: () => setSubmitted(true) })
  }

  if (submitted) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <div className="flex w-full max-w-sm flex-col gap-3 text-center">
          <h1 className="text-xl font-semibold">Check your email</h1>
          <p className="text-sm text-muted-foreground">
            If an account exists for that email, we&apos;ve sent a link to reset your password.
          </p>
          <Link to="/login" className={buttonVariants({ variant: 'outline' })}>
            Back to login
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-svh items-center justify-center">
      <form onSubmit={handleSubmit(onSubmit)} className="flex w-full max-w-sm flex-col gap-3">
        <h1 className="text-xl font-semibold">Forgot password</h1>
        <p className="text-sm text-muted-foreground">
          Enter your email and we&apos;ll send you a link to reset your password.
        </p>
        <div className="flex flex-col gap-1">
          <label htmlFor="email" className="text-sm font-medium">
            Email
          </label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            aria-invalid={!!errors.email}
            {...register('email')}
          />
          {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
        </div>

        <Button type="submit" disabled={forgotPassword.isPending}>
          {forgotPassword.isPending ? 'Sending…' : 'Send reset link'}
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Remembered your password?{' '}
          <Link to="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
            Log in
          </Link>
        </p>
      </form>
    </div>
  )
}
