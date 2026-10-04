import { http, pageParams, toPage } from "./client";
import type {
  Address,
  AddressInput,
  AdminUser,
  Analytics,
  AuthResponse,
  Cart,
  Category,
  CheckoutResponse,
  Coupon,
  CouponApplicationResult,
  CouponInput,
  CurrentUser,
  FulfillmentUpdate,
  LinkedAccounts,
  LoginInput,
  Order,
  OrderItem,
  OrderStatus,
  SellerBalance,
  Payout,
  LedgerEntry,
  EarningsSummary,
  Page,
  PageQuery,
  PaymentProviderName,
  Product,
  ProductInput,
  ProductQuery,
  Review,
  ReviewInput,
  ReviewSummary,
  Role,
  SellerApplication,
  SellerApplicationStatus,
  SignupInput,
} from "./types";
import { getErrorMessage, getStatus } from "./errors";

type Msg = { message: string };

// ---------------------------------------------------------------- auth
export const authApi = {
  login: (body: LoginInput) => http.post<AuthResponse>("/api/auth/login", body).then((r) => r.data),
  signup: (body: SignupInput) => http.post<Msg>("/api/auth/signup", body).then((r) => r.data),
  logout: () => http.post("/api/auth/logout_secure", null, { headers: { "X-Requested-With": "XMLHttpRequest" } }),
  me: () => http.get<CurrentUser>("/api/auth/user").then((r) => r.data),
  exchangeOAuthCode: (code: string) =>
    http.post<{ accessToken: string }>("/api/auth/exchange", { code }).then((r) => r.data),
  verifyEmail: (token: string) =>
    http.get<Msg>("/api/auth/verify-email", { params: { token } }).then((r) => r.data),
  forgotPassword: (email: string) => http.post<Msg>("/api/auth/forgot-password", { email }).then((r) => r.data),
  validateResetToken: (token: string) =>
    http
      .get<{ valid: boolean; reason?: string }>("/api/auth/reset-password/validate", { params: { token } })
      .then((r) => r.data)
      .catch((err) => ({ valid: false, reason: getErrorMessage(err, "Invalid reset link.") })),
  resetPassword: (token: string, newPassword: string) =>
    http.post<Msg>("/api/auth/reset-password", { token, newPassword }).then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    http.post<Msg>("/api/auth/change-password", { currentPassword, newPassword }).then((r) => r.data),
  linkedAccounts: () => http.get<LinkedAccounts>("/api/account/linked-accounts").then((r) => r.data),
  unlinkProvider: (provider: string) => http.delete(`/api/account/link/${encodeURIComponent(provider)}`),
};

// ---------------------------------------------------------------- catalog
export const catalogApi = {
  products: (q: ProductQuery = {}) =>
    http
      .get("/api/public/products", {
        params: {
          ...pageParams(q),
          ...(q.keyword ? { keyword: q.keyword } : {}),
          ...(q.category ? { category: q.category } : {}),
        },
      })
      .then((r) => toPage<Product>(r.data)),
  productsByCategory: (categoryId: number, q: PageQuery = {}) =>
    http
      .get(`/api/public/categories/${categoryId}/products`, { params: pageParams(q) })
      .then((r) => toPage<Product>(r.data)),
  product: (id: number) => http.get<Product>(`/api/public/products/${id}`).then((r) => r.data),
  categories: (q: PageQuery = { pageSize: 100, sortBy: "categoryName" }) =>
    http.get("/api/public/categories", { params: pageParams(q) }).then((r) => toPage<Category>(r.data)),
};

// ---------------------------------------------------------------- product management (seller + admin)
export type ProductScope = "seller" | "admin";

export const productAdminApi = {
  list: (scope: ProductScope, q: PageQuery = {}) =>
    http
      .get(scope === "seller" ? "/api/seller/products" : "/api/admin/products", { params: pageParams(q) })
      .then((r) => toPage<Product>(r.data)),
  create: (scope: ProductScope, body: ProductInput) =>
    http
      .post<Product>(
        scope === "seller"
          ? `/api/seller/categories/${body.categoryId}/products`
          : `/api/admin/categories/${body.categoryId}/products`,
        body,
      )
      .then((r) => r.data),
  update: (scope: ProductScope, id: number, body: ProductInput) =>
    http
      .put<Product>(scope === "seller" ? `/api/seller/products/${id}` : `/api/admin/products/${id}`, body)
      .then((r) => r.data),
  remove: (scope: ProductScope, id: number) =>
    http.delete<Product>(scope === "seller" ? `/api/seller/products/${id}` : `/api/admin/products/${id}`),
  uploadImage: (scope: ProductScope, id: number, file: File) => {
    const form = new FormData();
    form.append("Image", file); // backend expects capital "I"
    return http
      .put<Product>(scope === "seller" ? `/api/seller/products/${id}/image` : `/api/admin/products/${id}/image`, form)
      .then((r) => r.data);
  },
  sellerProductsForAdmin: (sellerId: number, q: PageQuery = {}) =>
    http
      .get(`/api/admin/sellers/${sellerId}/products`, { params: pageParams(q) })
      .then((r) => toPage<Product>(r.data)),
};

