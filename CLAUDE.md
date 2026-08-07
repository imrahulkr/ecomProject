# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 4 / Java 21 backend for a multi-vendor ecommerce platform (`com.ecommerce.project`, artifact name `ecom`). PostgreSQL + JPA/Hibernate, Flyway for newer schema changes, JWT + OAuth2 (Google/GitHub) auth, Stripe and Razorpay as pluggable payment providers, Thymeleaf-rendered transactional email, Swagger/OpenAPI docs.

A React + Tailwind frontend is being built in `frontend/` (monorepo-style, same repo). See `FRONTEND_PLAN.md` at repo root for the phase-by-phase plan, locked-in stack decisions, backend contract notes, and progress log — read it before resuming frontend work.

## Commands

```bash
# Build (skip tests for a quick compile check)
./mvnw clean install -DskipTests

# Run the app locally (defaults to the "test" Spring profile)
./mvnw spring-boot:run

# Run the full test suite
./mvnw test

# Run a single test class
./mvnw test -Dtest=EcomApplicationTests

# Run a single test method
./mvnw test -Dtest=EcomApplicationTests#contextLoads
```

Local setup: copy `src/main/resources/application-test.properties.example` to `application-test.properties` (gitignored) and fill in real DB credentials, `app.jwt.secret`, and Stripe/Razorpay/Resend keys. Same pattern for `application-prod.properties.example` on deploy targets. Never commit the non-`.example` files — see `.gitignore`.

Active profile is controlled by `SPRING_PROFILES_ACTIVE` (defaults to `test`, which also activates `NoOpEmailProvider` so local runs never send real email).

## Architecture

### Package layout
Code is organized by feature/domain, not by layer (`auth/`, `cart/`, `checkout/`, `order/`, `payment/`, `inventory/`, `product/`, `category/`, `address/`, `seller/`, `admin/`, `analytics/`, `security/`, `notification/email/`). Each feature package typically holds its entity, repository, service interface + impl, controller, and a `dto/` subpackage. There is no separate `controller/`/`model`/`repository` layer split at the top level.

### Checkout flow (the core transactional path)
`CheckoutController` → `CheckoutServiceImpl` → `CheckoutTransactionExecutor` (order + reservation creation) → `PaymentProviderRegistry` → provider adapter (network call).

- **Idempotency**: every checkout/retry-payment request carries an `Idempotency-Key` header, handled by `IdempotencyService`/`IdempotencyRepository`. A request hash is compared against stored records so replays return the original response and a differing payload under the same key is rejected (`IdempotencyConflictException`).
- **Order creation vs. payment call are deliberately split**: `CheckoutTransactionExecutor.createOrderWithReservation` is `@Transactional` (order + `OrderItem`s + stock reservations commit/rollback atomically), but the actual payment-provider network call in `CheckoutServiceImpl.attemptPayment` runs **outside** any DB transaction — a DB transaction must never sit open across external I/O. This is why the executor was split into its own component (self-invoking `@Transactional` on the same class silently no-ops due to Spring proxy semantics).
- **Order statuses** (`OrderStatus`): `PENDING_PAYMENT → PAID | PAYMENT_FAILED`, or `CANCELLED`. Note `Order.orderStatus` is still a plain `String` column (pre-existing schema) — the enum is just the typed vocabulary new code writes/reads via `.name()`/`valueOf()`.

### Payment providers
`PaymentProvider` interface + `PaymentProviderRegistry` (Spring auto-collects every `PaymentProvider` bean, keyed by `ProviderName`). Adapters live under `payment/adapter/{stripe,razorpay}/` and are the only places vendor SDK types should appear — business logic looks providers up by name.

