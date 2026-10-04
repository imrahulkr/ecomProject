import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { KeyRound, Link2, ShieldCheck, UserRound } from "lucide-react";
import { toast } from "sonner";
import { authApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import { titleCase } from "@/lib/format";
import { useAuthStore } from "@/store/auth";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, CardBody, CardHeader, PageHeader } from "@/components/ui/Card";
import { FormField, PasswordInput } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Skeleton } from "@/components/ui/Feedback";

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Enter your current password"),
    newPassword: z.string().min(8, "At least 8 characters").max(20, "At most 20 characters"),
    confirm: z.string(),
  })
  .refine((v) => v.newPassword === v.confirm, { path: ["confirm"], message: "Passwords don't match" });

type PasswordValues = z.infer<typeof passwordSchema>;

function ChangePassword() {
  const { register, handleSubmit, formState, reset } = useForm<PasswordValues>({ resolver: zodResolver(passwordSchema) });
  const qc = useQueryClient();
  const navigate = useNavigate();
  const clearSession = useAuthStore((s) => s.clear);
  const change = useMutation({
    mutationFn: (v: PasswordValues) => authApi.changePassword(v.currentPassword, v.newPassword),
    // The backend ends every session on a password change (including this one), so sign in again.
    onSuccess: () => {
      reset();
      clearSession();
      qc.clear();
      toast.success("Password updated. Please sign in with your new password.");
      navigate("/login", { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const e = formState.errors;
  return (
    <form onSubmit={handleSubmit((v) => change.mutate(v))} className="grid gap-4 sm:max-w-md">
      <FormField label="Current password" error={e.currentPassword?.message}>
        {(id) => <PasswordInput id={id} autoComplete="current-password" invalid={!!e.currentPassword} {...register("currentPassword")} />}
      </FormField>
      <FormField label="New password" error={e.newPassword?.message} hint="8–20 characters">
        {(id) => <PasswordInput id={id} autoComplete="new-password" invalid={!!e.newPassword} {...register("newPassword")} />}
      </FormField>
      <FormField label="Confirm new password" error={e.confirm?.message}>
        {(id) => <PasswordInput id={id} autoComplete="new-password" invalid={!!e.confirm} {...register("confirm")} />}
      </FormField>
      <div>
        <Button type="submit" loading={change.isPending}>
          Update password
        </Button>
      </div>
    </form>
  );
}

function LinkedAccounts() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["linked-accounts"], queryFn: authApi.linkedAccounts });
  const unlink = useMutation({
    mutationFn: authApi.unlinkProvider,
    onSuccess: () => {
      toast.success("Account unlinked");
      qc.invalidateQueries({ queryKey: ["linked-accounts"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) return <Skeleton className="h-16 w-full" />;
  return (
    <div className="space-y-3">
      {["google", "github"].map((provider) => {
        const linked = data?.linkedProviders.includes(provider);
        return (
          <div key={provider} className="flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3">
            <div className="flex items-center gap-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-sm font-bold text-slate-700">
                {provider === "google" ? "G" : "GH"}
              </span>
              <div>
                <p className="text-sm font-semibold text-slate-900">{titleCase(provider)}</p>
                <p className="text-xs text-slate-500">{linked ? "Connected — you can sign in with it" : "Not connected"}</p>
              </div>
            </div>
            {linked ? (
              <Button variant="outline" size="sm" loading={unlink.isPending && unlink.variables === provider} onClick={() => unlink.mutate(provider)}>
                Disconnect
              </Button>
            ) : (
              <Badge>Sign in with {titleCase(provider)} using this email to connect</Badge>
            )}
          </div>
        );
      })}
      {data && !data.hasPassword && (
        <p className="text-xs text-amber-700">Your account has no password, so you can't disconnect your only sign-in method.</p>
      )}
    </div>
  );
}

export default function AccountSettingsPage() {
  useDocumentTitle("Account settings");
  const user = useAuthStore((s) => s.user);

  return (
    <div className="space-y-5">
      <PageHeader title="Account settings" description="Your profile, password and connected sign-in methods." />

      <Card>
        <CardHeader title={<span className="flex items-center gap-2"><UserRound className="h-4 w-4 text-brand-600" /> Profile</span>} />
        <CardBody>
          <dl className="grid gap-4 sm:grid-cols-3">
            {[
              ["Name", user?.name || "—"],
              ["Username", user?.username || "—"],
              ["Email", user?.email || "—"],
            ].map(([k, v]) => (
              <div key={k}>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{k}</dt>
                <dd className="mt-1 truncate text-sm font-medium text-slate-900">{v}</dd>
              </div>
            ))}
          </dl>
          <div className="mt-4 flex flex-wrap gap-2">
            {user?.roles.map((r) => (
              <Badge key={r} tone={r === "ROLE_ADMIN" ? "danger" : r === "ROLE_SELLER" ? "warning" : "brand"}>
                <ShieldCheck className="h-3 w-3" /> {titleCase(r.replace("ROLE_", ""))}
              </Badge>
            ))}
          </div>
        </CardBody>
      </Card>

      {user?.hasPassword !== false && (
        <Card>
          <CardHeader title={<span className="flex items-center gap-2"><KeyRound className="h-4 w-4 text-brand-600" /> Change password</span>} />
          <CardBody>
            <ChangePassword />
          </CardBody>
        </Card>
      )}

      <Card>
        <CardHeader
          title={<span className="flex items-center gap-2"><Link2 className="h-4 w-4 text-brand-600" /> Connected accounts</span>}
          description="Sign in faster with Google or GitHub."
        />
        <CardBody>
          <LinkedAccounts />
        </CardBody>
      </Card>
    </div>
  );
}
