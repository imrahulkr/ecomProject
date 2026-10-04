import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { authApi } from "@/api/endpoints";
import { useAuthStore } from "@/store/auth";

export function useLogout() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const clear = useAuthStore((s) => s.clear);

  return async () => {
    try {
      await authApi.logout();
    } catch {
      // best effort - the session is dropped client-side regardless
    }
    clear();
    qc.clear();
    toast.success("You've been signed out");
    navigate("/");
  };
}
