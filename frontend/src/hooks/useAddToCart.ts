import { useLocation, useNavigate } from "react-router";
import { toast } from "sonner";
import type { Product } from "@/api/types";
import { useAuthStore } from "@/store/auth";
import { useCart, useCartActions } from "./queries";

export function useLoginRedirect() {
  const navigate = useNavigate();
  const location = useLocation();
  return () => navigate(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`);
}

export function useAddToCart() {
  const authed = useAuthStore((s) => s.status === "authenticated");
  const goToLogin = useLoginRedirect();
  const { data: cart } = useCart();
  const { add, increment } = useCartActions();

  const cartQuantity = (productId: number) => cart?.products.find((p) => p.productId === productId)?.quantity ?? 0;

  async function addToCart(product: Pick<Product, "productId" | "productName" | "quantity">, quantity = 1) {
    if (!authed) {
      toast.info("Sign in to start your cart");
      goToLogin();
      return false;
    }
    if (product.quantity <= 0) {
      toast.error("This item is out of stock");
      return false;
    }
    try {
      const existing = cartQuantity(product.productId);
      if (existing > 0) {
        // The add endpoint rejects products already in the cart - bump the quantity instead.
        for (let i = 0; i < quantity; i++) await increment.mutateAsync(product.productId);
      } else {
        await add.mutateAsync({ productId: product.productId, quantity });
      }
      toast.success("Added to cart", { description: product.productName });
      return true;
    } catch {
      return false; // useCartActions already surfaced the error
    }
  }

  return { addToCart, cartQuantity, isPending: add.isPending || increment.isPending };
}
