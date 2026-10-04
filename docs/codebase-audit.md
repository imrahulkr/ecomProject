# Codebase audit and fixes (October 2026)

A full review of the backend (Spring Boot) and frontend (React) found gaps in security, money
handling, reliability, the database and engineering practice. This document records each finding,
what was changed, and how it was verified. API-level details are in `BACKEND_CONTRACT.md`;
architecture notes are in `CLAUDE.md`.

**Status:** every finding in sections 1–3 and 5 is fixed. Of the marketplace features (section 4),
refunds/returns, the shipping fee and the seller commission/payout ledger were built; GST, product
variants, multiple images and SEO are a roadmap (section 6).

**Verification:**
- Backend: 59 unit tests, plus 4 integration tests (`PlatformIntegrationTest`, real Postgres via
  Testcontainers) that build the schema from V0, validate it against the entities, and run
  checkout → payment without a webhook → cancel and refund over HTTP. They were run against a local
  Postgres; in CI they run in Docker.
- Frontend: oxlint, typecheck, 10 Vitest tests, production build.
- Manual: each fix was exercised against the running app.

---

## 1. Security and money (high priority)

| # | Finding | Fix | Where |
|---|---|---|---|
| 1 | No brute-force protection on login (unlimited attempts). | Login is limited to 20 attempts per IP per 15 min, and **5 failed logins lock that account's password login for 15 min**. Signup is limited per IP, and password reset per IP and per email. Responses are 429 with `Retry-After`. | `RateLimiterService`, `SecureAuthController`, `AuthController` |
| 2 | The rate limiter keyed on `getRemoteAddr()` (one bucket for everyone behind a proxy) and its map never shrank. | Buckets now live in a bounded, expiring Caffeine cache. Production config sets `server.forward-headers-strategy=native` so the real client IP is used. | `RateLimiterService`, `application-prod.properties.example` |
| 3 | Password change/reset left every existing session alive (`revodeAllForUser` was never called). | Both now revoke all of the user's refresh tokens. The frontend signs the user out after a password change. | `AuthServiceImpl`, `PasswordResetTokenServiceImpl`, `AccountSettingsPage` |
| 4 | Seller/admin product edits overwrote live stock with the form's value, losing stock that open checkouts had reserved. | `Product.quantity` can no longer be written by an entity save. Edits send `expectedQuantity`, and the server applies the difference with an atomic UPDATE (and rejects edits that would go negative). | `Product`, `ProductRepository.adjustStock`, `ProductServiceImpl`, `ProductForm` |
| 5 | Checkout charged the price from when the item was added to the cart. | Price edits reprice open carts immediately, and checkout re-prices every line from the live product. | `CartItemRepository.repriceForProduct`, `CheckoutTransactionExecutor` |
| 6 | Coupon redemption limits could be exceeded by concurrent checkouts (count-then-insert). | Checkout locks the coupon row (`SELECT … FOR UPDATE`) before counting, until the order commits. | `CouponRepository.findByCodeForUpdate` |
| 7 | Cancelling a paid item neither refunded nor restocked it, and there was no returns flow. | A refund flow for cancelled and returned items (pro-rata refund, restock, seller earnings reversed, customer email, automatic retries), plus customer return requests within 7 days. See section 4. | `refund/`, `OrderServiceImpl.requestReturn` |
| 8 | Deleting a product was a hard delete: a 500 for products with orders, and broken carts, wishlists and reviews. | Soft delete (`products.active`). Deleted products are hidden everywhere and removed from open carts; the row stays for order history. | `Product.active`, `ProductRepository`, `ProductServiceImpl.softDelete` |

## 2. Reliability and correctness

