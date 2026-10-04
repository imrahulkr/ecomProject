import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ImagePlus } from "lucide-react";
import { toast } from "sonner";
import { catalogApi, productAdminApi, type ProductScope } from "@/api/endpoints";
import { getErrorMessage, getFieldErrors } from "@/api/errors";
import type { ProductInput } from "@/api/types";
import { formatMoney, toMajorUnits, toMinorUnits } from "@/lib/format";
import { useCategories } from "@/hooks/queries";
import { Card, CardBody, CardHeader, PageHeader } from "@/components/ui/Card";
import { FormField, Input, Select, Textarea } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { ErrorState, PageLoader } from "@/components/ui/Feedback";
import { ProductImage } from "@/components/ui/ProductImage";

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

const num = (msg: string) => z.number({ error: msg }).refine((n) => !Number.isNaN(n), msg);

const schema = z.object({
  productName: z.string().trim().min(3, "At least 3 characters"),
  description: z.string().trim().max(5000).optional(),
  categoryId: num("Choose a category").refine((n) => n > 0, "Choose a category"),
  price: num("Enter a price").refine((n) => n > 0, "Price must be greater than 0"),
  discount: num("Enter a discount (0 for none)").refine((n) => n >= 0 && n <= 100, "Between 0 and 100"),
  quantity: num("Enter the stock quantity").refine((n) => Number.isInteger(n) && n >= 0, "A whole number, 0 or more"),
});
type Values = z.infer<typeof schema>;

