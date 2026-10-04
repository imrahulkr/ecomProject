import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CircleCheck, Truck, Undo2, X } from "lucide-react";
import { toast } from "sonner";
import { orderApi } from "@/api/endpoints";
import { getErrorMessage } from "@/api/errors";
import type { FulfillmentUpdate, OrderItem, OrderStatus } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { FormField, Input, Select } from "@/components/ui/Field";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { formatMoney } from "@/lib/format";

const carriers = ["Delhivery", "Blue Dart", "DTDC", "Ekart", "India Post", "Xpressbees", "Other"];

/**
 * PENDING → SHIPPED (tracking required) → DELIVERED, or PENDING → CANCELLED (refunds the customer).
 * Returns: RETURN_REQUESTED → RETURNED (refund + restock) or RETURN_REJECTED. Order must be PAID.
 */
export function FulfillmentActions({
  item,
  orderStatus,
  scope,
  invalidateKey,
}: {
  item: OrderItem;
  orderStatus: OrderStatus;
  scope: "seller" | "admin";
  invalidateKey: readonly unknown[];
}) {
  const qc = useQueryClient();
  const [shipOpen, setShipOpen] = useState(false);
  const [carrier, setCarrier] = useState(carriers[0]);
  const [otherCarrier, setOtherCarrier] = useState("");
  const [tracking, setTracking] = useState("");
  const [confirmCancel, setConfirmCancel] = useState(false);

  const update = useMutation({
    mutationFn: (body: FulfillmentUpdate) =>
      scope === "seller" ? orderApi.sellerFulfillment(item.orderItemId, body) : orderApi.adminFulfillment(item.orderItemId, body),
    onSuccess: (updated, body) => {
      const messages: Partial<Record<FulfillmentUpdate["status"], string>> = {
        SHIPPED: "Marked as shipped",
        DELIVERED: "Marked as delivered",
        CANCELLED: "Item cancelled",
        RETURNED: "Return approved",
        RETURN_REJECTED: "Return rejected",
      };
      toast.success(messages[body.status] ?? "Updated", {
        description:
          updated.refundStatus === "FAILED"
            ? "The refund didn't go through yet - it will be retried automatically."
            : updated.refundStatus
              ? `${formatMoney(updated.refundedMinorUnits, updated.currency)} refunded to the customer and stock restored.`
              : undefined,
      });
      setShipOpen(false);
      setConfirmCancel(false);
      qc.invalidateQueries({ queryKey: invalidateKey });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (orderStatus !== "PAID") return <span className="text-xs text-slate-400">Awaiting payment</span>;

  if (item.fulfillmentStatus === "PENDING") {
    return (
      <div className="flex flex-wrap gap-2">
        <Button size="sm" onClick={() => setShipOpen(true)}>
          <Truck className="h-3.5 w-3.5" /> Ship
        </Button>
        <Button size="sm" variant="ghost" className="text-slate-500 hover:text-rose-600" onClick={() => setConfirmCancel(true)}>
          <X className="h-3.5 w-3.5" /> Cancel
        </Button>
        <ConfirmModal
          open={confirmCancel}
          onClose={() => setConfirmCancel(false)}
          onConfirm={() => update.mutate({ status: "CANCELLED" })}
          loading={update.isPending}
          title="Cancel this item?"
          description="The customer is refunded for this item straight away and its stock goes back on sale."
          confirmLabel="Cancel & refund"
        />
        <Modal
          open={shipOpen}
          onClose={() => setShipOpen(false)}
          title="Mark as shipped"
          description="The customer is emailed the carrier and tracking number."
          size="sm"
          footer={
            <>
              <Button variant="outline" onClick={() => setShipOpen(false)}>
                Cancel
              </Button>
              <Button
                loading={update.isPending}
                disabled={!tracking.trim() || (carrier === "Other" && !otherCarrier.trim())}
                onClick={() =>
                  update.mutate({ status: "SHIPPED", carrier: carrier === "Other" ? otherCarrier.trim() : carrier, trackingNumber: tracking.trim() })
                }
              >
                Confirm shipment
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <FormField label="Carrier">
              {(id) => (
                <Select id={id} value={carrier} onChange={(e) => setCarrier(e.target.value)}>
                  {carriers.map((c) => (
                    <option key={c}>{c}</option>
                  ))}
                </Select>
              )}
            </FormField>
            {carrier === "Other" && (
              <FormField label="Carrier name">{(id) => <Input id={id} value={otherCarrier} onChange={(e) => setOtherCarrier(e.target.value)} />}</FormField>
            )}
            <FormField label="Tracking number">
              {(id) => <Input id={id} value={tracking} onChange={(e) => setTracking(e.target.value)} className="font-mono" />}
            </FormField>
          </div>
        </Modal>
      </div>
    );
  }

  if (item.fulfillmentStatus === "RETURN_REQUESTED") {
    return (
      <div className="space-y-2">
        {item.returnReason && <p className="max-w-xs text-xs text-slate-600">“{item.returnReason}”</p>}
        <div className="flex flex-wrap gap-2">
          <Button size="sm" loading={update.isPending && update.variables?.status === "RETURNED"} onClick={() => update.mutate({ status: "RETURNED" })}>
            <Undo2 className="h-3.5 w-3.5" /> Approve return
          </Button>
          <Button size="sm" variant="ghost" className="text-slate-500 hover:text-rose-600" loading={update.isPending && update.variables?.status === "RETURN_REJECTED"} onClick={() => update.mutate({ status: "RETURN_REJECTED" })}>
            <X className="h-3.5 w-3.5" /> Reject
          </Button>
        </div>
      </div>
    );
  }

  if (item.refundStatus) {
    return <RefundNote item={item} />;
  }

  if (item.fulfillmentStatus === "SHIPPED") {
    return (
      <Button size="sm" variant="outline" loading={update.isPending} onClick={() => update.mutate({ status: "DELIVERED" })}>
        <CircleCheck className="h-3.5 w-3.5" /> Mark delivered
      </Button>
    );
  }

  return null;
}

/** Refund outcome for a cancelled/returned item (shared by the seller/admin board and the customer's order page). */
export function RefundNote({ item }: { item: OrderItem }) {
  if (!item.refundStatus) return null;
  const amount = formatMoney(item.refundedMinorUnits, item.currency);
  const text =
    item.refundStatus === "SUCCEEDED"
      ? `Refunded ${amount}`
      : item.refundStatus === "FAILED"
        ? `Refund of ${amount} pending - retrying`
        : `Refund of ${amount} processing`;
  return <p className={item.refundStatus === "SUCCEEDED" ? "text-xs text-emerald-700" : "text-xs text-amber-700"}>{text}</p>;
}
