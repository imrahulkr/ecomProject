# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 4 / Java 21 backend for a multi-vendor ecommerce platform (`com.ecommerce.project`, artifact name `ecom`). PostgreSQL + JPA/Hibernate, Flyway-owned schema, JWT + OAuth2 (Google/GitHub) auth, Stripe and Razorpay as pluggable payment providers, Thymeleaf-rendered transactional email, Swagger/OpenAPI docs.

The frontend lives in `frontend/` (React 19 + Vite + TypeScript + Tailwind v4, TanStack Query, React Router, zustand). **Read `BACKEND_CONTRACT.md` at repo root before any frontend or API work** — it documents every endpoint, DTO shape, auth/refresh flow, pagination/error quirks, and known backend issues. Keep it and `frontend/src/api/types.ts` in sync with backend DTO changes. All HTTP calls go through `frontend/src/api/endpoints.ts` (typed) and `frontend/src/api/client.ts` (axios instance with bearer token, single-flight refresh on 401, `toPage()` normaliser). The previous frontend attempt was moved aside to `frontend-old/` and `FRONTEND_PLAN.old.md` — don't build on them.

Frontend commands (from `frontend/`): `npm run dev` (port 5176, proxies `/api` and `/images` to `:8080`), `npm run typecheck`, `npm run lint` (oxlint, `.oxlintrc.json`), `npm test` (Vitest, `*.test.ts` next to the code), `npm run build`. Copy `frontend/.env.example` to `.env.local`; card payments need `VITE_STRIPE_PUBLISHABLE_KEY`.

## Commands

```bash
# Build (skip tests for a quick compile check)
./mvnw clean install -DskipTests

# Run the app locally (defaults to the "test" Spring profile)
./mvnw spring-boot:run

# Run the full test suite
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=PaymentReconciliationServiceTest
./mvnw test -Dtest=RefundTransitionsTest#couponDiscount_isSharedProRata

# Full local stack in Docker (Postgres, backend on the prod profile, nginx frontend, Mailpit)
cp .env.example .env && docker compose up --build
```

Tests: unit tests are plain Mockito (`@ExtendWith(MockitoExtension.class)`). `PlatformIntegrationTest` is the one integration test - Testcontainers Postgres, Flyway from V0, Hibernate `validate`, then the real HTTP API with Stripe as a `@MockitoSpyBean`; it skips itself when Docker isn't running. Keep it a single class (a second Testcontainers class would reuse a cached Spring context pointing at a stopped container). Its config is `src/test/resources/application-it.properties` (dummy values only). CI (`.github/workflows/ci.yml`) runs `./mvnw verify` plus frontend lint/test/build on every PR.

Local setup: copy `src/main/resources/application-test.properties.example` to `application-test.properties` (gitignored) and fill in real DB credentials, `app.jwt.secret`, and Stripe/Razorpay/Resend keys. Same pattern for `application-prod.properties.example` on deploy targets. Never commit the non-`.example` files — see `.gitignore`.

Active profile is controlled by `SPRING_PROFILES_ACTIVE` (defaults to `test`, which also activates `NoOpEmailProvider` so local runs never send real email). The Docker image runs `prod` and ships `application-prod.properties.example` (placeholders only) as its config, so every value comes from environment variables; `.dockerignore` keeps the real `application-*.properties` files out of the build context.

## Architecture

### Package layout
Code is organized by feature/domain, not by layer (`auth/`, `cart/`, `checkout/`, `order/`, `payment/`, `refund/`, `payout/`, `inventory/`, `product/`, `category/`, `address/`, `seller/`, `admin/`, `analytics/`, `security/`, `notification/email/`, `housekeeping/`). Each feature package typically holds its entity, repository, service interface + impl, controller, and a `dto/` subpackage. There is no separate `controller/`/`model`/`repository` layer split at the top level.

### Checkout flow (the core transactional path)
`CheckoutController` → `CheckoutServiceImpl` → `CheckoutTransactionExecutor` (order + reservation creation) → `PaymentProviderRegistry` → provider adapter (network call).

