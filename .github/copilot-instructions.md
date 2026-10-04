# E-commerce Project Context

This file is the workspace-level context for GitHub Copilot. Read it together with `CLAUDE.md` and `FRONTEND_PLAN.md` before changing code. `CLAUDE.md` contains deeper historical decisions and transaction-specific notes; `FRONTEND_PLAN.md` is the source of truth for frontend progress and backend contract notes.

## Project Identity

- Repository: multi-vendor e-commerce monorepo.
- Backend: Spring Boot 4.0.0, Java 21, Maven, PostgreSQL, JPA/Hibernate, Flyway.
- Frontend: React 19 + TypeScript + Vite 8 in `frontend/`.
- Base package: `com.ecommerce.project`.
- Maven artifact: `com.ecommerce:project:0.0.1-SNAPSHOT`; application name is `ecom`.
- API and frontend are developed as separate local processes. The backend normally listens on `http://localhost:8080`; the Vite frontend normally listens on a Vite development port.
- The application is an INR-first system. Money is represented as integer minor units, normally paise, plus a currency code.

## Repository Layout

- `src/main/java/com/ecommerce/project/`: backend source.
- `src/main/resources/`: Spring properties, Flyway migrations, Thymeleaf email templates.
- `src/test/java/`: backend tests. Keep automated tests here unless a different test layout is already established.
- `frontend/src/`: React application, feature modules, API types, shared components, and utilities.
- `frontend/public/`: static frontend assets.
- `db/demo-seed.sql`: explicit, idempotent demo data script. It is not a Flyway migration and must not run automatically in production.
- `FRONTEND_PLAN.md`: frontend phase checklist, API contract notes, known backend gaps, and session history.
- `CLAUDE.md`: detailed architecture and historical implementation guidance.
- `.github/copilot-instructions.md`: this file; keep it current when stable project conventions change.
- `target/` and frontend build output are generated artifacts; do not edit them.

## Backend Architecture

The backend is organized partly by feature and partly around older top-level layers. Do not assume every class follows one uniform layout.

Feature/domain packages include:

- `auth`: registration, login, verification, password reset, secure refresh-cookie auth, OAuth account linking.
- `address`, `cart`, `category`, `product`: storefront and customer data.
- `checkout`, `order`, `payment`, `inventory`: the transactional purchase path.
- `seller`, `admin`, `analytics`: seller workflows, administrative operations, and reporting.
- `notification/email`: event-driven email delivery, templates, providers, retries, and audit logs.
- `security`: JWT filtering, OAuth2, route authorization, CORS, and security configuration.
- `config`, `exceptions`, `payload`, `util`: shared infrastructure and cross-cutting concerns.
- `controller` and `model`: older/general-purpose areas that remain in the codebase. Inspect existing ownership before adding a class; prefer the nearest established feature package when extending a feature.

Typical feature classes are colocated or nearby: controller, service interface/implementation, repository, entity/model, and DTO. DTOs that are mapped by ModelMapper may be mutable Lombok classes; new request/response value objects should generally be Java records when the construction path permits.

## Core Checkout Flow

The critical purchase flow is:

`CheckoutController` -> `CheckoutServiceImpl` -> `CheckoutTransactionExecutor` -> payment provider adapter.

1. `CheckoutTransactionExecutor.createOrderWithReservation` runs the database transaction that creates the order, order items, and stock reservations.
2. The transaction commits before any Stripe or Razorpay network call.
3. `PaymentProviderRegistry` selects a provider implementation through the provider-agnostic `PaymentProvider` interface.
4. The provider adapter performs the external call and returns an application-level result.
5. Provider webhooks are verified, converted to a provider-neutral `PaymentEvent`, deduplicated, and published for reconciliation.
6. `OrderPaymentReconciliationListener` moves the order and confirms or releases reservations.

Never hold a database transaction open across external payment I/O. Do not put payment-provider SDK types into business logic; keep Stripe/Razorpay types inside `payment/adapter/stripe` and `payment/adapter/razorpay`.

### Checkout invariants

