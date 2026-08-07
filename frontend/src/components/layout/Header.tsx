import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuthStore } from '@/features/auth/store'

export function Header() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '')
  const user = useAuthStore((s) => s.user)

  function handleSearch(event: React.FormEvent) {
    event.preventDefault()
    const params = new URLSearchParams()
    if (keyword.trim()) params.set('keyword', keyword.trim())
    navigate(`/?${params.toString()}`)
  }

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-background">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link to="/" className="shrink-0 text-lg font-semibold">
          Ecom
        </Link>

        <form onSubmit={handleSearch} className="flex-1">
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Search products…"
            aria-label="Search products"
          />
        </form>

        <nav className="flex shrink-0 items-center gap-2">
          {user ? (
            <Link to="/account" className={buttonVariants({ variant: 'outline' })}>
              {user.email}
            </Link>
          ) : (
            <Link to="/login" className={buttonVariants({ variant: 'outline' })}>
              Log in
            </Link>
          )}
        </nav>
      </div>
    </header>
  )
}