- `ProviderHealthService` tracks rolling success/failure per provider (`payment.provider.failure-threshold`, `payment.provider.cooldown-minutes`) and `CheckoutServiceImpl.pickHealthiestProvider()` ranks providers when the client doesn't request one explicitly.
- Provider webhooks (`StripeWebhookController`, `RazorpayWebhookController`, under `/api/payments/webhooks/**`, `permitAll` in `SecurityConfig`) verify the signature, map vendor payloads to a provider-agnostic `PaymentEvent`, and hand off to `PaymentEventHandler`.
- `PaymentEventHandler` deduplicates via `WebhookDedupService` (unique constraint on provider+eventId; a caught `DataIntegrityViolationException` means "already processed, ignore") before publishing `PaymentEventOccurredEvent` for downstream listeners (order reconciliation, emails).
- `OrderPaymentReconciliationListener` reacts to payment events to move orders between `PENDING_PAYMENT`/`PAID`/`PAYMENT_FAILED` and confirm/release stock reservations accordingly.
- `order.Payment`/`order.PaymentDTO`/`order.PaymentRepository` are legacy and unused — superseded by `payment.PaymentAttempt` when the provider-agnostic checkout was introduced. `Order.payment` is never set by any code path, so `OrderDTO.payment` is always `null`. Don't build on `order.Payment`; if you need a payment record, use `PaymentAttempt`.

### Money representation
Money fields on `Product`, `CartItem`, `Cart`, `Order`, and `OrderItem` are integer minor units (`long amountMinorUnits`/`priceMinorUnits`/etc., e.g. paise for INR) plus a sibling `currency` (`String`, `VARCHAR(8)`) column — never `double`/`Double`. This mirrors the pattern `PaymentAttempt` already used. `discount` fields are a percentage, not a currency amount, and stay `double`/`Double`. The app-wide default currency comes from `app.currency` (`application.properties`); it's read once at cart/product creation and then carried on the entity itself rather than re-read from config later (e.g. `CheckoutServiceImpl.attemptPayment` reads `order.getCurrency()`, not the config value). When displaying a minor-units value, divide by 100 (see `EmailServiceImpl.sendOrderConfirmation`, `AnalyticsServiceImpl`) — this assumes a 2-decimal-digit currency (true for INR); a true multi-currency system would need per-currency decimal-places lookup.

### Inventory reservations
`InventoryServiceImpl` reserves stock atomically via a conditional `UPDATE ... WHERE stock >= ?` (`decrementStockIfAvailable`), never a read-then-write. Reservations (`StockReservation`, states in `ReservationStatus`) are created per cart item at checkout, confirmed when payment succeeds, released back to stock on failure/cancellation, and expired by a scheduled sweep (`ReservationExpiryJob`, interval/TTL from `inventory.reservation.*` properties) that publishes `OnCartReservationExpiredEvent` for the abandoned-cart email. `ReservationExpiryTransactionHelper` exists for the same reason `CheckoutTransactionExecutor` is split out — self-invoked `@Transactional` doesn't apply.

### Email notifications
Event-driven: domain code publishes typed events (`notification/email/event/*`, e.g. `OnOrderPaidEvent`, `OnPaymentFailedEvent`, `OnSellerApplicationApprovedEvent`) and dedicated `@EventListener`s under `event/listener/` call `EmailServiceImpl`, which renders a Thymeleaf template (`templates/email/*.html`, shared fragments in `templates/email/fragments/`) via `EmailTemplateEngine` and sends through `EmailRetryService` (retry with backoff, `app.email.retry.*`).

- `EmailProvider` implementations (`provider/{noop,resend,smtp}/`) are selected by `app.email.provider` (`EMAIL_PROVIDER` env var); `NoOpEmailProvider` is `@Profile("test")` so local/dev never sends real mail unless explicitly configured.
- Sends are async (`EmailAsyncConfig`, `app.email.async.*`) and logged to `EmailLog`/`EmailLogRepository` with status (`EmailStatus`) for auditing/retry.

