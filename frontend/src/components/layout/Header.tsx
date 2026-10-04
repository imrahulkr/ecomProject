import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { Link, NavLink, useLocation, useNavigate, useSearchParams } from "react-router";
import {
  ChevronDown,
  Heart,
  LayoutDashboard,
  LogOut,
  MapPin,
  Menu,
  Package,
  Search,
  Settings,
  Shield,
  ShoppingCart,
  Star,
  Store,
  Truck,
  User,
  X,
} from "lucide-react";
import { cn } from "@/lib/cn";
import { brand } from "@/config/brand";
import { formatMoney } from "@/lib/format";
import { isAdmin, isSeller, useAuthStore } from "@/store/auth";
import { useCartCount, useCategories, useWishlist } from "@/hooks/queries";
import { useClickOutside } from "@/hooks/useUtils";
import { Logo } from "./Logo";
import { useLogout } from "./useLogout";

function SearchBar({ className, autoFocus }: { className?: string; autoFocus?: boolean }) {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const location = useLocation();
  const [value, setValue] = useState(location.pathname === "/products" ? (params.get("q") ?? "") : "");

  useEffect(() => {
    if (location.pathname === "/products") setValue(params.get("q") ?? "");
  }, [location.pathname, params]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const q = value.trim();
    const next = new URLSearchParams();
    if (q) next.set("q", q);
    navigate(`/products${next.toString() ? `?${next}` : ""}`);
  };

  return (
    <form onSubmit={submit} className={cn("relative flex w-full", className)} role="search">
      <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        autoFocus={autoFocus}
        placeholder="Search for products, brands and more"
        className="h-11 w-full rounded-l-xl border border-r-0 border-slate-300 bg-slate-50 pl-10 pr-3 text-sm placeholder:text-slate-400 focus:border-brand-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-brand-500/15"
        aria-label="Search products"
      />
      <button
        type="submit"
        className="h-11 shrink-0 rounded-r-xl bg-brand-600 px-5 text-sm font-semibold text-white transition-colors hover:bg-brand-700"
      >
        Search
      </button>
    </form>
  );
}

