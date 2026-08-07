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

- No cart clear-all endpoint.
- No order cancellation endpoint.
- No reviews/ratings, wishlist, or coupon/discount APIs anywhere.
- `AddressDTO` has no `isDefault` flag.
- Address/Cart list endpoints return unscoped results (flagged, not yet fixed — out of scope unless asked).

## Phase checklist

- [x] **Phase 0 — Scaffold**: Vite React-TS app in `frontend/`, Tailwind, shadcn init, install
      axios/TanStack Query/Zustand/react-router-dom/react-hook-form+zod/sonner/openapi-typescript.
      Feature folders mirror backend packages (`features/auth`, `features/cart`, etc.).
- [x] **Phase 1 — API + auth infra**: `lib/axios.ts` with interceptors (attach JWT, refresh-on-401),
      generated `api/schema.d.ts`, Zustand auth store, `ProtectedRoute`/`RoleRoute`. Verified
      end-to-end in a real browser (Playwright/Chrome) — see 2026-08-07 log entry.
- [x] **Phase 2 — Storefront**: layout shell, category browse, product listing + search, product
      detail page, money-formatting util.
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
- 2026-08-07: Phase 1 code written (`lib/axios.ts`, `lib/jwt.ts`, `features/auth/{store,api,
  useBootstrapAuth,LoginPage,AccountPage}.ts(x)`, `components/routing/{ProtectedRoute,RoleRoute}
  .tsx`), then verified end-to-end in a real browser (Playwright driving Chrome against the live
  dev servers) rather than just by reading the code — this surfaced four real backend bugs, all
  now fixed:
  - **CORS was completely dead.** `CorsConfig.corsConfigurationSource()` was missing `@Bean`, so
    Spring silently autowired `SecurityConfig`'s `CorsConfigurationSource` constructor param to an
    unrelated bean (`mvcHandlerMappingIntrospector`) that happened to implement the same
    interface. Every browser call from the frontend origin was blocked. Fixed by adding `@Bean`.
  - **Every custom auth exception returned a generic 500 instead of its intended 4xx.**
    `GlobalAuthExceptionHandler` (401s for bad credentials/invalid refresh token, 409s for
    signup conflicts) was losing to `MyGlobalExceptionHandler`'s catch-all
    `@ExceptionHandler(Exception.class)` — two unordered `@RestControllerAdvice` beans tie, and
    Spring picks the first bean with *any* matching handler rather than the most specific match
    across beans. Fixed with `@Order(HIGHEST_PRECEDENCE)` / `@Order(LOWEST_PRECEDENCE)` on the two
    advice classes respectively.
  - **`POST /api/auth/refresh_secure` was broken on every single call, not just concurrent ones.**
    `RefreshTokenService.rotate()` returns a lazily-loaded `User` entity in its result, but
    `SecureAuthController.refresh()` was calling `jwtUtils.generateAccessToken(rotation.user())`
    *after* `rotate()`'s `@Transactional` scope had already closed (`open-in-view=false`) —
    touching `User.oAuthAccounts` (lazy `@OneToMany`) outside the session threw
    `LazyInitializationException`. This means silent session-restore-on-page-load had never
    actually worked. Fixed by generating the access token inside `rotate()`'s transaction and
    returning it directly.
  - **Concurrent refresh_secure calls with the same cookie destroyed the whole session.**
    `useBootstrapAuth`'s effect calls `refresh_secure` on every mount, and React StrictMode
    double-invokes effects in dev — so *every single page load* fired two concurrent refresh
    calls, and the backend's reuse-detection (correctly designed to treat a replayed/stolen token
    as compromise) couldn't tell that apart from two legitimate simultaneous requests, revoking
    the entire token family and logging the user out. Fixed on both ends: frontend now routes
    `useBootstrapAuth` through the same de-duplicated `refreshAccessToken()` single-flight promise
    the 401-retry interceptor already used (`lib/axios.ts` exports it now), so a double-invoked
    effect only ever fires one real network call; backend `RefreshTokenService.rotate()` now does
    an atomic conditional `UPDATE ... WHERE revoked = false` (same pattern as
    `ProductRepository.decrementStockIfAvailable`) instead of read-then-write, so a genuine
    near-simultaneous duplicate request cleanly loses the race with a 401 instead of triggering
    the family-wide revocation (which is now reserved for tokens revoked outside a 10s grace
    window — an actual stale replay).

  All four fixes verified both directly (curl, including concurrent-request repros) and via a
  10-check Playwright browser suite (unauth redirect, login, protected-route access, hard reload
  while logged in surviving StrictMode's double bootstrap-refresh, RoleRoute blocking a non-admin,
  and the 401→refresh→retry interceptor path) — all passing.

  Also noted, not fixed (pre-existing, out of scope for Phase 1): `GET /api/addresses` throws
  `LazyInitializationException` (same unscoped-list gap already flagged above); re-triggering
  `OnUserRegisteredEvent` on an unverified login attempt hits a unique-constraint violation on
  `verification_token.user_id` since one already exists from signup, surfacing as a 500 instead of
  the intended "please verify your email" message.

  Unrelated to the frontend work: mid-session, the VS Code "App Modernization for Java" extension
  (`vscjava.migrate-java-to-azure`) independently auto-stashed uncommitted changes and switched
  this same working directory to a new `appmod/java-upgrade-*` branch to run its own Java-version
  upgrade. No data was lost (recovered via `git stash pop` after checking out back to
  `feature-add-frontend`), but worth knowing this tool can act on the working tree unprompted if
  it's running in another window.

  Nothing from this session is committed yet. Next action is Phase 2 (storefront), still blocked
  on the missing `GET /api/public/products/{productId}` backend gap noted above.
