import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, ChartColumn, Package, RefreshCw, Store, Truck } from "lucide-react";
import { toast } from "sonner";
import { sellerApi } from "@/api/endpoints";
import { refreshSession } from "@/api/client";
import { getErrorMessage } from "@/api/errors";
import { formatDate } from "@/lib/format";
import { isSeller, useAuthStore } from "@/store/auth";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, CardBody, CardHeader, PageHeader } from "@/components/ui/Card";
import { FormField, Input, Textarea } from "@/components/ui/Field";
import { Button, ButtonLink } from "@/components/ui/Button";
import { ApplicationStatusBadge } from "@/components/ui/Badge";
import { Alert, Skeleton } from "@/components/ui/Feedback";

const schema = z.object({
  businessName: z.string().trim().min(3, "At least 3 characters").max(255),
  businessDescription: z.string().trim().max(2000).optional(),
});
type Values = z.infer<typeof schema>;

export default function SellerApplyPage() {
  useDocumentTitle("Sell on Vendora");
  const qc = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const { data: applications, isLoading } = useQuery({ queryKey: ["seller-applications", "me"], queryFn: sellerApi.myApplications });
  const { register, handleSubmit, formState, reset } = useForm<Values>({ resolver: zodResolver(schema) });

  const apply = useMutation({
    mutationFn: (v: Values) => sellerApi.apply(v.businessName, v.businessDescription || undefined),
    onSuccess: () => {
      toast.success("Application submitted", { description: "We'll email you once it's reviewed." });
      reset();
      qc.invalidateQueries({ queryKey: ["seller-applications", "me"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // Role changes only appear in the access token after a refresh.
  const recheck = useMutation({
    mutationFn: refreshSession,
    onSuccess: (token) => {
      const seller = isSeller(useAuthStore.getState().user);
      if (token && seller) toast.success("You're a seller now!");
      else toast.info("Your seller access isn't active yet.");
    },
  });

  const approved = applications?.some((a) => a.status === "APPROVED");
  const pending = applications?.some((a) => a.status === "PENDING");

  if (isSeller(user)) {
    return (
      <Card>
        <CardBody className="flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-900">You're a seller on Vendora</h1>
            <p className="text-sm text-slate-500">Manage your catalog, orders and reviews from the seller dashboard.</p>
          </div>
          <ButtonLink to="/seller/dashboard">
            Open seller dashboard <ArrowRight className="h-4 w-4" />
          </ButtonLink>
        </CardBody>
      </Card>
    );
  }

  return (
    <div className="space-y-5">
      <PageHeader eyebrow="Vendora for business" title="Start selling on Vendora" description="Reach thousands of shoppers with no setup fees." />

      <div className="grid gap-3 sm:grid-cols-3">
        {[
          { icon: Package, t: "List in minutes", s: "Add products with photos and pricing" },
          { icon: Truck, t: "Ship your way", s: "Add tracking and update customers" },
          { icon: ChartColumn, t: "Grow with reviews", s: "Reply to customers publicly" },
        ].map(({ icon: Icon, t, s }) => (
          <Card key={t} className="p-4">
            <Icon className="h-5 w-5 text-brand-600" />
            <p className="mt-2 text-sm font-semibold text-slate-900">{t}</p>
            <p className="text-xs text-slate-500">{s}</p>
          </Card>
        ))}
      </div>

      {approved && (
        <Alert tone="success" title="Your application was approved">
          <span className="flex flex-wrap items-center gap-3">
            Refresh your session to unlock the seller dashboard.
            <Button size="sm" variant="outline" loading={recheck.isPending} onClick={() => recheck.mutate()}>
              <RefreshCw className="h-3.5 w-3.5" /> Activate seller access
            </Button>
          </span>
        </Alert>
      )}

      {!pending && !approved && (
        <Card>
          <CardHeader title={<span className="flex items-center gap-2"><Store className="h-4 w-4 text-brand-600" /> Seller application</span>} />
          <CardBody>
            <form onSubmit={handleSubmit((v) => apply.mutate(v))} className="grid max-w-xl gap-4">
              <FormField label="Business name" error={formState.errors.businessName?.message}>
                {(id) => <Input id={id} placeholder="Acme Handcrafts" invalid={!!formState.errors.businessName} {...register("businessName")} />}
              </FormField>
              <FormField label="Tell us about your business" hint="What do you sell? Optional, up to 2000 characters.">
                {(id) => <Textarea id={id} rows={5} {...register("businessDescription")} />}
              </FormField>
              <div>
                <Button type="submit" loading={apply.isPending}>
                  Submit application
                </Button>
              </div>
            </form>
          </CardBody>
        </Card>
      )}

      <Card>
        <CardHeader title="Application history" />
        <CardBody>
          {isLoading ? (
            <Skeleton className="h-14 w-full" />
          ) : !applications?.length ? (
            <p className="text-sm text-slate-500">You haven't applied yet.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {applications.map((a) => (
                <li key={a.id} className="flex flex-wrap items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{a.businessName}</p>
                    <p className="text-xs text-slate-500">
                      Applied {formatDate(a.appliedAt)}
                      {a.decidedAt && ` · decided ${formatDate(a.decidedAt)}`}
                    </p>
                    {a.rejectionReason && <p className="mt-1 text-xs text-rose-700">Reason: {a.rejectionReason}</p>}
                  </div>
                  <ApplicationStatusBadge status={a.status} />
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
