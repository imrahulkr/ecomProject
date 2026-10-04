import { Suspense } from "react";
import { NavLink, Outlet } from "react-router";
import { PageLoader } from "@/components/ui/Feedback";
import { Heart, MapPin, Package, Settings, Star, Store } from "lucide-react";
import { cn } from "@/lib/cn";
import { useAuthStore } from "@/store/auth";
import { Header } from "./Header";
import { Footer } from "./Footer";

export function StoreLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">
        <Suspense fallback={<PageLoader />}>
          <Outlet />
        </Suspense>
      </main>
      <Footer />
    </div>
  );
}

const accountNav = [
  { to: "/orders", label: "Orders", icon: Package },
  { to: "/account/wishlist", label: "Wishlist", icon: Heart },
  { to: "/account/addresses", label: "Addresses", icon: MapPin },
  { to: "/account/reviews", label: "My reviews", icon: Star },
  { to: "/account/seller", label: "Sell on Vendora", icon: Store },
  { to: "/account", label: "Settings", icon: Settings, end: true },
];

export function AccountLayout() {
  const user = useAuthStore((s) => s.user);
  const display = user?.name || user?.username || user?.email || "";

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <div className="grid gap-6 lg:grid-cols-[240px_1fr]">
        <aside className="lg:sticky lg:top-40 lg:self-start">
          <div className="mb-3 hidden items-center gap-3 rounded-xl border border-slate-200/80 bg-white p-4 shadow-card lg:flex">
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-brand-100 text-lg font-bold text-brand-700">
              {display.charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="text-xs text-slate-500">Signed in as</p>
              <p className="truncate text-sm font-semibold text-slate-900">{display}</p>
            </div>
          </div>
          <nav className="flex gap-1 overflow-x-auto rounded-xl border border-slate-200/80 bg-white p-2 shadow-card scrollbar-none lg:flex-col">
            {accountNav.map(({ to, label, icon: Icon, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  cn(
                    "flex shrink-0 items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                    isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                  )
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <section className="min-w-0">
          <Outlet />
        </section>
      </div>
    </div>
  );
}