| # | Finding | Fix |
|---|---|---|
| 9 | Client mistakes returned 500 (bad `sortBy`, non-numeric `pageNumber`, malformed JSON, `GET /api/auth/user` without a token, wrong HTTP method, missing header). | `MyGlobalExceptionHandler` maps them to 400/401/403/405/409/415. FK and unique violations become 409 without leaking schema details. |
| 10 | Emails were sent even when the triggering transaction rolled back. | **Transactional email outbox**: the email row is written in the same transaction as the change and delivered afterwards by `EmailOutboxSender`. |
| 11 | A full in-memory email queue threw `TaskRejectedException` into the caller (for example payment handling), and queued emails were lost on restart. | The outbox replaces `@Async`/`@Retryable`. Delivery is persistent, uses exponential backoff, and is safe across several instances (`FOR UPDATE SKIP LOCKED`). Spring Retry and AspectJ were removed. |
| 12 | Disabled or deleted users' tokens kept working (`UserDetailsImpl.isEnabled()` always returned true), a deleted user's token caused a 500, and every expired token logged an ERROR with a stack trace. | `isEnabled()` now returns the real flag. The JWT filter ignores tokens of disabled, locked or deleted accounts, catches not-found, and logs expired tokens at DEBUG. |
| 13 | Anyone could review any product. | Reviews require a paid, non-cancelled order containing the product. |
| 14 | An idempotency claim stuck `IN_PROGRESS` (crash) answered 409 forever. Separately, reclaiming a FAILED claim threw `TransactionRequiredException`, because a custom `@Modifying` query ran outside a transaction. | Stale claims are reclaimable after 2 minutes, both queries are transactional, and an unexpected error after the order is created still completes the claim, so a same-key retry can't create a second order. |
| 15 | The refresh/logout endpoints authenticate by cookie (`SameSite=None` in prod) with CSRF off, so they could be triggered cross-site. | Both require an `X-Requested-With: XMLHttpRequest` header (403 without it), and the frontend sends it. |

Also fixed while working through the above:
- **Unverified-account login:** it re-sent the verification email and revealed that the account was unverified *before* checking the password. It now checks the password first.
- **Production startup:** the prod profile could not start. `reviews.rating` is `SMALLINT` in V14 but was mapped as `INTEGER`, and prod runs `ddl-auto=validate`.
- **Cart total:** the cart's "Total" and "Place order" amount were blank, because the frontend read `finalPriceMinorUnits`, which the backend never sent. The cart now returns `shippingMinorUnits` and `finalPriceMinorUnits`.
- **Analytics revenue:** it included unpaid and cancelled orders, and is now net of refunds.
- **Small API fixes:** `GET /api/carts` returned 302 (now 200), and a user with no cart got 400 (now 404).

## 3. Database and performance

- **Missing indexes added (V18):**
  - `refresh_token.token_hash` (unique; looked up on every refresh), plus `family_id` and `user_id`.
  - `orders.email` and `orders.order_status`.
  - `cart_items(cart_id)` and `cart_items(product_id)`.
  - `order_items(order_id)` and `order_items(product_id)`.
  - `products(category_id)` and `products(seller_id)`.
  - `addresses(user_id)`.
  - `payment_attempts(status, created_at)`.
- **Duplicate constraints dropped:** the duplicate unique constraints Hibernate had added on `reviews` and `wishlist_items`.
- **Search:** it now matches name *or* description, backed by `pg_trgm` GIN indexes where the extension is available. The migration degrades gracefully if it isn't.
- **N+1 queries:** `hibernate.default_batch_fetch_size=50` loads lazy associations in batches for paginated lists.
- **Retention:** `DataRetentionJob` (nightly) prunes expired refresh tokens, old idempotency and webhook-dedup records, expired reset and verification tokens, delivered outbox emails, and email logs older than 180 days.
- **Schema fully owned by Flyway:** `V0__legacy_schema.sql` recreates the tables Hibernate used to manage, as they were before V1.
  - Existing databases skip V0 (below their version-0 baseline); empty databases build entirely from migrations.
  - Dev now runs `ddl-auto=validate` like prod.
  - This was verified on an empty database (V0–V20 applied, then entity validation) and on an existing one.

## 4. Marketplace features built

**Refunds and returns** (`refund/`)
- **Cancellations:** a seller or admin cancelling a paid, unshipped item refunds it through Stripe/Razorpay.
- **Returns:** customers request a return within 7 days of delivery (`app.returns.window-days`), with a reason; the seller or admin approves (refund) or rejects.
- **Refund amount:** the item's share of what was actually paid, after any coupon, split pro rata. The last item refunded on an order also covers the shipping fee and any rounding remainder.
- **On refund:** stock is restocked and the seller's earnings are reversed.
- **Reliability:** failed refunds are retried every minute (up to 10 attempts). Stripe refunds carry an idempotency key, and the customer is emailed when the refund succeeds.
- **UI:**
  - Orders board: cancel-and-refund confirmation, and approve/reject return.
  - Customer order page: "Return this item", refund status, refunded total.

**Shipping fee**
- **Rule:** ₹49 per order (`app.shipping.fee-minor-units`), free from ₹499 (`app.shipping.free-threshold-minor-units`) after the coupon, which is what the UI already promised.
- **Where it applies:** charged at checkout, stored on the order (`shippingMinorUnits`), and shown in the cart, on the payment page and on the order page.

