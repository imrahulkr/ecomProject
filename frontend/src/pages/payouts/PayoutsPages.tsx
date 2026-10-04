import { useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Banknote, Clock, HandCoins, Wallet } from "lucide-react";
import { toast } from "sonner";
import { payoutApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { LedgerEntryType, SellerBalance } from "@/api/types";
import { cn } from "@/lib/cn";
import { formatDateTime, formatMoney } from "@/lib/format";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, CardHeader, PageHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { FormField, Input } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { Table, THead, Th, Tr, Td } from "@/components/ui/Table";
import { StatCard } from "@/components/dashboard/StatCard";

const entryLabels: Record<LedgerEntryType, string> = {
  SALE: "Sale",
  COMMISSION: "Commission",
  REFUND: "Refund",
  COMMISSION_REVERSAL: "Commission returned",
};

function SignedAmount({ value, currency }: { value: number; currency: string }) {
  return (
    <span className={cn("font-medium tabular-nums", value < 0 ? "text-rose-700" : "text-emerald-700")}>
      {value < 0 ? "−" : "+"}
      {formatMoney(Math.abs(value), currency)}
    </span>
  );
}

// ------------------------------------------------------------------ seller
export function SellerPayoutsPage() {
  useDocumentTitle("Seller · Payouts");
  const [page, setPage] = useState(0);
  const summary = useQuery({ queryKey: ["seller-earnings"], queryFn: payoutApi.summary });
  const ledger = useQuery({
    queryKey: ["seller-ledger", page],
    queryFn: () => payoutApi.ledger({ pageNumber: page, pageSize: 15 }),
    placeholderData: keepPreviousData,
  });
  const s = summary.data;

  if (summary.error) return <ErrorState error={summary.error} onRetry={() => summary.refetch()} />;
  return (
    <div className="space-y-6">
      <PageHeader
        title="Payouts"
        description={
          s
            ? `Each sale earns its price minus a ${s.commissionPercent}% marketplace commission. Earnings become available ${s.returnWindowDays} days after delivery, once returns are no longer possible.`
            : "Your earnings and payouts."
        }
      />
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Available for payout" value={formatMoney(s?.availableMinorUnits, s?.currency)} icon={<Wallet />} tone="emerald" loading={summary.isLoading} />
        <StatCard label="Pending" value={formatMoney(s?.pendingMinorUnits, s?.currency)} icon={<Clock />} tone="amber" loading={summary.isLoading} hint="Still inside the return window" />
        <StatCard label="Paid out" value={formatMoney(s?.paidOutMinorUnits, s?.currency)} icon={<Banknote />} tone="sky" loading={summary.isLoading} />
      </div>
      <Card>
        <CardHeader title="Earnings activity" />
        {ledger.isLoading ? (
          <div className="space-y-2 p-5">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-10" />
            ))}
          </div>
        ) : ledger.error ? (
          <ErrorState error={ledger.error} onRetry={() => ledger.refetch()} />
        ) : !ledger.data?.content.length ? (
          <EmptyState icon={<HandCoins />} title="No earnings yet" description="Sales appear here as soon as an order containing your products is paid." />
        ) : (
          <>
            <Table>
              <THead>
                <tr>
                  <Th>Date</Th>
                  <Th>Type</Th>
                  <Th>Order</Th>
                  <Th className="text-right">Amount</Th>
                  <Th>Status</Th>
                </tr>
              </THead>
              <tbody>
                {ledger.data.content.map((e) => (
                  <Tr key={e.id}>
                    <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateTime(e.createdAt)}</Td>
                    <Td>{entryLabels[e.entryType]}</Td>
                    <Td>#{e.orderId}</Td>
                    <Td className="text-right">
                      <SignedAmount value={e.amountMinorUnits} currency={e.currency} />
                    </Td>
                    <Td>{e.paidOut ? <Badge tone="success">Paid out</Badge> : <Badge tone="neutral">Unpaid</Badge>}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <Pagination className="p-4" pageNumber={ledger.data.pageNumber} totalPages={ledger.data.totalPages} onChange={setPage} />
          </>
        )}
      </Card>
    </div>
  );
}

