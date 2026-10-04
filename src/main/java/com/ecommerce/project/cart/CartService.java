package com.ecommerce.project.cart;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.dto.CartDTO;
import com.ecommerce.project.cart.dto.CartItemDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface CartService {
    public List<CartDTO> getAllCarts();
    public CartDTO addProductToCart(Long productId, Integer quantity);

    CartDTO getCart(String emailId, Long cartId);
    @Transactional
    CartDTO updateCartProduct(Long productId, Integer quantity);

    String deleteProductFromCart(Long cartId, Long productId);

    // Caller must have already resolved `cart` through a trusted path (e.g. by the order's own
    // email during payment reconciliation) - unlike deleteProductFromCart(Long, Long), this does
    // not re-check ownership against the currently authenticated user.
    String removeProductFromCart(Cart cart, Long productId);

    String createOrUpdateCartWithItems(List<CartItemDTO> cartItems);

    String clearCart();
}
