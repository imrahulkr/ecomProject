import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useChangePassword } from '@/features/auth/hooks'

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  })

type ChangePasswordValues = z.infer<typeof changePasswordSchema>

export function ChangePasswordForm() {
  const changePassword = useChangePassword()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  function onSubmit(values: ChangePasswordValues) {
    changePassword.mutate(
      { currentPassword: values.currentPassword, newPassword: values.newPassword },
      { onSuccess: () => reset() }
    )
  }

  const fields: { name: keyof ChangePasswordValues; label: string; autoComplete: string }[] = [
    { name: 'currentPassword', label: 'Current password', autoComplete: 'current-password' },
    { name: 'newPassword', label: 'New password', autoComplete: 'new-password' },
    { name: 'confirmPassword', label: 'Confirm new password', autoComplete: 'new-password' },
  ]

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-3">
      {fields.map(({ name, label, autoComplete }) => (
        <div key={name} className="flex flex-col gap-1">
          <label htmlFor={name} className="text-sm font-medium">
            {label}
          </label>
          <Input
            id={name}
            type="password"
            autoComplete={autoComplete}
            aria-invalid={!!errors[name]}
            {...register(name)}
          />
          {errors[name] && <p className="text-xs text-destructive">{errors[name]?.message}</p>}
        </div>
      ))}

      <Button type="submit" disabled={changePassword.isPending} className="mt-1 self-start">
        {changePassword.isPending ? 'Saving…' : 'Change password'}
      </Button>
    </form>
  )
}