- **Idempotency**: every checkout/retry-payment request carries an `Idempotency-Key` header, handled by `IdempotencyService`/`IdempotencyRepository`. A request hash is compared against stored records so replays return the original response and a differing payload under the same key is rejected (`IdempotencyConflictException`).
- **Order creation vs. payment call are deliberately split**: `CheckoutTransactionExecutor.createOrderWithReservation` is `@Transactional` (order + `OrderItem`s + stock reservations commit/rollback atomically), but the actual payment-provider network call in `CheckoutServiceImpl.attemptPayment` runs **outside** any DB transaction — a DB transaction must never sit open across external I/O. This is why the executor was split into its own component (self-invoking `@Transactional` on the same class silently no-ops due to Spring proxy semantics).
- **Order statuses** (`OrderStatus`): `PENDING_PAYMENT → PAID | CANCELLED` (customer cancel, or the stock hold expiring). A failed payment leaves the order `PENDING_PAYMENT` so the customer can retry until the hold expires — `PAYMENT_FAILED` is never written. Note `Order.orderStatus` is still a plain `String` column (pre-existing schema) — the enum is just the typed vocabulary new code writes/reads via `.name()`/`valueOf()`.
- **Retry-payment** settles any earlier attempt that was actually paid, then cancels the earlier provider payments before creating a new one (one payable payment per order).
- **Pricing at checkout**: the executor re-prices every cart line from the live product (`specialPriceMinorUnits`) before totalling, rejects soft-deleted products, adds shipping (`ShippingCalculator`: `app.shipping.fee-minor-units`, waived from `app.shipping.free-threshold-minor-units`; stored on `Order.shippingMinorUnits` and included in `amountMinorUnits`), and revalidates the coupon under a row lock (`CouponRepository.findByCodeForUpdate`) so redemption limits hold under concurrency. Product price edits also reprice open carts immediately (`CartItemRepository.repriceForProduct`).
- An unexpected exception after the order exists still completes the idempotency record (as a failed payment attempt) so a same-key retry can't create a second order; a claim stuck `IN_PROGRESS` for 2+ minutes (crash) is reclaimable.

### Payment providers
`PaymentProvider` interface + `PaymentProviderRegistry` (Spring auto-collects every `PaymentProvider` bean, keyed by `ProviderName`). Adapters live under `payment/adapter/{stripe,razorpay}/` and are the only places vendor SDK types should appear — business logic looks providers up by name.

- `ProviderHealthService` tracks rolling success/failure per provider (`payment.provider.failure-threshold`, `payment.provider.cooldown-minutes`) and `CheckoutServiceImpl.pickHealthiestProvider()` ranks providers when the client doesn't request one explicitly.
- Provider webhooks (`StripeWebhookController`, `RazorpayWebhookController`, under `/api/payments/webhooks/**`, `permitAll` in `SecurityConfig`) verify the signature, map vendor payloads to a provider-agnostic `PaymentEvent`, and hand off to `PaymentEventHandler`.
- `PaymentEventHandler` deduplicates via `WebhookDedupService` (unique constraint on provider+eventId; a caught `DataIntegrityViolationException` means "already processed, ignore") before publishing `PaymentEventOccurredEvent` for downstream listeners (order reconciliation, emails).
- `OrderPaymentReconciliationListener` hands payment events to `PaymentReconciliationService`, which owns what a payment outcome means for an order however it's learned: from a webhook, or by asking the provider (`PaymentProvider.fetchStatus`) when no webhook came — `PaymentReconciliationJob` polls unsettled attempts (`payment.reconciliation.*`) and the order page polls `POST /api/checkout/{id}/sync-payment`. It also owns ending unpaid orders (customer cancel, hold expiry), which cancels the provider-side payment (`PaymentProvider.cancel`; a no-op for Razorpay, which can't cancel orders).
- **Every order/attempt payment-state change goes through `OrderPaymentTransitions`**, whose methods each lock the order row first (`OrderRepository.findByIdForUpdate`) so webhook, poller, cancel and expiry can't overwrite each other or double-apply side effects. Don't set `orderStatus` on the payment path anywhere else. A success for an order that can no longer use it (already `PAID` by another attempt, or `CANCELLED`) moves the attempt to `REFUND_PENDING` and is refunded automatically; failed refunds are retried every sweep. `PaymentAttemptStatus`: `INITIATED → SUCCEEDED | FAILED | CANCELLED`, plus `REFUND_PENDING → REFUNDED`.
- **Payment success records `PaymentAttempt.providerPaymentId`** - the id a refund must target (Stripe: the PaymentIntent id, same as the reference; Razorpay: the captured `pay_...` id, since its reference is the Razorpay order). Refund requests carry an idempotency key (Stripe honours it).
- `order.Payment`/`order.PaymentDTO`/`order.PaymentRepository` are legacy and unused — superseded by `payment.PaymentAttempt` when the provider-agnostic checkout was introduced. `Order.payment` is never set by any code path, so `OrderDTO.payment` is always `null`. Don't build on `order.Payment`; if you need a payment record, use `PaymentAttempt`.

