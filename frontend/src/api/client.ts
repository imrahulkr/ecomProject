import axios, { type InternalAxiosRequestConfig } from "axios";
import { env } from "@/config/env";
import { useAuthStore } from "@/store/auth";
import type { AuthResponse, Page, PageQuery } from "./types";

declare module "axios" {
  interface InternalAxiosRequestConfig {
    _retried?: boolean;
  }
}

export const http = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true, // the refresh-token cookie must travel with every call
});

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshInFlight: Promise<string | null> | null = null;

/** Single-flight refresh: concurrent 401s share one rotation of the refresh cookie. */
export function refreshSession(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = axios
      // X-Requested-With is required by the backend on cookie-authenticated endpoints (CSRF guard).
      .post<AuthResponse>(`${env.apiBaseUrl}/api/auth/refresh_secure`, null, {
        withCredentials: true,
        headers: { "X-Requested-With": "XMLHttpRequest" },
      })
      .then((res) => {
        useAuthStore.getState().setFromAuthResponse(res.data);
        return res.data.accessToken;
      })
      .catch(() => {
        useAuthStore.getState().clear();
        return null;
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config as InternalAxiosRequestConfig | undefined;
    const url = original?.url ?? "";
    // /api/auth/* 401s are real answers (bad password etc.), never a reason to refresh.
    if (error.response?.status === 401 && original && !original._retried && !url.includes("/api/auth/")) {
      original._retried = true;
      const token = await refreshSession();
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return http(original);
      }
    }
    return Promise.reject(error);
  },
);

type RawPage<T> = {
  content?: T[];
  pageNumber?: number;
  pageSize?: number;
  totalElement?: number;
  totalElements?: number;
  totalPages?: number;
  lastPage?: boolean;
  number?: number;
  size?: number;
  last?: boolean;
  page?: { number?: number; size?: number; totalElements?: number; totalPages?: number };
};

/** The backend uses three different page shapes - normalize them all here. */
export function toPage<T>(raw: RawPage<T>): Page<T> {
  const content = raw.content ?? [];
  const pageNumber = raw.pageNumber ?? raw.number ?? raw.page?.number ?? 0;
  const pageSize = raw.pageSize ?? raw.size ?? raw.page?.size ?? content.length;
  const totalElements = raw.totalElements ?? raw.totalElement ?? raw.page?.totalElements ?? content.length;
  const totalPages = raw.totalPages ?? raw.page?.totalPages ?? 1;
  const lastPage = raw.lastPage ?? raw.last ?? pageNumber >= totalPages - 1;
  return { content, pageNumber, pageSize, totalElements, totalPages, lastPage };
}

export function pageParams(q: PageQuery = {}) {
  const params: Record<string, string | number> = {};
  if (q.pageNumber != null) params.pageNumber = q.pageNumber;
  if (q.pageSize != null) params.pageSize = q.pageSize;
  if (q.sortBy) params.sortBy = q.sortBy;
  if (q.sortOrder) params.sortOrder = q.sortOrder;
  return params;
}
