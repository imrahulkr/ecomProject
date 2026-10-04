import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { ChevronRight, Heart, PackageCheck, RotateCcw, ShieldCheck, ShoppingCart, Store, Truck, Zap } from "lucide-react";
import { cn } from "@/lib/cn";
import { formatMoney } from "@/lib/format";
import { getStatus } from "@/api/errors";
import { useCategories, useProduct, useProductsByCategory, useReviewSummary, useWishlistToggle } from "@/hooks/queries";
import { useAddToCart, useLoginRedirect } from "@/hooks/useAddToCart";
import { useDocumentTitle } from "@/hooks/useUtils";
import { useAuthStore } from "@/store/auth";
import { ProductImage } from "@/components/ui/ProductImage";
import { Price } from "@/components/ui/Price";
import { RatingPill } from "@/components/ui/Stars";
import { Button, ButtonLink } from "@/components/ui/Button";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Badge } from "@/components/ui/Badge";
import { ProductGrid } from "@/components/product/ProductCard";
import { ReviewsSection } from "@/components/product/ReviewsSection";

function DetailSkeleton() {
  return (
    <div className="mx-auto grid max-w-7xl gap-8 px-4 py-8 sm:px-6 lg:grid-cols-2">
      <Skeleton className="aspect-square rounded-2xl" />
      <div className="space-y-4">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    </div>
  );
}

