import { Heart } from "lucide-react";
import { useWishlist } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, PageHeader } from "@/components/ui/Card";
import { EmptyState, ErrorState } from "@/components/ui/Feedback";
import { ButtonLink } from "@/components/ui/Button";
import { ProductGrid } from "@/components/product/ProductCard";

export default function WishlistPage() {
  useDocumentTitle("Wishlist");
  const { data, isLoading, error, refetch } = useWishlist();

  return (
    <div>
      <PageHeader title="Wishlist" description={data ? `${data.length} saved item${data.length === 1 ? "" : "s"}` : undefined} />
      {error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !isLoading && !data?.length ? (
        <Card>
          <EmptyState
            icon={<Heart />}
            title="Your wishlist is empty"
            description="Tap the heart on any product to save it for later."
            action={<ButtonLink to="/products">Discover products</ButtonLink>}
          />
        </Card>
      ) : (
        <ProductGrid products={data} loading={isLoading} skeletonCount={4} className="lg:grid-cols-3 xl:grid-cols-4" />
      )}
    </div>
  );
}
