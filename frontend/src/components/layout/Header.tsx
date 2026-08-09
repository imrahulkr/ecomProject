import { ShoppingCart } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useCart } from '@/features/cart/hooks'
import { useAuthStore } from '@/features/auth/store'

export function Header() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '')
  const user = useAuthStore((s) => s.user)
  const { data: cart } = useCart()
  const itemCount = cart?.products?.reduce((sum, p) => sum + (p.quantity ?? 0), 0) ?? 0

  function handleSearch(event: React.FormEvent) {
    event.preventDefault()
    const params = new URLSearchParams()
    if (keyword.trim()) params.set('keyword', keyword.trim())
    navigate(`/products?${params.toString()}`)
  }

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-background">
      <div className="mx-auto flex max-w-6xl items-center gap-2 px-4 py-3 sm:gap-4">
        <Link to="/" className="shrink-0 text-lg font-semibold">
          Ecom
        </Link>

        <Link
          to="/products"
          className={buttonVariants({ variant: 'ghost', className: 'shrink-0' })}
        >
          Products
        </Link>

        <form onSubmit={handleSearch} className="min-w-0 flex-1">
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Search products…"
            aria-label="Search products"
          />
        </form>

        <nav className="flex shrink-0 items-center gap-2">
          {user && (
            <Link
              to="/cart"
              className={buttonVariants({ variant: 'outline', size: 'icon' })}
              aria-label="Cart"
            >
              <span className="relative">
                <ShoppingCart className="size-4" />
                {itemCount > 0 && (
                  <span className="absolute -top-2 -right-2 flex size-4 items-center justify-center rounded-full bg-primary text-[10px] text-primary-foreground">
                    {itemCount}
                  </span>
                )}
              </span>
            </Link>
          )}
          {user ? (
            <Link
              to="/account"
              className={buttonVariants({ variant: 'outline', className: 'max-w-24 truncate sm:max-w-none' })}
              title={user.email}
            >
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