- Checkout and retry-payment require an `Idempotency-Key` HTTP header.
- A reused key with a different request hash must be rejected as an idempotency conflict.
- `PENDING_PAYMENT` is expected immediately after checkout because final payment state is webhook-driven.
- Normal order transitions are `PENDING_PAYMENT -> PAID`, `PENDING_PAYMENT -> PAYMENT_FAILED`, or cancellation-related transitions.
- `Order.orderStatus` is still stored as a string for schema compatibility; use the `OrderStatus` vocabulary and write/read `.name()` values.
- `order.Payment` and its legacy DTO/repository are not the active payment model. Use `payment.PaymentAttempt` for new payment records.

## Transactions, Persistence, and Concurrency

- `spring.jpa.open-in-view=false` is intentional. Map entities to DTOs while the service transaction is active; do not rely on lazy loading in controllers or response serialization.
- A method that reads a lazy relation must be transactional or use a repository query that fetches the needed data.
- Custom Spring Data `@Modifying` queries need a transactional caller.
- Spring `@Transactional` does not apply to self-invocation. If a transactional method must be called from another method in the same logical service, use a separate injected component, following `CheckoutTransactionExecutor` and `ReservationExpiryTransactionHelper`.
- Inventory reservation uses conditional database updates such as `decrementStockIfAvailable`, not read-then-write stock checks.
- Refresh-token rotation and other one-time state transitions must use atomic conditional updates where concurrent requests are possible.
- Webhook deduplication uses a unique provider/event identifier. A duplicate constraint violation is an expected duplicate signal and must not poison the caller transaction; preserve the existing `REQUIRES_NEW` design.
- Avoid adding `@Data` to JPA entities. Bidirectional relations can recurse through generated `equals`, `hashCode`, or `toString`. Follow the existing explicit-ID equality and excluded-relation patterns.
- Mutable entities generally maintain `createdAt` and `updatedAt` with entity lifecycle callbacks. Static/reference data entities are the exception.

## Money and Inventory Rules

- Product, cart, order, order-item, and payment amounts are integer minor units (`long`/`Long`) with a sibling `currency` string.
- Do not introduce `double`, `Double`, floating-point arithmetic, or currency strings into monetary fields.
- `discount` is a percentage and may remain `double`/`Double`.
- The configured default is `app.currency=INR`; the currency is copied onto records at creation and should travel with the entity.
- Frontend display currently assumes two decimal digits and divides minor units by 100. Use the existing `frontend/src/lib/money.ts` helper rather than duplicating formatting.
- Reservations are created per cart item, expire through `ReservationExpiryJob`, are confirmed after payment success, and are released after failure/cancellation/expiry.

## Security and API Boundaries

- Authentication is stateless JWT access-token authentication plus an httpOnly refresh-token cookie handled by `SecureAuthController`.
- Frontend requests must use Axios `withCredentials: true` so refresh/logout cookies work.
- `AuthTokenFilter` runs before the username/password authentication filter.
- `/api/auth/**`, `/api/public/**`, and `/api/payments/webhooks/**` are public as appropriate; `/api/admin/**` requires `ROLE_ADMIN`; `/api/seller/**` requires `ROLE_ADMIN` or `ROLE_SELLER`; other business endpoints generally require authentication. Verify `SecurityConfig` before assuming a route is public.
- Webhook endpoints are public only because they authenticate through provider signature verification. Never remove signature verification or broaden those routes casually.
- CORS allows credentials and must include the actual frontend development origin. When changing frontend ports, update the backend CORS configuration.
- Do not commit JWT secrets, database passwords, Stripe/Razorpay keys, OAuth secrets, Resend keys, or AWS credentials.

## Database and Migrations

The database uses a hybrid schema strategy:

- In the test/local profile, Hibernate uses `ddl-auto=update` for older schema areas.
- In production, Hibernate uses `ddl-auto=validate`; it never creates missing production columns.
- Flyway is enabled with baseline version 0. Migrations in `src/main/resources/db/migration/` own newer schema changes, beginning with provider health and subsequent payment, inventory, idempotency, seller, audit, and money migrations.
- Any new table or column, including a column on an older Hibernate-managed table, requires a new Flyway migration. Never edit an already-applied migration.
- Hibernate's physical naming strategy converts logical names to snake_case. Verify actual table/column names before writing SQL; annotation names are not always the physical names.
- `db/demo-seed.sql` assumes the application has initialized the schema and roles. It is safe to run intentionally with `psql`, but must not be moved into Flyway.

## Configuration and Local Setup

