import { lazy } from "react";
import { createBrowserRouter, Navigate } from "react-router";
import { RootLayout } from "@/components/layout/RootLayout";
import { AccountLayout, StoreLayout } from "@/components/layout/StoreLayout";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { GuestOnly, RequireAuth, RequireRole } from "@/components/auth/Guards";
import HomePage from "@/pages/store/HomePage";
import ProductsPage from "@/pages/store/ProductsPage";
import ProductDetailPage from "@/pages/store/ProductDetailPage";
import CartPage from "@/pages/store/CartPage";
import CheckoutPage from "@/pages/store/CheckoutPage";
import OrdersPage from "@/pages/account/OrdersPage";
import OrderDetailPage from "@/pages/account/OrderDetailPage";
import AddressesPage from "@/pages/account/AddressesPage";
import WishlistPage from "@/pages/account/WishlistPage";
import MyReviewsPage from "@/pages/account/MyReviewsPage";
import AccountSettingsPage from "@/pages/account/AccountSettingsPage";
import SellerApplyPage from "@/pages/account/SellerApplyPage";
import LoginPage from "@/pages/auth/LoginPage";
import SignupPage from "@/pages/auth/SignupPage";
import { ForgotPasswordPage, ResetPasswordPage } from "@/pages/auth/PasswordPages";
import { OAuthCallbackPage, OAuthErrorPage, VerifyEmailPage } from "@/pages/auth/CallbackPages";
import NotFoundPage, { RouteErrorPage } from "@/pages/NotFoundPage";

// Payment (Stripe SDK) and the seller/admin areas are code-split - most visitors never load them.
const PaymentPage = lazy(() => import("@/pages/store/PaymentPage"));
const seller = () => import("@/pages/seller/SellerPages");
const admin = () => import("@/pages/admin/AdminPages");
const SellerDashboardPage = lazy(() => seller().then((m) => ({ default: m.SellerDashboardPage })));
const SellerProductsPage = lazy(() => seller().then((m) => ({ default: m.SellerProductsPage })));
const SellerProductFormPage = lazy(() => seller().then((m) => ({ default: m.SellerProductFormPage })));
const SellerOrdersPage = lazy(() => seller().then((m) => ({ default: m.SellerOrdersPage })));
const SellerReviewsPage = lazy(() => seller().then((m) => ({ default: m.SellerReviewsPage })));
const payouts = () => import("@/pages/payouts/PayoutsPages");
const SellerPayoutsPage = lazy(() => payouts().then((m) => ({ default: m.SellerPayoutsPage })));
const AdminPayoutsPage = lazy(() => payouts().then((m) => ({ default: m.AdminPayoutsPage })));
const AdminDashboardPage = lazy(() => admin().then((m) => ({ default: m.AdminDashboardPage })));
const AdminProductsPage = lazy(() => admin().then((m) => ({ default: m.AdminProductsPage })));
const AdminProductFormPage = lazy(() => admin().then((m) => ({ default: m.AdminProductFormPage })));
const AdminOrdersPage = lazy(() => admin().then((m) => ({ default: m.AdminOrdersPage })));
const AdminCategoriesPage = lazy(() => admin().then((m) => ({ default: m.AdminCategoriesPage })));
const AdminUsersPage = lazy(() => admin().then((m) => ({ default: m.AdminUsersPage })));
const AdminSellerApplicationsPage = lazy(() => admin().then((m) => ({ default: m.AdminSellerApplicationsPage })));
const AdminCouponsPage = lazy(() => import("@/pages/admin/AdminCouponsPage"));
const AdminReviewsPage = lazy(() => import("@/pages/admin/AdminReviewsPage"));

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <StoreLayout />,
        children: [
          { index: true, element: <HomePage /> },
          { path: "products", element: <ProductsPage /> },
          { path: "products/:productId", element: <ProductDetailPage /> },

          { path: "login", element: <GuestOnly><LoginPage /></GuestOnly> },
          { path: "signup", element: <GuestOnly><SignupPage /></GuestOnly> },
          { path: "forgot-password", element: <ForgotPasswordPage /> },
          { path: "reset-password", element: <ResetPasswordPage /> },
          { path: "verify-email", element: <VerifyEmailPage /> },
          { path: "oauth/callback", element: <OAuthCallbackPage /> },
          { path: "oauth/error", element: <OAuthErrorPage /> },

          {
            element: <RequireAuth />,
            children: [
              { path: "cart", element: <CartPage /> },
              { path: "checkout", element: <CheckoutPage /> },
              { path: "orders/:orderId/pay", element: <PaymentPage /> },
              { path: "wishlist", element: <Navigate to="/account/wishlist" replace /> },
              {
                element: <AccountLayout />,
                children: [
                  { path: "orders", element: <OrdersPage /> },
                  { path: "orders/:orderId", element: <OrderDetailPage /> },
                  { path: "account", element: <AccountSettingsPage /> },
                  { path: "account/addresses", element: <AddressesPage /> },
                  { path: "account/wishlist", element: <WishlistPage /> },
                  { path: "account/reviews", element: <MyReviewsPage /> },
                  { path: "account/seller", element: <SellerApplyPage /> },
                ],
              },
            ],
          },
          { path: "*", element: <NotFoundPage /> },
        ],
      },
      {
        path: "seller",
        element: (
          <RequireRole role="seller">
            <DashboardLayout kind="seller" />
          </RequireRole>
        ),
        children: [
          { index: true, element: <Navigate to="/seller/dashboard" replace /> },
          { path: "dashboard", element: <SellerDashboardPage /> },
          { path: "products", element: <SellerProductsPage /> },
          { path: "products/new", element: <SellerProductFormPage /> },
          { path: "products/:productId/edit", element: <SellerProductFormPage /> },
          { path: "orders", element: <SellerOrdersPage /> },
          { path: "reviews", element: <SellerReviewsPage /> },
          { path: "payouts", element: <SellerPayoutsPage /> },
        ],
      },
      {
        path: "admin",
        element: (
          <RequireRole role="admin">
            <DashboardLayout kind="admin" />
          </RequireRole>
        ),
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: "orders", element: <AdminOrdersPage /> },
          { path: "products", element: <AdminProductsPage /> },
          { path: "products/new", element: <AdminProductFormPage /> },
          { path: "products/:productId/edit", element: <AdminProductFormPage /> },
          { path: "categories", element: <AdminCategoriesPage /> },
          { path: "users", element: <AdminUsersPage /> },
          { path: "seller-applications", element: <AdminSellerApplicationsPage /> },
          { path: "coupons", element: <AdminCouponsPage /> },
          { path: "payouts", element: <AdminPayoutsPage /> },
          { path: "reviews", element: <AdminReviewsPage /> },
        ],
      },
    ],
  },
]);
