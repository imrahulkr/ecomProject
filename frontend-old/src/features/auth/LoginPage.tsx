import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { login } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'
import { getErrorMessage } from '@/lib/errors'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setIsSubmitting(true)
    try {
      const data = await login({ email, username, password })
      if (data.accessToken) {
        useAuthStore.getState().setAuth(data.accessToken, data.user)
        toast.success('Logged in')
        const from = (location.state as { from?: Location })?.from?.pathname ?? '/'
        navigate(from, { replace: true })
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Login failed'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-svh items-center justify-center">
      <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-3">
        <h1 className="text-xl font-semibold">Log in</h1>
        <input
          className="rounded-lg border border-border bg-background px-2.5 py-1.5 text-sm outline-none focus-visible:border-ring"
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          required
        />
        <input
          className="rounded-lg border border-border bg-background px-2.5 py-1.5 text-sm outline-none focus-visible:border-ring"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
          required
        />
        <input
          className="rounded-lg border border-border bg-background px-2.5 py-1.5 text-sm outline-none focus-visible:border-ring"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
        <Link
          to="/forgot-password"
          className="self-end text-sm text-muted-foreground underline-offset-4 hover:underline"
        >
          Forgot password?
        </Link>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Logging in…' : 'Log in'}
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Don&apos;t have an account?{' '}
          <Link to="/signup" className="font-medium text-foreground underline-offset-4 hover:underline">
            Sign up
          </Link>
        </p>
      </form>
    </div>
  )
}