### Refunds and returns (`refund/`)
Item-level refunds: a seller/admin cancelling a paid item (`PENDING → CANCELLED`) or approving a customer's return (`RETURN_REQUESTED → RETURNED`; customers request via `POST /api/orders/{id}/items/{itemId}/return` within `app.returns.window-days` of delivery). The fulfillment controllers route those two targets to `RefundService` instead of `OrderService` (whose `applyFulfillmentTransition` refuses them). Same split as payments: `RefundTransitions.prepare` locks the order row and, in one transaction, moves the item, restocks (`ProductRepository.incrementStock`), reverses seller earnings and inserts a `Refund` row (`PENDING`); `RefundService` then calls the provider with no transaction open and marks it `SUCCEEDED` (emails the customer) or `FAILED`. `PaymentReconciliationJob` retries `FAILED` and stale `PENDING` refunds (up to 10 attempts). Amount: the item's pro-rata share of what was actually paid (order total minus shipping, i.e. after the coupon); the last item refunded on an order takes the remaining balance including shipping. `OrderItem.refundStatus`/`refundedMinorUnits` mirror the refund for display.

### Seller earnings and payouts (`payout/`)
`seller_ledger_entries` is append-only: `SellerLedgerService.recordSale` (called from `OrderPaymentTransitions.applySuccess`) books `SALE` (+price × qty) and `COMMISSION` (−`app.marketplace.commission-percent`) per item; `recordRefund` books the exact reversal. Coupons are marketplace-funded - they don't reduce seller earnings. Entries are *available* once their item is settled (cancelled/returned, or delivered/return-rejected longer ago than the return window), otherwise *pending*. `payout()` locks the settled unpaid entries, records a `SellerPayout` and stamps `payoutId` on them; the money itself moves outside the app.

### Product catalog rules
- **Soft delete**: `Product.active`. Every catalog-facing lookup uses an `...AndActiveTrue` repository method (and the wishlist query filters inactive products); deleting removes the product from open carts but keeps the row so orders/reviews still resolve. Never call `productRepository.findById` for a customer-facing lookup - use `findByProductIdAndActiveTrue`.
- **Stock is never written through an entity save**: `Product.quantity` is `@Column(updatable = false)`. All changes go through the atomic UPDATEs on `ProductRepository` (`decrementStockIfAvailable`, `incrementStock`, `adjustStock`). Seller/admin edits send `expectedQuantity` and the service applies `quantity - expectedQuantity` with `adjustStock`.
- Reviews require a verified purchase (`OrderItemRepository.hasPurchased`: a `PAID` order with the item, not cancelled).

### Money representation
Money fields on `Product`, `CartItem`, `Cart`, `Order`, and `OrderItem` are integer minor units (`long amountMinorUnits`/`priceMinorUnits`/etc., e.g. paise for INR) plus a sibling `currency` (`String`, `VARCHAR(8)`) column — never `double`/`Double`. This mirrors the pattern `PaymentAttempt` already used. `discount` fields are a percentage, not a currency amount, and stay `double`/`Double`. The app-wide default currency comes from `app.currency` (`application.properties`); it's read once at cart/product creation and then carried on the entity itself rather than re-read from config later (e.g. `CheckoutServiceImpl.attemptPayment` reads `order.getCurrency()`, not the config value). When displaying a minor-units value, divide by 100 (see `EmailServiceImpl.sendOrderConfirmation`, `AnalyticsServiceImpl`) — this assumes a 2-decimal-digit currency (true for INR); a true multi-currency system would need per-currency decimal-places lookup.

