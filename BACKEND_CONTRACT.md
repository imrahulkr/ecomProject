# Backend Contract — Frontend Integration Reference

Source of truth for how the frontend talks to the Spring Boot backend. Derived from a full read of
the controllers, DTOs, security config, and exception handlers. When a backend DTO or endpoint
changes, update this file **and** `frontend/src/api/types.ts` in the same change.

- Backend base: `http://localhost:8080` (dev). Frontend dev server: `http://localhost:5176`
  (must match `FRONTEND_URL` on the backend — it drives CORS and every link in emails/OAuth redirects).
- In dev the Vite server proxies `/api` and `/images` to `:8080`, so API calls are same-origin.

---

## 1. Conventions

### Money
Every amount is an integer in **minor units** (paise for INR) with a sibling `currency` string.
Display = `minor / 100` formatted with `Intl.NumberFormat`. Never send floats.
`discount` on products/items is a **percentage** (0–100), not money.

### Dates
`Instant` fields → ISO-8601 UTC strings (`2026-10-03T09:12:44.123Z`).
`Order.orderDate` is a `LocalDate` (`2026-10-03`). Fulfillment `shippedAt`/`deliveredAt` are
`LocalDateTime` without zone (`2026-10-03T09:12:44`).

### Pagination
Query params everywhere: `pageNumber` (0-based, default 0), `pageSize` (default 10),
`sortBy` (entity field name), `sortOrder` (`asc` | `desc`, default `asc`).
**Response shapes are inconsistent** — always go through `toPage()` in `src/api/client.ts`:

| Shape | Used by |
|---|---|
| `{content, pageNumber, pageSize, totalElement, totalPages, lastPage}` (note singular `totalElement`) | products, categories, orders |
| `{content, pageNumber, pageSize, totalElements, totalPages, lastPage}` | reviews, coupons, `/api/auth/sellers` |
| Raw Spring `Page` (`{content, number, size, totalElements, totalPages, last, ...}` or `{content, page:{...}}`) | `/api/admin/users`, `/api/admin/seller-applications` |

### Errors
Two envelopes exist; read `message` from either.

```jsonc
// Most errors (validation, not-found, business rules, 401, 403, 500)
{ "timestamp": "...", "status": 400, "error": "VALIDATION_ERROR", "message": "Validation failed",
  "path": "/api/addresses", "errors": { "city": "City name must be atleast 4 characters" } }

// Auth-specific (login/signup/refresh)
{ "error": "BAD_CREDENTIALS", "message": "Invalid email or password.", "timestamp": "..." }
// PASSWORD_SIGNUP_BLOCKED additionally carries "existingProvider": "google"
```

`error` codes: `VALIDATION_ERROR`, `NOT_FOUND`, `BAD_REQUEST`, `MALFORMED_REQUEST` (missing/invalid
JSON body), `CONFLICT` (409 - state conflicts and unique/foreign-key violations), `IDEMPOTENCY_CONFLICT`,
`TOO_MANY_REQUESTS` (429, with a `Retry-After` header in seconds), `PAYLOAD_TOO_LARGE`, `INTERNAL_ERROR`,
`UNAUTHORIZED`, `FORBIDDEN`, `BAD_CREDENTIALS`, `INVALID_REFRESH_TOKEN`, `Email_ALREADY_REGISTERED`
(sic), `PASSWORD_SIGNUP_BLOCKED`, `ACCOUNT_LINKING_REQUIRED`, plus the HTTP status name for Spring
MVC errors (e.g. `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`). Business-rule failures
(`APIException`) are all **400 BAD_REQUEST**. Client mistakes - a bad `sortBy`, a non-numeric
`pageNumber`, malformed JSON, a missing header - are 4xx, never 500.
A few endpoints return plain strings or `{message}` instead (noted below).

### Access control (path-prefix based, `SecurityConfig`)
| Prefix | Who |
|---|---|
| `/api/auth/**`, `/api/public/**` | anyone |
| `/api/admin/**` | `ROLE_ADMIN` |
| `/api/seller/**` | `ROLE_SELLER` or `ROLE_ADMIN` |
| everything else under `/api` | any authenticated user |

Roles: `ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN`. 401 → missing/expired token, or the account was
disabled/deleted (roles and the enabled flag are re-read from the database on every request, so
revoking a role takes effect immediately). 403 → wrong role.

