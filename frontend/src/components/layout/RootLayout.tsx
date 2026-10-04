import { useEffect } from "react";
import { Outlet, ScrollRestoration } from "react-router";
import { Toaster } from "sonner";
import { refreshSession } from "@/api/client";

/** Restores the session from the httpOnly refresh cookie once per page load. */
function useSessionBootstrap() {
  useEffect(() => {
    void refreshSession();
  }, []);
}

export function RootLayout() {
  useSessionBootstrap();
  return (
    <>
      <Outlet />
      <ScrollRestoration />
      <Toaster position="top-center" richColors closeButton toastOptions={{ className: "font-sans" }} />
    </>
  );
}