export function ProductForm({ scope }: { scope: ProductScope }) {
  const { productId } = useParams();
  const editingId = productId ? Number(productId) : null;
  const base = scope === "seller" ? "/seller/products" : "/admin/products";
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { data: categories } = useCategories();
  const [file, setFile] = useState<File | null>(null);
  const preview = useMemo(() => (file ? URL.createObjectURL(file) : null), [file]);
  useEffect(() => () => (preview ? URL.revokeObjectURL(preview) : undefined), [preview]);

  const existing = useQuery({
    queryKey: ["product", editingId],
    queryFn: () => catalogApi.product(editingId!),
    enabled: !!editingId,
  });

  const { register, handleSubmit, formState, watch, setError } = useForm<Values>({
    resolver: zodResolver(schema),
    values: existing.data
      ? {
          productName: existing.data.productName,
          description: existing.data.description ?? "",
          categoryId: existing.data.categoryId ?? 0,
          price: toMajorUnits(existing.data.priceMinorUnits),
          discount: existing.data.discount,
          quantity: existing.data.quantity,
        }
      : undefined,
    defaultValues: { productName: "", description: "", categoryId: 0, discount: 0, quantity: 1 },
  });

  const price = Number(watch("price")) || 0;
  const discount = Number(watch("discount")) || 0;
  const special = Math.round(toMinorUnits(price) - (discount * toMinorUnits(price)) / 100);

  const save = useMutation({
    mutationFn: async (v: Values) => {
      const body: ProductInput = {
        productName: v.productName,
        description: v.description ?? "",
        categoryId: v.categoryId,
        priceMinorUnits: toMinorUnits(v.price),
        discount: v.discount,
        quantity: v.quantity,
        // Lets the backend apply only the change made here, so stock reserved by checkouts
        // while this form was open isn't overwritten.
        ...(editingId && existing.data ? { expectedQuantity: existing.data.quantity } : {}),
      };
      const saved = editingId
        ? await productAdminApi.update(scope, editingId, body)
        : await productAdminApi.create(scope, body);
      if (file) await productAdminApi.uploadImage(scope, saved.productId, file);
      return saved;
    },
    onSuccess: () => {
      toast.success(editingId ? "Product updated" : "Product created");
      qc.invalidateQueries({ queryKey: ["manage-products"] });
      qc.invalidateQueries({ queryKey: ["products"] });
      if (editingId) qc.invalidateQueries({ queryKey: ["product", editingId] });
      navigate(base);
    },
    onError: (err) => {
      const fields = getFieldErrors(err);
      const map: Record<string, keyof Values> = { priceMinorUnits: "price" };
      Object.entries(fields).forEach(([k, msg]) => setError(map[k] ?? (k as keyof Values), { message: msg }));
      toast.error(getErrorMessage(err));
    },
  });

  if (editingId && existing.isLoading) return <PageLoader />;
  if (editingId && existing.error) return <ErrorState error={existing.error} onRetry={() => existing.refetch()} />;

  const e = formState.errors;
  const currentImage = preview ?? existing.data?.image ?? null;

  return (
    <div>
      <Link to={base} className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-900">
        <ArrowLeft className="h-4 w-4" /> Back to products
      </Link>
      <PageHeader title={editingId ? "Edit product" : "Add a new product"} description="Fields match what shoppers see on the product page." />

      <form onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-5 lg:grid-cols-[1fr_340px]">
        <div className="space-y-5">
          <Card>
            <CardHeader title="Details" />
            <CardBody className="grid gap-4">
              <FormField label="Product name" error={e.productName?.message} hint="Must be unique across the store">
                {(id) => <Input id={id} invalid={!!e.productName} {...register("productName")} />}
              </FormField>
              <FormField label="Description" error={e.description?.message}>
                {(id) => <Textarea id={id} rows={6} placeholder="Materials, size, what's in the box…" {...register("description")} />}
              </FormField>
              <FormField label="Category" error={e.categoryId?.message}>
                {(id) => (
                  <Select id={id} invalid={!!e.categoryId} {...register("categoryId", { valueAsNumber: true })}>
                    <option value={0}>Select a category</option>
                    {categories?.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.categoryName}
                      </option>
                    ))}
                  </Select>
                )}
              </FormField>
            </CardBody>
          </Card>

          <Card>
            <CardHeader title="Pricing & inventory" />
            <CardBody className="grid gap-4 sm:grid-cols-3">
              <FormField label="Price (₹)" error={e.price?.message}>
                {(id) => <Input id={id} type="number" step="0.01" min="0" invalid={!!e.price} {...register("price", { valueAsNumber: true })} />}
              </FormField>
              <FormField label="Discount (%)" error={e.discount?.message}>
                {(id) => <Input id={id} type="number" step="0.1" min="0" max="100" invalid={!!e.discount} {...register("discount", { valueAsNumber: true })} />}
              </FormField>
              <FormField label="Stock" error={e.quantity?.message}>
                {(id) => <Input id={id} type="number" min="0" step="1" invalid={!!e.quantity} {...register("quantity", { valueAsNumber: true })} />}
              </FormField>
              <p className="text-sm text-slate-600 sm:col-span-3">
                Customers pay <span className="font-semibold text-slate-900">{formatMoney(special)}</span>
                {discount > 0 && <span className="text-slate-400"> (was {formatMoney(toMinorUnits(price))})</span>}
              </p>
            </CardBody>
          </Card>
        </div>

        <div className="space-y-5">
          <Card>
            <CardHeader title="Product image" description="PNG, JPG or WebP up to 5 MB" />
            <CardBody>
              <ProductImage src={currentImage} alt="Product preview" className="aspect-square w-full rounded-lg border border-slate-100" />
              <label className="mt-4 flex cursor-pointer items-center justify-center gap-2 rounded-lg border-2 border-dashed border-slate-300 py-3 text-sm font-semibold text-slate-600 transition-colors hover:border-brand-400 hover:text-brand-700">
                <ImagePlus className="h-4 w-4" />
                {file ? file.name : "Choose image"}
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  className="sr-only"
                  onChange={(ev) => {
                    const f = ev.target.files?.[0] ?? null;
                    if (f && f.size > MAX_IMAGE_BYTES) {
                      toast.error("Image must be 5 MB or smaller");
                      return;
                    }
                    setFile(f);
                  }}
                />
              </label>
            </CardBody>
          </Card>
          <Button type="submit" size="lg" className="w-full" loading={save.isPending}>
            {editingId ? "Save changes" : "Create product"}
          </Button>
        </div>
      </form>
    </div>
  );
}
