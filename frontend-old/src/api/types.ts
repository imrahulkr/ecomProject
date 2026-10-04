import type { components } from './schema'

export type AuthResponse = components['schemas']['AuthResponse']
export type UserSummary = components['schemas']['UserSummary']
export type LoginRequest = components['schemas']['LoginRequest']
export type SignupRequest = components['schemas']['SignupRequest']
export type ProductDTO = components['schemas']['ProductDTO']
export type ProductResponse = components['schemas']['ProductResponse']
export type CategoryDTO = components['schemas']['CategoryDTO']
export type CategoryResponse = components['schemas']['CategoryResponse']
export type CartDTO = components['schemas']['CartDTO']
export type CartItemDTO = components['schemas']['CartItemDTO']
export type AddressDTO = components['schemas']['AddressDTO']
export type CheckoutRequest = components['schemas']['CheckoutRequest']
export type CheckoutResponse = components['schemas']['CheckoutResponse']
export type RetryPaymentRequest = components['schemas']['RetryPaymentRequest']
export type OrderDTO = components['schemas']['OrderDTO']
export type OrderItemDTO = components['schemas']['OrderItemDTO']
export type OrderResponse = components['schemas']['OrderResponse']
export type PaymentDTO = components['schemas']['PaymentDTO']
export type PasswordChangeRequestDTO = components['schemas']['PasswordChangeRequestDTO']
export type ForgotPasswordRequestDTO = components['schemas']['ForgotPasswordRequestDTO']
export type ResetPasswordRequestDTO = components['schemas']['ResetPasswordRequestDTO']
export type SellerApplicationDTO = components['schemas']['SellerApplicationDTO']
export type ApplySellerRequest = components['schemas']['ApplySellerRequest']
export type FulfillmentUpdateDTO = components['schemas']['FulfillmentUpdateDTO']
export type AdminUserSummaryDTO = components['schemas']['AdminUserSummaryDTO']
export type RejectSellerApplicationRequest = components['schemas']['RejectSellerApplicationRequest']
export type OrderStatusUpdateDTO = components['schemas']['OrderStatusUpdateDTO']
export type AnalyticsResponse = components['schemas']['AnalyticsResponse']
export type PageAdminUserSummaryDTO = components['schemas']['PageAdminUserSummaryDTO']
export type PageSellerApplicationDTO = components['schemas']['PageSellerApplicationDTO']

// AccountLinkController returns a hand-built Map, not a generated OpenAPI schema.
export interface LinkedAccounts {
  hasPassword: boolean
  linkedProviders: string[]
}
