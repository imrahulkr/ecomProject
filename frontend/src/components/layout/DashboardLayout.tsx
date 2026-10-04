import { Suspense, useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router";
import { ArrowLeft, ChartColumn, ClipboardList, FolderTree, LayoutDashboard, LogOut, Menu, MessageSquare, Package, Store, Ticket, Users, Wallet, X } from "lucide-react";
import { cn } from "@/lib/cn";
import { useAuthStore } from "@/store/auth";
import { PageLoader } from "@/components/ui/Feedback";
import { Logo } from "./Logo";
import { useLogout } from "./useLogout";

const nav = {
  seller: [
    { to: "/seller/dashboard", label: "Overview", icon: LayoutDashboard },
    { to: "/seller/products", label: "Products", icon: Package },
    { to: "/seller/orders", label: "Orders", icon: ClipboardList },
    { to: "/seller/reviews", label: "Reviews", icon: MessageSquare },
    { to: "/seller/payouts", label: "Payouts", icon: Wallet },
  ],
  admin: [
    { to: "/admin", label: "Analytics", icon: ChartColumn, end: true },
    { to: "/admin/orders", label: "Orders", icon: ClipboardList },
    { to: "/admin/products", label: "Products", icon: Package },
    { to: "/admin/categories", label: "Categories", icon: FolderTree },
    { to: "/admin/users", label: "Users & roles", icon: Users },
    { to: "/admin/seller-applications", label: "Seller applications", icon: Store },
    { to: "/admin/coupons", label: "Coupons", icon: Ticket },
    { to: "/admin/payouts", label: "Seller payouts", icon: Wallet },
    { to: "/admin/reviews", label: "Review moderation", icon: MessageSquare },
  ],
};

export function DashboardLayout({ kind }: { kind: "seller" | "admin" }) {
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  useEffect(() => setOpen(false), [location.pathname]);

  const sidebar = (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center justify-between border-b border-slate-100 px-5">
        <Logo to={kind === "admin" ? "/admin" : "/seller/dashboard"} />
        <span
          className={cn(
            "rounded-md px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider",
            kind === "admin" ? "bg-slate-900 text-white" : "bg-amber-100 text-amber-800",
          )}
        >
          {kind === "admin" ? "Admin" : "Seller"}
        </span>
      </div>
      <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
        {nav[kind].map(({ to, label, icon: Icon, ...rest }) => (
          <NavLink
            key={to}
            to={to}
            end={"end" in rest ? rest.end : false}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                isActive ? "bg-brand-600 text-white shadow-sm" : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="space-y-1 border-t border-slate-100 p-3">
        <Link to="/" className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">
          <ArrowLeft className="h-4 w-4" /> Back to store
        </Link>
        <button
          onClick={logout}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-rose-50 hover:text-rose-700"
        >
          <LogOut className="h-4 w-4" /> Sign out
        </button>
        <div className="mt-2 rounded-lg bg-slate-50 px-3 py-2.5">
          <p className="truncate text-sm font-semibold text-slate-900">{user?.name || user?.username}</p>
          <p className="truncate text-xs text-slate-500">{user?.email}</p>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-canvas">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-200 bg-white lg:block">{sidebar}</aside>

      {open && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 animate-fade-in bg-slate-950/40" onClick={() => setOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-72 animate-pop-in bg-white shadow-pop">
            <button className="absolute right-3 top-4 rounded-lg p-1.5 hover:bg-slate-100" onClick={() => setOpen(false)} aria-label="Close menu">
              <X className="h-5 w-5" />
            </button>
            {sidebar}
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur lg:hidden">
          <button className="rounded-lg p-2 hover:bg-slate-100" onClick={() => setOpen(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>
          <Logo />
        </header>
        <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <Suspense fallback={<PageLoader />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  );
}
