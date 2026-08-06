# Frontend Build — Progress Tracker

Living document for the React frontend build. Update the checklist and session log
as work happens so progress survives across Claude sessions. This file is the
source of truth for "where are we" — read it first when resuming frontend work.

## Decisions (locked in)

- **Location**: `frontend/` subfolder of this repo (monorepo-style), not a separate repo.
- **Stack**: React + Vite (`react-ts` template), Tailwind CSS, Axios.
- **UI components**: shadcn/ui (Radix + Tailwind, copy-in components).
- **State/data**: TanStack Query (server state) + Zustand (client state, e.g. auth).
- **Toasts**: sonner (pairs naturally with shadcn/ui).
- **Types**: generated from the live OpenAPI spec via `openapi-typescript`, not hand-written —
  regenerate whenever backend DTOs change: `npx openapi-typescript http://localhost:8080/v3/api-docs -o frontend/src/api/schema.d.ts`

## Backend contract notes (don't re-derive these — read them here)

- Auth: JWT access token (header) + httpOnly refresh-token cookie via `SecureAuthController`
  (`/api/auth/refresh_secure`, `/api/auth/logout_secure`). Axios instance needs `withCredentials: true`.
- Route security prefixes (`SecurityConfig`): `/api/admin/**` → `ROLE_ADMIN`, `/api/seller/**` →
  `ROLE_ADMIN`/`ROLE_SELLER`, `/api/public/**` and `/api/auth/**` → open, everything else → authenticated.
- Money: integer minor units (`long amountMinorUnits` etc.) + sibling `currency` string. Divide by
  100 to display. `app.currency=INR` (2-decimal assumption baked into backend formatting).
- Checkout requires an `Idempotency-Key` header (UUID per checkout attempt, not per retry).
- CORS: `allowCredentials(true)`, allowed headers `Authorization`/`Content-Type`, methods
  GET/POST/PUT/PATCH/DELETE/OPTIONS. Frontend dev server origin must be added to `allowedOrigins` config.
- OpenAPI spec live at `/v3/api-docs` (springdoc dependency confirmed active in `pom.xml`).

## Known backend gaps affecting frontend phases

- No `GET /api/public/products/{productId}` (product-detail-by-id) — **blocks Phase 2** product
  detail page until added.
- No cart clear-all endpoint.
- No order cancellation endpoint.
- No reviews/ratings, wishlist, or coupon/discount APIs anywhere.
- `AddressDTO` has no `isDefault` flag.
- Address/Cart list endpoints return unscoped results (flagged, not yet fixed — out of scope unless asked).

## Phase checklist

- [x] **Phase 0 — Scaffold**: Vite React-TS app in `frontend/`, Tailwind, shadcn init, install
      axios/TanStack Query/Zustand/react-router-dom/react-hook-form+zod/sonner/openapi-typescript.
      Feature folders mirror backend packages (`features/auth`, `features/cart`, etc.).
- [ ] **Phase 1 — API + auth infra**: `lib/axios.ts` with interceptors (attach JWT, refresh-on-401),
      generated `api/schema.d.ts`, Zustand auth store, `ProtectedRoute`/`RoleRoute`.
- [ ] **Phase 2 — Storefront**: layout shell, category browse, product listing + search, product
      detail page (blocked on backend gap above), money-formatting util.
- [ ] **Phase 3 — Cart & addresses**: cart page (TanStack Query mutations, optimistic updates),
      address CRUD forms.
- [ ] **Phase 4 — Checkout**: address select → summary → `POST /api/checkout` with Idempotency-Key
      → payment provider handoff → retry-payment path.
- [ ] **Phase 5 — Order history & account**: order list/detail, linked accounts, change password.
- [ ] **Phase 6 — Seller dashboard**: seller application flow, product CRUD + image upload,
      seller order fulfillment.
- [ ] **Phase 7 — Admin dashboard**: user/role management, order override, seller-application
      approval, analytics, category management.
- [ ] **Phase 8 — Polish**: toast wiring on every mutation, loading skeletons, empty states,
      responsive pass.

## Session log

- 2026-08-06: Plan authored and confirmed with user (repo layout, shadcn/ui, TanStack Query +
  Zustand decisions locked in via AskUserQuestion). Backend fixes shipped ahead of frontend work:
  `CategoryController` create/update moved from `/api/public/categories` to `/api/admin/categories`
  (was publicly-reachable mutation bug); `AuthController.resetPasswordRequest` changed from
  `@GetMapping` to `@PostMapping` (was GET-with-body). This file created to track phase progress
  across sessions. No frontend code written yet — next action is Phase 0 scaffold.
- 2026-08-06: Phase 0 complete. `npm create vite@latest frontend -- --template react-ts`, Tailwind
  v4 via `@tailwindcss/vite` (no `tailwind.config.js` needed — v4 is CSS-first, theme lives in
  `src/index.css`), shadcn/ui initialized (`style: base-nova`, `baseColor: neutral`, Base UI
  primitives not Radix — shadcn's new default). Installed axios, @tanstack/react-query, zustand,
  react-router-dom, react-hook-form, zod, @hookform/resolvers, sonner, openapi-typescript (dev).
  `@` path alias wired in both `vite.config.ts` and `tsconfig.app.json`/`tsconfig.json` (no
  `baseUrl` — deprecated in TS 6, which this Vite template pulls in; bare `paths` resolves fine
  without it). Feature folders created empty under `src/features/` mirroring backend packages
  (auth, cart, checkout, order, payment, inventory, product, category, address, seller, admin,
  analytics), plus `src/{api,lib,hooks,components/layout}`. Dev server verified working
  (`npm run dev`, HTTP 200, no console errors). Nothing committed to git yet. Next action is
  Phase 1: `lib/axios.ts` interceptors, generate `api/schema.d.ts` from the running backend's
  OpenAPI spec, Zustand auth store, `ProtectedRoute`/`RoleRoute`.
