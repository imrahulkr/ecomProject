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
- [x] **Phase 3 — Cart & addresses**: cart page (TanStack Query mutations, optimistic updates),
      address CRUD forms.
- [x] **Phase 4 — Checkout**: address select → summary → `POST /api/checkout` with Idempotency-Key
      → payment provider handoff → retry-payment path.
- [x] **Phase 5 — Order history & account**: order list/detail, linked accounts, change password.
- [x] **Phase 6 — Seller dashboard**: seller application flow, product CRUD + image upload,
      seller order fulfillment.
- [x] **Phase 7 — Admin dashboard**: user/role management, order override, seller-application
      approval, analytics, category management.
- [x] **Phase 8 — Polish**: toast wiring on every mutation, loading skeletons, empty states,
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
- 2026-08-07 (later session): Phase 3 complete. Closed several blocking backend bugs first — every
  cart/address mutation path outside `updateCartProduct` (which already had `@Transactional`) threw
  `LazyInitializationException`/`InvalidDataAccessApiUsageException` under `open-in-view=false`:
  - `AddressServiceImpl.getUserAddresses` touched `user.getAddresses()` (lazy `@OneToMany`) after
    `AuthUtil.loggedInUser()`'s own transaction had already closed. Fixed by adding
    `AddressRepository.findByUser_UserId` and querying directly instead of navigating the lazy
    collection. `createAddress`/`updateAddress`/`deleteAddress` had the same problem *and* were
    doing unnecessary work — mutating `user.getAddresses()` in memory and re-saving the `User` isn't
    needed since `Address.user` (the `@JoinColumn` side) already owns the relationship; deleted that
    dead code from all three methods (also let `AddressServiceImpl` drop its now-unused
    `UserRepository` dependency).
  - `CartServiceImpl.getCart`, `addProductToCart`, and `getAllCarts` all touched
    `cart.getCartItems()` (lazy `@OneToMany`) outside a transaction — worked by accident for a
    *brand-new* cart (the field's plain `ArrayList` initializer, never proxied) but threw for any
    already-persisted cart, i.e. every real usage past the first add. Added `@Transactional` to all
    three (safe here — none of these call each other, so this isn't the self-invocation trap
    `CheckoutTransactionExecutor` exists for).
  - `CartServiceImpl.deleteProductFromCart` threw `InvalidDataAccessApiUsageException: No active
    transaction for update or delete query` on `CartItemRepository.deleteCartItemByProductIdAndCartId`
    (a custom `@Modifying @Query` method) — Spring Data only auto-wraps the inherited CRUD methods
    (`save`/`delete`/etc.) in a transaction by default, not custom query methods on the same
    repository interface; those need a transactional caller. Added `@Transactional`.
  All four fixed and curl-verified individually (create/list/update/delete address; add/get/
  increment/decrement/remove cart item) before touching the frontend, same as the Phase 1/2
  precedent of fixing blocking backend bugs before building against them.

  Frontend: `features/cart/{api,hooks,CartPage}.ts(x)` (increment/decrement/remove use optimistic
  `onMutate` updates against the `['cart']` query, recomputing `totalPriceMinorUnits` client-side
  from `specialPriceMinorUnits`, with rollback in `onError`; add-to-cart just invalidates since it's
  not on the hot interaction path), `features/address/{api,hooks,AddressForm,AddressBookPage}.tsx`
  (react-hook-form + zod, field mins mirrored from `Address.java`'s `@Size` constraints so
  client/server validation can't drift silently), quantity stepper + "Add to cart" wired into
  `ProductDetailPage` (redirects to `/login` with `state.from` if not authenticated, same pattern
  `ProtectedRoute` already uses), cart icon + item-count badge in `Header` (query gated on
  `!!accessToken` so logged-out visitors don't fire a doomed authenticated request on every page
  load), "Manage addresses" link on `AccountPage`. Extracted `lib/errors.ts#getErrorMessage` (was
  about to be duplicated a third time across cart/address mutation error toasts; refactored
  `LoginPage`'s existing inline version to use it too).
  - `GET /api/carts/users/cart` responds `400` (not an empty cart) when the user has no cart row
    yet — `features/cart/api.ts#getCart` catches that specific case and resolves `null` rather than
    surfacing it as a query error, so the console still logs one benign failed-request line the
    first time a user with no cart visits any page; this matches the already-documented "expected
    401 on refresh_secure for a logged-out visit" pattern from Phase 1/2, not a new problem.
  - Noted, not fixed (pre-existing, same "flagged not in scope unless asked" bucket as the
    unscoped-list issue already in Known backend gaps): cart total is maintained as a running
    `total ± delta` on every mutation rather than recomputed from line items, so any bug or partial
    failure along that path (as the now-fixed `deleteProductFromCart` transaction bug briefly caused
    mid-session, before the fix) permanently drifts the stored total until the cart is recreated.
    Also, `PUT /api/cart/products/{id}/quantity/{operation}` only checks
    `product.quantity < 1` (the increment delta), never the cart's *existing* quantity against
    stock, so it under-validates once more than one unit is already reserved in-cart — the frontend
    doesn't attempt to compensate client-side and just surfaces whatever the backend decides via a
    toast.
  - Browser-verified with a throwaway Playwright script (same `playwright-core` + system
    `google-chrome-stable` approach as Phase 2, reinstalled into scratchpad since it isn't a project
    dependency): login → open a product detail page → bump quantity to 2 → add to cart → header
    badge shows "2" → cart page shows the item at qty 2 → increment to 3 → decrement back to 2 →
    total renders correctly → remove → empty-cart state; then address create → edit → delete, each
    reflected in the list immediately. 12/12 scripted checks passed; only console noise was the two
    expected benign 400/401s described above. Restarted the backend twice during this session to
    pick up the Java fixes (`kill`/relaunch via `./mvnw spring-boot:run`, not devtools — this repo
    doesn't have the devtools dependency).
  - Test data: created a dedicated `phase3test@example.com` user (enabled via direct SQL, same as
    `phase1test`) rather than reusing `phase1test` since its password wasn't available in this
    session; deleted all its cart/cart_item/address rows at the end of the session so the DB is
    clean for the next one.
  - Nothing from this session committed yet (backend fixes + all new frontend files). Next action
    is Phase 4 (checkout).
- 2026-08-08: Phase 4 complete. Researched the full checkout contract first (`CheckoutController`,
  `CheckoutRequest`/`CheckoutResponse`/`RetryPaymentRequest`, `ProviderName` (`STRIPE`/`RAZORPAY`),
  the Stripe/Razorpay adapter response shapes, `IdempotencyConflictException` → `409`) before writing
  any frontend code, same precedent as prior phases. Key contract notes for future sessions: checkout
  returns `201`, retry-payment returns `200` (the generated `schema.d.ts` wrongly documents both as
  `200` — trust the controller source); `CheckoutResponse.orderStatus` stays `"PENDING_PAYMENT"` in
  both the success and `paymentAttemptFailed` branches since payment confirmation is asynchronous via
  webhook, so the frontend must branch UI on `paymentAttemptFailed`, not `orderStatus`; there's no
  checkout preview/summary endpoint, the cart's own `totalPriceMinorUnits` is the order total: the
  backend copies it straight onto the `Order` row.

  Asked the user up front how to handle payment-widget scope given the backend's Stripe/Razorpay keys
  are dummy placeholders (`sk_test_dummy...`, no publishable key anywhere) — chose real SDKs
  (`@stripe/stripe-js` + `@stripe/react-stripe-js`, Razorpay's `checkout.js` via a dynamically
  injected script tag, no npm package exists for it) wired against the real response shapes, accepting
  that a live charge can't be completed locally until real test-mode keys replace the placeholders on
  both ends. Added `VITE_STRIPE_PUBLISHABLE_KEY` to `frontend/.env` (publishable keys are safe to
  expose client-side by design, so no backend change needed for this half).

  Frontend: `api/types.ts` gained `CheckoutRequest`/`CheckoutResponse`/`RetryPaymentRequest`/
  `OrderDTO`/`OrderItemDTO`/`PaymentDTO`; `features/checkout/{api,hooks}.ts` (`checkout`/
  `retryPayment`, each mutation takes an explicit `idempotencyKey` generated by the caller via
  `crypto.randomUUID()` per user-initiated click — deliberately not persisted/reused across clicks,
  since TanStack Query mutations don't auto-retry here); `features/checkout/ProviderPicker.tsx`
  (radio-style provider selector, `allowAuto` only offered on the initial checkout since retry-payment
  requires an explicit provider server-side); `features/checkout/StripePaymentForm.tsx` (Stripe
  Elements `PaymentElement` + `confirmPayment`); `features/checkout/RazorpayCheckout.tsx` (loads
  `checkout.js`, opens the widget with `clientPayload`'s `keyId`/`razorpayOrderId`/`amount`/
  `currency`); `features/checkout/CheckoutPage.tsx` (address select → cart summary → provider →
  place order → branches on the response into either a payment-widget step or a
  `paymentAttemptFailed` retry step with the failed provider excluded from the retry picker);
  `features/order/{api,hooks,OrderStatusPage}.tsx` (new — `useOrder` polls `GET /api/orders/{id}`
  every 2s until `orderStatus` is terminal, since confirmation lands via webhook after checkout
  returns; the page also renders the retry-payment flow for a still-`PENDING_PAYMENT` order in case
  the user abandoned the payment widget without completing it). Wired `/checkout` and
  `/orders/:orderId` into `App.tsx`, enabled `CartPage`'s previously-disabled "Proceed to checkout"
  button (as a `buttonVariants`-styled `Link`, not `Button asChild` — same `@base-ui/react` Button
  constraint noted in the Phase 2 log).

  Backend fixes shipped ahead of/during frontend work, found via real browser testing before touching
  any UI code (same precedent as Phases 1–3):
  - **CORS blocked every checkout/retry-payment request outright.** `CorsConfig`'s allowed-headers
    list only had `Authorization`/`Content-Type` — the required `Idempotency-Key` header failed
    CORS preflight, so the browser never even sent the request (surfaced as a browser-console CORS
    error, not a backend error response). Added `"Idempotency-Key"` to the allowed-headers list.
  - **`AddressDTO` had no validation annotations at all** — its `@NotBlank`/`@Size` imports were
    unused dead code, so `@Valid`/`@Validated` on `AddressDTO` in `AddressController` was a silent
    no-op. Invalid data (e.g. a 2-character `state`) sailed through the controller and service layer
    untouched, only to be rejected by the `Address` *entity's* own `@Size`/`@NotBlank` constraints at
    Hibernate flush time via `ConstraintViolationException` — an exception type `MyGlobalExceptionHandler`
    has no specific handler for, so it fell through to the generic `Exception.class` handler and
    surfaced as an opaque `500 INTERNAL_ERROR` instead of the intended `400 VALIDATION_ERROR` with a
    field-level message. Fixed by mirroring the entity's exact constraints onto `AddressDTO`; verified
    with curl that the same invalid payload now returns `400` with the right field/message and that
    valid payloads still succeed.
  Both fixed and curl-verified individually before the browser pass.

  Browser-verified with a throwaway Playwright script (same `playwright-core` + system
  `google-chrome-stable` approach as prior phases, reinstalled into scratchpad): login → cart with a
  seeded item → checkout page shows address + provider options → place order (auto-select provider)
  → backend's dummy Stripe/Razorpay keys make the provider call fail synchronously as expected →
  `paymentAttemptFailed` branch renders the failure reason and a provider picker excluding the
  provider that just failed → retry with the other provider → new `CheckoutResponse` reflected
  correctly (new failure reason worded differently since Stripe's failure is non-retryable, appending
  "with a different payment method", vs. Razorpay's plain retryable message) → separately verified
  `OrderStatusPage` renders correctly for a still-`PENDING_PAYMENT` order (order items, total, both
  providers offered for retry) with no console errors. The Stripe Elements / Razorpay widget
  rendering itself (the actual `clientSecret`/`clientPayload` success path) could **not** be
  exercised end-to-end locally, since both providers always fail synchronously against the dummy
  keys — this is the accepted limitation from the scope decision above, not a bug; the integration
  code path is written correctly against the real response shapes and just needs real test-mode keys
  on both ends to complete a live charge.

  Test data: created a new `phase4test@example.com` user (enabled via direct SQL, same pattern as
  `phase1test`/`phase3test`), one address, and 2×`Wireless Mouse` in cart. Deliberately **left this
  data in place** (including several `PENDING_PAYMENT` orders created by repeated checkout-flow runs)
  since Phase 5 (order history) will need existing orders to test against — unlike Phase 3's cleanup,
  don't delete this user's data without checking Phase 5's needs first.

  Nothing from this session committed yet (2 backend fixes + all new frontend files). Next action is
  Phase 5 (order history & account).
- 2026-08-08 (later session): Phase 5 complete. `GET /api/orders` (paginated) and `GET /api/orders/{id}`
  already existed in `OrderController`/`OrderServiceImpl` with `@Transactional` already in place from
  an earlier session — no backend work needed for order history itself, unlike every prior phase.

  Found and fixed two backend bugs in `AccountLinkController` (linked-accounts / unlink-provider,
  needed for the account page), both curl-verified before touching frontend code, same precedent as
  Phases 1–4:
  - **`GET /api/account/linked-accounts` 500'd on every call.** Same lazy-collection-outside-a-
    transaction bug as `AddressServiceImpl` in Phase 3: `user.getOAuthAccounts()` is a lazy
    `@OneToMany`, and this controller has no `@Transactional` anywhere in its call path
    (`open-in-view=false`). Fixed by using `OAuthAccountRepository.findByUser(user)` directly (the
    repo method already existed, just unused) instead of navigating the lazy collection - same fix
    in both `getLinkedAccounts` and `deleteLinkedAccount`'s last-login-method size check.
  - **The "can't unlink your only sign-in method" business-rule check threw `IllegalStateException`,
    which had no handler** in `MyGlobalExceptionHandler` and fell through to the generic
    `Exception.class` catch-all - same "opaque 500 instead of the intended 4xx" shape as the
    `AddressDTO` validation bug found in Phase 4. Added a handler mapping it to `409 CONFLICT`.
  Verified `linked-accounts` went from a 500 to a correct `200` via curl. The 409 path itself
  (unlinking an OAuth-only user's last provider) couldn't be curl/browser-tested - it requires a real
  linked OAuth account, which needs a live Google/GitHub flow this sandbox can't drive - so it's
  verified by reading the code path only, not exercised end-to-end. Flag this if it looks wrong once
  someone tests it against a real linked account.

  Frontend: `features/order/{api,hooks}.ts` gained `getOrders`/`useOrders` (paginated, sorted
  `orderId desc` for most-recent-first - the backend default is `orderId asc`), new
  `features/order/OrderListPage.tsx` (status-badged order rows linking to the existing
  `OrderStatusPage` from Phase 4, same pagination-controls pattern as `ProductListPage`).
  `features/auth/api.ts` gained `getLinkedAccounts`/`unlinkProvider`/`changePassword`; new
  `features/auth/hooks.ts` (`useLinkedAccounts`/`useUnlinkProvider`/`useChangePassword`), new
  `features/auth/LinkedAccountsSection.tsx` (shows password-set status + linked providers, disables
  the unlink button client-side when it would strand the user with no login method - the 409 above
  is the server-side backstop for that same rule) and `features/auth/ChangePasswordForm.tsx`
  (react-hook-form + zod, mirrors `AddressForm`'s conventions). `AccountPage.tsx` rebuilt into an
  actual account hub (order-history link, linked accounts section, change-password section) rather
  than just an email/logout stub. Wired `/orders` into `App.tsx`. `api/types.ts` gained
  `OrderResponse`/`PasswordChangeRequestDTO` (from the regenerated OpenAPI schema) and a hand-written
  `LinkedAccounts` interface, since `/api/account/linked-accounts` returns a hand-built `Map`, not a
  generated DTO.

  Browser-verified with a throwaway Playwright script (`playwright-core` + system
  `google-chrome-stable`, reinstalled into scratchpad per the established pattern). Hit a new
  environment issue not seen in prior phases: Chrome's navigation hung indefinitely on both
  `http://localhost:5173` (DNS/connection) and, separately, on Playwright's default `waitUntil:
  'load'` even once `127.0.0.1` resolved - switching to `waitUntil: 'domcontentloaded'` fixed the
  second one. Net fix: use `localhost:5173` (not `127.0.0.1` - the backend's CORS `allowedOrigins`
  only lists `localhost:5173`, so `127.0.0.1` gets CORS-blocked) with explicit
  `waitUntil: 'domcontentloaded'` on every `goto`/`waitForURL` call. Worth remembering for any future
  Playwright script against this dev server.

  13/13 scripted checks passed: login → account page shows email/password-set badge/no-linked-
  providers/order-history link → change-password rejects wrong current password → rejects mismatched
  confirmation (client-side zod) → succeeds and shows a toast → log out → log back in with the new
  password (correctly redirected back to `/account` via `ProtectedRoute`'s `state.from`, not `/` -
  fixed the test's assumption, not the app) → order list shows the seeded order with a "Pending
  payment" badge → clicking through lands on the existing `OrderStatusPage`. No unexpected console
  errors (same benign 401-on-first-load pattern as every prior phase).

  Test data: created `phase5test@example.com` (signup + SQL-enable, same pattern as prior phases),
  one address, one product in cart, one `PENDING_PAYMENT` order (`orderId 13`) via checkout against
  the still-dummy provider keys (same synchronous-failure limitation as Phase 4). Left in place for
  Phase 6+; password is currently `NewPassword123!` (changed mid-session by the Playwright script's
  change-password test) - reset via direct SQL (bcrypt hash of `Password123!`) if a future session
  needs the original.

  Nothing from this session committed yet (2 backend fixes + all new/changed frontend files). Next
  action is Phase 6 (seller dashboard).
- 2026-08-08 (later session): Phase 6 complete. Researched the full seller contract first
  (`SellerApplicationController`, `ProductController`'s `/api/seller/**` routes, `OrderController`'s
  seller-scoped routes, `FulfillmentStatus` state machine) before writing any frontend code, same
  precedent as every prior phase. Key contract notes for future sessions: `/api/seller-applications/**`
  is deliberately *not* under `/api/seller/**` (any authenticated user can apply, since applying is
  what a non-seller does); fulfillment is tracked per `OrderItem` not per `Order`, gated on the parent
  order being `PAID`, with a strict `PENDING→SHIPPED→DELIVERED`/`PENDING→CANCELLED` state machine
  enforced server-side; the image-upload endpoints (`PUT .../products/{id}/image`, multipart field
  `"Image"`, capital I) return a **bare filename**, not a full URL, unlike every other product-returning
  endpoint - the nested `ProductDTO` inside `OrderItemDTO` has this same bare-filename shape permanently
  (it never routes through `constructImageUrl`), which matters for the seller orders view specifically.

  Found and fixed three real backend bugs in `ProductDTO`/`ProductServiceImpl`, all curl-verified before
  touching frontend code (same precedent as Phases 1-5):
  - **Creating or updating a product 500'd on every call that omitted `specialPriceMinorUnits`** (which
    is every realistic call - it's meant to be server-computed). `priceMinorUnits`/`specialPriceMinorUnits`
    were primitive `long` on `ProductDTO`, and this Spring Boot's Jackson version deserializes via the
    `@AllArgsConstructor` (constructor-based binding), which hard-fails on a missing primitive rather than
    leaving it at a default. Fixed by boxing both to `Long`.
  - **`updateProduct`/`updateProductAsSeller` silently dropped `quantity` and `discount` edits, and took
    `specialPriceMinorUnits` raw from the client instead of recomputing it** - the pre-existing code
    only copied `priceMinorUnits`/`category`/`productName`/`description` from a
    `modelMapper.map(productDTO, Product.class)` call, so a price or discount change never updated the
    special price shown anywhere, and a seller had no way to restock or change discount after creating a
    product. Also, that `modelMapper.map()` call's implicit `categoryId → category` nested-property
    mapping was never actually verified to populate the `Category` association correctly (ModelMapper's
    default matching for a bare foreign-key id isn't reliable). Replaced both methods' bodies with a
    shared `applyProductEdits` helper that looks up the `Category` explicitly by `categoryDTO.getCategoryId()`
    and recomputes `specialPriceMinorUnits` from `priceMinorUnits`/`discount` the same way `addProduct`
    does, so price/discount/quantity/category edits and the special-price/display are never inconsistent.
  - **`ProductDTO` had zero validation annotations**, so an invalid `productName` (the entity's own
    `@Size(min=3)`) would only fail at Hibernate flush time via an unhandled `ConstraintViolationException`
    → opaque `500`, same bug shape as `AddressDTO` in Phase 4. Added `@NotBlank`/`@Size`/`@NotNull`/
    `@Positive`/`@Min`/`@DecimalMin`/`@DecimalMax` to `ProductDTO` and `@Valid` to all four product
    mutation endpoints (admin + seller create/update); verified a short name now returns a clean `400`
    with a field message instead of a `500`.

  Frontend: `features/seller/{applicationApi,applicationHooks}.ts` + `SellerApplyForm.tsx` +
  `SellerApplyPage.tsx` (apply form, application history with status badges, hides the form while a
  `PENDING` application exists or once the user already holds `ROLE_SELLER`); `features/seller/
  {productsApi,productsHooks}.ts` + `SellerProductForm.tsx` + `SellerProductListPage.tsx` (create/edit/
  delete/image-upload, category `<select>` populated from the existing `useCategories` hook, price
  entered in rupees and converted to `priceMinorUnits` on submit/back on edit-load, matching the
  documented "divide by 100 to display" convention in reverse); `features/seller/{ordersApi,ordersHooks}.ts`
  + `SellerOrdersPage.tsx` (per-order-item fulfillment controls: tracking number + carrier inputs and
  "Mark shipped" only shown while `PENDING`, "Mark delivered" only while `SHIPPED`, matching the backend's
  state machine exactly rather than re-deriving it client-side); `features/seller/SellerLayout.tsx`
  (Products/Orders tab nav for the `/seller/**` section). New `lib/images.ts#resolveProductImageUrl`
  (prefixes a bare filename with `VITE_API_URL` + `/images/`, passes a value through unchanged if it's
  already a full URL) - used only in `SellerOrdersPage` where the nested product image is permanently
  bare per the contract note above; every other product-image usage in the app already gets a full URL
  from the backend and doesn't need it. Wired `/sell` (any authenticated user, `ProtectedRoute` only) and
  `/seller/{products,orders}` (nested under `RoleRoute allowedRoles={['ROLE_SELLER','ROLE_ADMIN']}`) into
  `App.tsx`; `AccountPage` now links to "Seller dashboard" or "Become a seller" depending on `roles`.

  One frontend-only bug caught and fixed during browser verification (not a backend issue):
  `SellerApplyPage`'s form-visibility condition was `!isSeller && !hasPendingApplication`, and
  `hasPendingApplication` is `undefined` (falsy) before the applications query resolves - so on a fresh
  page load with an actual pending application, the apply form flashed visible for one render before
  disappearing once data arrived. Fixed by gating on `!isPending` too.

  Browser-verified with two throwaway Playwright scripts (`playwright-core` + system
  `google-chrome-stable`, reinstalled into scratchpad per the established pattern - this session's
  scratchpad was fresh, no reused state from prior sessions). First pass surfaced a test-script bug worth
  noting for future sessions: unscoped `page.click('button:has-text("Edit")')` matched the *first* Edit
  button in DOM order (the pre-existing seeded product, listed before a newly-created one), silently
  editing the wrong row and corrupting its data - fixed by scoping every action to
  `page.locator('div.rounded-xl', { hasText: '<product name>' })` per row. 25/25 checks passed after the
  fix: seller-application submit → PENDING status shown → re-apply form correctly hidden while pending →
  no flash-of-form-during-load (regression check for the fix above) → non-seller blocked from
  `/seller/products` by `RoleRoute` → approved seller sees "Seller dashboard" link → existing product
  listed → create product (price/discount math verified: ₹250 @ 10% → ₹225.00) → edit only the targeted
  row, other row provably untouched → image upload changes the rendered `<img src>` to a full URL → delete
  removes only the targeted product → seller orders list shows the seeded order, item starts `PENDING` →
  order-item product image resolves to a full URL (the `lib/images.ts` fix, directly verified) → fill
  tracking/carrier → "Mark shipped" transitions to `SHIPPED` and displays them → "Mark delivered"
  transitions to `DELIVERED`. No unexpected console errors (same benign 401/400 pattern as every prior
  phase).

  Test data: `phase6test@example.com` (signup + SQL-enable, `ROLE_SELLER` granted directly via SQL rather
  than the admin-approval flow, since there's no admin user/frontend yet - Phase 7's job) - has one
  product (`productId 13`, restored to quantity 25/₹2,000/20% off after a test-script mishap, real
  uploaded image) and one `PAID` order (`orderId 14`, `orderItemId 15`, crafted directly via SQL since
  checkout can't reach `PAID` locally with the dummy provider keys - see Phase 4/5 notes - now in
  `DELIVERED` fulfillment state from the verification run, tracking `PW-TRACK-1`/`Playwright Express`).
  `phase6browser@example.com` and `phase6browser2@example.com` (signup + SQL-enable) each have one
  `PENDING` seller application ("Browser Test Shop" / "Browser Test Shop 2") - deliberately left pending,
  useful fixtures for Phase 7's admin approve/reject UI. All left in place for Phase 7.

  Nothing from this session committed yet (3 backend fixes in `ProductDTO`/`ProductServiceImpl`/
  `ProductController` + all new frontend files + `AccountPage.tsx` nav change). Next action is Phase 7
  (admin dashboard).
- 2026-08-08 (later session): Phase 7 complete. Unlike every prior phase, the entire admin backend
  surface already existed and needed **no fixes** - `AdminUserController`/`AdminUserServiceImpl`
  (`/api/admin/users`, grant/revoke role), `AdminOrderController`/`AdminOrderItemController`
  (`/api/admin/orders/{id}/status`, `/api/admin/order-items/{id}/fulfillment` - reuses
  `OrderServiceImpl`'s existing `applyFulfillmentTransition` state machine), `AdminSellerController`
  (view any seller's products), `SellerApplicationAdminController` (`/api/admin/seller-applications`,
  approve/reject), `AnalyticsController` (`/api/admin/app/analytics`), and `CategoryController`'s
  existing `/api/admin/categories` CRUD were all already `@Transactional` where needed and already
  curl/browser-correct - confirmed by reading every method body before writing any frontend code
  (same research-first precedent as prior phases), not by re-discovering bugs. Key contract notes:
  `AdminUserController`/`SellerApplicationAdminController` return Spring's *native* `Page<T>`
  serialization (`number`/`last`/`totalElements`), not this app's own custom `content`/`pageNumber`/
  `lastPage` shape every other paginated endpoint uses (`ProductResponse`/`OrderResponse`/
  `CategoryResponse`) - easy to mix up, added `PageAdminUserSummaryDTO`/`PageSellerApplicationDTO` to
  `api/types.ts` from the already-current generated `schema.d.ts` (no regeneration needed this phase,
  first time that's been true) to keep the two shapes distinct at the type level rather than
  hand-waving both through one interface. `AdminUserServiceImpl.revokeRole` blocks an admin revoking
  their *own* `ROLE_ADMIN` (409 via `APIException`) - mirrored client-side by disabling that specific
  button for the logged-in user's own row, with the server check as backstop, same defense-in-depth
  pattern `LinkedAccountsSection` already used in Phase 5 for "can't unlink your last login method".

  Frontend: `features/admin/{usersApi,usersHooks,AdminUsersPage}.ts(x)` (role chips per user, grant/
  revoke via `PUT`/`DELETE /api/admin/users/{id}/roles/{role}`), `{ordersApi,ordersHooks,
  AdminOrdersPage}.ts(x)` (status-override `<select>` per order reusing `OrderStatus` values, plus a
  per-item fulfillment section copied from `SellerOrdersPage`'s `FulfillmentRow` but pointed at the
  admin endpoint - no ownership scoping needed since the admin controller already has none),
  `{applicationsApi,applicationsHooks,AdminApplicationsPage}.ts(x)` (PENDING/APPROVED/REJECTED status
  tabs, approve button, reject reveals an inline reason input matching the tracking-number/carrier
  inline-reveal pattern from seller fulfillment), `{categoriesApi,categoriesHooks,
  AdminCategoriesPage}.tsx` (create/edit/delete, reuses the existing public `useCategories` hook for
  the list rather than duplicating a fetch), `{analyticsApi,analyticsHooks,AdminOverviewPage}.ts(x)`
  (three stat cards using the `Card` component - first real usage of `components/ui/card.tsx` in the
  app, it existed since Phase 2 but had gone unused), `AdminLayout.tsx` (five-tab nav, same
  `NavLink`/`cn` pattern as `SellerLayout`). Wired `/admin` (index → overview) and `/admin/{users,
  orders,applications,categories}` into `App.tsx`, replacing the Phase-0-era placeholder `AdminPage`
  stub that just rendered "Admin only"; `AccountPage` gained an "Admin dashboard" link gated on
  `roles.includes('ROLE_ADMIN')`. Added `aria-label`s to the category-form and reject-reason inputs
  (matching `Header`'s existing search-input convention) since the plain visual layout had multiple
  same-shaped inputs on one page with no other reliable way to target one specifically.

  Browser-verified with a throwaway Playwright script (`playwright-core` + system
  `google-chrome-stable`, reinstalled into scratchpad per the established pattern). No admin user
  existed in the dev DB yet (expected - Phase 6 explicitly deferred admin bootstrap to this phase);
  signed up `phase7admin@example.com` via the real API and granted `ROLE_ADMIN` directly via SQL as
  the one-time bootstrap (same "can't get in without already being in" problem every real admin panel
  has), then used the *app's own UI* to grant/revoke roles on other test users for the rest of the
  run rather than more SQL. Two script bugs worth noting for future Playwright scripts against this
  app, neither an app bug: (1) `LoginPage` requires both a `username` *and* `email` field despite only
  authenticating by one of them - a script that fills only the email input leaves the form stuck on
  HTML5 required-field validation with no visible error; (2) this app's session lives in an httpOnly
  refresh-cookie + in-memory Zustand store, not `localStorage` - `localStorage.clear()` does nothing
  to log a user out, the script has to click the real "Log out" button. 14/15 scripted checks passed
  on the final run; the one "failure" (PENDING tab showing seeded applications) was the test script
  re-running against fixtures its own *first* attempt had already approved/rejected before crashing
  on an unrelated selector bug - the underlying approve/reject flow itself had already passed a full
  check in that earlier run, so this isn't a product bug, just non-idempotent test data. Full pass
  covered: admin-only nav link visibility, all five tabs, grant/revoke role round-trip (with the
  self-admin-revoke guard verified disabled), order list + status-override control rendering, seller
  application approve → moves to APPROVED tab, reject-with-reason → moves to REJECTED tab with the
  reason shown, category create/rename/delete, and a non-admin user (fresh signup, no roles beyond
  `ROLE_USER`) correctly bounced off `/admin` by `RoleRoute`. No unexpected console errors (same
  benign 401/400 pattern as every prior phase).

  Test data: `phase7admin@example.com` (ROLE_ADMIN, bootstrapped via SQL as described above) and
  `phase7plain@example.com` (plain signup, no extra roles) both left in place as reusable fixtures -
  first phase where a dedicated admin login is available for future sessions, worth keeping. Side
  effects on existing fixtures from exercising real approve/reject during verification: Phase 6's
  `phase6browser2@example.com` seller application ("Browser Test Shop 2") is now `APPROVED` (grants
  `ROLE_SELLER`), `phase6browser@example.com`'s ("Browser Test Shop") is now `REJECTED`, and
  `phase6test@example.com`'s ("Phase6 Test Shop") is now `APPROVED` too - all three were explicitly
  left `PENDING` at the end of Phase 6 as fixtures for this exact UI, so consuming them here is
  expected, not a regression to fix.

  Nothing from this session committed yet (no backend changes this phase + all new
  `features/admin/*` files + `App.tsx`/`AccountPage.tsx`/`api/types.ts` changes). Next action is
  Phase 8 (polish: toast wiring on every mutation, loading skeletons, empty states, responsive pass).
- 2026-08-08 (later session): Phase 8 complete. Unlike every prior phase, this one was mostly an audit
  rather than new construction: toast wiring (`sonner`, success + `getErrorMessage`-backed error toasts),
  `Skeleton`-based loading states, and empty-state copy had already been built into every mutation/query
  as each feature was written in Phases 2–7, rather than deferred to this phase — read every hook file
  (`useMutation` in `address`, `admin/*`, `auth`, `cart`, `checkout`, `seller/*`) and every list/detail
  page before touching anything, and found nothing missing on that front. `checkout/hooks.ts`'s two
  mutations are the one place without `onError` toasts baked into the hook itself, but that's deliberate,
  not a gap - `CheckoutPage`/`OrderStatusPage` both attach `onError: (err) => toast.error(...)` at the
  call site instead, since the same mutation needs different handling depending on which page invoked it.

  The actual remaining work was the responsive pass, done by browser-verifying (not just reading
  Tailwind classes) every route at three viewports (375/768/1440px) - same throwaway-Playwright-script
  approach as every prior phase, `playwright-core` + system `google-chrome-stable` reinstalled into
  scratchpad. Checked `document.documentElement.scrollWidth` vs `clientWidth` for horizontal overflow
  across all 16 routes × 3 widths (48 checks) plus screenshots, both logged out and logged in (as an
  admin/seller test user, so nav/tabs render their full-width authenticated state). Found and fixed two
  real bugs, both only visible at 375px and both missed by just reading the JSX since the classes
  *looked* responsive:
  - **`ProductCard`'s price row caused page-wide horizontal scroll on the storefront grid at mobile
    width.** `CardFooter`'s `flex items-baseline gap-2` (price + strikethrough original price + discount
    badge) had no wrap, so on a 2-column mobile grid a discounted product's footer forced its card wider
    than its grid cell, blowing out `document.documentElement.scrollWidth` for the whole page. Fixed by
    adding `flex-wrap` (`gap-x-2 gap-y-1`) to that `CardFooter` usage in `ProductCard.tsx`.
  - **`Header`'s search bar collapsed to an ~30px unusable sliver on mobile whenever logged in.** The
    search `<form>` was `flex-1` around an `Input` with (via the shared `input.tsx`) `min-w-0`/`w-full`,
    but `nav` (cart icon + the account link showing the full `user.email`) was `shrink-0` - so on any
    viewport too narrow to fit "Ecom" + a full email address + the cart icon + gaps, the *email* never
    shrank and the search input absorbed the entire deficit, sometimes down to a few px. Didn't show up
    as `scrollWidth` overflow (the input dutifully shrank instead of overflowing) so screenshots were
    needed to catch it, not just the overflow check. Fixed in `Header.tsx`: the account `Link` now gets
    `max-w-24 truncate sm:max-w-none` (via `buttonVariants`' `className` merge) plus a `title={user.email}`
    tooltip, and the header's outer flex gap tightens (`gap-2` → `sm:gap-4`) to give the search input more
    room on small screens. Verified after the fix: search input goes from ~30px to ~143px wide at 375px
    with a long test email, `scrollWidth` still exactly 375 (no overflow introduced), truncation confirmed
    via computed styles (`overflow: hidden`, `text-overflow: ellipsis`, `max-width: 96px`) not just visual
    inspection. Re-ran the full 48-check sweep after both fixes - zero overflows anywhere, including with
    forms open (`SellerProductForm`'s 3-column price/discount/quantity grid, `AddressForm`) at 375px.

  Noted, not fixed (out of scope for a polish pass - this is a missing *feature*, not broken polish): there
  is no signup UI anywhere in the app. `features/auth/api.ts#signup` (`POST /api/auth/signup`) exists and
  is wired to nothing - `LoginPage` has no "create an account" link or form, and every test user across
  every phase's session log was created via direct API/SQL, never through the app itself. Every real
  visitor hitting this app today has no way to register. Worth a dedicated phase/task if the app is meant
  to be used by anyone other than pre-seeded test accounts.

  Test data: reset the password on three existing fixture users to a known value
  (`Password123!`, bcrypt via `python3 -c "import bcrypt; ..."`, same approach as Phase 5's password
  reset) purely so this session could log in and browser-test authenticated/admin/seller views:
  `phase7admin@example.com`, `phase6test@example.com`, `phase5test@example.com` (this one was already
  `Password123!` from Phase 5's reset, unchanged). No application data (products/orders/addresses)
  touched this session.

  Nothing from this session committed yet (`ProductCard.tsx`, `Header.tsx` changed). No backend changes
  this phase - all eight planned phases are now checked off; next action is whatever the user wants next
  (e.g. the signup-UI gap above, or committing the accumulated frontend work).</new_string>
</invoke>