### Inventory reservations
`InventoryServiceImpl` reserves stock atomically via a conditional `UPDATE ... WHERE stock >= ?` (`decrementStockIfAvailable`), never a read-then-write. Reservations (`StockReservation`, states in `ReservationStatus`) are created per cart item at checkout, confirmed when payment succeeds, and released back to stock on cancellation. A reservation attached to an order expires *with its order*: `PaymentReconciliationService.expireUnpaidOrders` (run by `PaymentReconciliationJob`) re-checks the payment with the provider, then cancels the order, expires its reservations and cancels the provider payment in one step, so stock is never handed back while the customer can still pay. `ReservationExpiryJob` only sweeps reservations with no order. Both publish `OnCartReservationExpiredEvent` for the abandoned-cart email; TTL from `inventory.reservation.ttl-minutes`. `ReservationExpiryTransactionHelper` exists for the same reason `CheckoutTransactionExecutor` is split out — self-invoked `@Transactional` doesn't apply.

### Email notifications
Event-driven: domain code publishes typed events (`notification/email/event/*`, e.g. `OnOrderPaidEvent`, `OnPaymentFailedEvent`, `OnRefundProcessedEvent`) and dedicated `@EventListener`s under `event/listener/` call `EmailServiceImpl`, which renders a Thymeleaf template (`templates/email/*.html`, shared fragments in `templates/email/fragments/`) via `EmailTemplateEngine` and **enqueues** it in the transactional outbox (`notification/email/outbox/`).

- **Outbox**: `EmailOutbox.enqueue` inserts an `email_outbox` row in the *caller's* transaction, so an email exists only if the change that triggered it committed, and survives restarts. `EmailOutboxSender` (`@Scheduled`, `app.email.outbox.*`) claims due rows with `FOR UPDATE SKIP LOCKED` (safe with several instances), sends outside any transaction, and retries with exponential backoff (`app.email.retry.*`) before marking `FAILED`. Final outcomes are also written to `EmailLog`. There is no `@Async` email path any more - don't add one (it used to send emails for rolled-back transactions and could throw `TaskRejectedException` into payment handling).
- `EmailProvider` implementations (`provider/{noop,resend,smtp}/`) are selected by `app.email.provider` (`EMAIL_PROVIDER` env var); `NoOpEmailProvider` is `@Profile("test")` so local/dev never sends real mail unless explicitly configured. The Docker stack uses `smtp` against Mailpit.

