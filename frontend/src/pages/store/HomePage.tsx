import { Link } from "react-router";
import { ArrowRight, BadgePercent, ShieldCheck, Sparkles, Store, Truck, Zap } from "lucide-react";
import { brand } from "@/config/brand";
import { formatMoney } from "@/lib/format";
import { useCategories, useProducts } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { ProductGrid } from "@/components/product/ProductCard";
import { ButtonLink } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Feedback";
import type { ProductQuery } from "@/api/types";

const categoryTints = [
  "from-sky-50 to-sky-100 text-sky-700",
  "from-amber-50 to-amber-100 text-amber-700",
  "from-emerald-50 to-emerald-100 text-emerald-700",
  "from-rose-50 to-rose-100 text-rose-700",
  "from-violet-50 to-violet-100 text-violet-700",
  "from-teal-50 to-teal-100 text-teal-700",
];

function Section({
  title,
  subtitle,
  href,
  query,
}: {
  title: string;
  subtitle: string;
  href: string;
  query: ProductQuery;
}) {
  const { data, isLoading } = useProducts(query);
  if (!isLoading && !data?.content.length) return null;
  return (
    <section className="mx-auto max-w-7xl px-4 sm:px-6">
      <div className="mb-4 flex items-end justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-slate-900">{title}</h2>
          <p className="text-sm text-slate-500">{subtitle}</p>
        </div>
        <Link to={href} className="flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800">
          View all <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
      <ProductGrid products={data?.content} loading={isLoading} skeletonCount={5} />
    </section>
  );
}

export default function HomePage() {
  useDocumentTitle("Home");
  const { data: categories, isLoading: catLoading } = useCategories();

  return (
    <div className="space-y-12 pb-4">
      {/* Hero */}
      <section className="mx-auto max-w-7xl px-4 pt-6 sm:px-6">
        <div className="grid gap-4 lg:grid-cols-3">
          <div className="relative overflow-hidden rounded-2xl bg-linear-to-br from-brand-700 via-brand-600 to-brand-500 p-8 text-white shadow-pop sm:p-10 lg:col-span-2">
            <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
            <div className="absolute -bottom-24 right-24 h-64 w-64 rounded-full bg-amber-300/20 blur-3xl" />
            <div className="relative max-w-lg">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1 text-xs font-semibold backdrop-blur">
                <Sparkles className="h-3.5 w-3.5 text-amber-300" /> The independent marketplace
              </span>
              <h1 className="mt-4 text-3xl font-extrabold leading-tight tracking-tight sm:text-5xl">
                Great finds from sellers you&apos;ll love.
              </h1>
              <p className="mt-4 text-base text-brand-100 sm:text-lg">
                Discover thousands of products, honest reviews and fair prices — all in one place.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <ButtonLink to="/products" variant="accent" size="lg">
                  Start shopping <ArrowRight className="h-4 w-4" />
                </ButtonLink>
                <ButtonLink to="/products?sort=discount" size="lg" className="bg-white/15 backdrop-blur hover:bg-white/25">
                  Today&apos;s deals
                </ButtonLink>
              </div>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1">
            <Link
              to="/products?sort=discount"
              className="group relative overflow-hidden rounded-2xl bg-amber-400 p-6 text-slate-900 shadow-card transition-transform hover:-translate-y-0.5"
            >
              <BadgePercent className="h-8 w-8" />
              <p className="mt-3 text-lg font-bold">Top deals</p>
              <p className="text-sm text-slate-800/80">Biggest discounts across every category</p>
              <ArrowRight className="absolute bottom-6 right-6 h-5 w-5 transition-transform group-hover:translate-x-1" />
            </Link>
            <Link
              to="/account/seller"
              className="group relative overflow-hidden rounded-2xl bg-slate-900 p-6 text-white shadow-card transition-transform hover:-translate-y-0.5"
            >
              <Store className="h-8 w-8 text-amber-400" />
              <p className="mt-3 text-lg font-bold">Sell on {brand.name}</p>
              <p className="text-sm text-slate-300">Reach new customers with zero setup</p>
              <ArrowRight className="absolute bottom-6 right-6 h-5 w-5 transition-transform group-hover:translate-x-1" />
            </Link>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-card sm:grid-cols-3">
          {[
            { icon: Truck, t: "Free delivery", s: `On orders over ${formatMoney(brand.freeShippingThresholdMinor)}` },
            { icon: ShieldCheck, t: "Secure checkout", s: "Cards, UPI & wallets" },
            { icon: Zap, t: "Verified sellers", s: "Every seller is reviewed" },
          ].map(({ icon: Icon, t, s }) => (
            <div key={t} className="flex items-center gap-3 px-2">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                <Icon className="h-5 w-5" />
              </span>
              <div>
                <p className="text-sm font-semibold text-slate-900">{t}</p>
                <p className="text-xs text-slate-500">{s}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Categories */}
      <section className="mx-auto max-w-7xl px-4 sm:px-6">
        <h2 className="mb-4 text-xl font-bold tracking-tight text-slate-900">Shop by category</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {catLoading
            ? Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-28 rounded-xl" />)
            : categories?.slice(0, 12).map((c, i) => (
                <Link
                  key={c.categoryId}
                  to={`/products?category=${encodeURIComponent(c.categoryName)}`}
                  className={`group flex h-28 flex-col justify-between rounded-xl bg-linear-to-br p-4 transition-all hover:-translate-y-0.5 hover:shadow-card ${categoryTints[i % categoryTints.length]}`}
                >
                  <span className="text-2xl font-extrabold opacity-30">{c.categoryName.charAt(0)}</span>
                  <span className="flex items-center justify-between text-sm font-semibold text-slate-900">
                    <span className="truncate">{c.categoryName}</span>
                    <ArrowRight className="h-4 w-4 shrink-0 opacity-0 transition-opacity group-hover:opacity-100" />
                  </span>
                </Link>
              ))}
        </div>
      </section>

      <Section
        title="Top deals"
        subtitle="Handpicked offers with the biggest savings"
        href="/products?sort=discount"
        query={{ pageSize: 10, sortBy: "discount", sortOrder: "desc" }}
      />
      <Section
        title="New arrivals"
        subtitle="Fresh from our sellers this week"
        href="/products?sort=newest"
        query={{ pageSize: 10, sortBy: "createdAt", sortOrder: "desc" }}
      />

      {/* Seller CTA */}
      <section className="mx-auto max-w-7xl px-4 sm:px-6">
        <div className="flex flex-col items-start justify-between gap-6 rounded-2xl border border-slate-200/80 bg-white p-8 shadow-card md:flex-row md:items-center">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-brand-600">For businesses</p>
            <h3 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Turn your products into a thriving store</h3>
            <p className="mt-1 text-sm text-slate-500">Apply in minutes. List products, ship orders and reply to reviews from one dashboard.</p>
          </div>
          <ButtonLink to="/account/seller" size="lg" variant="secondary">
            Start selling <ArrowRight className="h-4 w-4" />
          </ButtonLink>
        </div>
      </section>
    </div>
  );
}