export const categoryAdminApi = {
  create: (categoryName: string) => http.post<Category>("/api/admin/categories", { categoryName }).then((r) => r.data),
  update: (id: number, categoryName: string) =>
    http.put<Category>(`/api/admin/categories/${id}`, { categoryName }).then((r) => r.data),
  remove: (id: number) => http.delete(`/api/admin/categories/${id}`),
};

// ---------------------------------------------------------------- cart
export const cartApi = {
  /** Returns null when the user has no cart yet (backend answers 404; older builds answered 400). */
  get: async (): Promise<Cart | null> => {
    try {
      const res = await http.get<Cart>("/api/carts/users/cart");
      return res.data;
    } catch (err) {
      if (getStatus(err) === 400 || getStatus(err) === 404) return null;
      throw err;
    }
  },
  add: (productId: number, quantity: number) =>
    http.post<Cart>(`/api/carts/products/${productId}/quantity/${quantity}`).then((r) => r.data),
  increment: (productId: number) =>
    http.put<Cart>(`/api/carts/products/${productId}/quantity/increment`).then((r) => r.data),
  decrement: (productId: number) =>
    http.put<Cart>(`/api/carts/products/${productId}/quantity/decrement`).then((r) => r.data),
  remove: (cartId: number, productId: number) => http.delete<string>(`/api/carts/${cartId}/products/${productId}`),
  clear: () => http.delete<string>("/api/carts/users/cart"),
  applyCoupon: (code: string) =>
    http.post<CouponApplicationResult>(`/api/carts/coupon/${encodeURIComponent(code.trim())}`).then((r) => r.data),
  removeCoupon: () => http.delete("/api/carts/coupon"),
};

// ---------------------------------------------------------------- wishlist
export const wishlistApi = {
  list: () => http.get<Product[]>("/api/wishlist").then((r) => r.data),
  add: (productId: number) => http.post(`/api/wishlist/products/${productId}`),
  remove: (productId: number) => http.delete(`/api/wishlist/products/${productId}`),
};

// ---------------------------------------------------------------- addresses
export const addressApi = {
  list: () => http.get<Address[]>("/api/addresses").then((r) => r.data),
  get: (id: number) => http.get<Address>(`/api/addresses/${id}`).then((r) => r.data),
  create: (body: AddressInput) => http.post<Address>("/api/addresses", body).then((r) => r.data),
  update: (id: number, body: AddressInput) => http.put<Address>(`/api/addresses/${id}`, body).then((r) => r.data),
  remove: (id: number) => http.delete(`/api/addresses/${id}`),
};

// ---------------------------------------------------------------- checkout
export const checkoutApi = {
  checkout: (idempotencyKey: string, addressId: number, provider?: PaymentProviderName) =>
    http
      .post<CheckoutResponse>(
        "/api/checkout",
        { addressId, ...(provider ? { provider } : {}) },
        { headers: { "Idempotency-Key": idempotencyKey } },
      )
      .then((r) => r.data),
  retryPayment: (idempotencyKey: string, orderId: number, provider: PaymentProviderName) =>
    http
      .post<CheckoutResponse>(
        `/api/checkout/${orderId}/retry-payment`,
        { provider },
        { headers: { "Idempotency-Key": idempotencyKey } },
      )
      .then((r) => r.data),
  /** Asks the provider directly whether the order has been paid (covers a slow/missed webhook), then returns the order. */
  syncPayment: (orderId: number) =>
    http.post<Order>(`/api/checkout/${orderId}/sync-payment`).then((r) => r.data),
};

// ---------------------------------------------------------------- payouts
export const payoutApi = {
  summary: () => http.get<EarningsSummary>("/api/seller/payouts/summary").then((r) => r.data),
  ledger: (q: PageQuery = {}) =>
    http.get("/api/seller/payouts/ledger", { params: pageParams(q) }).then((r) => toPage<LedgerEntry>(r.data)),
  mine: (q: PageQuery = {}) => http.get("/api/seller/payouts", { params: pageParams(q) }).then((r) => toPage<Payout>(r.data)),
  balances: () => http.get<SellerBalance[]>("/api/admin/payouts/balances").then((r) => r.data),
  all: (q: PageQuery = {}) => http.get("/api/admin/payouts", { params: pageParams(q) }).then((r) => toPage<Payout>(r.data)),
  pay: (sellerId: number, reference: string) =>
    http.post<Payout>(`/api/admin/sellers/${sellerId}/payouts`, { reference }).then((r) => r.data),
};