function AccountMenu() {
  const status = useAuthStore((s) => s.status);
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const location = useLocation();
  useClickOutside(ref, () => setOpen(false), open);
  useEffect(() => setOpen(false), [location.pathname]);

  if (status !== "authenticated" || !user) {
    return (
      <Link
        to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`}
        className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-left hover:bg-slate-100"
      >
        <User className="h-5 w-5 text-slate-700" />
        <span className="hidden leading-tight lg:block">
          <span className="block text-[11px] text-slate-500">Hello, sign in</span>
          <span className="block text-sm font-semibold text-slate-900">Account</span>
        </span>
      </Link>
    );
  }

  const display = user.name || user.username || user.email;
  const initial = display.charAt(0).toUpperCase();

  const item = "flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 hover:text-slate-900";

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-slate-100"
        aria-expanded={open}
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-700">
          {initial}
        </span>
        <span className="hidden max-w-36 text-left leading-tight lg:block">
          <span className="block text-[11px] text-slate-500">Hello,</span>
          <span className="block truncate text-sm font-semibold text-slate-900">{display}</span>
        </span>
        <ChevronDown className="hidden h-4 w-4 text-slate-500 lg:block" />
      </button>
      {open && (
        <div className="absolute right-0 top-full z-40 mt-2 w-64 animate-pop-in rounded-xl border border-slate-200 bg-white p-2 shadow-pop">
          <div className="mb-1 border-b border-slate-100 px-3 pb-2.5 pt-1.5">
            <p className="truncate text-sm font-semibold text-slate-900">{display}</p>
            <p className="truncate text-xs text-slate-500">{user.email}</p>
          </div>
          <Link to="/orders" className={item}>
            <Package className="h-4 w-4" /> My orders
          </Link>
          <Link to="/account/wishlist" className={item}>
            <Heart className="h-4 w-4" /> Wishlist
          </Link>
          <Link to="/account/addresses" className={item}>
            <MapPin className="h-4 w-4" /> Addresses
          </Link>
          <Link to="/account/reviews" className={item}>
            <Star className="h-4 w-4" /> My reviews
          </Link>
          <Link to="/account" className={item}>
            <Settings className="h-4 w-4" /> Account settings
          </Link>
          <div className="my-1 border-t border-slate-100" />
          {isSeller(user) ? (
            <Link to="/seller/dashboard" className={item}>
              <LayoutDashboard className="h-4 w-4" /> Seller dashboard
            </Link>
          ) : (
            <Link to="/account/seller" className={item}>
              <Store className="h-4 w-4" /> Become a seller
            </Link>
          )}
          {isAdmin(user) && (
            <Link to="/admin" className={item}>
              <Shield className="h-4 w-4" /> Admin console
            </Link>
          )}
          <div className="my-1 border-t border-slate-100" />
          <button onClick={logout} className={cn(item, "w-full text-rose-600 hover:bg-rose-50 hover:text-rose-700")}>
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </div>
      )}
    </div>
  );
}

function IconLink({ to, label, icon, count }: { to: string; label: string; icon: ReactNode; count?: number }) {
  return (
    <Link to={to} className="relative flex items-center gap-2 rounded-lg px-2 py-1.5 text-slate-700 hover:bg-slate-100" aria-label={label}>
      <span className="relative">
        {icon}
        {!!count && (
          <span className="absolute -right-2 -top-2 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-amber-400 px-1 text-[10px] font-bold text-slate-900 ring-2 ring-white">
            {count > 99 ? "99+" : count}
          </span>
        )}
      </span>
      <span className="hidden text-sm font-semibold lg:block">{label}</span>
    </Link>
  );
}

function CategoryNav() {
  const { data: categories } = useCategories();
  const [params] = useSearchParams();
  const location = useLocation();
  const active = location.pathname === "/products" ? params.get("category") : null;

  const pill = (isActive: boolean) =>
    cn(
      "shrink-0 rounded-full px-3 py-1.5 text-sm font-medium transition-colors",
      isActive ? "bg-brand-600 text-white" : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
    );

  return (
    <nav className="border-t border-slate-100 bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-1 overflow-x-auto px-4 py-2 scrollbar-none sm:px-6">
        <NavLink to="/products" end className={() => pill(location.pathname === "/products" && !active && !params.get("q"))}>
          All products
        </NavLink>
        <Link to="/products?sort=discount" className={pill(false)}>
          Top deals
        </Link>
        <Link to="/products?sort=newest" className={pill(false)}>
          New arrivals
        </Link>
        <span className="mx-1 h-5 w-px shrink-0 bg-slate-200" />
        {categories?.map((c) => (
          <Link
            key={c.categoryId}
            to={`/products?category=${encodeURIComponent(c.categoryName)}`}
            className={pill(active === c.categoryName)}
          >
            {c.categoryName}
          </Link>
        ))}
      </div>
    </nav>
  );
}

function MobileDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { data: categories } = useCategories();
  const user = useAuthStore((s) => s.user);
  if (!open) return null;
  const link = "block rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-100";
  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <div className="absolute inset-0 animate-fade-in bg-slate-950/40" onClick={onClose} />
      <aside className="absolute inset-y-0 left-0 flex w-80 max-w-[85vw] animate-pop-in flex-col bg-white shadow-pop">
        <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <Logo />
          <button onClick={onClose} className="rounded-lg p-2 hover:bg-slate-100" aria-label="Close menu">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-3" onClick={onClose}>
          <p className="px-3 pb-1 pt-2 text-xs font-semibold uppercase tracking-wider text-slate-400">Shop</p>
          <Link to="/products" className={link}>All products</Link>
          <Link to="/products?sort=discount" className={link}>Top deals</Link>
          {categories?.map((c) => (
            <Link key={c.categoryId} to={`/products?category=${encodeURIComponent(c.categoryName)}`} className={link}>
              {c.categoryName}
            </Link>
          ))}
          <p className="px-3 pb-1 pt-4 text-xs font-semibold uppercase tracking-wider text-slate-400">Account</p>
          <Link to="/orders" className={link}>My orders</Link>
          <Link to="/account/wishlist" className={link}>Wishlist</Link>
          <Link to="/account" className={link}>Settings</Link>
          {isSeller(user) && <Link to="/seller/dashboard" className={link}>Seller dashboard</Link>}
          {isAdmin(user) && <Link to="/admin" className={link}>Admin console</Link>}
        </div>
      </aside>
    </div>
  );
}

export function Header() {
  const cartCount = useCartCount();
  const { data: wishlist } = useWishlist();
  const [drawer, setDrawer] = useState(false);
  const location = useLocation();
  useEffect(() => setDrawer(false), [location.pathname]);

  return (
    <header className="sticky top-0 z-30">
      <div className="bg-slate-900 text-slate-300">
        <div className="mx-auto flex h-9 max-w-7xl items-center justify-between px-4 text-xs sm:px-6">
          <span className="flex items-center gap-1.5">
            <Truck className="h-3.5 w-3.5 text-amber-400" />
            Free delivery on orders over {formatMoney(brand.freeShippingThresholdMinor)}
          </span>
          <span className="hidden items-center gap-5 sm:flex">
            <Link to="/account/seller" className="hover:text-white">Sell on {brand.name}</Link>
            <Link to="/orders" className="hover:text-white">Track order</Link>
            <a href={`mailto:${brand.supportEmail}`} className="hover:text-white">Help</a>
          </span>
        </div>
      </div>

      <div className="border-b border-slate-200/70 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3 sm:gap-6 sm:px-6">
          <button className="rounded-lg p-2 hover:bg-slate-100 lg:hidden" onClick={() => setDrawer(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>
          <Logo className="shrink-0" />
          <SearchBar className="hidden max-w-2xl flex-1 md:flex" />
          <div className="ml-auto flex items-center gap-1 sm:gap-2">
            <AccountMenu />
            <IconLink to="/account/wishlist" label="Wishlist" icon={<Heart className="h-5 w-5" />} count={wishlist?.length} />
            <IconLink to="/cart" label="Cart" icon={<ShoppingCart className="h-5 w-5" />} count={cartCount} />
          </div>
        </div>
        <div className="px-4 pb-3 md:hidden">
          <SearchBar />
        </div>
        <CategoryNav />
      </div>
      <MobileDrawer open={drawer} onClose={() => setDrawer(false)} />
    </header>
  );
}