- 2026-08-07 (later session): Phase 2 complete. Closed the blocking backend gap first —
  `GET /api/public/products/{productId}` added to `ProductService`/`ProductServiceImpl`/
  `ProductController`, reusing the existing `toProductDTO`/`constructImageUrl` helpers so the
  shape matches the list endpoints exactly. Backend restarted, `schema.d.ts` regenerated.
  Frontend: `lib/money.ts` (`formatMoney`), `features/product/{api,hooks}.ts`,
  `features/category/{api,hooks}.ts` (first real `useQuery` usage in the codebase — installed
  since Phase 0 but unused until now), `components/layout/{Header,Footer,Layout}.tsx`,
  `features/product/{ProductCard,ProductListPage,ProductDetailPage}.tsx`. `ProductListPage` is
  now the storefront home (`/`); category/keyword/sort/page all live in the URL query string.
  Category filtering reuses the existing `/api/public/products?category=<name>` endpoint (not
  the separate `/public/categories/{id}/products` route) so it composes with keyword+pagination
  through one hook.
  - **shadcn's registry (`ui.shadcn.com`) is unreachable from this sandbox** (`npx shadcn add`
    times out — npm registry itself works fine, just that host). Hand-wrote `card.tsx`,
    `input.tsx`, `skeleton.tsx`, `badge.tsx` under `components/ui/` matching the existing
    `button.tsx` conventions (`cn`/`cva`/`data-slot`) instead. For the sort/category picker,
    used a plain native `<select>` rather than blind-porting base-ui's `Select` primitive
    (`@base-ui/react/select` is present in `node_modules` but its API wasn't verified against
    the registry source) — worth swapping to a real shadcn `select.tsx` once the registry is
    reachable, for visual consistency with future dropdown/combobox needs.
  - `@base-ui/react`'s `Button` has no Radix-style `asChild` prop (it's a `render`-prop API,
    unlike shadcn's Radix-based default) — `Header`'s nav links use `buttonVariants({...})`
    applied directly to `<Link>` instead of wrapping `Button` around it. Keep this in mind for
    any future button-as-link composition.
  - Database had zero categories/products (fresh schema, no seed script in the repo). Seeded 3
    categories and 7 products directly via SQL against the local Postgres instance, owned by the
    existing `phase1test` user (`seller_id`), for browser verification — this is local dev data
    only, not a migration, not committed.
  - Browser-verified with a throwaway Playwright script (`playwright-core` + system
    `google-chrome-stable`, since `chromium-cli` wasn't available and `npx playwright install`
    would have downloaded a browser rather than reusing the one already on the box): product
    grid (7 items), category filter (Books → 2 items), search (`kettle` → 1 item, URL synced),
    sort by price descending (correct order, discount math verified — e.g. 27-inch 4K Monitor
    ₹29,999 → ₹25,499.15 at 15% off), product detail page (price/discount/stock/description/
    seller name all correct), out-of-stock badge (Electric Kettle, quantity 0), and back-to-list
    navigation. No console/network errors other than expected `401` on `refresh_secure` (logged
    out, no session cookie — matches documented Phase 1 behavior, not a regression). Did **not**
    re-verify the logged-in header state (email link vs. "Log in") independently this session —
    no stored credentials for the Phase 1 test user; the logic reads the same `useAuthStore.user`
    already proven correct in `AccountPage` during Phase 1, so risk is low, but flag this if
    something looks off in Phase 3+.
  - Nothing from this session committed yet (backend endpoint + all new frontend files). Next
    action is Phase 3 (cart & addresses).