export default function ProductDetailPage() {
  const { productId } = useParams();
  const id = Number(productId);
  const navigate = useNavigate();
  const { data: product, isLoading, error, refetch } = useProduct(id);
  const { data: summary } = useReviewSummary(id);
  const { data: categories } = useCategories();
  const { data: related } = useProductsByCategory(product?.categoryId, { pageSize: 6, sortBy: "discount", sortOrder: "desc" });
  const authed = useAuthStore((s) => s.status === "authenticated");
  const { isWishlisted, toggle } = useWishlistToggle();
  const { addToCart, cartQuantity, isPending } = useAddToCart();
  const goToLogin = useLoginRedirect();
  const [qty, setQty] = useState(1);

  useDocumentTitle(product?.productName);

  if (isLoading) return <DetailSkeleton />;
  if (error || !product) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
        {getStatus(error) === 404 ? (
          <EmptyState
            icon={<PackageCheck />}
            title="This product isn't available"
            description="It may have been removed by the seller."
            action={<ButtonLink to="/products">Browse products</ButtonLink>}
          />
        ) : (
          <ErrorState error={error} onRetry={() => refetch()} />
        )}
      </div>
    );
  }

  const category = categories?.find((c) => c.categoryId === product.categoryId);
  const inStock = product.quantity > 0;
  const lowStock = inStock && product.quantity <= 5;
  const wished = isWishlisted(product.productId);
  const inCart = cartQuantity(product.productId);
  const maxQty = Math.max(1, Math.min(product.quantity, 10));
  const savings = product.priceMinorUnits - product.specialPriceMinorUnits;
  const relatedProducts = related?.content.filter((p) => p.productId !== product.productId).slice(0, 5);

  const buyNow = async () => {
    if (inCart || (await addToCart(product, qty))) navigate("/checkout");
  };

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-4 py-6 sm:px-6">
      <nav className="flex items-center gap-1.5 text-sm text-slate-500" aria-label="Breadcrumb">
        <Link to="/" className="hover:text-slate-900">Home</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        {category ? (
          <Link to={`/products?category=${encodeURIComponent(category.categoryName)}`} className="hover:text-slate-900">
            {category.categoryName}
          </Link>
        ) : (
          <Link to="/products" className="hover:text-slate-900">Products</Link>
        )}
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="truncate text-slate-800">{product.productName}</span>
      </nav>

      <div className="grid gap-8 lg:grid-cols-2">
        <div className="relative">
          <div className="sticky top-40 overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-8 shadow-card">
            <ProductImage src={product.image} alt={product.productName} className="aspect-square w-full" />
            {product.discount > 0 && (
              <span className="absolute left-5 top-5 rounded-lg bg-rose-600 px-2.5 py-1 text-sm font-bold text-white">
                −{Math.round(product.discount)}%
              </span>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div>
            {product.sellerName && (
              <p className="flex items-center gap-1.5 text-sm font-medium text-brand-700">
                <Store className="h-4 w-4" /> Sold by {product.sellerName}
              </p>
            )}
            <h1 className="mt-2 text-2xl font-bold leading-tight tracking-tight text-slate-900 sm:text-3xl">{product.productName}</h1>
            <a href="#reviews" className="mt-3 inline-flex items-center gap-2">
              {summary && summary.reviewCount > 0 ? (
                <>
                  <RatingPill value={summary.averageRating} />
                  <span className="text-sm text-slate-500 hover:text-slate-800">
                    {summary.reviewCount.toLocaleString("en-IN")} ratings & reviews
                  </span>
                </>
              ) : (
                <span className="text-sm text-slate-500 hover:text-slate-800">No reviews yet — be the first</span>
              )}
            </a>
          </div>

          <div className="rounded-xl border border-slate-200/80 bg-white p-5 shadow-card">
            {product.discount > 0 && <Badge tone="danger" className="mb-2">Deal of the day</Badge>}
            <Price size="lg" price={product.priceMinorUnits} special={product.specialPriceMinorUnits} discount={product.discount} currency={product.currency} />
            {savings > 0 && (
              <p className="mt-1 text-sm font-medium text-emerald-700">
                You save {formatMoney(savings, product.currency)}
              </p>
            )}
            <p className="mt-1 text-xs text-slate-500">Inclusive of all taxes</p>

            <div className="mt-5">
              {inStock ? (
                <p className={cn("text-sm font-semibold", lowStock ? "text-amber-700" : "text-emerald-700")}>
                  {lowStock ? `Hurry — only ${product.quantity} left in stock` : "In stock"}
                </p>
              ) : (
                <p className="text-sm font-semibold text-rose-600">Currently out of stock</p>
              )}
            </div>

            {inStock && !inCart && (
              <div className="mt-4 flex items-center gap-3">
                <label htmlFor="qty" className="text-sm font-medium text-slate-700">Quantity</label>
                <select
                  id="qty"
                  value={qty}
                  onChange={(e) => setQty(Number(e.target.value))}
                  className="h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium focus:border-brand-500 focus:outline-none"
                >
                  {Array.from({ length: maxQty }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            )}

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {inCart ? (
                <ButtonLink to="/cart" variant="outline" size="lg">
                  <ShoppingCart className="h-4 w-4" /> In cart ({inCart}) · View
                </ButtonLink>
              ) : (
                <Button size="lg" variant="outline" disabled={!inStock} loading={isPending} onClick={() => addToCart(product, qty)}>
                  <ShoppingCart className="h-4 w-4" /> Add to cart
                </Button>
              )}
              <Button size="lg" variant="accent" disabled={!inStock} onClick={buyNow}>
                <Zap className="h-4 w-4" /> Buy now
              </Button>
            </div>
            <button
              onClick={() => (authed ? toggle(product.productId) : goToLogin())}
              className={cn(
                "mt-3 flex w-full items-center justify-center gap-2 rounded-lg py-2 text-sm font-medium transition-colors",
                wished ? "text-rose-600 hover:bg-rose-50" : "text-slate-600 hover:bg-slate-50",
              )}
            >
              <Heart className={cn("h-4 w-4", wished && "fill-current")} />
              {wished ? "Saved to your wishlist" : "Add to wishlist"}
            </button>
          </div>

          <div className="grid grid-cols-3 gap-3 text-center">
            {[
              { icon: Truck, t: "Fast delivery" },
              { icon: RotateCcw, t: "7-day returns" },
              { icon: ShieldCheck, t: "Secure payment" },
            ].map(({ icon: Icon, t }) => (
              <div key={t} className="rounded-xl border border-slate-200/80 bg-white px-2 py-3 shadow-card">
                <Icon className="mx-auto h-5 w-5 text-brand-600" />
                <p className="mt-1.5 text-xs font-medium text-slate-700">{t}</p>
              </div>
            ))}
          </div>

          <div className="rounded-xl border border-slate-200/80 bg-white p-5 shadow-card">
            <h2 className="text-base font-semibold text-slate-900">About this item</h2>
            <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-slate-600">
              {product.description || "The seller hasn't added a description yet."}
            </p>
          </div>
        </div>
      </div>

      <ReviewsSection productId={product.productId} />

      {!!relatedProducts?.length && (
        <section>
          <h2 className="mb-4 text-xl font-bold tracking-tight text-slate-900">
            More in {category?.categoryName ?? "this category"}
          </h2>
          <ProductGrid products={relatedProducts} />
        </section>
      )}
    </div>
  );
}