1. Copy `src/main/resources/application-test.properties.example` to `src/main/resources/application-test.properties`.
2. Fill in local PostgreSQL credentials, `app.jwt.secret`, and provider placeholders. The real file is gitignored.
3. Use the test profile for local development. It activates the no-op email provider by default.
4. Start PostgreSQL and create/use the `ecommerce` database.
5. Start the backend with `mvnw.cmd spring-boot:run`.
6. Start the frontend from `frontend/` with `npm run dev`.
7. For demo users/products, intentionally run `db/demo-seed.sql` after schema initialization.

Useful Windows commands:

```powershell
.\mvnw.cmd test
.\mvnw.cmd test -Dtest=EcomApplicationTests
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run
Set-Location frontend
npm install
npm run build
npm run lint
npm run dev
```

The backend OpenAPI document is available at `http://localhost:8080/v3/api-docs`. After backend DTO or endpoint changes, regenerate frontend types from the running backend:

```powershell
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/api/schema.d.ts
```

Run the backend build/test before reporting a backend change complete. Run `npm run build` and, when relevant, `npm run lint` for frontend changes. The committed Java test suite is currently small, so a passing build is not proof that all checkout, security, or browser flows are covered.

## Frontend Architecture

- Entry and routing live in `frontend/src/App.tsx`.
- Global server state uses TanStack Query; auth/client state uses Zustand.
- `frontend/src/lib/axios.ts` owns the API client, JWT attachment, refresh-on-401 behavior, and refresh single-flight protection.
- `frontend/src/api/schema.d.ts` is generated from OpenAPI; `frontend/src/api/types.ts` contains local type helpers.
- Feature code lives under `frontend/src/features/` and is grouped by backend domain: auth, address, cart, category, checkout, order, payment, inventory, product, seller, admin, analytics, and home.
- Shared shell components are in `frontend/src/components/layout/`; route guards are in `frontend/src/components/routing/`; reusable UI primitives are in `frontend/src/components/ui/`.
- Use the `@` alias for `frontend/src` imports.
- Use `react-hook-form` + `zod` for forms, `sonner` for mutation feedback, existing UI primitives for visual consistency, and query invalidation/optimistic updates according to the neighboring feature pattern.
- Keep URL-driven storefront state in route query parameters where the existing product listing does so.
- Keep protected requests gated by auth state when the existing hook pattern does so; do not create expected unauthenticated request noise on every public page.

### Current frontend routes

- Public: `/`, `/products`, `/products/:productId`, `/login`, `/signup`, `/verify-email`, `/forgot-password`, `/reset-password`.
- Authenticated: `/account`, `/cart`, `/addresses`, `/checkout`, `/orders`, `/orders/:orderId`, `/sell`.
- Seller/admin role-gated: `/seller/*` and `/admin/*`.
- `ProtectedRoute` redirects unauthenticated users; `RoleRoute` enforces `ROLE_SELLER`, `ROLE_ADMIN`, or the relevant configured role set.

## Known Gaps and Constraints

Confirm the current backend before designing around these areas:

- No cart clear-all endpoint.
- No order cancellation endpoint.
- No reviews/ratings, wishlist, or coupon APIs.
- `AddressDTO` has no default-address flag.
- Some address/cart list behavior has historically been unscoped and should be checked before assuming user isolation.
- Cart totals have historically been maintained incrementally and can drift if a mutation path fails; do not silently spread that behavior into new code.
- Product/cart quantity and stock rules should be validated server-side; client-side controls are not an authority.
- The frontend plan records browser/manual checks that are broader than the committed automated tests. Treat those logs as historical evidence, not as a replacement for adding focused regression tests when fixing a bug.

## Change Discipline

- Start at the nearest code that actually decides the behavior, not just a forwarding controller or route.
- Make the smallest change consistent with local patterns and preserve public API shapes unless the task requires a contract change.
- Before changing a backend DTO, entity field, endpoint, or migration-owned schema, inspect its callers, OpenAPI output, and frontend usage.
- For endpoint or DTO changes, regenerate `frontend/src/api/schema.d.ts` and update affected frontend feature code.
- Add focused tests for security, transactions, concurrency, webhook deduplication, money, inventory, or checkout changes; these are high-risk paths.
- Do not edit generated `target/` files or hand-edit generated OpenAPI types.
- Do not commit or alter unrelated user worktree changes.
- Do not use destructive git commands or commit changes unless explicitly requested.
