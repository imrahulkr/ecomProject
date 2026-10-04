import { Link } from "react-router";
import { Heart, ShoppingCart } from "lucide-react";
import type { Product } from "@/api/types";
import { cn } from "@/lib/cn";
import { ProductImage } from "@/components/ui/ProductImage";
import { Price } from "@/components/ui/Price";
import { Skeleton } from "@/components/ui/Feedback";
import { useWishlistToggle } from "@/hooks/queries";
import { useAddToCart, useLoginRedirect } from "@/hooks/useAddToCart";
import { useAuthStore } from "@/store/auth";

export function ProductCard({ product, className }: { product: Product; className?: string }) {
  const authed = useAuthStore((s) => s.status === "authenticated");
  const { isWishlisted, toggle } = useWishlistToggle();
  const { addToCart, cartQuantity } = useAddToCart();
  const goToLogin = useLoginRedirect();
  const wished = isWishlisted(product.productId);
  const outOfStock = product.quantity <= 0;
  const inCart = cartQuantity(product.productId) > 0;

  return (
    <div
      className={cn(
        "group relative flex flex-col overflow-hidden rounded-xl border border-slate-200/80 bg-white shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-pop",
        className,
      )}
    >
      <Link to={`/products/${product.productId}`} className="relative block aspect-square p-4">
        <ProductImage
          src={product.image}
          alt={product.productName}
          className="h-full w-full"
          imgClassName="transition-transform duration-300 group-hover:scale-105"
        />
        {product.discount > 0 && (
          <span className="absolute left-3 top-3 rounded-md bg-rose-600 px-1.5 py-0.5 text-[11px] font-bold text-white">
            −{Math.round(product.discount)}%
          </span>
        )}
        {outOfStock && (
          <span className="absolute inset-x-3 bottom-3 rounded-md bg-slate-900/80 py-1 text-center text-xs font-semibold text-white">
            Out of stock
          </span>
        )}
      </Link>

      <button
        type="button"
        onClick={() => (authed ? toggle(product.productId) : goToLogin())}
        className={cn(
          "absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 shadow-card backdrop-blur transition-colors",
          wished ? "text-rose-600" : "text-slate-400 hover:text-rose-500",
        )}
        aria-label={wished ? "Remove from wishlist" : "Save to wishlist"}
      >
        <Heart className={cn("h-4 w-4", wished && "fill-current")} />
      </button>

      <div className="flex flex-1 flex-col border-t border-slate-100 p-4">
        {product.sellerName && <p className="truncate text-xs font-medium text-slate-500">{product.sellerName}</p>}
        <Link
          to={`/products/${product.productId}`}
          className="mt-0.5 line-clamp-2 min-h-[2.5rem] text-sm font-medium leading-5 text-slate-800 hover:text-brand-700"
        >
          {product.productName}
        </Link>
        <Price
          className="mt-2"
          size="sm"
          price={product.priceMinorUnits}
          special={product.specialPriceMinorUnits}
          discount={product.discount}
          currency={product.currency}
        />
        <button
          type="button"
          disabled={outOfStock}
          onClick={() => addToCart(product)}
          className={cn(
            "mt-3 inline-flex h-9 items-center justify-center gap-1.5 rounded-lg border text-xs font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50",
            inCart
              ? "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
              : "border-brand-200 bg-brand-50 text-brand-700 hover:bg-brand-600 hover:text-white",
          )}
        >
          <ShoppingCart className="h-3.5 w-3.5" />
          {inCart ? "In cart · add one more" : "Add to cart"}
        </button>
      </div>
    </div>
  );
}

export function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-200/80 bg-white">
      <Skeleton className="aspect-square rounded-none" />
      <div className="space-y-2 p-4">
        <Skeleton className="h-3 w-1/3" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="mt-3 h-9 w-full" />
      </div>
    </div>
  );
}

export function ProductGrid({
  products,
  loading,
  skeletonCount = 10,
  className,
}: {
  products?: Product[];
  loading?: boolean;
  skeletonCount?: number;
  className?: string;
}) {
  return (
    <div className={cn("grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-4 xl:grid-cols-5", className)}>
      {loading
        ? Array.from({ length: skeletonCount }, (_, i) => <ProductCardSkeleton key={i} />)
        : products?.map((p) => <ProductCard key={p.productId} product={p} />)}
    </div>
  );
}
