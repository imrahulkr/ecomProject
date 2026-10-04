import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'

import { Layout } from '@/components/layout/Layout'
import { ProtectedRoute } from '@/components/routing/ProtectedRoute'
import { RoleRoute } from '@/components/routing/RoleRoute'
import { AdminApplicationsPage } from '@/features/admin/AdminApplicationsPage'
import { AdminCategoriesPage } from '@/features/admin/AdminCategoriesPage'
import { AdminLayout } from '@/features/admin/AdminLayout'
import { AdminOrdersPage } from '@/features/admin/AdminOrdersPage'
import { AdminOverviewPage } from '@/features/admin/AdminOverviewPage'
import { AdminUsersPage } from '@/features/admin/AdminUsersPage'
import { AccountPage } from '@/features/auth/AccountPage'
import { ForgotPasswordPage } from '@/features/auth/ForgotPasswordPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage'
import { SignupPage } from '@/features/auth/SignupPage'
import { VerifyEmailPage } from '@/features/auth/VerifyEmailPage'
import { useBootstrapAuth } from '@/features/auth/useBootstrapAuth'
import { AddressBookPage } from '@/features/address/AddressBookPage'
import { CartPage } from '@/features/cart/CartPage'
import { CheckoutPage } from '@/features/checkout/CheckoutPage'
import { HomePage } from '@/features/home/HomePage'
import { OrderListPage } from '@/features/order/OrderListPage'
import { OrderStatusPage } from '@/features/order/OrderStatusPage'
import { ProductDetailPage } from '@/features/product/ProductDetailPage'
import { ProductListPage } from '@/features/product/ProductListPage'
import { SellerApplyPage } from '@/features/seller/SellerApplyPage'
import { SellerLayout } from '@/features/seller/SellerLayout'
import { SellerOrdersPage } from '@/features/seller/SellerOrdersPage'
import { SellerProductListPage } from '@/features/seller/SellerProductListPage'

const queryClient = new QueryClient()

function AppRoutes() {
  useBootstrapAuth()

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductListPage />} />
        <Route path="/products/:productId" element={<ProductDetailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/account" element={<AccountPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/addresses" element={<AddressBookPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/orders" element={<OrderListPage />} />
          <Route path="/orders/:orderId" element={<OrderStatusPage />} />
          <Route path="/sell" element={<SellerApplyPage />} />

          <Route element={<RoleRoute allowedRoles={['ROLE_SELLER', 'ROLE_ADMIN']} />}>
            <Route path="/seller" element={<SellerLayout />}>
              <Route index element={<SellerProductListPage />} />
              <Route path="products" element={<SellerProductListPage />} />
              <Route path="orders" element={<SellerOrdersPage />} />
            </Route>
          </Route>

          <Route element={<RoleRoute allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminOverviewPage />} />
              <Route path="users" element={<AdminUsersPage />} />
              <Route path="orders" element={<AdminOrdersPage />} />
              <Route path="applications" element={<AdminApplicationsPage />} />
              <Route path="categories" element={<AdminCategoriesPage />} />
            </Route>
          </Route>
        </Route>
      </Route>
    </Routes>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
      <Toaster />
    </QueryClientProvider>
  )
}

export default App
