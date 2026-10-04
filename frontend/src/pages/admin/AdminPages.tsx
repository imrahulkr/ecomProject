import { useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  BadgePercent,
  CircleDollarSign,
  ClipboardList,
  FolderTree,
  Heart,
  Package,
  Pencil,
  Plus,
  RefreshCw,
  Star,
  Store,
  Ticket,
  Trash,
  Users,
} from "lucide-react";
import { toast } from "sonner";
import { adminApi, categoryAdminApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { AdminUser, Category, Role, SellerApplication, SellerApplicationStatus } from "@/api/types";
import { formatDate, titleCase } from "@/lib/format";
import { qk, useCategories } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { useAuthStore } from "@/store/auth";
import { Card, PageHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { FormField, Input, Textarea } from "@/components/ui/Field";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { Segmented, Table, THead, Th, Tr, Td } from "@/components/ui/Table";
import { StatCard } from "@/components/dashboard/StatCard";
import { ProductsTable } from "@/components/dashboard/ProductsTable";
import { ProductForm } from "@/components/dashboard/ProductForm";
import { OrdersBoard } from "@/components/dashboard/OrdersBoard";

// ------------------------------------------------------------------ analytics
const ANALYTICS_REFRESH_MS = 30_000;

export function AdminDashboardPage() {
  useDocumentTitle("Admin · Analytics");
  // Live: refetched every 30s while the tab is visible, and whenever the admin returns to the tab.
  const { data, isLoading, isFetching, error, refetch, dataUpdatedAt } = useQuery({
    queryKey: ["admin-analytics"],
    queryFn: adminApi.analytics,
    refetchInterval: ANALYTICS_REFRESH_MS,
    refetchOnWindowFocus: true,
  });
  const inr = (v?: string) =>
    new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(v ?? 0));
  const n = (v?: string) => Number(v ?? 0).toLocaleString("en-IN");
  const paid = Number(data?.paidOrders ?? 0);

  if (error) return <ErrorState error={error} onRetry={() => refetch()} />;
  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Admin console"
        title="Marketplace overview"
        description="Live totals across the whole store. Select a card to see what's behind it."
        action={
          <>
            {dataUpdatedAt > 0 && (
              <span className="text-xs text-slate-500" aria-live="polite">
                Updated {new Date(dataUpdatedAt).toLocaleTimeString("en-IN")}
              </span>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={() => refetch()}
              disabled={isFetching}
              leftIcon={<RefreshCw className={isFetching ? "h-3.5 w-3.5 animate-spin" : "h-3.5 w-3.5"} />}
            >
              Refresh
            </Button>
          </>
        }
      />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Total revenue"
          value={inr(data?.totalRevenue)}
          icon={<CircleDollarSign />}
          tone="emerald"
          loading={isLoading}
          hint={`From ${n(data?.paidOrders)} paid order${paid === 1 ? "" : "s"}`}
          to="/admin/orders?status=PAID"
        />
        <StatCard
          label="Orders"
          value={n(data?.totalOrders)}
          icon={<ClipboardList />}
          loading={isLoading}
          hint={`${n(data?.paidOrders)} paid · ${n(data?.pendingPaymentOrders)} awaiting payment · ${n(data?.cancelledOrders)} cancelled`}
          to="/admin/orders"
        />
        <StatCard label="Products" value={n(data?.productCount)} icon={<Package />} tone="violet" loading={isLoading} to="/admin/products" />
        <StatCard
          label="Wishlist saves"
          value={n(data?.wishlistItemCount)}
          icon={<Heart />}
          tone="rose"
          loading={isLoading}
          hint="Across all products"
          to="/admin/products"
        />
        <StatCard
          label="Average rating"
          value={
            <span className="flex items-center gap-1.5">
              {Number(data?.averageRating ?? 0).toFixed(2)} <Star className="h-5 w-5 fill-amber-400 text-amber-400" />
            </span>
          }
          icon={<Star />}
          tone="amber"
          loading={isLoading}
          to="/admin/reviews"
        />
        <StatCard label="Reviews" value={n(data?.reviewCount)} icon={<Users />} tone="sky" loading={isLoading} hint="Visible reviews only" to="/admin/reviews" />
        <StatCard label="Coupon redemptions" value={n(data?.couponRedemptionCount)} icon={<Ticket />} loading={isLoading} to="/admin/coupons" />
        <StatCard
          label="Coupon discounts given"
          value={inr(data?.couponDiscountTotal)}
          icon={<BadgePercent />}
          tone="amber"
          loading={isLoading}
          to="/admin/coupons"
        />
      </div>
    </div>
  );
}

export function AdminProductsPage() {
  useDocumentTitle("Admin · Products");
  return <ProductsTable scope="admin" />;
}

export function AdminProductFormPage() {
  useDocumentTitle("Admin · Product");
  return <ProductForm scope="admin" />;
}

export function AdminOrdersPage() {
  useDocumentTitle("Admin · Orders");
  return <OrdersBoard scope="admin" />;
}

// ------------------------------------------------------------------ categories
export function AdminCategoriesPage() {
  useDocumentTitle("Admin · Categories");
  const qc = useQueryClient();
  const { data, isLoading, error, refetch } = useCategories();
  const [editing, setEditing] = useState<Category | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Category | null>(null);
  const [name, setName] = useState("");

  const done = () => {
    qc.invalidateQueries({ queryKey: qk.categories });
    setEditing(null);
    setCreating(false);
    setDeleting(null);
  };
  const save = useMutation({
    mutationFn: () => (editing ? categoryAdminApi.update(editing.categoryId, name.trim()) : categoryAdminApi.create(name.trim())),
    onSuccess: () => (toast.success(editing ? "Category renamed" : "Category created"), done()),
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const remove = useMutation({
    mutationFn: (id: number) => categoryAdminApi.remove(id),
    onSuccess: () => (toast.success("Category deleted"), done()),
    onError: (err) => toast.error(getErrorMessage(err, "This category still has products and can't be deleted.")),
  });

  const open = (c: Category | null) => {
    setName(c?.categoryName ?? "");
    if (c) setEditing(c);
    else setCreating(true);
  };

  return (
    <div>
      <PageHeader
        title="Categories"
        description="Organise the catalog. Names must be unique and at least 5 characters."
        action={
          <Button onClick={() => open(null)}>
            <Plus className="h-4 w-4" /> New category
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
        ) : !data?.length ? (
          <EmptyState icon={<FolderTree />} title="No categories yet" action={<Button onClick={() => open(null)}>Create one</Button>} />
        ) : (
          <ul className="divide-y divide-slate-100">
            {data.map((c) => (
              <li key={c.categoryId} className="flex items-center justify-between px-5 py-3">
                <div className="flex items-center gap-3">
                  <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-50 text-sm font-bold text-brand-700">{c.categoryName.charAt(0)}</span>
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{c.categoryName}</p>
                    <p className="text-xs text-slate-400">#{c.categoryId}</p>
                  </div>
                </div>
                <div className="flex gap-1">
                  <Button variant="ghost" size="icon-sm" onClick={() => open(c)} aria-label="Rename">
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon-sm" className="hover:text-rose-600" onClick={() => setDeleting(c)} aria-label="Delete">
                    <Trash className="h-4 w-4" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Modal
        open={creating || !!editing}
        onClose={() => (setCreating(false), setEditing(null))}
        title={editing ? "Rename category" : "New category"}
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => (setCreating(false), setEditing(null))}>
              Cancel
            </Button>
            <Button loading={save.isPending} disabled={name.trim().length < 5} onClick={() => save.mutate()}>
              Save
            </Button>
          </>
        }
      >
        <FormField label="Category name" hint="At least 5 characters">
          {(id) => <Input id={id} value={name} onChange={(e) => setName(e.target.value)} autoFocus />}
        </FormField>
      </Modal>
      <ConfirmModal
        open={!!deleting}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.categoryId)}
        loading={remove.isPending}
        title={`Delete “${deleting?.categoryName}”?`}
        description="Products in this category may block deletion."
        confirmLabel="Delete"
      />
    </div>
  );
}

// ------------------------------------------------------------------ users
const ROLES: Role[] = ["ROLE_USER", "ROLE_SELLER", "ROLE_ADMIN"];

function RoleToggle({ user, role }: { user: AdminUser; role: Role }) {
  const qc = useQueryClient();
  const me = useAuthStore((s) => s.user);
  const has = user.roles.includes(role);
  const toggle = useMutation({
    mutationFn: () => (has ? adminApi.revokeRole(user.userId, role) : adminApi.grantRole(user.userId, role)),
    onSuccess: () => {
      toast.success(`${has ? "Removed" : "Granted"} ${titleCase(role.replace("ROLE_", ""))} for ${user.username}`);
      qc.invalidateQueries({ queryKey: ["admin-users"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const locked = has && role === "ROLE_ADMIN" && me?.userId === user.userId;
  return (
    <button
      disabled={toggle.isPending || locked}
      onClick={() => toggle.mutate()}
      title={locked ? "You can't remove your own admin role" : has ? "Click to revoke" : "Click to grant"}
      className={
        has
          ? "rounded-full bg-brand-600 px-2.5 py-1 text-xs font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
          : "rounded-full border border-dashed border-slate-300 px-2.5 py-1 text-xs font-semibold text-slate-400 hover:border-brand-400 hover:text-brand-700 disabled:opacity-60"
      }
    >
      {titleCase(role.replace("ROLE_", ""))}
    </button>
  );
}

export function AdminUsersPage() {
  useDocumentTitle("Admin · Users");
  const [page, setPage] = useState(0);
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["admin-users", page],
    queryFn: () => adminApi.users({ pageNumber: page, pageSize: 20 }),
    placeholderData: keepPreviousData,
  });
  return (
    <div>
      <PageHeader title="Users & roles" description="Click a role chip to grant or revoke it." />
      <Card>
        {isLoading ? (
          <div className="space-y-2 p-5">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-10" />
            ))}
          </div>
        ) : error ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : (
          <Table>
            <THead>
              <tr>
                <Th>User</Th>
                <Th>Email</Th>
                <Th>Status</Th>
                <Th>Roles</Th>
              </tr>
            </THead>
            <tbody>
              {data?.content.map((u) => (
                <Tr key={u.userId}>
                  <Td>
                    <p className="font-semibold text-slate-900">{u.name || u.username}</p>
                    <p className="text-xs text-slate-400">@{u.username} · #{u.userId}</p>
                  </Td>
                  <Td>{u.email}</Td>
                  <Td>{u.enabled ? <Badge tone="success">Active</Badge> : <Badge tone="warning">Unverified</Badge>}</Td>
                  <Td>
                    <div className="flex flex-wrap gap-1.5">
                      {ROLES.map((r) => (
                        <RoleToggle key={r} user={u} role={r} />
                      ))}
                    </div>
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>
      {data && <Pagination className="mt-6" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}

// ------------------------------------------------------------------ seller applications
export function AdminSellerApplicationsPage() {
  useDocumentTitle("Admin · Seller applications");
  const qc = useQueryClient();
  const [status, setStatus] = useState<SellerApplicationStatus>("PENDING");
  const [page, setPage] = useState(0);
  const [rejecting, setRejecting] = useState<SellerApplication | null>(null);
  const [reason, setReason] = useState("");
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["admin-applications", status, page],
    queryFn: () => adminApi.sellerApplications(status, { pageNumber: page, pageSize: 10 }),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["admin-applications"] });
  const approve = useMutation({
    mutationFn: adminApi.approveApplication,
    onSuccess: (a) => (toast.success(`${a.businessName} approved — the user is now a seller`), invalidate()),
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const reject = useMutation({
    mutationFn: () => adminApi.rejectApplication(rejecting!.id, reason.trim()),
    onSuccess: () => {
      toast.success("Application rejected");
      setRejecting(null);
      setReason("");
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div>
      <PageHeader
        title="Seller applications"
        action={
          <Segmented
            value={status}
            onChange={(v) => (setStatus(v), setPage(0))}
            options={[
              { value: "PENDING", label: "Pending" },
              { value: "APPROVED", label: "Approved" },
              { value: "REJECTED", label: "Rejected" },
            ]}
          />
        }
      />
      {isLoading ? (
        <Skeleton className="h-40 rounded-xl" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !data?.content.length ? (
        <Card>
          <EmptyState icon={<Store />} title={`No ${status.toLowerCase()} applications`} />
        </Card>
      ) : (
        <div className="space-y-3">
          {data.content.map((a) => (
            <Card key={a.id} className="p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 max-w-2xl">
                  <p className="text-base font-semibold text-slate-900">{a.businessName}</p>
                  <p className="text-xs text-slate-500">
                    User #{a.userId} · applied {formatDate(a.appliedAt)}
                    {a.decidedAt && ` · decided ${formatDate(a.decidedAt)}`}
                  </p>
                  {a.businessDescription && <p className="mt-2 whitespace-pre-line text-sm text-slate-600">{a.businessDescription}</p>}
                  {a.rejectionReason && <p className="mt-2 text-sm text-rose-700">Reason: {a.rejectionReason}</p>}
                </div>
                {a.status === "PENDING" && (
                  <div className="flex gap-2">
                    <Button variant="outline" onClick={() => setRejecting(a)}>
                      Reject
                    </Button>
                    <Button loading={approve.isPending && approve.variables === a.id} onClick={() => approve.mutate(a.id)}>
                      Approve
                    </Button>
                  </div>
                )}
              </div>
            </Card>
          ))}
          <Pagination className="pt-3" pageNumber={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
        </div>
      )}

      <Modal
        open={!!rejecting}
        onClose={() => setRejecting(null)}
        title={`Reject ${rejecting?.businessName}?`}
        description="The applicant is emailed this reason."
        footer={
          <>
            <Button variant="outline" onClick={() => setRejecting(null)}>
              Cancel
            </Button>
            <Button variant="danger" disabled={!reason.trim()} loading={reject.isPending} onClick={() => reject.mutate()}>
              Reject application
            </Button>
          </>
        }
      >
        <FormField label="Reason">{(id) => <Textarea id={id} rows={4} value={reason} onChange={(e) => setReason(e.target.value)} />}</FormField>
      </Modal>
    </div>
  );
}
