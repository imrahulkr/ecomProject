import { useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Pencil, Plus, Ticket, Trash } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { Coupon, CouponInput } from "@/api/types";
import { formatDateTime, formatMoney, toMajorUnits, toMinorUnits } from "@/lib/format";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, PageHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Checkbox, FormField, Input } from "@/components/ui/Field";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { Segmented, Table, THead, Th, Tr, Td } from "@/components/ui/Table";

// Empty number inputs register as NaN with valueAsNumber - treat them as "not set".
const optionalNumber = z.preprocess((v) => (typeof v === "number" && Number.isNaN(v) ? undefined : v), z.number().optional());

const schema = z
  .object({
    code: z.string().trim().min(3, "At least 3 characters").max(64).regex(/^[A-Za-z0-9_-]+$/, "Letters, numbers, - and _ only"),
    description: z.string().trim().max(255).optional(),
    discountType: z.enum(["PERCENTAGE", "FIXED_AMOUNT"]),
    discountPercentage: optionalNumber,
    discountAmount: optionalNumber,
    minOrderAmount: optionalNumber,
    maxRedemptions: optionalNumber,
    perUserLimit: optionalNumber,
    expiresAt: z.string().optional(),
    active: z.boolean(),
  })
  .superRefine((v, ctx) => {
    if (v.discountType === "PERCENTAGE" && (v.discountPercentage == null || v.discountPercentage <= 0 || v.discountPercentage > 100))
      ctx.addIssue({ code: "custom", path: ["discountPercentage"], message: "Enter a percentage between 0 and 100" });
    if (v.discountType === "FIXED_AMOUNT" && (v.discountAmount == null || v.discountAmount <= 0))
      ctx.addIssue({ code: "custom", path: ["discountAmount"], message: "Enter an amount greater than 0" });
    for (const k of ["maxRedemptions", "perUserLimit"] as const)
      if (v[k] != null && (!Number.isInteger(v[k]) || v[k]! < 1)) ctx.addIssue({ code: "custom", path: [k], message: "A whole number ≥ 1" });
    if (v.minOrderAmount != null && v.minOrderAmount <= 0) ctx.addIssue({ code: "custom", path: ["minOrderAmount"], message: "Must be greater than 0" });
  });

type FormIn = z.input<typeof schema>;
type Values = z.output<typeof schema>;