// ------------------------------------------------------------------ admin
export function AdminPayoutsPage() {
  useDocumentTitle("Admin · Payouts");
  const qc = useQueryClient();
  const [paying, setPaying] = useState<SellerBalance | null>(null);
  const [reference, setReference] = useState("");
  const [page, setPage] = useState(0);
  const balances = useQuery({ queryKey: ["admin-payout-balances"], queryFn: payoutApi.balances });
  const history = useQuery({
    queryKey: ["admin-payouts", page],
    queryFn: () => payoutApi.all({ pageNumber: page, pageSize: 10 }),
    placeholderData: keepPreviousData,
  });

  const pay = useMutation({
    mutationFn: () => payoutApi.pay(paying!.sellerId, reference.trim()),
    onSuccess: (p) => {
      toast.success(`Recorded payout of ${formatMoney(p.amountMinorUnits, p.currency)}`);
      setPaying(null);
      setReference("");
      qc.invalidateQueries({ queryKey: ["admin-payout-balances"] });
      qc.invalidateQueries({ queryKey: ["admin-payouts"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Seller payouts"
        description="Pay sellers what has settled (delivered and past the return window, net of commission and refunds). Transfer the money first, then record it here."
      />
      <Card>
        <CardHeader title="Balances" />
        {balances.isLoading ? (
          <div className="p-5">
            <Skeleton className="h-24" />
          </div>
        ) : balances.error ? (
          <ErrorState error={balances.error} onRetry={() => balances.refetch()} />
        ) : !balances.data?.length ? (
          <EmptyState icon={<Wallet />} title="Nothing owed" description="Seller earnings appear here once their orders are paid." />
        ) : (
          <Table>
            <THead>
              <tr>
                <Th>Seller</Th>
                <Th className="text-right">Pending</Th>
                <Th className="text-right">Available</Th>
                <Th className="text-right">Paid out</Th>
                <Th />
              </tr>
            </THead>
            <tbody>
              {balances.data.map((b) => (
                <Tr key={b.sellerId}>
                  <Td>
                    <p className="font-medium text-slate-900">{b.sellerName}</p>
                    {b.email && <p className="text-xs text-slate-500">{b.email}</p>}
                  </Td>
                  <Td className="text-right tabular-nums">{formatMoney(b.pendingMinorUnits, b.currency)}</Td>
                  <Td className="text-right font-semibold tabular-nums text-slate-900">{formatMoney(b.availableMinorUnits, b.currency)}</Td>
                  <Td className="text-right tabular-nums">{formatMoney(b.paidOutMinorUnits, b.currency)}</Td>
                  <Td className="text-right">
                    <Button size="sm" disabled={b.availableMinorUnits <= 0} onClick={() => setPaying(b)}>
                      Record payout
                    </Button>
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>

      <Card>
        <CardHeader title="Payout history" />
        {history.data?.content.length ? (
          <>
            <Table>
              <THead>
                <tr>
                  <Th>Date</Th>
                  <Th>Seller</Th>
                  <Th>Reference</Th>
                  <Th className="text-right">Amount</Th>
                </tr>
              </THead>
              <tbody>
                {history.data.content.map((p) => (
                  <Tr key={p.id}>
                    <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateTime(p.createdAt)}</Td>
                    <Td>{p.sellerName ?? `Seller #${p.sellerId}`}</Td>
                    <Td className="font-mono text-xs">{p.reference ?? "—"}</Td>
                    <Td className="text-right font-medium tabular-nums">{formatMoney(p.amountMinorUnits, p.currency)}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <Pagination className="p-4" pageNumber={history.data.pageNumber} totalPages={history.data.totalPages} onChange={setPage} />
          </>
        ) : (
          <p className="px-5 pb-5 text-sm text-slate-500">{history.isLoading ? "Loading…" : "No payouts recorded yet."}</p>
        )}
      </Card>

      <Modal
        open={!!paying}
        onClose={() => setPaying(null)}
        title={`Pay ${paying?.sellerName ?? "seller"}`}
        description={paying ? `Records a payout of ${formatMoney(paying.availableMinorUnits, paying.currency)} - everything currently available.` : undefined}
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setPaying(null)}>
              Cancel
            </Button>
            <Button loading={pay.isPending} onClick={() => pay.mutate()}>
              Record payout
            </Button>
          </>
        }
      >
        <FormField label="Transfer reference" hint="Optional - e.g. the bank UTR number">
          {(id) => <Input id={id} value={reference} onChange={(e) => setReference(e.target.value)} maxLength={255} className="font-mono" />}
        </FormField>
      </Modal>
    </div>
  );
}
