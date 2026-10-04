import { NavLink, Outlet } from 'react-router-dom'

import { cn } from '@/lib/utils'

const TABS = [
  { to: '/seller/products', label: 'Products' },
  { to: '/seller/orders', label: 'Orders' },
]

export function SellerLayout() {
  return (
    <div className="flex flex-col gap-6">
      <nav className="flex gap-1 border-b border-border">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) =>
              cn(
                'border-b-2 px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              )
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}
