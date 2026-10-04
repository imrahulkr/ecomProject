import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MapPin, Pencil, Plus, Trash } from "lucide-react";
import { toast } from "sonner";
import { addressApi } from "@/api/endpoints";
import { getErrorMessage, getStatus } from "@/api/errors";
import type { Address } from "@/api/types";
import { qk, useAddresses } from "@/hooks/queries";
import { useDocumentTitle } from "@/hooks/useUtils";
import { Card, PageHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/Feedback";
import { ConfirmModal } from "@/components/ui/Modal";
import { AddressFormModal, AddressText } from "@/components/checkout/AddressForm";

export default function AddressesPage() {
  useDocumentTitle("Addresses");
  const qc = useQueryClient();
  const { data, isLoading, error, refetch } = useAddresses();
  const [editing, setEditing] = useState<Address | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Address | null>(null);

  const remove = useMutation({
    mutationFn: (id: number) => addressApi.remove(id),
    onSuccess: () => {
      toast.success("Address removed");
      setDeleting(null);
      qc.invalidateQueries({ queryKey: qk.addresses });
    },
    onError: (err) =>
      toast.error(
        getStatus(err) === 500 ? "This address is linked to an existing order and can't be removed." : getErrorMessage(err),
      ),
  });

  return (
    <div>
      <PageHeader
        title="Addresses"
        description="Manage where your orders are delivered."
        action={
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4" /> Add address
          </Button>
        }
      />
      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-36 rounded-xl" />
          <Skeleton className="h-36 rounded-xl" />
        </div>
      ) : error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !data?.length ? (
        <Card>
          <EmptyState icon={<MapPin />} title="No saved addresses" description="Add an address to speed up checkout." action={<Button onClick={() => setCreating(true)}>Add address</Button>} />
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {data.map((a) => (
            <Card key={a.addressId} className="flex flex-col p-5">
              <MapPin className="mb-3 h-5 w-5 text-brand-600" />
              <AddressText address={a} className="flex-1" />
              <div className="mt-4 flex gap-2 border-t border-slate-100 pt-3">
                <Button variant="ghost" size="sm" onClick={() => setEditing(a)}>
                  <Pencil className="h-3.5 w-3.5" /> Edit
                </Button>
                <Button variant="ghost" size="sm" className="hover:text-rose-600" onClick={() => setDeleting(a)}>
                  <Trash className="h-3.5 w-3.5" /> Remove
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <AddressFormModal open={creating || !!editing} address={editing} onClose={() => (setCreating(false), setEditing(null))} />
      <ConfirmModal
        open={!!deleting}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.addressId)}
        loading={remove.isPending}
        title="Remove this address?"
        confirmLabel="Remove"
      />
    </div>
  );
}
