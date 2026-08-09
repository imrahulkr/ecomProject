import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'
import { z } from 'zod'

import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { validateResetToken } from '@/features/auth/api'
import { useResetPassword } from '@/features/auth/hooks'

const resetPasswordSchema = z
  .object({
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  })

type ResetPasswordValues = z.infer<typeof resetPasswordSchema>

function Message({ title, message }: { title: string; message: string }) {
  return (
    <div className="flex min-h-svh items-center justify-center">
      <div className="flex w-full max-w-sm flex-col gap-3 text-center">
        <h1 className="text-xl font-semibold">{title}</h1>
        <p className="text-sm text-muted-foreground">{message}</p>
        <Link to="/login" className={buttonVariants({ variant: 'outline' })}>
          Back to login
        </Link>
      </div>
    </div>
  )
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [done, setDone] = useState(false)

  const { data, error, isPending } = useQuery({
    queryKey: ['validateResetToken', token],
    queryFn: () => validateResetToken(token!),
    enabled: !!token,
    retry: false,
  })

  const resetPassword = useResetPassword()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })

  function onSubmit(values: ResetPasswordValues) {
    resetPassword.mutate(
      { token: token!, newPassword: values.newPassword },
      { onSuccess: () => setDone(true) }
    )
  }

  if (!token) {
    return <Message title="Invalid link" message="This reset link is missing its token." />
  }

  if (done) {
    return (
      <Message
        title="Password reset"
        message="Your password has been reset successfully. You can now log in."
      />
    )
  }

  if (isPending) {
    return <Message title="Checking link…" message="Hang on while we verify your reset link." />
  }

  if (error || !data?.valid) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <div className="flex w-full max-w-sm flex-col gap-3 text-center">
          <h1 className="text-xl font-semibold">Invalid link</h1>
          <p className="text-sm text-muted-foreground">
            {data?.reason ?? 'This reset link is invalid or has expired.'}
          </p>
          <Link to="/forgot-password" className={buttonVariants({ variant: 'outline' })}>
            Request a new link
          </Link>
        </div>
      </div>
    )
  }

  const fields: { name: keyof ResetPasswordValues; label: string }[] = [
    { name: 'newPassword', label: 'New password' },
    { name: 'confirmPassword', label: 'Confirm new password' },
  ]

  return (
    <div className="flex min-h-svh items-center justify-center">
      <form onSubmit={handleSubmit(onSubmit)} className="flex w-full max-w-sm flex-col gap-3">
        <h1 className="text-xl font-semibold">Reset password</h1>
        {fields.map(({ name, label }) => (
          <div key={name} className="flex flex-col gap-1">
            <label htmlFor={name} className="text-sm font-medium">
              {label}
            </label>
            <Input
              id={name}
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors[name]}
              {...register(name)}
            />
            {errors[name] && <p className="text-xs text-destructive">{errors[name]?.message}</p>}
          </div>
        ))}

        <Button type="submit" disabled={resetPassword.isPending}>
          {resetPassword.isPending ? 'Resetting…' : 'Reset password'}
        </Button>
      </form>
    </div>
  )
}
