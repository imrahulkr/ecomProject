import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { addressApi } from "@/api/endpoints";
import { getErrorMessage, getFieldErrors } from "@/api/errors";
import type { Address, AddressInput } from "@/api/types";
import { qk } from "@/hooks/queries";
import { FormField, Input } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";

// Mirrors the backend's AddressDTO constraints so users see errors before submitting.
const schema = z.object({
  buildingName: z.string().trim().min(4, "At least 4 characters"),
  street: z.string().trim().min(5, "At least 5 characters").max(50, "At most 50 characters"),
  city: z.string().trim().min(4, "At least 4 characters"),
  state: z.string().trim().min(3, "At least 3 characters"),
  country: z.string().trim().min(3, "At least 3 characters"),
  pincode: z.string().trim().min(6, "At least 6 characters"),
});

type Values = z.infer<typeof schema>;

export function AddressFormModal({
  open,
  onClose,
  address,
  onSaved,
}: {
  open: boolean;
  onClose: () => void;
  address?: Address | null;
  onSaved?: (a: Address) => void;
}) {
  const qc = useQueryClient();
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    values: address
      ? {
          buildingName: address.buildingName,
          street: address.street,
          city: address.city,
          state: address.state,
          country: address.country,
          pincode: address.pincode,
        }
      : { buildingName: "", street: "", city: "", state: "", country: "India", pincode: "" },
  });
  const { register, handleSubmit, formState, setError } = form;

  const save = useMutation({
    mutationFn: (v: AddressInput) => (address ? addressApi.update(address.addressId, v) : addressApi.create(v)),
    onSuccess: (saved) => {
      toast.success(address ? "Address updated" : "Address added");
      qc.invalidateQueries({ queryKey: qk.addresses });
      onSaved?.(saved);
      onClose();
    },
    onError: (err) => {
      const fields = getFieldErrors(err);
      Object.entries(fields).forEach(([k, msg]) => setError(k as keyof Values, { message: msg }));
      if (!Object.keys(fields).length) toast.error(getErrorMessage(err));
    },
  });

  const err = (k: keyof Values) => formState.errors[k]?.message;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={address ? "Edit address" : "Add a new address"}
      description="Where should we deliver your order?"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="address-form" loading={save.isPending}>
            Save address
          </Button>
        </>
      }
    >
      <form id="address-form" onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-4 sm:grid-cols-2">
        <FormField label="Flat / building" error={err("buildingName")} className="sm:col-span-2">
          {(id) => <Input id={id} placeholder="Flat 12B, Sunrise Towers" invalid={!!err("buildingName")} {...register("buildingName")} />}
        </FormField>
        <FormField label="Street / area" error={err("street")} className="sm:col-span-2">
          {(id) => <Input id={id} placeholder="MG Road, Indiranagar" invalid={!!err("street")} {...register("street")} />}
        </FormField>
        <FormField label="City" error={err("city")}>
          {(id) => <Input id={id} placeholder="Bengaluru" invalid={!!err("city")} {...register("city")} />}
        </FormField>
        <FormField label="State" error={err("state")}>
          {(id) => <Input id={id} placeholder="Karnataka" invalid={!!err("state")} {...register("state")} />}
        </FormField>
        <FormField label="Country" error={err("country")}>
          {(id) => <Input id={id} invalid={!!err("country")} {...register("country")} />}
        </FormField>
        <FormField label="PIN code" error={err("pincode")}>
          {(id) => <Input id={id} placeholder="560038" inputMode="numeric" invalid={!!err("pincode")} {...register("pincode")} />}
        </FormField>
      </form>
    </Modal>
  );
}

export function AddressText({ address, className }: { address: Address; className?: string }) {
  return (
    <div className={className}>
      <p className="text-sm font-semibold text-slate-900">{address.buildingName}</p>
      <p className="text-sm text-slate-600">
        {address.street}, {address.city}
      </p>
      <p className="text-sm text-slate-600">
        {address.state} {address.pincode}, {address.country}
      </p>
    </div>
  );
}
