import { create } from "zustand";
import { decodeJwt } from "@/lib/jwt";
import type { AuthResponse, Role } from "@/api/types";

export type SessionUser = {
  userId: number;
  username: string;
  email: string;
  name: string | null;
  roles: Role[];
  hasPassword: boolean;
  linkedProviders: string[];
};

type AuthStatus = "loading" | "authenticated" | "anonymous";

type AuthState = {
  status: AuthStatus;
  accessToken: string | null;
  user: SessionUser | null;
  setFromAuthResponse: (res: AuthResponse) => void;
  setFromAccessToken: (token: string) => void;
  clear: () => void;
};

function userFromToken(token: string, summary?: AuthResponse["user"]): SessionUser {
  const claims = decodeJwt(token);
  return {
    userId: Number(claims.userId ?? summary?.id ?? 0),
    username: claims.sub ?? "",
    email: claims.email ?? summary?.email ?? "",
    name: summary?.name ?? null,
    roles: (claims.roles ?? []) as Role[],
    hasPassword: summary?.hasPassword ?? true,
    linkedProviders: summary?.linkedProviders ?? claims.providers ?? [],
  };
}

// The access token is intentionally kept in memory only; the httpOnly refresh cookie restores
// the session on reload.
export const useAuthStore = create<AuthState>((set, get) => ({
  status: "loading",
  accessToken: null,
  user: null,
  setFromAuthResponse: (res) =>
    set({ status: "authenticated", accessToken: res.accessToken, user: userFromToken(res.accessToken, res.user) }),
  setFromAccessToken: (token) => {
    const prev = get().user;
    const next = userFromToken(token);
    set({
      status: "authenticated",
      accessToken: token,
      user: prev && prev.userId === next.userId ? { ...prev, ...next, name: prev.name, hasPassword: prev.hasPassword } : next,
    });
  },
  clear: () => set({ status: "anonymous", accessToken: null, user: null }),
}));

export const hasRole = (user: SessionUser | null, role: Role) => !!user?.roles.includes(role);
export const isSeller = (user: SessionUser | null) => hasRole(user, "ROLE_SELLER") || hasRole(user, "ROLE_ADMIN");
export const isAdmin = (user: SessionUser | null) => hasRole(user, "ROLE_ADMIN");
