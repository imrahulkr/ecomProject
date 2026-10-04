// Mirrors the backend DTOs - see BACKEND_CONTRACT.md. Money is integer minor units.

export type Role = "ROLE_USER" | "ROLE_SELLER" | "ROLE_ADMIN";

export type Page<T> = {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  lastPage: boolean;
};

export type PageQuery = {
  pageNumber?: number;
  pageSize?: number;
  sortBy?: string;
  sortOrder?: "asc" | "desc";
};

// ---- Auth ----
export type AuthResponse = {
  accessToken: string;
  expiresInSecond: number;
  user: {
    id: string;
    email: string;
    name: string | null;
    hasPassword: boolean;
    linkedProviders: string[];
  };
};

export type CurrentUser = {
  id: number;
  email: string;
  username: string;
  roles: Role[];
};

export type LinkedAccounts = { hasPassword: boolean; linkedProviders: string[] };

export type SignupInput = { name?: string; username: string; email: string; password: string };
export type LoginInput = { email: string; password: string };

// ---- Catalog ----
export type Product = {
  productId: number;
  productName: string;
  description: string | null;
  image: string | null;
  quantity: number;
  priceMinorUnits: number;
  discount: number;
  specialPriceMinorUnits: number;
  currency: string;
  categoryId: number | null;
  sellerId: number | null;
  sellerName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ProductInput = {
  productName: string;
  description: string;
  quantity: number;
  /** Updates only: the stock the form was loaded with; the backend applies the difference atomically. */
  expectedQuantity?: number;
  priceMinorUnits: number;
  discount: number;
  categoryId: number;
};

export type Category = { categoryId: number; categoryName: string };

export type ProductQuery = PageQuery & { keyword?: string; category?: string };

// ---- Cart ----
export type CartProduct = Product; // `quantity` = quantity in cart, `image` = bare filename

export type Cart = {
  cartId: number;
  totalPriceMinorUnits: number;
  currency: string;
  appliedCouponCode: string | null;
  discountMinorUnits: number;
  /** Delivery fee checkout will add (0 when the order qualifies for free delivery). */
  shippingMinorUnits: number;
  /** What checkout will charge: items - coupon + shipping. */
  finalPriceMinorUnits: number;
  products: CartProduct[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type CouponApplicationResult = {
  couponCode: string;
  subtotalMinorUnits: number;
  discountMinorUnits: number;
  finalPriceMinorUnits: number;
  currency: string;
};

// ---- Addresses ----
export type Address = {
  addressId: number;
  street: string;
  buildingName: string;
  city: string;
  state: string;
  country: string;
  pincode: string;
  createdAt?: string | null;
  updatedAt?: string | null;
};
export type AddressInput = Omit<Address, "addressId" | "createdAt" | "updatedAt">;

// ---- Checkout ----
export type PaymentProviderName = "STRIPE" | "RAZORPAY";

export type RazorpayClientPayload = {
  keyId: string;
  razorpayOrderId: string;
  amount: number;
  currency: string;
};

export type CheckoutResponse = {
  orderId: number;
  orderStatus: OrderStatus;
  provider: PaymentProviderName;
  amountMinorUnits: number;
  currency: string;
  clientSecret: string | null;
  clientPayload: Partial<RazorpayClientPayload> | null;
  paymentAttemptFailed: boolean;
  failureReason: string | null;
};

// ---- Orders ----
export type OrderStatus = "PENDING_PAYMENT" | "PAID" | "PAYMENT_FAILED" | "CANCELLED";
export type FulfillmentStatus =
  | "PENDING"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "RETURN_REQUESTED"
  | "RETURNED"
  | "RETURN_REJECTED";

export type RefundStatus = "PENDING" | "SUCCEEDED" | "FAILED";

export type OrderItem = {
  orderItemId: string;
  product: Product | null;
  quantity: number;
  discount: number | null;
  priceMinorUnits: number;
  orderedProductPriceMinorUnits: number;
  currency: string;
  fulfillmentStatus: FulfillmentStatus;
  trackingNumber: string | null;
  carrier: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  returnReason: string | null;
  returnRequestedAt: string | null;
  /** Set once the item is cancelled/returned and a refund is issued. */
  refundStatus: RefundStatus | null;
  refundedMinorUnits: number;
};

export type Order = {
  orderId: number;
  email: string;
  orderItems: OrderItem[] | null;
  orderDate: string;
  amountMinorUnits: number;
  currency: string;
  orderStatus: OrderStatus;
  couponCode: string | null;
  discountMinorUnits: number;
  /** Delivery fee included in amountMinorUnits. */
  shippingMinorUnits: number;
  addressId: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type FulfillmentUpdate = {
  status: FulfillmentStatus;
  trackingNumber?: string;
  carrier?: string;
};

// ---- Seller earnings & payouts ----
export type LedgerEntryType = "SALE" | "COMMISSION" | "REFUND" | "COMMISSION_REVERSAL";

export type EarningsSummary = {
  currency: string;
  commissionPercent: number;
  returnWindowDays: number;
  /** Earned on items still inside the return window. */
  pendingMinorUnits: number;
  /** Settled and not yet paid out. */
  availableMinorUnits: number;
  paidOutMinorUnits: number;
};

export type LedgerEntry = {
  id: number;
  entryType: LedgerEntryType;
  amountMinorUnits: number;
  currency: string;
  orderId: number;
  orderItemId: number;
  paidOut: boolean;
  createdAt: string;
};

export type Payout = {
  id: number;
  sellerId: number;
  sellerName: string | null;
  amountMinorUnits: number;
  currency: string;
  reference: string | null;
  createdAt: string;
};

export type SellerBalance = {
  sellerId: number;
  sellerName: string;
  email: string | null;
  currency: string;
  pendingMinorUnits: number;
  availableMinorUnits: number;
  paidOutMinorUnits: number;
};

// ---- Reviews ----
export type Review = {
  reviewId: number;
  productId: number;
  userId: number;
  userName: string | null;
  rating: number;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
  sellerReply: string | null;
  sellerRepliedAt: string | null;
  hidden: boolean;
  hiddenReason: string | null;
};
export type ReviewSummary = { averageRating: number; reviewCount: number };
export type ReviewInput = { rating: number; comment?: string };

// ---- Sellers ----
export type SellerApplicationStatus = "PENDING" | "APPROVED" | "REJECTED";
export type SellerApplication = {
  id: number;
  userId: number;
  businessName: string;
  businessDescription: string | null;
  status: SellerApplicationStatus;
  rejectionReason: string | null;
  appliedAt: string;
  decidedAt: string | null;
};

// ---- Admin ----
export type AdminUser = {
  userId: number;
  username: string;
  email: string;
  name: string | null;
  enabled: boolean;
  roles: Role[];
};

export type Analytics = {
  productCount: string;
  totalRevenue: string;
  totalOrders: string;
  reviewCount: string;
  averageRating: string;
  wishlistItemCount: string;
  couponRedemptionCount: string;
  couponDiscountTotal: string;
  paidOrders: string;
  pendingPaymentOrders: string;
  cancelledOrders: string;
};

export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";

export type CouponInput = {
  code: string;
  description?: string | null;
  discountType: DiscountType;
  discountPercentage?: number | null;
  discountAmountMinorUnits?: number | null;
  currency?: string | null;
  minOrderAmountMinorUnits?: number | null;
  maxRedemptions?: number | null;
  perUserLimit?: number | null;
  expiresAt?: string | null;
  active?: boolean;
};

export type Coupon = Required<Pick<CouponInput, "code" | "discountType">> &
  Omit<CouponInput, "code" | "discountType"> & {
    couponId: number;
    redemptionCount: number;
    active: boolean;
    createdAt: string | null;
    updatedAt: string | null;
  };