const toLocalInput = (iso?: string | null) => {
  if (!iso) return "";
  const d = new Date(iso);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

function CouponModal({ open, onClose, coupon }: { open: boolean; onClose: () => void; coupon: Coupon | null }) {
  const qc = useQueryClient();
  const { register, handleSubmit, watch, setValue, formState } = useForm<FormIn, unknown, Values>({
    resolver: zodResolver(schema),
    values: coupon
      ? {
          code: coupon.code,
          description: coupon.description ?? "",
          discountType: coupon.discountType,
          discountPercentage: coupon.discountPercentage ?? undefined,
          discountAmount: coupon.discountAmountMinorUnits != null ? toMajorUnits(coupon.discountAmountMinorUnits) : undefined,
          minOrderAmount: coupon.minOrderAmountMinorUnits != null ? toMajorUnits(coupon.minOrderAmountMinorUnits) : undefined,
          maxRedemptions: coupon.maxRedemptions ?? undefined,
          perUserLimit: coupon.perUserLimit ?? undefined,
          expiresAt: toLocalInput(coupon.expiresAt),
          active: coupon.active,
        }
      : { code: "", description: "", discountType: "PERCENTAGE", expiresAt: "", active: true },
  });
  const type = watch("discountType");

  const save = useMutation({
    mutationFn: (v: Values) => {
      const body: CouponInput = {
        code: v.code.toUpperCase(),
        description: v.description || null,
        discountType: v.discountType,
        discountPercentage: v.discountType === "PERCENTAGE" ? v.discountPercentage : null,
        discountAmountMinorUnits: v.discountType === "FIXED_AMOUNT" && v.discountAmount != null ? toMinorUnits(v.discountAmount) : null,
        currency: v.discountType === "FIXED_AMOUNT" ? "INR" : null,
        minOrderAmountMinorUnits: v.minOrderAmount != null ? toMinorUnits(v.minOrderAmount) : null,
        maxRedemptions: v.maxRedemptions ?? null,
        perUserLimit: v.perUserLimit ?? null,
        expiresAt: v.expiresAt ? new Date(v.expiresAt).toISOString() : null,
        active: v.active,
      };
      return coupon ? adminApi.updateCoupon(coupon.couponId, body) : adminApi.createCoupon(body);
    },
    onSuccess: () => {
      toast.success(coupon ? "Coupon updated" : "Coupon created");
      qc.invalidateQueries({ queryKey: ["admin-coupons"] });
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const e = formState.errors;
  const num = { valueAsNumber: true } as const;

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={coupon ? `Edit ${coupon.code}` : "Create coupon"}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="coupon-form" loading={save.isPending}>
            Save coupon
          </Button>
        </>
      }
    >
      <form id="coupon-form" onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-4 sm:grid-cols-2">
        <FormField label="Code" error={e.code?.message}>
          {(id) => <Input id={id} className="uppercase tracking-wide" placeholder="WELCOME10" invalid={!!e.code} {...register("code")} />}
        </FormField>
        <FormField label="Discount type">
          {() => (
            <Segmented
              value={type}
              onChange={(v) => setValue("discountType", v, { shouldValidate: true })}
              options={[
                { value: "PERCENTAGE", label: "Percentage" },
                { value: "FIXED_AMOUNT", label: "Fixed amount" },
              ]}
            />
          )}
        </FormField>
        <FormField label="Description" className="sm:col-span-2">
          {(id) => <Input id={id} placeholder="10% off your first order" {...register("description")} />}
        </FormField>
        {type === "PERCENTAGE" ? (
          <FormField label="Discount (%)" error={e.discountPercentage?.message}>
            {(id) => <Input id={id} type="number" step="0.1" invalid={!!e.discountPercentage} {...register("discountPercentage", num)} />}
          </FormField>
        ) : (
          <FormField label="Discount amount (₹)" error={e.discountAmount?.message}>
            {(id) => <Input id={id} type="number" step="0.01" invalid={!!e.discountAmount} {...register("discountAmount", num)} />}
          </FormField>
        )}
        <FormField label="Minimum order (₹)" error={e.minOrderAmount?.message} hint="Optional">
          {(id) => <Input id={id} type="number" step="0.01" {...register("minOrderAmount", num)} />}
        </FormField>
        <FormField label="Total redemptions limit" error={e.maxRedemptions?.message} hint="Empty = unlimited">
          {(id) => <Input id={id} type="number" step="1" {...register("maxRedemptions", num)} />}
        </FormField>
        <FormField label="Uses per customer" error={e.perUserLimit?.message} hint="Empty = unlimited">
          {(id) => <Input id={id} type="number" step="1" {...register("perUserLimit", num)} />}
        </FormField>
        <FormField label="Expires at" hint="Optional">
          {(id) => <Input id={id} type="datetime-local" {...register("expiresAt")} />}
        </FormField>
        <div className="flex items-end pb-2">
          <Checkbox label="Active — customers can use it" {...register("active")} />
        </div>
      </form>
    </Modal>
  );
}

export default function AdminCouponsPage() {
  useDocumentTitle("Admin · Coupons");
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [modal, setModal] = useState<{ open: boolean; coupon: Coupon | null }>({ open: false, coupon: null });
  const [deleting, setDeleting] = useState<Coupon | null>(null);
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["admin-coupons", page],
    queryFn: () => adminApi.coupons({ pageNumber: page, pageSize: 15, sortBy: "couponId", sortOrder: "desc" }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: number) => adminApi.deleteCoupon(id),
    onSuccess: () => {
      toast.success("Coupon deleted");
      setDeleting(null);
      qc.invalidateQueries({ queryKey: ["admin-coupons"] });
    },
    onError: (err) => toast.error(getErrorMessage(err, "This coupon has redemptions and can't be deleted — deactivate it instead.")),
  });

  const expired = (c: Coupon) => !!c.expiresAt && new Date(c.expiresAt) < new Date();

  return (
    <div>
      <PageHeader
        title="Coupons"
        description="Create discount codes customers apply in their cart."
        action={
          <Button onClick={() => setModal({ open: true, coupon: null })}>
            <Plus className="h-4 w-4" /> New coupon
          </Button>
        }
      />
      <Card>
        {isLoading ? (
          <div className="space-y-2 p-5">
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
          </div>
        ) : error ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : !data?.content.length ? (
          <EmptyState icon={<Ticket />} title="No coupons yet" action={<Button onClick={() => setModal({ open: true, coupon: null })}>Create a coupon</Button>} />
        ) : (
          <Table>
            <THead>
              <tr>
                <Th>Code</Th>
                <Th>Discount</Th>
                <Th>Min. order</Th>
                <Th>Used</Th>
                <Th>Expires</Th>
                <Th>Status</Th>
                <Th className="text-right">Actions</Th>
              </tr>
            </THead>
            <tbody>
              {data.content.map((c) => (
                <Tr key={c.couponId}>
                  <Td>
                    <p className="font-mono text-sm font-bold tracking-wide text-slate-900">{c.code}</p>
                    {c.description && <p className="line-clamp-1 text-xs text-slate-500">{c.description}</p>}
                  </Td>
                  <Td className="font-semibold">
                    {c.discountType === "PERCENTAGE" ? `${c.discountPercentage}%` : formatMoney(c.discountAmountMinorUnits ?? 0, c.currency ?? "INR")}
                  </Td>
                  <Td>{c.minOrderAmountMinorUnits ? formatMoney(c.minOrderAmountMinorUnits) : "—"}</Td>
                  <Td>
                    {c.redemptionCount}
                    {c.maxRedemptions ? ` / ${c.maxRedemptions}` : ""}
                    {c.perUserLimit ? <span className="block text-xs text-slate-400">{c.perUserLimit} per user</span> : null}
                  </Td>
                  <Td className="text-xs">{c.expiresAt ? formatDateTime(c.expiresAt) : "Never"}</Td>
                  <Td>
                    {expired(c) ? <Badge tone="neutral">Expired</Badge> : c.active ? <Badge tone="success">Active</Badge> : <Badge tone="warning">Paused</Badge>}
                  </Td>
                  <Td>
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="icon-sm" onClick={() => setModal({ open: true, coupon: c })} aria-label="Edit">
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon-sm" className="hover:text-rose-600" onClick={() => setDeleting(c)} aria-label="Delete">
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

      {modal.open && <CouponModal open onClose={() => setModal({ open: false, coupon: null })} coupon={modal.coupon} />}
      <ConfirmModal
        open={!!deleting}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.couponId)}
        loading={remove.isPending}
        title={`Delete ${deleting?.code}?`}
        description="Customers will no longer be able to apply this code."
        confirmLabel="Delete coupon"
      />
    </div>
  );
}
