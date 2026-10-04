import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { z } from 'zod'

import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { signup } from '@/features/auth/api'
import { getErrorMessage } from '@/lib/errors'

const signupSchema = z
  .object({
    name: z.string().optional(),
    username: z.string().min(3, 'Username must be at least 3 characters').max(20),
    email: z.string().email('Enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters').max(20),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  })

type SignupValues = z.infer<typeof signupSchema>

export function SignupPage() {
  const [submittedEmail, setSubmittedEmail] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: { name: '', username: '', email: '', password: '', confirmPassword: '' },
  })

  async function onSubmit(values: SignupValues) {
    try {
      await signup({
        name: values.name || undefined,
        username: values.username,
        email: values.email,
        password: values.password,
      })
      setSubmittedEmail(values.email)
    } catch (err) {
      toast.error(getErrorMessage(err, 'Signup failed'))
    }
  }

  if (submittedEmail) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <div className="flex w-full max-w-sm flex-col gap-3 text-center">
          <h1 className="text-xl font-semibold">Check your email</h1>
          <p className="text-sm text-muted-foreground">
            We sent a verification link to <span className="font-medium">{submittedEmail}</span>.
            Verify your account before logging in.
          </p>
          <Link to="/login" className={buttonVariants({ variant: 'outline' })}>
            Back to login
          </Link>
        </div>
      </div>
    )
  }

  const fields: { name: keyof SignupValues; label: string; type: string; autoComplete: string }[] = [
    { name: 'name', label: 'Name', type: 'text', autoComplete: 'name' },
    { name: 'username', label: 'Username', type: 'text', autoComplete: 'username' },
    { name: 'email', label: 'Email', type: 'email', autoComplete: 'email' },
    { name: 'password', label: 'Password', type: 'password', autoComplete: 'new-password' },
    {
      name: 'confirmPassword',
      label: 'Confirm password',
      type: 'password',
      autoComplete: 'new-password',
    },
  ]

  return (
    <div className="flex min-h-svh items-center justify-center">
      <form onSubmit={handleSubmit(onSubmit)} className="flex w-full max-w-sm flex-col gap-3">
        <h1 className="text-xl font-semibold">Create an account</h1>
        {fields.map(({ name, label, type, autoComplete }) => (
          <div key={name} className="flex flex-col gap-1">
            <label htmlFor={name} className="text-sm font-medium">
              {label}
            </label>
            <Input
              id={name}
              type={type}
              autoComplete={autoComplete}
              aria-invalid={!!errors[name]}
              {...register(name)}
            />
            {errors[name] && <p className="text-xs text-destructive">{errors[name]?.message}</p>}
          </div>
        ))}

        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creating account…' : 'Sign up'}
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
            Log in
          </Link>
        </p>
      </form>
    </div>
  )
}