`GET /actuator/health` (and `/actuator/health/liveness|readiness`) is public, for load balancers.

---

## 2. Authentication

Stateless JWT access token (10 min TTL) sent as `Authorization: Bearer <token>`, plus an
**httpOnly refresh cookie** `refreshToken` (path `/api/auth`, 7 days, rotated on every refresh,
reuse of an old one revokes the whole session family). Every request must use
`withCredentials: true` so the cookie travels.

Access token lives **in memory only**. On app start call `refresh_secure`; if it succeeds the user is
logged in. On any 401 from a non-auth endpoint: refresh once (single-flight), retry, else log out.

**`refresh_secure` and `logout_secure` require the header `X-Requested-With: XMLHttpRequest`** (403
without it). They authenticate with the cookie alone, and the header is the CSRF guard: browsers only
send a custom header cross-origin after a CORS preflight that only the allowed frontend origins pass.

**Rate limits** (429 `TOO_MANY_REQUESTS` + `Retry-After`): login 20/15 min per IP; **5 failed logins
lock that account's password login for 15 min** (a successful login resets the count); signup 5/hour
per IP; forgot/reset password 3/15 min per IP and 3/hour per email address.

**Changing or resetting the password ends every session** (all refresh tokens revoked). After
`change-password` succeeds, clear the local session and send the user to log in again.

JWT claims (decode client-side, no verification needed for UI): `sub` (username), `email`, `userId`,
`roles` (e.g. `["ROLE_USER","ROLE_SELLER"]`), `providers`, `enabled`, `exp`.

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/signup` | `{email, password(8–20), username(3–20), name?}` | `{message}` — account is **disabled until email verified** |
| POST | `/api/auth/login` | `{email, password}` (`username` optional/ignored) | `AuthResponse` + sets refresh cookie |
| POST | `/api/auth/refresh_secure` | — (cookie) | `AuthResponse` + rotated cookie; 401 `INVALID_REFRESH_TOKEN` if absent/expired/reused |
| POST | `/api/auth/logout_secure` | — (cookie) | 200, clears cookie |
| GET | `/api/auth/user` | — | `{id, email, username, roles[]}` (roles come from the JWT) |
| GET | `/api/auth/verify-email?token=` | — | `{message}`; 404 bad token, **408** expired. (`/verif-yemail` still works - deprecated alias) |
| POST | `/api/auth/forgot-password` | `{email}` | `{message}` (always 200 unless rate-limited 429) |
| GET | `/api/auth/reset-password/validate?token=` | — | `{valid:true}` or 400 `{valid:false, reason}` |
| POST | `/api/auth/reset-password` | `{token, newPassword(≥8)}` | `{message}`; 400 `{message}`; 429 |
| POST | `/api/auth/change-password` | `{currentPassword, newPassword(≥8)}` | `{message}`; 401 wrong current pw; 400 same pw |
| POST | `/api/auth/exchange` | `{code}` | `{accessToken}` (OAuth one-time code, 30 s TTL, single use) |
| GET | `/api/account/linked-accounts` | — | `{hasPassword, linkedProviders[]}` |
| DELETE | `/api/account/link/{provider}` | — | 204; 409 if it's the last sign-in method |

```ts
AuthResponse = { accessToken: string; expiresInSecond: number;
  user: { id: string; email: string; name: string | null; hasPassword: boolean; linkedProviders: string[] } }
```

Login failure cases (all 401 `BAD_CREDENTIALS`): wrong email/password; social-only account;
**unverified account with the correct password** (message "Account is not verified…", and the backend
re-sends the verification email - with a wrong password it's just "Invalid email or password", so the
endpoint can't be used to probe accounts or spam verification emails). 429 when rate-limited.

### OAuth (Google / GitHub)
1. Full-page navigate to `{BACKEND_ORIGIN}/oauth2/authorization/google|github` (backend origin, not
   the proxy — the provider redirect URI is registered against `:8080`).
2. Success → backend sets the refresh cookie and redirects to `{FRONTEND_URL}/oauth/callback?code=…`.
3. Frontend POSTs the code to `/api/auth/exchange` (exactly once — guard against StrictMode double
   effects), stores the access token, then loads the user.
4. Failure → redirect to `/oauth/error?reason=account_linking_required|oauth_login_failed`.

### Email links into the frontend (routes the SPA must serve)
`/verify-email?token=`, `/reset-password?token=`, `/orders/:orderId`, `/cart`, `/seller/dashboard`,
`/oauth/callback?code=`, `/oauth/error?reason=`.

---

## 3. Catalog

### Types
```ts
ProductDTO = { productId; productName (≥3, unique case-insensitive); description; image;
  quantity /*stock*/; priceMinorUnits; discount /*%*/; specialPriceMinorUnits /*server-computed*/;
  currency; categoryId; sellerId; sellerName /*seller's username*/; createdAt; updatedAt }