// ---------------------------------------------------------------- orders
export const orderApi = {
  mine: (q: PageQuery = {}) => http.get("/api/orders", { params: pageParams(q) }).then((r) => toPage<Order>(r.data)),
  get: (id: number) => http.get<Order>(`/api/orders/${id}`).then((r) => r.data),
  cancel: (id: number) => http.delete<Order>(`/api/orders/${id}`).then((r) => r.data),
  requestReturn: (orderId: number, orderItemId: string, reason: string) =>
    http.post<OrderItem>(`/api/orders/${orderId}/items/${orderItemId}/return`, { reason }).then((r) => r.data),
  sellerOrders: (q: PageQuery = {}) =>
    http.get("/api/seller/orders", { params: pageParams(q) }).then((r) => toPage<Order>(r.data)),
  sellerFulfillment: (orderItemId: string, body: FulfillmentUpdate) =>
    http.put<OrderItem>(`/api/seller/order-items/${orderItemId}/fulfillment`, body).then((r) => r.data),
  adminOrders: (q: PageQuery = {}, status?: OrderStatus) =>
    http
      .get("/api/admin/orders", { params: { ...pageParams(q), ...(status ? { status } : {}) } })
      .then((r) => toPage<Order>(r.data)),
  adminSetStatus: (orderId: number, status: OrderStatus) =>
    http.put<Order>(`/api/admin/orders/${orderId}/status`, { status }).then((r) => r.data),
  adminFulfillment: (orderItemId: string, body: FulfillmentUpdate) =>
    http.put<OrderItem>(`/api/admin/order-items/${orderItemId}/fulfillment`, body).then((r) => r.data),
};

// ---------------------------------------------------------------- reviews
export const reviewApi = {
  forProduct: (productId: number, q: PageQuery = {}) =>
    http
      .get(`/api/public/products/${productId}/reviews`, { params: pageParams(q) })
      .then((r) => toPage<Review>(r.data)),
  summary: (productId: number) =>
    http.get<ReviewSummary>(`/api/public/products/${productId}/reviews/summary`).then((r) => r.data),
  create: (productId: number, body: ReviewInput) =>
    http.post<Review>(`/api/products/${productId}/reviews`, body).then((r) => r.data),
  update: (reviewId: number, body: ReviewInput) => http.put<Review>(`/api/reviews/${reviewId}`, body).then((r) => r.data),
  remove: (reviewId: number) => http.delete(`/api/reviews/${reviewId}`),
  mine: (q: PageQuery = {}) =>
    http.get("/api/users/reviews", { params: pageParams(q) }).then((r) => toPage<Review>(r.data)),
  reply: (reviewId: number, reply: string) =>
    http.put<Review>(`/api/seller/reviews/${reviewId}/reply`, { reply }).then((r) => r.data),
  removeReply: (reviewId: number) => http.delete<Review>(`/api/seller/reviews/${reviewId}/reply`).then((r) => r.data),
  moderate: (reviewId: number, hidden: boolean, reason?: string) =>
    http.put<Review>(`/api/admin/reviews/${reviewId}/moderate`, { hidden, reason }).then((r) => r.data),
};

// ---------------------------------------------------------------- seller onboarding
export const sellerApi = {
  apply: (businessName: string, businessDescription?: string) =>
    http.post<SellerApplication>("/api/seller-applications", { businessName, businessDescription }).then((r) => r.data),
  myApplications: () => http.get<SellerApplication[]>("/api/seller-applications/me").then((r) => r.data),
};

// ---------------------------------------------------------------- admin
export const adminApi = {
  analytics: () => http.get<Analytics>("/api/admin/app/analytics").then((r) => r.data),
  users: (q: PageQuery = {}) =>
    http.get("/api/admin/users", { params: pageParams(q) }).then((r) => toPage<AdminUser>(r.data)),
  grantRole: (userId: number, role: Role) => http.put<AdminUser>(`/api/admin/users/${userId}/roles/${role}`).then((r) => r.data),
  revokeRole: (userId: number, role: Role) =>
    http.delete<AdminUser>(`/api/admin/users/${userId}/roles/${role}`).then((r) => r.data),
  sellerApplications: (status: SellerApplicationStatus, q: PageQuery = {}) =>
    http
      .get("/api/admin/seller-applications", { params: { status, ...pageParams(q) } })
      .then((r) => toPage<SellerApplication>(r.data)),
  approveApplication: (id: number) =>
    http.put<SellerApplication>(`/api/admin/seller-applications/${id}/approve`).then((r) => r.data),
  rejectApplication: (id: number, reason: string) =>
    http.put<SellerApplication>(`/api/admin/seller-applications/${id}/reject`, { reason }).then((r) => r.data),
  coupons: (q: PageQuery = {}) =>
    http.get("/api/admin/coupons", { params: pageParams(q) }).then((r) => toPage<Coupon>(r.data)),
  createCoupon: (body: CouponInput) => http.post<Coupon>("/api/admin/coupons", body).then((r) => r.data),
  updateCoupon: (id: number, body: CouponInput) => http.put<Coupon>(`/api/admin/coupons/${id}`, body).then((r) => r.data),
  deleteCoupon: (id: number) => http.delete(`/api/admin/coupons/${id}`),
};

export type { Page };