### Security
`SecurityConfig` is stateless JWT (`AuthTokenFilter` before `UsernamePasswordAuthenticationFilter`) plus OAuth2 login (Google/GitHub, configured in `application.properties`, real client id/secret via env vars). CSRF is disabled (JWT-in-header isn't CSRF-vulnerable). Route rules of note: `/api/admin/**` requires `ROLE_ADMIN`, `/api/seller/**` requires `ROLE_ADMIN` or `ROLE_SELLER`, `/api/payments/webhooks/**` and `/api/auth/**` are `permitAll`. Roles are `AppRole`: `ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN`.

Account linking for OAuth vs. password signups is handled by `OAuthUserService`/`OAuth2LoginSuccessHandler`/`AccountLinkController`, raising `AccountLinkingRequiredException`/`EmailAlreadyRegisteredException` when a signup method conflicts with an existing account.

### Database / migrations
Hybrid schema management: older tables are still Hibernate `ddl-auto=update`-managed (test profile) / `validate` (prod profile, migrations own the schema), while newer tables (from `provider_health` onward — see `src/main/resources/db/migration/V1__...` onward) are Flyway-owned. `spring.flyway.baseline-on-migrate=true` with `baseline-version=0` means Flyway treats the current Hibernate-managed schema as already up to date, so only new numbered migrations apply. When adding a new table/column — including new columns on an already Hibernate-managed table — add a Flyway migration under `db/migration/` rather than relying on `ddl-auto`; prod runs `ddl-auto=validate`, which never creates columns, only checks they already exist.

Physical table/column names are snake_case even when an entity's `@Table`/`@Column` name is camelCase (e.g. `@Table(name = "verificationToken")` on `UserVerificationToken` physically resolves to `verification_token`) — Spring's default Hibernate physical naming strategy rewrites the logical name. Double-check the real name with `\dt`/`\d <table>` before writing a migration against it rather than assuming the annotation's literal string.

`spring.jpa.open-in-view=false` is deliberate: with it on, a single Hibernate session spans the whole HTTP request, which breaks `PROPAGATION_REQUIRES_NEW` calls (e.g. `WebhookDedupService`'s dedup insert) — a caught constraint violation in a "new" transaction would otherwise poison the caller's transaction because it's silently sharing the same session/connection.

### Recurring gotcha: self-invoked `@Transactional`
Several places (`CheckoutTransactionExecutor`, `ReservationExpiryTransactionHelper`) exist purely because a class cannot call its own `@Transactional` method and have the proxy apply — the transactional logic is split into a separate `@Component`/`@Service` and injected. Keep this pattern in mind before adding `@Transactional` to a private/self-called method; split it out instead.

### Entity and DTO conventions
- **No `@Data` on `@Entity` classes.** Lombok's `@Data`-generated `equals()`/`hashCode()`/`toString()` walk every field, and several entities have bidirectional JPA relations (`User↔Address`, `Order↔Payment`, `Order↔OrderItem`, `Cart↔CartItem`, `Product↔CartItem`) — calling `toString()`/`equals()`/`hashCode()` on either side recurses forever. Entities instead use `@Getter @Setter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on just the `@Id` field, and `@ToString.Exclude` on both sides of every bidirectional relation field.
- **Audit timestamps**: every mutable entity should carry `createdAt`/`updatedAt` (`Instant`, via a `@PrePersist`/`@PreUpdate` `onCreate()`/`onUpdate()` pair — see `Order`, `Product`, `PaymentAttempt` for the pattern). There's no shared `@MappedSuperclass`/`AuditingEntityListener`, so each entity hand-rolls it; static/reference-data entities (`Role`, `Category`) are the deliberate exception. New columns on existing tables need a Flyway migration (see Database section above).
- **DTOs are `record`s wherever the construction path allows it** (request bodies, response wrappers, internal value objects — use `@Builder` on the record if a fluent builder is useful, `@Jacksonized` too if it round-trips through JSON). The exception is DTOs that ModelMapper maps into/out of via reflection (`AddressDTO`, `CategoryDTO`, `ProductDTO`, `CartDTO`, `OrderDTO`, `OrderItemDTO`, `PaymentDTO`, `UserDTO`) — ModelMapper's default strategy needs a no-arg constructor + setters, so those stay `@Data` classes. Records use bare accessors (`dto.email()`, not `dto.getEmail()`) — remember this when wiring a new record DTO into existing call sites.