CategoryDTO = { categoryId; categoryName (≥5, unique) }
```

`image` is a **full URL** (`{IMAGE_BASE_URL}/{file}`) from list/detail/wishlist endpoints, but a
**bare filename** from create/update/delete/image-upload responses, cart products, and order-item
products. Always pass through `resolveImageUrl()`. New products start as `Default.png`, which may not
exist on disk — always render with an `onError` fallback.

### Endpoints
| Method | Path | Notes |
|---|---|---|
| GET | `/api/public/products?keyword&category&page…` | `keyword` = case-insensitive match on name **or description**; `category` = **exact category name**. Unknown `sortBy` → 400 |
| GET | `/api/public/products/{id}` | |
| GET | `/api/public/categories/{categoryId}/products?page…` | 404 if category missing |
| GET | `/api/public/products/keyword/{keyword}?page…` | legacy; prefer `?keyword=` above |
| GET | `/api/public/categories?page…` | `CategoryResponse` |
| POST | `/api/admin/categories` | `{categoryName}` → 201 |
| PUT | `/api/admin/categories/{id}` | `{categoryName}` |
| DELETE | `/api/admin/categories/{id}` | returns deleted DTO |
| GET | `/api/admin/products?page…` | all products |
| POST | `/api/admin/categories/{categoryId}/products` | body `ProductDTO` (name, description, quantity, priceMinorUnits, discount) → 201. (`.../product` = deprecated alias) |
| PUT | `/api/admin/products/{id}` | body must include `categoryId`; send `expectedQuantity` (see below). (`/admin/product/{id}` = deprecated alias) |
| DELETE | `/api/admin/products/{id}` | **soft delete** (see below). (`/admin/product/{id}` = deprecated alias) |
| PUT | `/api/admin/products/{id}/image` | multipart, field name **`Image`** (capital I), ≤5 MB |
| GET | `/api/admin/sellers/{sellerId}/products?page…` | |
| GET | `/api/seller/products?page…` | caller's own products |
| POST | `/api/seller/categories/{categoryId}/products` | same body as admin create |
| PUT | `/api/seller/products/{id}` | own products only (404 otherwise); `categoryId` required; send `expectedQuantity` |
| DELETE | `/api/seller/products/{id}` | soft delete |
| PUT | `/api/seller/products/{id}/image` | multipart `Image` |
| GET | `/api/auth/sellers?pageNumber` | public list of sellers `{userId, username, email, name}` |

Useful `sortBy` values for products: `productId`, `productName`, `specialPriceMinorUnits`,
`priceMinorUnits`, `discount`, `createdAt`, `quantity`.

**Stock edits** - product updates take `quantity` (the new stock) **and `expectedQuantity`** (the stock
value the edit form was loaded with). The server applies the *difference* atomically, so units
reserved or released by checkouts while the form was open are kept rather than overwritten. A change
that would take stock below zero → 400. Without `expectedQuantity`, `quantity` is applied as an
absolute value (older clients).

**Price edits** reprice every open cart holding the product immediately; checkout also re-prices from
the live product, so the customer always pays the current price.

**Deleting a product hides it** (it disappears from listings, search, product pages, wishlists, and is
removed from open carts) but keeps the row, so past orders and reviews still resolve. Deleted products
can't be added to a cart, wishlisted or reviewed (404).

---

## 4. Cart & coupons (authenticated)

```ts
CartDTO = { cartId; totalPriceMinorUnits /*subtotal*/; currency; appliedCouponCode | null;
  discountMinorUnits; shippingMinorUnits /*delivery fee checkout will add, 0 = free*/;
  finalPriceMinorUnits /*what checkout charges: subtotal - coupon + shipping*/;
  products: ProductDTO[]; createdAt; updatedAt }