### Security
`SecurityConfig` is stateless JWT (`AuthTokenFilter` before `UsernamePasswordAuthenticationFilter`) plus OAuth2 login (Google/GitHub, configured in `application.properties`, real client id/secret via env vars). CSRF tokens are disabled (JWT-in-header isn't CSRF-vulnerable); the two cookie-authenticated endpoints (`refresh_secure`, `logout_secure`) instead require an `X-Requested-With: XMLHttpRequest` header. `AuthTokenFilter` re-reads the user on every request and ignores tokens of disabled/locked/deleted accounts (`UserDetailsImpl.isEnabled` reflects the real flag). Route rules of note: `/api/admin/**` requires `ROLE_ADMIN`, `/api/seller/**` requires `ROLE_ADMIN` or `ROLE_SELLER`, `/api/payments/webhooks/**` and `/api/auth/**` are `permitAll`. Roles are `AppRole`: `ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN`. `/actuator/health/**` and `/actuator/info` are public.

- **Rate limiting** (`service/RateLimiterService`, Bucket4j buckets in a bounded Caffeine cache, per instance): login per IP + failed logins per account (lockout), signup per IP, password reset per IP and per email. Rejections throw `TooManyRequestsException` → 429 with `Retry-After`. Behind a proxy set `server.forward-headers-strategy=native` so `getRemoteAddr()` is the client.
- **Password change/reset revokes every refresh token** of the user (`RefreshTokenService.revokeAllForUser`).
- `MyGlobalExceptionHandler` maps client mistakes (malformed JSON, type mismatch, unknown sort property, constraint violations, Spring MVC `ErrorResponse` exceptions) to 4xx and FK/unique violations to 409; only genuinely unexpected errors are 500.

Account linking for OAuth vs. password signups is handled by `OAuthUserService`/`OAuth2LoginSuccessHandler`/`AccountLinkController`, raising `AccountLinkingRequiredException`/`EmailAlreadyRegisteredException` when a signup method conflicts with an existing account.

### Database / migrations
**Flyway owns the entire schema; every profile runs `ddl-auto=validate`.** `V0__legacy_schema.sql` recreates the tables Hibernate used to manage, exactly as they stood before V1 (later migrations then add/convert columns on them). Databases created before Flyway were baselined at version 0 (`baseline-on-migrate=true`, `baseline-version=0`), so V0 is "below baseline" there and never runs; an empty database runs V0 and everything after it. Any table/column change needs a new numbered migration under `db/migration/` - validate never creates anything, it only fails startup on a mismatch (map column types exactly, e.g. `@JdbcTypeCode(SqlTypes.SMALLINT)` for a `SMALLINT`). `PlatformIntegrationTest` builds a fresh schema from V0 on every CI run, so a migration that doesn't match the entities fails the build.

Known local drift: databases that were once managed by `ddl-auto=update` can carry leftovers (e.g. an orphan `addresses.is_default` column, now defaulting to false). They're harmless to validate; drop them by hand if you like.

`housekeeping.DataRetentionJob` (`app.retention.cron`, nightly) prunes expired refresh tokens, old idempotency/webhook-dedup records, expired reset/verification tokens, delivered outbox emails and old email logs - see `DataRetentionService` for each window. Lazy associations load with `hibernate.default_batch_fetch_size=50` (fixes N+1 on paginated lists without fetch-join pagination).

Physical table/column names are snake_case even when an entity's `@Table`/`@Column` name is camelCase (e.g. `@Table(name = "verificationToken")` on `UserVerificationToken` physically resolves to `verification_token`) — Spring's default Hibernate physical naming strategy rewrites the logical name. Double-check the real name with `\dt`/`\d <table>` before writing a migration against it rather than assuming the annotation's literal string.

`spring.jpa.open-in-view=false` is deliberate: with it on, a single Hibernate session spans the whole HTTP request, which breaks `PROPAGATION_REQUIRES_NEW` calls (e.g. `WebhookDedupService`'s dedup insert) — a caught constraint violation in a "new" transaction would otherwise poison the caller's transaction because it's silently sharing the same session/connection.

### Recurring gotcha: self-invoked `@Transactional`
Several places (`CheckoutTransactionExecutor`, `ReservationExpiryTransactionHelper`, `OrderPaymentTransitions`, `RefundTransitions`, `EmailOutbox` vs `EmailOutboxSender`) exist purely because a class cannot call its own `@Transactional` method and have the proxy apply — the transactional logic is split into a separate `@Component`/`@Service` and injected. Keep this pattern in mind before adding `@Transactional` to a private/self-called method; split it out instead. Related: custom `@Modifying` repository queries are **not** transactional by themselves - if one is called outside a transaction, annotate it `@Transactional` (see `IdempotencyRepository`).

### Entity and DTO conventions
- **No `@Data` on `@Entity` classes.** Lombok's `@Data`-generated `equals()`/`hashCode()`/`toString()` walk every field, and several entities have bidirectional JPA relations (`User↔Address`, `Order↔Payment`, `Order↔OrderItem`, `Cart↔CartItem`, `Product↔CartItem`) — calling `toString()`/`equals()`/`hashCode()` on either side recurses forever. Entities instead use `@Getter @Setter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on just the `@Id` field, and `@ToString.Exclude` on both sides of every bidirectional relation field.
- **Audit timestamps**: every mutable entity should carry `createdAt`/`updatedAt` (`Instant`, via a `@PrePersist`/`@PreUpdate` `onCreate()`/`onUpdate()` pair — see `Order`, `Product`, `PaymentAttempt` for the pattern). There's no shared `@MappedSuperclass`/`AuditingEntityListener`, so each entity hand-rolls it; static/reference-data entities (`Role`, `Category`) are the deliberate exception. New columns on existing tables need a Flyway migration (see Database section above).
- **DTOs are `record`s wherever the construction path allows it** (request bodies, response wrappers, internal value objects — use `@Builder` on the record if a fluent builder is useful, `@Jacksonized` too if it round-trips through JSON). The exception is DTOs that ModelMapper maps into/out of via reflection (`AddressDTO`, `CategoryDTO`, `ProductDTO`, `CartDTO`, `OrderDTO`, `OrderItemDTO`, `PaymentDTO`, `UserDTO`) — ModelMapper's default strategy needs a no-arg constructor + setters, so those stay `@Data` classes. Records use bare accessors (`dto.email()`, not `dto.getEmail()`) — remember this when wiring a new record DTO into existing call sites.