**Seller commission and payouts** (`payout/`)
- **Ledger entries:** every paid item books SALE and COMMISSION (10%, `app.marketplace.commission-percent`); refunds book the reversal. Coupons are marketplace-funded and don't reduce what sellers earn.
- **Pending vs. available:** earnings become *available* once the item is settled (delivered and past the return window, or cancelled/returned); until then they are *pending*.
- **Payout recording:** admins transfer the money outside the app, then record the payout, which marks those ledger entries as paid out.
- **Pages:** seller **Payouts** (summary and activity ledger) and admin **Seller payouts** (balances, record payout, history).
- **Backfill:** V20 backfills ledger entries for orders that were already paid, at 10%.

## 5. Engineering quality

- **Tests:**
  - New unit tests for refunds, the seller ledger, shipping, payment transitions/reconciliation and verified-purchase reviews.
  - `PlatformIntegrationTest` (Testcontainers) replaces the old context-load test, which needed a live local database.
  - Frontend: oxlint (`.oxlintrc.json`) and Vitest (`*.test.ts`).
- **CI:** `.github/workflows/ci.yml` runs `./mvnw verify` (including the Testcontainers test) and frontend lint, tests and build on every PR and push to `master`.
- **Docker:**
  - Backend `Dockerfile` (multi-stage, non-root, prod profile configured entirely by environment variables).
  - `frontend/Dockerfile` with nginx, a single origin and an API proxy.
  - `docker-compose.yml` with Postgres, backend, frontend, and Mailpit to catch emails. `.env.example` lists the variables.
  - `.dockerignore` keeps the real `application-*.properties` (with secrets) out of the image.
- **Health checks:** `/actuator/health`, with liveness and readiness probes, is public; details are hidden.
- **Logging:** the dev profile now logs at INFO instead of DEBUG for all of Spring and every SQL statement.
- **Dead code removed:**
  - `WebSecurityConfig` and `WebConfig`.
  - The legacy cookie-JWT login/register/logout path, with its `JwtUtils` helpers and two config properties.
  - The `/api/test/**` rule, empty packages, and commented-out blocks in `SecurityConfig`.
  - The typos `revodeAllForUser` and `isEspired`.
- **API paths:** clean paths were added, and the old ones are kept as deprecated aliases. The frontend uses the new paths.
  - `/api/carts/...` and `/api/carts/coupon` (were `/api/cart/...`).
  - `/api/carts/{id}/products/{pid}`.
  - `/api/admin/products/{id}` and `/api/admin/categories/{id}/products` (were singular).
  - `/api/auth/verify-email` (was `/verif-yemail`).

## 6. Not done: roadmap

- **GST / tax:** needs per-category tax rates and invoice rules, which are a business decision. Tax-inclusive or exclusive prices, an HSN code per category, and invoice numbering would plug into `CheckoutTransactionExecutor` next to `ShippingCalculator`.
- **Product variants (size/colour) and multiple images:** a `product_variants` table with stock per variant (reservations and the cart would then key on variant), and a `product_images` table with an ordering column.
- **SEO:** the storefront is a client-rendered SPA. Prerender or server-render product and category pages, with per-page title/description/OG tags and a sitemap.
- **Still open from `BACKEND_CONTRACT.md`:** first-time OAuth signup, the hardcoded OAuth failure redirect, the public `/api/auth/sellers`, coupon discount not recomputed on quantity change, and the admin status override that bypasses the payment flow.
- **Razorpay refunds** have no idempotency key, because the API offers none.

## 7. Follow-ups for whoever deploys this

- **Migrations V18–V20 run on the next start.**
  - V18 tries `CREATE EXTENSION pg_trgm`. On managed Postgres without that privilege it logs a notice and skips the search indexes.
  - V20 backfills seller earnings at 10%. If production uses a different commission, adjust the backfilled rows.
- **New settings** (all have defaults):
  - `app.shipping.*` (shipping fee and threshold).
  - `app.marketplace.commission-percent`.
  - `app.returns.window-days`.
  - `app.email.outbox.*` (outbox polling).
  - `app.retention.cron` (nightly pruning).
  - `payment.reconciliation.*` (payment polling).
  - `management.*` (Actuator).
- **Changed email retry defaults:** 6 attempts, starting at 30 s, doubling each time.
- **API change for other clients:** `refresh_secure` and `logout_secure` now require the `X-Requested-With: XMLHttpRequest` header.
- **Local cleanup, not done automatically:**
  - The orphan `addresses.is_default` column in older dev databases (now defaults to false).
  - The scratch database `ecommerce_fresh` used for migration testing.
  - `frontend-old/` and `FRONTEND_PLAN.old.md` if they're no longer needed.