```
In `products`, **`quantity` is the quantity in the cart** (not stock) and `image` is a bare filename.
Line price = `specialPriceMinorUnits × quantity`.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/carts/users/cart` | **404 when the user has no cart yet → treat as empty** (older builds answered 400) |
| POST | `/api/carts/products/{productId}/quantity/{qty}` | add; **400 if already in cart** (use increment instead) or insufficient stock |
| PUT | `/api/carts/products/{productId}/quantity/increment` / `decrement` | ±1; reaching 0 removes the line. (`delete` op only decrements — don't use it). `/api/cart/...` = deprecated alias |
| DELETE | `/api/carts/{cartId}/products/{productId}` | remove line; returns a plain string. (`.../product/...` = deprecated alias) |
| DELETE | `/api/carts/users/cart` | clear cart (also drops coupon); plain string |
| POST | `/api/carts` | body `[{productId, quantity}]` — **replaces** the whole cart; plain string. (`/api/cart/create` = deprecated alias) |
| POST | `/api/carts/coupon/{code}` | → `{couponCode, subtotalMinorUnits, discountMinorUnits, finalPriceMinorUnits, currency}` (`/api/cart/coupon/...` = deprecated alias) |
| DELETE | `/api/carts/coupon` | 204 (`/api/cart/coupon` = deprecated alias) |
| GET | `/api/carts` | the caller's cart as a one-element list (200) |

**Shipping**: a flat fee per order (`app.shipping.fee-minor-units`, default ₹49) waived when the
subtotal after coupon reaches `app.shipping.free-threshold-minor-units` (default ₹499). Use
`shippingMinorUnits` / `finalPriceMinorUnits` from the cart - don't compute it client-side.

Coupon caveat: `discountMinorUnits` is computed when the coupon is applied and **not recomputed** on
later quantity changes. After any cart mutation with a coupon applied, re-POST the coupon to refresh
the discount (and remove it if it no longer qualifies). Checkout re-validates anyway and fails with
400 if the coupon became invalid.

---

## 5. Addresses (authenticated, owner-scoped)

```ts
AddressDTO = { addressId; street (5–50); buildingName (≥4); city (≥4); state (≥3); country (≥3);
  pincode (≥6); createdAt; updatedAt }
```
`GET /api/addresses` (or `/api/users/addresses`, identical), `GET /api/addresses/{id}`,
`POST /api/addresses` → 201, `PUT /api/addresses/{id}`, `DELETE /api/addresses/{id}` (plain string).
No default-address flag. Deleting an address referenced by an order fails with **409 CONFLICT**.

---

## 6. Checkout & payment

| Method | Path | Headers | Body |
|---|---|---|---|
| POST | `/api/checkout` | `Idempotency-Key: <uuid>` (required) | `{addressId, provider?: "STRIPE"|"RAZORPAY"}` → 201 |
| POST | `/api/checkout/{orderId}/retry-payment` | `Idempotency-Key` (new uuid) | `{provider}` (required) → 200 |
| POST | `/api/checkout/{orderId}/sync-payment` | — | — → 200 `OrderDTO` (§7); asks the provider whether the order was paid. 404 if not the caller's |

```ts
CheckoutResponse = { orderId; orderStatus; provider: "STRIPE"|"RAZORPAY"; amountMinorUnits; currency;
  clientSecret: string | null;          // Stripe PaymentIntent client secret
  clientPayload: { keyId, razorpayOrderId, amount, currency } | {};  // Razorpay
  paymentAttemptFailed: boolean; failureReason: string | null }
```

Flow:
1. Generate one UUID per checkout attempt; reuse it if the same request is re-sent after a network
   error (a replay returns the original response). Same key + different body → 409 `IDEMPOTENCY_CONFLICT`;
   same key still in flight → 409.
2. Checkout creates the order (`PENDING_PAYMENT`), reserves stock for **10 minutes**, applies the
   coupon. The cart is **not** emptied yet.
3. If `paymentAttemptFailed` → show `failureReason`, offer retry-payment with an explicit provider.
4. Stripe: `stripe.confirmPayment` with Payment Element using `clientSecret`. The publishable key is
   **not** served by the backend → `VITE_STRIPE_PUBLISHABLE_KEY`.
   Razorpay: load `checkout.js`, open with `key=keyId`, `order_id=razorpayOrderId`, `amount`, `currency`.
5. The order becomes `PAID` when the backend learns the payment succeeded: from the provider
   **webhook**, or by asking the provider itself. While waiting, poll
   `POST /api/checkout/{id}/sync-payment` (returns the order; the provider lookup is throttled to
   once per 5 s per order server-side) until the status changes. A scheduled job also checks every
   unsettled payment about once a minute, so a missed webhook never leaves a paid order pending.
   On `PAID` the backend removes the purchased items from the cart and sends the confirmation email.
6. A failed payment leaves the order `PENDING_PAYMENT` so the customer can retry until the
   10-minute reservation expires. When it expires the backend **cancels the order** (`CANCELLED`),
   cancels the pending provider payment and sends the abandoned-cart email; retry-payment then
   fails with 400. `PAYMENT_FAILED` is never written.
7. Retry-payment first settles any earlier attempt that was actually paid (→ 400 "Order is not
   awaiting payment" if so), then cancels the earlier provider payments before creating a new one,
   so the customer can't be charged twice.
8. A payment that succeeds for an order that can no longer use it (already paid by another
   attempt, or cancelled/expired) is **refunded automatically**; failed refunds are retried.
9. Checkout charges **current** prices (cart lines are re-priced from the live product), adds the
   delivery fee (`OrderDTO.shippingMinorUnits`), and fails with 400 if a cart item was deleted
   ("... is no longer available"). Coupon redemption limits are enforced under a row lock, so
   concurrent checkouts can't over-redeem a limited coupon.

---

## 7. Orders

```ts
OrderDTO = { orderId; email; orderItems: OrderItemDTO[]; orderDate /*LocalDate*/; amountMinorUnits /*after discount*/;
  currency; orderStatus: "PENDING_PAYMENT"|"PAID"|"PAYMENT_FAILED"|"CANCELLED"; couponCode; discountMinorUnits;
  payment: null /*legacy, always null*/; addressId; createdAt; updatedAt }
OrderItemDTO = { orderItemId: string /*numeric string*/; product: ProductDTO /*bare image*/; quantity; discount;
  priceMinorUnits; orderedProductPriceMinorUnits /*unit price paid*/; currency;
  fulfillmentStatus: "PENDING"|"SHIPPED"|"DELIVERED"|"CANCELLED"|"RETURN_REQUESTED"|"RETURNED"|"RETURN_REJECTED";
  trackingNumber; carrier; shippedAt; deliveredAt; returnReason; returnRequestedAt;
  refundStatus: "PENDING"|"SUCCEEDED"|"FAILED"|null; refundedMinorUnits }
// OrderDTO also has shippingMinorUnits (delivery fee, included in amountMinorUnits).
```

| Method | Path | Notes |
|---|---|---|
| GET | `/api/orders?page…` | caller's orders (sort by `orderId`/`createdAt`) |
| GET | `/api/orders/{id}` | 404 if not the caller's |
| DELETE | `/api/orders/{id}` | cancel — only `PENDING_PAYMENT`; releases stock and cancels the pending provider payment. 400 if the order turns out to be paid already (checked with the provider first) |
| GET | `/api/seller/orders?page…` | orders containing the seller's items; `orderItems` filtered to the seller's own lines |
| POST | `/api/orders/{orderId}/items/{orderItemId}/return` | customer: `{reason}` (required, ≤1000) → `OrderItemDTO`; only `DELIVERED` items within `app.returns.window-days` (default 7) of delivery |
| PUT | `/api/seller/order-items/{orderItemId}/fulfillment` | `{status, trackingNumber?, carrier?}` |
| GET | `/api/admin/orders?page…&status=` | all orders; optional `status` = `PENDING_PAYMENT`\|`PAID`\|`PAYMENT_FAILED`\|`CANCELLED` (400 if unknown) |
| PUT | `/api/admin/orders/{id}/status` | `{status}` — free-form override, no validation |
| PUT | `/api/admin/order-items/{orderItemId}/fulfillment` | same body as seller, no ownership check |

Fulfillment state machine (order must be `PAID`): `PENDING → SHIPPED` (requires `trackingNumber` and
`carrier`), `SHIPPED → DELIVERED`, `PENDING → CANCELLED`; returns: `DELIVERED → RETURN_REQUESTED`
(customer, endpoint above), then seller/admin `RETURN_REQUESTED → RETURNED` or `→ RETURN_REJECTED`.
Anything else → 400.

**`CANCELLED` and `RETURNED` refund the customer**: the item's stock goes back on sale, the seller's
earnings for it are reversed, and the item's share of what was paid (after the coupon discount,
split pro rata) is refunded through Stripe/Razorpay; the last item refunded on an order also gets the
remaining balance, including shipping. The response's `refundStatus` is `SUCCEEDED`, or `FAILED` if
the provider call failed - it is then retried automatically every minute. The customer is emailed when
the refund succeeds.

---

## 8. Reviews

```ts
ReviewDTO = { reviewId; productId; userId; userName; rating (1–5); comment (≤2000); createdAt; updatedAt;
  sellerReply; sellerRepliedAt; hidden; hiddenReason }
ReviewSummaryDTO = { averageRating; reviewCount }   // hidden reviews excluded
```

| Method | Path | Notes |
|---|---|---|
| GET | `/api/public/products/{id}/reviews?page…` | visible (non-hidden) only; sortBy `reviewId`/`rating`/`createdAt` |
| GET | `/api/public/products/{id}/reviews/summary` | |
| POST | `/api/products/{id}/reviews` | `{rating, comment?}` → 201; 400 if already reviewed, or if the caller hasn't bought the product (a paid order with that item, not cancelled) |
| PUT / DELETE | `/api/reviews/{reviewId}` | author only |
| GET | `/api/users/reviews?page…` | caller's reviews (including hidden ones) |
| PUT | `/api/seller/reviews/{reviewId}/reply` | `{reply}` — only on the seller's own products (404 otherwise) |
| DELETE | `/api/seller/reviews/{reviewId}/reply` | returns updated `ReviewDTO` |
| PUT | `/api/admin/reviews/{reviewId}/moderate` | `{hidden, reason?}` |

There is no "list all reviews" admin endpoint; moderation starts from a product's public reviews, so a
hidden review can only be un-hidden if its id is known.

---

## 9. Wishlist (authenticated)
`GET /api/wishlist` → `ProductDTO[]` (full image URLs). `POST /api/wishlist/products/{id}` → 201
(400 if already there). `DELETE /api/wishlist/products/{id}` → 204 (idempotent).

---

## 10. Seller onboarding
`POST /api/seller-applications` `{businessName (3–255), businessDescription? (≤2000)}` → 201
(400 if already a seller or a PENDING application exists). `GET /api/seller-applications/me` → list.

```ts
SellerApplicationDTO = { id; userId; businessName; businessDescription; status: "PENDING"|"APPROVED"|"REJECTED";
  rejectionReason; appliedAt; decidedAt }
```
After approval, server-side authorization sees `ROLE_SELLER` immediately, but the role only appears
in the JWT / `/api/auth/user` after the next token refresh — call `refresh_secure` to pick it up.

---

## 11. Admin

| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/app/analytics` | all values are **strings**: `productCount, totalRevenue (major units, PAID orders net of item refunds), totalOrders (every status), paidOrders, pendingPaymentOrders, cancelledOrders, reviewCount, averageRating, wishlistItemCount, couponRedemptionCount, couponDiscountTotal (major units)` |
| GET | `/api/admin/payouts/balances` | `SellerBalance[]` - every seller with unpaid earnings |
| GET | `/api/admin/payouts?page…` | payout history, raw Spring Page of `Payout` |
| POST | `/api/admin/sellers/{sellerId}/payouts` | `{reference?}` → 201 `Payout` for everything currently available; 400 if nothing is |
| GET | `/api/admin/users?pageNumber&pageSize` | raw Spring Page of `{userId, username, email, name, enabled, roles[]}` |
| PUT / DELETE | `/api/admin/users/{userId}/roles/{ROLE_X}` | grant / revoke; can't revoke own admin |
| GET | `/api/admin/seller-applications?status=PENDING|APPROVED|REJECTED&pageNumber&pageSize` | raw Spring Page |
| PUT | `/api/admin/seller-applications/{id}/approve` | |
| PUT | `/api/admin/seller-applications/{id}/reject` | `{reason}` (required) |
| GET | `/api/admin/coupons?page…` / `/{id}` | `CouponDTO` |
| POST / PUT | `/api/admin/coupons[/{id}]` | `CouponRequest` |
| DELETE | `/api/admin/coupons/{id}` | 204 |

```ts
CouponRequest = { code; description?; discountType: "PERCENTAGE"|"FIXED_AMOUNT";
  discountPercentage? /*required for PERCENTAGE*/; discountAmountMinorUnits? /*required for FIXED_AMOUNT*/;
  currency? /*required for FIXED_AMOUNT*/; minOrderAmountMinorUnits?; maxRedemptions?; perUserLimit?;
  expiresAt? /*ISO instant*/; active? /*default true*/ }
CouponDTO = CouponRequest fields + { couponId; redemptionCount; createdAt; updatedAt }
```
Codes are upper-cased server-side.

### Seller earnings & payouts
Each paid item books a `SALE` (+price × quantity) and a `COMMISSION` (−`app.marketplace.commission-percent`,
default 10%) to its seller; a refunded item books the reverse (`REFUND`, `COMMISSION_REVERSAL`). Coupon
discounts are marketplace-funded and don't reduce seller earnings. Earnings are **pending** until the
item can no longer change (delivered and past the return window, or cancelled/returned), then
**available**. An admin pays the seller outside the app (bank transfer), then records it here, which
marks those entries as paid out.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/seller/payouts/summary` | `EarningsSummary` for the caller |
| GET | `/api/seller/payouts/ledger?page…` | raw Spring Page of `LedgerEntry`, newest first |
| GET | `/api/seller/payouts?page…` | the caller's payouts |

```ts
EarningsSummary = { currency; commissionPercent; returnWindowDays; pendingMinorUnits; availableMinorUnits; paidOutMinorUnits }
LedgerEntry = { id; entryType: "SALE"|"COMMISSION"|"REFUND"|"COMMISSION_REVERSAL"; amountMinorUnits /*signed*/;
  currency; orderId; orderItemId; paidOut; createdAt }
Payout = { id; sellerId; sellerName; amountMinorUnits; currency; reference; createdAt }
SellerBalance = { sellerId; sellerName; email; currency; pendingMinorUnits; availableMinorUnits; paidOutMinorUnits }
```

---

## 12. Known backend issues (as of this analysis)

Fixed during this analysis:
- **Login account takeover** — `/api/auth/login` checked the password of the *username* sent but
  issued tokens for the *email* sent. Now authenticates the account resolved from the email;
  `username` is no longer required.
- **Payment flow gaps** — payment status no longer depends solely on webhooks (provider
  reconciliation + `sync-payment`); expired reservations now cancel the order and its provider
  payment; cancel/retry cancel earlier provider payments; late or duplicate successful payments are
  refunded. See §6.
- **Codebase audit fixes** (see `docs/codebase-audit.md`): login brute-force protection and lockout;
  sessions revoked on password change/reset; CSRF guard on refresh/logout; disabled accounts rejected
  immediately; stock edits applied as deltas; checkout at current prices; coupon limits enforced under
  a lock; refunds + restock on cancelled/returned items; soft-deleted products; client errors no longer
  500; verified-purchase reviews; transactional email outbox; `GET /api/carts` 302 and no-cart 400 fixed.

Still open (frontend works around where possible):
1. **First-time OAuth sign-up is broken**: `OAuthUserService` builds a new `User` but never saves it
   or assigns `ROLE_USER`, so token generation fails for an email the backend hasn't seen before.
   OAuth only works for emails that already have an account.
2. OAuth failure redirect is hardcoded to `https://frontend.com/oauth/error` (`AppConstants`)
   instead of `{app.frontend-url}/oauth/error`.
3. Cart coupon discount goes stale after quantity changes (see §4).
4. Inconsistent image URLs and pagination field names (see §1, §3).
5. `GET /api/auth/sellers` is public and exposes seller emails.
6. No admin "list all reviews" endpoint; no default-address flag; no seller analytics endpoint.
7. Admin `PUT /api/admin/orders/{id}/status` writes any status directly — setting `CANCELLED` there
   does not release stock or cancel the provider payment (use the customer cancel path for that).
8. Razorpay refunds have no idempotency key (its API offers none), so a refund retried after an
   unrecorded success could in theory be issued twice; Stripe refunds are idempotent.
