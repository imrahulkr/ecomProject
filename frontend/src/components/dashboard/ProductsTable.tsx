import { useState } from "react";
import { Link } from "react-router";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink, Package, Pencil, Plus, Trash } from "lucide-react";
import { toast } from "sonner";
import { productAdminApi, type ProductScope } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Product } from "@/api/types";
import { formatMoney } from "@/lib/format";
import { useCategories } from "@/hooks/queries";
import { Card, PageHeader } from "@/components/ui/Card";
import { Table, THead, Th, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button, ButtonLink } from "@/components/ui/Button";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Pagination } from "@/components/ui/Pagination";
import { ProductImage } from "@/components/ui/ProductImage";
import { ConfirmModal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Field";

const sorts = {
  newest: { sortBy: "createdAt", sortOrder: "desc" },
  name: { sortBy: "productName", sortOrder: "asc" },
  stock: { sortBy: "quantity", sortOrder: "asc" },
  price: { sortBy: "specialPriceMinorUnits", sortOrder: "desc" },
} as const;

export function ProductsTable({ scope }: { scope: ProductScope }) {
  const base = scope === "seller" ? "/seller/products" : "/admin/products";
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<keyof typeof sorts>("newest");
  const [deleting, setDeleting] = useState<Product | null>(null);
  const { data: categories } = useCategories();
  const q = { pageNumber: page, pageSize: 15, ...sorts[sort] };
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["manage-products", scope, q],
    queryFn: () => productAdminApi.list(scope, q),
    placeholderData: keepPreviousData,
  });

  const remove = useMutation({
    mutationFn: (id: number) => productAdminApi.remove(scope, id),
    onSuccess: () => {
      toast.success("Product deleted");
      setDeleting(null);
      qc.invalidateQueries({ queryKey: ["manage-products"] });
      qc.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (err) => toast.error(getErrorMessage(err, "This product can't be deleted — it may be part of existing orders.")),
  });

  const categoryName = (id: number | null) => categories?.find((c) => c.categoryId === id)?.categoryName ?? "—";

  return (
    <div>
      <PageHeader
        title={scope === "seller" ? "Your products" : "All products"}
        description={data ? `${data.totalElements} product${data.totalElements === 1 ? "" : "s"}` : undefined}
        action={
          <>
            <Select value={sort} onChange={(e) => (setSort(e.target.value as keyof typeof sorts), setPage(0))} className="w-44" aria-label="Sort">
              <option value="newest">Newest first</option>
              <option value="name">Name A–Z</option>
              <option value="stock">Lowest stock</option>
              <option value="price">Highest price</option>
            </Select>
            <ButtonLink to={`${base}/new`}>
              <Plus className="h-4 w-4" /> Add product
            </ButtonLink>
          </>
        }
      />
      <Card>
        {isLoading ? (
          <div className="space-y-2 p-5">
            {Array.from({ length: 6 }, (_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : error ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : !data?.content.length ? (
          <EmptyState
            icon={<Package />}
            title="No products yet"
            description="Add your first product to start selling."
            action={<ButtonLink to={`${base}/new`}>Add product</ButtonLink>}
          />
        ) : (
          <Table>
            <THead>
              <tr>
                <Th>Product</Th>
                <Th>Category</Th>
                <Th>Price</Th>
                <Th>Stock</Th>
                {scope === "admin" && <Th>Seller</Th>}
                <Th className="text-right">Actions</Th>
              </tr>
            </THead>
            <tbody>
              {data.content.map((p) => (
                <Tr key={p.productId}>
                  <Td>
                    <div className="flex items-center gap-3">
                      <ProductImage src={p.image} alt={p.productName} className="h-11 w-11 shrink-0 rounded-lg border border-slate-100 p-1" />
                      <div className="min-w-0">
                        <p className="line-clamp-1 font-medium text-slate-900">{p.productName}</p>
                        <p className="text-xs text-slate-400">#{p.productId}</p>
                      </div>
                    </div>
                  </Td>
                  <Td>{categoryName(p.categoryId)}</Td>
                  <Td>
                    <p className="font-semibold text-slate-900">{formatMoney(p.specialPriceMinorUnits, p.currency)}</p>
                    {p.discount > 0 && <p className="text-xs text-emerald-600">{Math.round(p.discount)}% off {formatMoney(p.priceMinorUnits, p.currency)}</p>}
                  </Td>
                  <Td>
                    {p.quantity <= 0 ? (
                      <Badge tone="danger">Out of stock</Badge>
                    ) : p.quantity <= 5 ? (
                      <Badge tone="warning">{p.quantity} left</Badge>
                    ) : (
                      <span className="font-medium">{p.quantity}</span>
                    )}
                  </Td>
                  {scope === "admin" && <Td>{p.sellerName ?? "—"}</Td>}
                  <Td>
                    <div className="flex justify-end gap-1">
                      <Link to={`/products/${p.productId}`} target="_blank" className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900" aria-label="View in store">
                        <ExternalLink className="h-4 w-4" />
                      </Link>
                      <Link to={`${base}/${p.productId}/edit`} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900" aria-label="Edit">
                        <Pencil className="h-4 w-4" />
                      </Link>
                      <Button variant="ghost" size="icon-sm" onClick={() => setDeleting(p)} aria-label="Delete" className="text-slate-500 hover:text-rose-600">
                        <Trash className="h-4 w-4" />
                      </Button>
                    </div>
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>
      {data && <Pagination className="mt-6" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />}

      <ConfirmModal
        open={!!deleting}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.productId)}
        loading={remove.isPending}
        title={`Delete “${deleting?.productName}”?`}
        description="The product will disappear from the store immediately."
        confirmLabel="Delete product"
      />
    </div>
  );
}
